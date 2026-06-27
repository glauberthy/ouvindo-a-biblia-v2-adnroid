# Diagnóstico 01 — Saúde do Build + Integridade da Arquitetura

> Auditoria somente-leitura. Escopo: build e arquitetura. **Não** inclui a lógica de playback (passada separada). Nenhuma correção foi aplicada.
> Fonte da verdade: o **código**. Os `.md` do repo foram tratados apenas como intenção original.
> Data: 2026-06-27 · Branch: `analise-media3`

Classificação: **[BLOQUEADOR]** quebra build/test ou perda de dados · **[RISCO]** falha provável em runtime · **[DÍVIDA TÉCNICA]** corrosão estrutural sem falha imediata.

---

## 1. BUILD — resultado bruto

| Comando | Resultado | Detalhe |
|---|---|---|
| `./gradlew assembleDebug` | ✅ **BUILD SUCCESSFUL** (32s) | APK debug gerado (`:app:assembleDebug`). |
| `./gradlew :app:lint` | ✅ **BUILD SUCCESSFUL** (47s) | **0 errors, 100 warnings**. |
| `./gradlew test` | ❌ **BUILD FAILED** (5s) | `:data:remote:compileDebugUnitTestKotlin` não compila. |

**O projeto compila hoje?** O **código de produção sim** — `assembleDebug` passa limpo. A **suíte de testes não roda**: o source set de teste de `:data:remote` nem compila.

### 1a. [BLOQUEADOR] — `./gradlew test` quebra no `:data:remote`

```
e: .../data/remote/src/test/java/.../ExampleUnitTest.kt:3:12 Unresolved reference 'junit'.
e: .../ExampleUnitTest.kt:13:6  Unresolved reference 'Test'.
e: .../ExampleUnitTest.kt:15:9  Unresolved reference 'assertEquals'.
```

- **Causa:** `data/remote/build.gradle.kts` **não declara `testImplementation(libs.junit)`** (bloco `dependencies` não tem nenhuma linha `testImplementation`/`junit`). Os módulos `:data:local` (build.gradle.kts:51) e `:data:repository` (build.gradle.kts:43) declaram. O `:data:remote` ficou de fora.
- **Efeito:** `data/remote/src/test/java/br/app/ide/ouvindoabiblia/data/remote/ExampleUnitTest.kt` referencia JUnit sem tê-lo no classpath → qualquer `./gradlew test` global falha. Como é o task agregador, **nenhum** teste dos outros módulos chega a rodar.
- Observação: os únicos testes existentes são os stubs gerados pelo template (`ExampleUnitTest` com `assertEquals(4, 2 + 2)`); não há cobertura real. O bloqueador é de **build do source set de teste**, não de regressão funcional.

### 1b. Lint — 100 warnings, 0 errors

Distribuição por categoria (de `app/build/reports/lint-results-debug.txt`):

| # | Categoria | Natureza |
|---|---|---|
| 35 | `Typos` | Texto/strings |
| 23 | `GradleDependency` | Dependências desatualizadas |
| 21 | `UnusedResources` | Recursos não usados |
| 11 | `NewerVersionAvailable` | Versões mais novas |
| 3 | `AndroidGradlePluginVersion` | AGP desatualizado |
| 2 | `MonochromeLauncherIcon` | Ícone adaptativo |
| 1 | `OldTargetApi` | `targetSdk = 35` (app/build.gradle.kts:16) |
| 1 | `ObsoleteSdkInt` | `mipmap-anydpi-v26` redundante (minSdk já é 26) |
| 1 | `IconXmlAndPng` / 1 `IconLocation` | Ícones |

Nenhum warning é de correção/segurança; são higiene de dependências, recursos e textos. **[DÍVIDA TÉCNICA]** em bloco, baixa severidade individual.

---

## 2. INTEGRIDADE MODULAR

**Regra declarada:** `app → repository → {local, remote}` (uma direção só).

**Realidade:** a regra **não é respeitada**. O `:app` depende **diretamente** das três camadas:

```
app/build.gradle.kts:48  implementation(project(":data:repository"))
app/build.gradle.kts:49  implementation(project(":data:local"))   ← acesso direto
app/build.gradle.kts:50  implementation(project(":data:remote"))  ← acesso direto
```

### 2a. [DÍVIDA TÉCNICA] Causa-raiz: a interface do repositório expõe tipos das camadas internas

`BibleRepository` (a fronteira pública) devolve entidades Room e DTOs de rede em vez de modelos de domínio. Por isso o `:app` é **obrigado** a depender de `:data:local` e `:data:remote` só para enxergar os tipos de retorno:

```
data/repository/.../BibleRepository.kt:19  fun getBooks(): Flow<List<BookEntity>>          ← entidade Room
data/repository/.../BibleRepository.kt:21  suspend fun getBook(...): BookEntity?           ← entidade Room
data/repository/.../BibleRepository.kt:45  fun getThemes(): Flow<List<ThemeEntity>>        ← entidade Room
data/repository/.../BibleRepository.kt:51  fun getStudies(): Flow<List<StudyEntity>>       ← entidade Room
data/repository/.../BibleRepository.kt:67  fun getMoreContent(): Flow<MoreContentDto?>     ← DTO de REDE
```

Não há camada de domínio: persistência (`*Entity`) e rede (`*Dto`) atravessam o repositório direto para a UI. (Nota: `:data:repository` usa `implementation(project(...))` e não `api(...)`, então esses tipos **não** vazam transitivamente — o que faz o build passar é o `:app` redeclarar as dependências em 2a.)

### 2b. [DÍVIDA TÉCNICA] Vazamento de DTOs de `:data:remote` para dentro da UI

A UI manipula DTOs de rede diretamente (sem mapeamento para modelo de UI):

```
ui/more/MoreViewModel.kt:5                import ...data.remote.dto.MoreContentDto   (e UiState.Success(content: MoreContentDto))
ui/more/MoreSectionDetailsViewModel.kt:6  import ...data.remote.dto.MoreSectionDto
ui/more/MoreScreen.kt:56-65               import ...data.remote.dto.{MoreAssetDto, MoreContactDto, MoreContentDto, MoreLibraryDto, MorePersonDto, MoreRightsContentTypeDto, MoreRightsSourceDto, MoreSectionContentDto, MoreSectionDto, MoreSectionTypeDto}
ui/more/MoreSectionDetailsScreen.kt:54-59 import ...data.remote.dto.{MoreAssetDto, MoreLibraryDto, MorePersonDto, MoreRightsSourceDto, MoreSectionDto, MoreSectionTypeDto}
```

A feature "Mais" inteira renderiza Compose em cima de DTOs de serialização. Mudar o JSON do servidor quebra a UI diretamente.

### 2c. [DÍVIDA TÉCNICA] Vazamento de entidades Room (`:data:local`) para a UI

```
ui/themas/ThemesContract.kt:3            import ...data.local.entity.ThemeEntity   (UiState.Success expõe List<ThemeEntity>)
ui/themas/ThemesScreen.kt:55             import ...data.local.entity.ThemeEntity
ui/themas/ThemeDetailsScreen.kt:54-55    import ...data.local.entity.{MomentEntity, ThemeEntity}
ui/themas/ThemeDetailsViewModel.kt:22    val theme: ...data.local.entity.ThemeEntity   (FQN inline no UiState)
ui/studies/StudiesScreen.kt:59-60        import ...data.local.entity.{StudyEntity, StudyLessonEntity}
ui/studies/StudyDetailsScreen.kt:53-54   import ...data.local.entity.{StudyEntity, StudyLessonEntity}
ui/studies/StudyDetailsViewModel.kt:7    import ...data.local.entity.StudyLessonEntity
ui/player/PlayerViewModel.kt:15, :860    import/usa ...data.local.entity.{ChapterEntity, StudyLessonEntity}
```

Além de `data.local.model.*` (modelos de JOIN do Room: `ChapterWithBookInfo`, `StudyWithLessons`, `MomentWithAudio`, `FavoriteStudyLessonDto`) consumidos amplamente em `Favorites*`, `Player*`, `Studies*`, `Themes*`, `NavigationGraph.kt:14` e até no `service/PlaybackService.kt:27`.

### 2d. Fronteira que **se mantém** (ponto positivo)

Nenhum `import ...data.local.dao` ou `...data.local.database` em `:app` (grep vazio). A UI **não** acessa DAO/`RoomDatabase` direto — toda leitura passa pelo repositório. O vazamento é de **tipos de dados**, não de **mecanismo de acesso**.

---

## 3. REPOSITÓRIO — é o único ponto de orquestração?

**Parcialmente.** A decisão *rede-vs-Room* (o "smart sync") está corretamente no repositório, mas a orquestração de *loading/cache* foi replicada para dentro de cada ViewModel.

### 3a. O que está corretamente no repositório (positivo)

`BibleRepositoryImpl` faz o version-gating: compara `meta.version` remoto contra valor salvo no DataStore e só reescreve o Room se mudou.

