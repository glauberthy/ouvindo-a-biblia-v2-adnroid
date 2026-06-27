# Roadmap de Continuidade — Ouvindo a Bíblia

> Derivado dos achados reais de `DIAGNOSTICO_01_ARQUITETURA.md` e `DIAGNOSTICO_02_PLAYER.md`. O **código é a fonte da verdade**; os `.md` antigos (README, REFACTOR_MEDIA3_TODO, PLANO_IMPLEMENTACAO_MELHORIAS_UAMP, ANALISE_MODULARIZACAO_PLAYER) entram só como intenção e foram conferidos contra o código.
> Data: 2026-06-27 · Branch: `analise-media3`
> Esforço: **P** (horas) · **M** (1–3 dias) · **G** (semana+). Estimativa, não medida.

## ✅ Já resolvido (não é pendência — registro)
**Bug CRÍTICO 5.1 + ciclo de vida (Diag 02 §5.1/§5.2/§5.3/§5.4):** ExoPlayer deixou de ser `@Singleton` e passou a pertencer ao serviço (release **único** em `onDestroy`); o serviço virou *started + foreground* via `startForegroundService` nos pontos de play (`PlayerViewModel.ensureServiceStarted`) → **sobrevive à morte da Activity**; `onTaskRemoved` (opção A) só salva estado; threadpool do `CoilBitmapLoader` agora tem `shutdown()`. **Verificado em device (moto g53, Media3 1.9.2):** tocando + background/swipe continua; reabrir + play funciona sem "Player is released".

**FASE 0 fechada:**
- **0.1** — `testImplementation(libs.junit)` no `:data:remote`; `./gradlew test` passa globalmente. ✅
- **0.2** — migração destrutiva → `MIGRATION_8_9` (sem perda de favoritos/retomada) + teste instrumentado de preservação, validado em device. ✅
- **0.3** — teste instrumentado `PlaybackServiceLifecycleTest` (serviço sobrevive ao unbind, player não liberado); `connectedAndroidTest` passa. ✅ **Limitação:** o teste inicia o serviço via `startService` (mesma garantia de sobrevivência de um serviço *started*), evitando o contrato de 5s do foreground e a necessidade de áudio real (player de produção é só-HTTP); valida a propriedade de sobrevivência ao unbind, **não** o gatilho `ensureServiceStarted` dentro do `PlayerViewModel`.

---

## BLOCO 0 — Estabilização (build / impede uso ou rotina de dev) — ✅ CONCLUÍDO

> Todos os itens da Fase 0 foram resolvidos e validados em device (ver "Já resolvido" acima). Mantidos abaixo como registro.

### 0.1 — ✅ [BLOQUEADOR] `:data:remote` quebra `./gradlew test`
- **O quê / porquê:** `data/remote/build.gradle.kts` **não tem `testImplementation(libs.junit)`** (confirmado: 0 ocorrências), mas há um `ExampleUnitTest` usando JUnit → `./gradlew test` falha globalmente e **nenhum** teste do projeto roda (Diag 01 §1a).
- **Arquivos:** `data/remote/build.gradle.kts` (e o stub `data/remote/src/test/.../ExampleUnitTest.kt`).
- **Esforço:** P · **Depende de:** nada.

### 0.2 — ✅ [ALTO/UX] `fallbackToDestructiveMigration` apaga favoritos e retomada
- **O quê / porquê:** `DatabaseModule.kt:29` usa `fallbackToDestructiveMigration()` e o schema já está em `version = 8` (`BibleDatabase.kt:26`). Todo bump de versão **zera o Room**. Favoritos (`toggleFavorite`/`toggleStudyFavorite`) e a posição de "continuar ouvindo" (`PlaybackStateEntity`) são os **únicos dados só-locais** — não voltam por sync (Diag 01 §4b). Impacto de UX **alto**: o usuário perde favoritos e progresso silenciosamente a cada atualização de schema.
- **Como resolver:** escrever `Migration` reais a partir da v8 (ou, no mínimo, exportar/restaurar favoritos + último estado de playback antes da recriação).
- **Arquivos:** `data/local/.../di/DatabaseModule.kt`, `data/local/.../database/BibleDatabase.kt`, DAOs/migrations.
- **Esforço:** M · **Depende de:** nada. **Fazer antes de qualquer mudança de schema** (vários itens do Bloco 2 mexem em entidades).