```
data/repository/.../BibleRepositoryImpl.kt:43-45  KEY_BIBLE_VERSION / KEY_THEMES_VERSION / KEY_STUDIES_VERSION
data/repository/.../BibleRepositoryImpl.kt:134     syncBibleData(): compara remoteVersion vs localVersion, grava só se diferente (:184)
data/repository/.../BibleRepositoryImpl.kt:198,268,345  idem para themes / studies / more
```

### 3b. [DÍVIDA TÉCNICA] Orquestração de loading vaza, duplicada, para todas as ViewModels

`syncX()` devolve `Result<Unit>` — **sem sinalizar se houve mudança ou se serviu do cache**. Consequência: cada VM reimplementa a mesma dança "checa cache → decide loading → trata falha silenciosa vs visível":

```
ui/home/HomeViewModel.kt:97-129     getBooks().firstOrNull() → if(!hasData) loading=true → syncBibleData() → onFailure: se hasData loga, senão mostra erro
ui/themas/ThemesViewModel.kt:62-86  getThemes().firstOrNull() → if(isNullOrEmpty) loading=true → syncThemes() → mesma lógica de fallback
ui/studies/StudiesViewModel.kt:71   repository.syncStudies() no mesmo padrão
ui/more/MoreViewModel.kt:55         repository.syncMoreContent() no mesmo padrão
```

A regra de negócio "se já tenho cache, falha de sync é silenciosa" está copiada em 4 lugares. É lógica de orquestração que deveria estar atrás da fronteira do repositório (ex.: expor um `Flow<Resource<T>>` com Loading/Success/Error), não reescrita por tela.

---

## 4. RISCOS ESTRUTURAIS CONHECIDOS

### 4a. compileSdk divergente (35 no `:app`, 36 nas libs `:data:*`) — **[RISCO]** baixo

```
app/build.gradle.kts:11         compileSdk = 35
data/local/build.gradle.kts:13  compileSdk = 36
data/remote/build.gradle.kts    compileSdk = 36
data/repository/build.gradle.kts compileSdk = 36
```

Hoje **não quebra** (build passa). Mas as libs compilam contra a API 36 e o app consome os AARs compilando contra 35: se um módulo `:data:*` expuser na API pública um símbolo só existente no 36, o `:app` pode referenciá-lo sem ter sido compilado contra ele — inconsistência latente. Lint também sinaliza `OldTargetApi` em `targetSdk = 35` (app/build.gradle.kts:16). Recomendação de unificação fica para a passada de correção. Não é problema agudo; é incoerência de configuração.

### 4b. `fallbackToDestructiveMigration()` no banco — **[RISCO]** (perda de estado do usuário)

```
data/local/.../di/DatabaseModule.kt:25  .fallbackToDestructiveMigration()
data/local/.../database/BibleDatabase.kt:21  version = 8
```

Qualquer bump de versão do schema **apaga todo o Room**. O conteúdo (livros, temas, estudos) é recuperável via re-sync do servidor — mas **dois tipos de dado são exclusivamente locais e não voltam**:

- **Favoritos** — `toggleFavorite`/`toggleStudyFavorite` gravam flag em Room, sem espelho no servidor (BibleRepository.kt:26, :58).
- **Posição de retomada** — `PlaybackStateEntity` (estado salvo do player) é puramente local.

Ou seja, subir de v8 para v9 zera silenciosamente favoritos e "continuar ouvindo" do usuário. Já está em `version = 8`, então isso provavelmente já ocorreu em campo durante o desenvolvimento.

### 4c. ExoPlayer `@Singleton` injetado no serviço — **[RISCO]** + correção factual da premissa

**Correção da premissa da auditoria:** o ExoPlayer **não** é injetado no caminho da UI. Só o `PlaybackService` o injeta:

```
di/MediaModule.kt:32-35    @Provides @Singleton fun provideExoPlayer(...): ExoPlayer
service/PlaybackService.kt:44-45  @Inject lateinit var player: ExoPlayer
```

A UI (`PlayerViewModel`) fala com a sessão via **`MediaController`/`SessionToken`** (PlayerViewModel.kt:24-25, 59-61), **não** com o ExoPlayer. Logo o acoplamento "serviço E UI no mesmo player" não existe.

O risco real é outro: o ExoPlayer é **`@Singleton` no `SingletonComponent`** (escopo de processo), mas o serviço o **libera** no fim do ciclo:

```
service/PlaybackService.kt (onTaskRemoved)  player.stop(); player.release(); stopSelf()
service/PlaybackService.kt (onDestroy)      player.release()
```