### 0.3 — ✅ [Qualidade] Teste instrumentado do ciclo de vida do player
- **O quê / porquê:** cobertura ~zero (só `ExampleUnitTest` template). O 5.1 teve que ser validado **manualmente** em device; sem teste, regride sem aviso. Criar teste instrumentado que cubra: tocar → background/Activity destruída → serviço sobrevive → reabrir + play funciona (sem player liberado).
- **Arquivos:** novo `app/src/androidTest/.../PlaybackServiceLifecycleTest.kt`.
- **Esforço:** M · **Depende de:** 0.1 (suíte de testes precisa compilar primeiro).

---

## BLOCO 1 — Bugs de playback restantes (Diag 02, com 5.1–5.4 já feitos)

### 1.1 — [ALTO] Cast: posição não volta Cast→local ao desconectar
- **O quê / porquê:** ao sair do Chromecast, o celular retoma da posição **antiga** (de quando começou a castar), perdendo o progresso ouvido na TV (Diag 02 §6.1). `onSessionEnded` só chama `syncStateWithController`, nunca lê `remoteMediaClient.approximateStreamPosition` para dar `seekTo` no local.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`sessionManagerListener.onSessionEnded`, `loadMediaOnCast`).
- **Esforço:** M · **Depende de:** nada.

### 1.2 — [ALTO] Cast: sem fila → auto-avanço quebrado na TV
- **O quê / porquê:** `loadMediaOnCast` envia só a faixa atual; ao terminar, a TV para e `checkCastCompletion` chama `skipToNextChapter()` no controller **local**, não no Cast (Diag 02 §6.2). Reprodução contínua na TV não funciona.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`loadMediaOnCast`, `checkCastCompletion`, `skipToNextChapter`).
- **Esforço:** M · **Depende de:** 1.1 (mesma área; fazer junto).

### 1.3 — [ALTO-latente] Posição relativa ao clipping restaurada como absoluta
- **O quê / porquê:** com recorte (`ClippingConfiguration`), `currentPosition` é relativo ao clip, mas o restore aplica `state.positionMs` como absoluto (Diag 02 §3b). Latente hoje (a navegação chama `playBook` com `startMs=0`), vira ativo no instante em que recorte de Bíblia for usado.
- **Arquivos:** `service/PlaybackService.kt` (`buildPlaylistFromState`/`saveCurrentState`), `ui/player/PlayerViewModel.kt` (`buildBibleMediaItems`).
- **Esforço:** M · **Depende de:** 2.5 (unificação do `mediaId`/mapper).

### 1.4 — [MÉDIO] Sem save periódico → perde posição se morto no meio da faixa
- **O quê / porquê:** só há save em transição e em pause; processo morto no meio de uma faixa retoma do início do capítulo (Diag 02 §3d). Adicionar save por intervalo (10–15s) enquanto toca.
- **Arquivos:** `service/PlaybackService.kt` (loop em `serviceScope`).
- **Esforço:** M · **Depende de:** nada (o ciclo de vida do player do 5.1 já está estável).

### 1.5 — [MÉDIO] `extractChaptersFromPlayer` assume Bíblia (id=0 p/ estudo)
- **O quê / porquê:** trata todo `mediaId` como numérico → em Estudo, `id=0` e número de capítulo sintético; degrada a folha de capítulos no modo Estudo (Diag 02 §1c).
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`extractChaptersFromPlayer`).
- **Esforço:** P–M · **Depende de:** idealmente 2.5 (parser único).

### 1.6 — [MÉDIO] Cast: metadados errados p/ Estudo/Tema
- **O quê / porquê:** `loadMediaOnCast` força `"Capítulo N"` e `contentType="audio/ogg"`, errado para Estudo/Tema (Diag 02 §6.3).
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`loadMediaOnCast`).
- **Esforço:** P · **Depende de:** 1.5 (lista de capítulos correta).

### 1.7 — [MÉDIO] Alvo de controle divergente na transição de sessão Cast
- **O quê / porquê:** play/seek escolhem alvo por `castSession?.isConnected`; durante `onSessionStarting`/`onSessionResuming` (vazios) há janela em que o comando vai pro alvo errado (Diag 02 §6.4).
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`togglePlayPause`, `seekTo`, `sessionManagerListener`).
- **Esforço:** P · **Depende de:** nada.

### 1.8 — [MÉDIO] Duração 0 no buffering → barra cheia no cold start
- **O quê / porquê:** save antes da duração conhecida grava `duration=0`; o `init` do VM vira `1L` e `progress = pos/1` → barra 100% até o controller conectar (Diag 02 §3c).
- **Arquivos:** `service/PlaybackService.kt` (`saveCurrentState`), `ui/player/PlayerViewModel.kt` (init), `PlayerUiState`.
- **Esforço:** P · **Depende de:** nada.

### 1.9 — [MÉDIO] `onPlaybackResumption` `@Deprecated` + corrida com restore
- **O quê / porquê:** o override está marcado `@Deprecated("Deprecated in Media3")` e corre com `restoreLastSession` no cold start (ambos leem o banco e reconstroem) (Diag 02 §4b). Revisar a API e remover a duplicação de reconstrução.
- **Arquivos:** `service/PlaybackService.kt` (`onPlaybackResumption`, `restoreLastSession`).
- **Esforço:** M · **Depende de:** 2.5.

### 1.10 — [BAIXO] Restore aborta sem log + controles no-op silencioso
- **O quê / porquê:** `buildPlaylistFromState`/`restoreLastSession` abortam sem log com `mediaId` inválido (Diag 02 §1e); vários controles fazem `?: return` sem feedback quando o controller ainda não conectou.
- **Arquivos:** `service/PlaybackService.kt`, `ui/player/PlayerViewModel.kt`.
- **Esforço:** P · **Depende de:** nada.

---

## BLOCO 2 — Dívida técnica / arquitetura (Diag 01 + estruturais do Diag 02)

### 2.1 — Vazamento de DTOs/entidades para a UI; sem camada de domínio
- **O quê / porquê:** `:app` depende **direto** de `:data:local` e `:data:remote` (`app/build.gradle.kts:49-50`) porque a interface `BibleRepository` devolve `*Entity` e `MoreContentDto`. A UI manipula tipos de persistência/rede crus (Diag 01 §2a-2c). Introduzir modelos de domínio/UI + mappers e estreitar a fronteira.
- **Arquivos:** `data/repository/.../BibleRepository.kt` + Impl, telas/VMs de `ui/more`, `ui/themas`, `ui/studies`, `ui/favorites`, `app/build.gradle.kts`.
- **Esforço:** G · **Depende de:** convergir com 2.5.

### 2.2 — Orquestração de loading duplicada nas ViewModels
- **O quê / porquê:** a lógica "checa cache → decide loading → falha silenciosa vs visível" está copiada em Home/Themes/Studies/More porque `syncX()` devolve `Result<Unit>` sem sinalizar mudança (Diag 01 §3b). Expor `Flow<Resource<T>>` (Loading/Success/Error) do repositório.
- **Arquivos:** `data/repository/.../BibleRepositoryImpl.kt`, `ui/home`, `ui/themas`, `ui/studies`, `ui/more` ViewModels.
- **Esforço:** M · **Depende de:** nada.

### 2.3 — Telas fora do padrão LCE/MVI
- **O quê / porquê:** Favorites, Chapters e Player não têm `UiState` selado (LCE); More/MoreSectionDetails têm estado mas **sem Intent/Contract** (Diag 01 §5). Padronizar. Inclui **renomear o arquivo quebrado `ui/more/MoreSectionDetailsRoute.k.kt`** (confirmado, `.k.kt`).
- **Arquivos:** `ui/favorites/*`, `ui/chapters/*`, `ui/player/*`, `ui/more/*`.
- **Esforço:** M · **Depende de:** 2.1 (modelos de UI ajudam a fechar os Contracts).