Se o serviço for recriado dentro do mesmo processo, o Hilt reinjeta **a mesma instância já liberada** → uso de `ExoPlayer` released lança `IllegalStateException`. O lifecycle do player (processo) não casa com o lifecycle do serviço (efêmero). *Validação detalhada fica para a passada de playback.*

### 4d. `@UnstableApi` do Media3 — **bem isolado, não é problema**

9 ocorrências, confinadas a **2 arquivos**, ambos na camada de playback:

```
service/PlaybackService.kt
di/MediaModule.kt
```

Nenhum vazamento de API instável para ViewModels, telas ou camada de dados. Contenção correta.

---

## 5. PADRÕES — consistência LCE/MVI

Padrão alvo: `Contract` com `UiState` selado (Loading/Error/Success) + `Intent` selado.

| Tela | UiState LCE selado | Intent selado | Arquivo Contract | Veredito |
|---|---|---|---|---|
| Home | ✅ Loading/Error/Success | ✅ | HomeContract.kt | **No padrão** |
| Themes | ✅ Loading/Error/Success | ✅ | ThemesContract.kt | **No padrão** |
| Studies | ✅ +Empty | ✅ | StudyContract.kt | **No padrão** |
| StudyDetails | ✅ Loading/Error/Success | ✅ (só Retry) | StudyDetailsContract.kt | **No padrão** |
| More | ✅ Loading/Success/Error | ❌ | inline no VM (sem Contract) | **Parcial** |
| MoreSectionDetails | ✅ Loading/Success/Error | ❌ | inline no VM (sem Contract) | **Parcial** |
| Favorites | ❌ | ❌ | — | **Fora do padrão** |
| Chapters | ❌ | ❌ | — | **Fora do padrão** |
| Player | ❌ | ❌ | — | **Fora do padrão** |

### 5a. [DÍVIDA TÉCNICA] Telas fora do padrão (evidência)

- **Favorites** — `data class FavoritesUiState(... isLoading: Boolean = true)` plano, sem estados Error/Empty selados e sem Intent. Estado montado por `combine(...)` direto.
  `ui/favorites/FavoritesViewModel.kt:17-21` (data class), `:30` (combine).
- **Chapters** — sem wrapper de UiState; expõe a lista crua e não tem Loading/Error nem Intent:
  `ui/chapters/ChaptersViewModel.kt:36  val chapters: StateFlow<List<ChapterUiModel>>`.
- **Player** — `data class PlayerUiState` plano (`ui/player/PlayerUiState.kt:5`), dirigido por chamadas de método diretas no VM, não por `Intent`. (Parcialmente esperado para um player, mas diverge do contrato das demais.)
- **More / MoreSectionDetails** — têm o `UiState` LCE mas **sem `Intent`** e **sem arquivo `Contract`** (definidos inline no VM): `ui/more/MoreViewModel.kt:16-19`, `ui/more/MoreSectionDetailsViewModel.kt:16-19`. Note ainda o arquivo com nome quebrado `ui/more/MoreSectionDetailsRoute.k.kt` (`.k.kt`).

Resumo: das 9 telas, **4 seguem** o padrão completo, **2 parciais** (sem Intent/Contract) e **3 fora**.

---

## Próxima passada (playback) — 3 pontos prioritários

1. **Lifecycle do ExoPlayer `@Singleton` × `release()` no serviço** (MediaModule.kt:32 + PlaybackService onTaskRemoved/onDestroy). Quero confirmar se a recriação do serviço reinjeta um player já liberado e se isso gera crash de "player released" — é o risco de runtime mais concreto que vi.

2. **Contrato de `mediaId` codificado em string, espalhado** — convenções `study_{id}_{lesson}`, `moment_*` e `{bookId}|{index}` são parseadas em vários pontos de `PlaybackService.kt` (`buildPlaylistFromState`, `onSetMediaItems`, `saveCurrentState`) e replicadas em `PlayerViewModel.kt` (`currentSourceType`). Quero mapear todos os locais de parse/format e achar onde um id malformado escapa sem validação.

3. **Restauração de sessão × `shouldBlockDatabaseResumption` (janela de 5s)** — interação entre `restoreLastSession()`, `onPlaybackResumption()` e `markExplicitPlaybackRequest()`. Quero investigar a corrida entre restaurar estado salvo do Room e um play explícito da UI, e por que momentos de tema são deliberadamente excluídos do save (`saveCurrentState` ignora `moment_`).