### 2.4 — Decisão sobre `ChaptersScreen` (código morto)
- **O quê / porquê:** `ChaptersScreen`/`ChaptersViewModel` **não são navegados** por ninguém (confirmado: nenhum `navigate(Screen.Chapters`) e usam o parser de índice `"bookId|index"` órfão (Diag 02 §0.2). Decidir: **remover** ou **religar**. Se religar, os itens 1.3 (clipping) e o §1d (índice `number-1` divergente de `playBook`) **acordam** e precisam ser corrigidos junto.
- **Arquivos:** `ui/chapters/*`, `ui/navigation/*`, `service/PlaybackService.kt` (branch `split("|")`).
- **Esforço:** P (remover) / M (religar) · **Depende de:** 2.5 se religar.

### 2.5 — Parser único de `mediaId` (sealed type) + mapper de `MediaItem`
- **O quê / porquê:** `mediaId` é formatado/parseado em ~13 lugares com convenções divergentes (`numérico`, `study_x_y`, `moment_`, `bookId|index`) e os builders de `MediaItem` estão duplicados entre serviço e VM (Diag 02 §1). Centralizar num sealed type + mapper resolve de uma vez 1.5, 1.3, o §1d e a corrida do §2c. É o `MediaItemMapper` que o `REFACTOR_MEDIA3_TODO.md` ainda lista como pendente (o resto daquele refactor — fim do God-object — já foi feito).
- **Arquivos:** novo mapper/sealed type, `service/PlaybackService.kt`, `ui/player/PlayerViewModel.kt`.
- **Esforço:** M–G · **Depende de:** nada (habilita vários do Bloco 1).

### 2.6 — Unificar `compileSdk` (35 no `:app`, 36 nas libs) + `targetSdk`
- **O quê / porquê:** divergência confirmada (`app/build.gradle.kts:11` = 35; `data/*` = 36); lint aponta `OldTargetApi` em `targetSdk=35` (Diag 01 §4a). Não quebra hoje, mas é incoerência latente.
- **Arquivos:** `app/build.gradle.kts`, `data/*/build.gradle.kts`.
- **Esforço:** P · **Depende de:** nada.

### 2.7 — (Opcional, intenção) Quebrar o `PlayerViewModel` (~960 linhas)
- **O quê / porquê:** `ANALISE_MODULARIZACAO_PLAYER.md` propõe separar Cast de Media3. O God-object antigo já morreu; resta o VM inchado (Cast + MediaController + sleep timer + favoritos). Manutenibilidade, não bug.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (+ possível `:core:media`).
- **Esforço:** G · **Depende de:** 2.5 e Bloco 1 estáveis.

---

## BLOCO 3 — Features novas / polimento

### 3.1 — Retomada "tipo Spotify" + notificação pausada persistente (Opção A)
- **O quê / porquê:** **decisão de produto registrada:** hoje, **pausado + remover o app → a notificação NÃO persiste** (efetivamente opção B) — verificado em device; o serviço, fora do foreground quando pausado, é recuperado pelo sistema na remoção da tarefa. A **Opção A** (notificação pausada permanece, estilo Spotify) foi **adiada conscientemente**. Implementá-la exige manter o serviço em foreground mesmo pausado e tornar a notificação **dismissível** (em vez de sumir sozinha), além de concluir `onPlaybackResumption` (ver 1.9). É a meta do `README.md` antigo.
- **Arquivos:** `service/PlaybackService.kt` (foreground/`onUpdateNotification`), `di/MediaModule.kt`.
- **Esforço:** G · **Depende de:** 1.4 (save confiável) e 1.9. Baixa prioridade.

### 3.2 — Android Auto (concluir e declarar)
- **O quê / porquê:** a árvore de navegação (`onGetLibraryRoot`/`onGetChildren`) **já existe** no serviço, mas o app **não está declarado** para o Auto (sem `meta-data com.google.android.gms.car.application` no manifest) e tocar um capítulo avulso pelo Auto não monta a fila do livro (`PLANO_..._UAMP.md` Fase 5).
- **Arquivos:** `app/src/main/AndroidManifest.xml`, novo `res/xml/automotive_app_desc.xml`, `service/PlaybackService.kt`.
- **Esforço:** M · **Depende de:** 2.5 (expansão de fila unificada).

### 3.3 — StrictMode em debug
- **O quê / porquê:** `PLANO_..._UAMP.md` Fase 1 previa ferramentas de debug; LeakCanary **já está**, StrictMode **não**. Ajuda a flagrar I/O na main thread.
- **Arquivos:** `OuvindoBibliaApp.kt` (bloco debug).
- **Esforço:** P · **Depende de:** nada.

### 3.4 — [BAIXO] Higiene de lint (100 warnings)
- **O quê / porquê:** 35 Typos, 23 GradleDependency, 21 UnusedResources, etc. (Diag 01 §1b). Zero erros; faxina + fixar versões.
- **Arquivos:** recursos, `gradle/libs.versions.toml`, strings.
- **Esforço:** P–M · **Depende de:** nada.

> **Já concluídos dos planos antigos** (não entram): fim do God-object `BibleMediaItemTree` / refactor Media3, Splash (`installSplashScreen`), LeakCanary, e o ciclo de vida do player (5.1–5.4).

---

## Estado atual honesto (em prosa)

**O que está sólido.** O app compila e gera APK, e a fundação está bem montada: multi-módulo com a fronteira de *acesso* a dados preservada (a UI nunca toca DAO/Room direto), sync de conteúdo versionado por `meta.version` centralizado no repositório, e o refactor do Media3 que os docs antigos pediam já feito. As telas principais seguem LCE/MVI. E, agora, **o coração do app — o ciclo de vida do player — está estável e validado em device**: o bug crítico 5.1 foi corrigido e a reprodução sobrevive ao app ir para segundo plano / Activity ser destruída, com reabrir-e-tocar funcionando sem player liberado. Esse era o maior risco do projeto e saiu da lista.

**O que está pela metade.** O **Cast** é o que está mais incompleto e visível: transfere posição só na ida (local→Cast), não traz de volta ao desconectar, não monta fila (sem auto-avanço na TV) e manda metadados de Bíblia para Estudos/Temas. A **persistência de posição** funciona no caminho feliz, mas não tem save periódico (perde progresso se o processo morre no meio de uma faixa) e a barra de progresso pisca cheia no cold start. A **convenção de `mediaId`** continua espalhada e frágil — é a raiz de vários bugs médios. Na **arquitetura**, a fronteira de *tipos* ainda vaza (entidades/DTOs chegam crus à UI, sem camada de domínio) e a orquestração de loading está duplicada em quatro ViewModels. A **suíte de testes não roda** e a cobertura é ~zero. E há a decisão de produto consciente: a notificação pausada não persiste hoje (opção B); a opção A (Spotify) ficou para depois.

**Próximo passo mais sensato.** Começar pelo **Bloco 0**, nesta ordem: **0.1** (destravar `./gradlew test` — é P e devolve a rede de segurança), **0.2** (migração do Room — alto impacto de UX; e precisa vir **antes** de qualquer item que mexa em schema) e **0.3** (teste instrumentado do ciclo de vida do player, para travar a regressão do 5.1 que validamos só na unha). Com a base estável e testável, o caminho natural é o **Cast (1.1 + 1.2)**, que andam juntos e são os bugs ALTOS mais perceptíveis, e então a **unificação do `mediaId` (2.5)**, que destrava de uma vez a maior parte dos bugs médios restantes.

---

## Documentos a arquivar

Os 4 `.md` de intenção cumpriram o papel de registrar planos, mas estão desalinhados do código e induzem a erro quem os ler como estado atual. Sugiro mover para `docs/archive/` (preservar histórico, não apagar):

| Arquivo | Situação | Para onde foi |
|---|---|---|
| `REFACTOR_MEDIA3_TODO.md` | Em grande parte **feito** (God-object morto) | Resto vira o item **2.5** |
| `ANALISE_MODULARIZACAO_PLAYER.md` | **Não executado** | Vira o item **2.7** (opcional) |
| `PLANO_IMPLEMENTACAO_MELHORIAS_UAMP.md` | **Parcial** (Splash/LeakCanary feitos) | Resto em **3.2/3.3** |
| `README.md` | Doc viva + roadmap "Spotify" defasado | Manter README, mover a parte de roadmap para **3.1**; refletir que o ciclo de vida do player já é persistente |

Referência viva passa a ser: `CLAUDE.md`, `DIAGNOSTICO_01_ARQUITETURA.md`, `DIAGNOSTICO_02_PLAYER.md` e este `ROADMAP.md`.
