# Plano de Execução — Fases & Issues (Ouvindo a Bíblia)

Derivado do `ROADMAP.md` (gerado pelo Claude Code a partir do código real) e dos dois
`DIAGNOSTICO_*`. Cada issue é **autocontida**: dá pra colar isolada no Claude Code como uma tarefa.

**Fonte da verdade do arquivo:linha:** o `ROADMAP.md` no repo. As referências `§` apontam para a
seção de origem nos diagnósticos. Como a correção do 5.1 mudou linhas, peça ao Claude Code para
reconfirmar a linha exata ao pegar cada issue.

**Convenção:** `Validação` = como saber que ficou pronto · `device?` = precisa de celular/emulador ·
`Chromecast?` = precisa de TV.

---

## Status atual

- ✅ **5.1** (player persistente) — resolvido e validado em device.
- ✅ **FASE 0 fechada** — `./gradlew test` e `connectedAndroidTest` passam:
  - ✅ **0.1** — JUnit no classpath de teste do `:data:remote`; `./gradlew test` passa globalmente.
  - ✅ **0.2** — migração destrutiva → Migration 8→9 com teste de preservação de favoritos/retomada.
  - ✅ **0.3** — teste instrumentado do ciclo de vida do player (serviço sobrevive ao unbind).
- ✅ **1.A** (save periódico de posição) — resolvido e validado em device.
- ✅ **1.B** (barra cheia no cold start) — resolvido; travado por teste unitário.
- ✅ **FASE 1 fechada.**
- ✅ **2.A** (parser único de `mediaId`) — resolvido; testes unitários + smoke em device.
- ✅ **2.B** (folha de capítulos por tipo) — resolvido; teste unitário + validado em device.
- ✅ **2.C** (log no restore) — resolvido; todo abort de `buildPlaylistFromState` deixa rastro.
- ✅ **2.D** (clipping) — guarda explícita: não persistir itens recortados (decisão do usuário).
- ✅ **FASE 2 fechada.**
- ✅ **3.E** (SDKs) — compileSdk/targetSdk unificados em 36; build limpo.
- ✅ **3.D** (`ChaptersScreen` morto) — **removido** (decisão do usuário): apagados
  `ui/chapters/*`, rota/composable `Screen.Chapters`, tipo `MediaContentId.BookFolder`
  (parser `"|"`), ramo `isBookFolder` do `onSetMediaItems` e o clipping órfão de
  `createMediaItemsFromChapters`. Guarda 2.D mantida como defesa. Build + testes verdes.
- ✅ **3.C** (LCE/MVI + rename) — Favorites virou LCE selado; Favorites/More/
  MoreSectionDetails/ThemeDetails ganharam `*Contract.kt`; `handle(Intent)` onde há
  ação; `MoreSectionDetailsRoute.k.kt` renomeado. Build + testes verdes (device pendente).
- ✅ **3.A** (camada de domínio) — `:app` consome só modelos de domínio de
  `:data:repository` (Book/Chapter/Study/Lesson/Theme/Moment/FavoriteLesson/árvore
  MoreContent) + mappers internal; deps `:data:local`/`:data:remote` removidas do
  `:app`. `grep 'data.local.entity|data.remote.dto|data.local.model' app/src` → 0.
  Fatiado em 7 commits, cada um verde. Smoke test no device OK.
- ✅ **3.B** (loading nas VMs) — `Resource<T>` + `syncedListResource`/`getXResource`
  no repositório; Home/Themes/Studies/More VMs sem `_isLoading/_error/combine/syncData`
  (refreshTrigger+flatMapLatest+stateIn(WhileSubscribed(5s))). Validado por logcat no
  device: 1 GET por load; <5s não re-dispara; >5s re-dispara 1x; Retry 1 GET.
- ✅ **4.C** (higiene) — 2 lint errors (Media3 OptIn) zerados; 21 UnusedResources
  removidos; StrictMode em debug; Android Auto declarado; **Cast DESLIGADO** nesta
  versão (kill-switch `CastConfig.ENABLED`). Vazamento do LeakCanary: **não reproduz**
  no build atual (0 leaks após repro do zero + heap dump forçado).
- ✅ **4.B** — `onPlaybackResumption` migrado para a overload com `isForPlayback`
  (Media3 1.7+); removido o `@Deprecated`. Comportamento preservado.
- ✅ **4.A** — `onTaskRemoved` sem `super` (evita `stopSelf` ao pausar). Opção A já
  ocorre no moto g53 por conta do SO; mudança é hardening. Achado à parte: persistência
  "estilo-Spotify" (foreground pausado) conflita com notificação dismissível.
- ❌ **Cast** (§6.1–6.4) — **FORA DE ESCOPO desta versão** (desligado via kill-switch;
  código dormente no repo).
- ✅ **4.D** — Android Auto de verdade (browse + playback por mediaId); lint zerado
  (commit `96a7dfa`). Falta só validar end-to-end em DHU; busca por voz é backlog.
- 🏁 **Plano original concluído** (0→4 + 4.D). Backlog opcional: validação DHU do Auto, busca
  por voz, persistência estilo-Spotify, 35 Typos (baseline), 37 bumps de dependência.
- 🆕 **FASE 5 aberta** (2026-07-15) — varredura de bugs clássicos de app de áudio. O núcleo
  (audio focus, becoming-noisy, wake lock, tipo de FGS, ciclo de vida do player) já está
  **correto**; os achados estão na periferia. 4 issues novas, ordenadas por criticidade:
  - ✅ **5.A** (CRÍTICA, FEITA 2026-07-15) — `POST_NOTIFICATIONS` declarada + pedida em runtime
    na `MainActivity`; validada no device (concedida→notificação de mídia; negada→áudio sem crash).
  - ✅ **5.B** (MÉDIA, FEITA 2026-07-15) — play em cold start guardado (`pendingPlayAction`) e
    executado ao conectar o controller; validado no device (6 toques adiados → Êxodo tocou).
  - ✅ **5.C** (MÉDIA, FEITA 2026-07-15) — sleep timer conta tempo de reprodução (tick só quando
    `isPlaying`); pausa junto com o áudio. Validado no device.
  - ✅ **5.D** (BAIXA, FEITA 2026-07-15) — `fastForward` com `coerceAtMost(duration)` (rewind já OK).
  - (2 achados de Cast entraram no backlog de Cast, abaixo — estão fora de escopo desta versão.)
- 🆕 **FASES 6 e 7 abertas** (2026-07-15) — 2ª varredura, agora fora do core de playback
  (repositório, DAO, ViewModels de conteúdo, app-level, código morto). Detalhes nas seções
  FASE 6 (bugs funcionais/robustez) e FASE 7 (código morto). Destaques:
  - ↪️ **6.A REBAIXADA → 7.E** (2026-07-15) — as seções "Continuar Ouvindo"/"Favoritos" da Home
    NÃO são bug: são scaffolding morto, já substituído por soluções vivas (mini player restaura a
    sessão no cold start; tela de Favoritos dedicada). Virou remoção de código morto (ver 7.E).
  - ✅ **6.B** (MÉDIA, FEITA 2026-07-15) — `ThemeDetails`/`StudyDetails` migradas p/
    `refreshTrigger.flatMapLatest{...}.stateIn` → Retry não vaza mais coletores.
  - ✅ **6.C** (MÉDIA, FEITA 2026-07-15) — `extractDominantColorFromUrl` usa `context.imageLoader`
    (singleton com UA+cache). Achado: extração já funcionava (CDN não exige UA); ganho = cache/perf.
  - ✅ **6.D/6.E** (BAIXA, FEITAS 2026-07-15) — Home vazio→Error com Retry (era Loading eterno);
    `syncMoreContent` com version-gating via `dao.get()?.version`.
  - ✅ **6.F** (FECHADA 2026-07-16, sem código) — nenhuma base Room < 8 no mundo real (ver seção).
  - ✅ **6.G** (MÉDIA, user-facing, FEITA 2026-07-15) — coração de favorito. Eram 2 bugs (metadata do
    controller como falsa fonte de verdade): (1) `syncStateWithController` revertia o otimista;
    (2) `toggleFavorite` lia `oldStatus` do metadata → nunca desfavoritava. Fix: `currentIsFavorite`
    (DB-backed) vira a fonte única. Validado no device (favorita/desfavorita alternando).
  - ✅ **FASE 7 CONCLUÍDA (2026-07-16)** — código morto removido (7.A–7.E; ver seções).
- 🆕 **2 bugs de uso real corrigidos (2026-07-17):** BUG A cold start lento → cache-first +
  GET condicional 304 (commit `09f7094`); BUG B notificação-fantasma de sessão restaurada
  nunca tocada → gate em `onUpdateNotification` (commit `62e8e54`). Ambos validados no
  device em debug e no release ofuscado.
- ✅ **Formulários do Play Console FEITOS (2026-07-18, pelo dono):** PUB-20 (política hospedada
  e informada), PUB-21 (Data Safety — o que derrubou o app em mai/2024), PUB-22 (rating L),
  PUB-24 (ficha, exceto `04_estudos`), PUB-25 e as 6 declarações de "Conteúdo do app"
  ("Tudo em dia"). Detalhe na FASE 8. Restam: PUB-23 (vídeo FGS), novela da chave,
  placeholders de Estudos e testes PUB-11/12/13/16 no release.

---

## FASE 0 — Fechar a estabilização

Objetivo: build e testes 100% saudáveis antes de tocar em feature.

### ISSUE 0.1 — ✅ FEITA — `:data:remote` sem JUnit no classpath de teste

- **Problema:** `data/remote/build.gradle.kts` não declarava `testImplementation(libs.junit)`;
  `./gradlew test` global falhava no compile do source set de teste (§ Diag01 1a).
- **Arquivos:** `data/remote/build.gradle.kts`.
- **Critério de aceitação:** `./gradlew test` passa em todos os módulos.
- **Resultado:** adicionado `testImplementation(libs.junit)` (+ androidTest junit/espresso),
  espelhando `:data:local`/`:data:repository`. `./gradlew test` → **BUILD SUCCESSFUL**. (commit `94a4c69`)
- **Esforço:** P · **Depende de:** nada.

### ISSUE 0.3 — ✅ FEITA — Confirmar/cobrir teste do ciclo de vida do player

- **Problema:** cobertura ~zero; o 5.1 foi validado manualmente. Garantir um teste instrumentado que
  trave regressão do "serviço sobrevive ao unbind".
- **Arquivos:** `app/src/androidTest/.../PlaybackServiceLifecycleTest.kt` (novo),
  `service/PlaybackService.kt` (contadores `@VisibleForTesting` create/destroy).
- **Critério de aceitação:** teste instrumentado que, tocando → destruir Activity, verifica que o
  serviço NÃO é destruído e o player não é liberado.
- **Resultado:** `PlaybackServiceLifecycleTest` valida que um serviço *started* sobrevive ao unbind
  do `MediaController` (`destroyCount==0`) e que, ao reconectar, a playlist persiste com o mesmo
  `mediaId` (player não liberado). `connectedAndroidTest` → **BUILD SUCCESSFUL** (moto g53). (commit `48d08c3`)
- **⚠️ Limitação:** o teste inicia o serviço via `startService` (mesma garantia de sobrevivência de um
  serviço *started*), evitando o contrato de 5s do `startForegroundService` e a necessidade de áudio
  real (o player de produção usa data source só-HTTP, inviável de tocar de forma hermética). Valida a
  **propriedade de sobrevivência ao unbind**, não o gatilho `ensureServiceStarted` dentro do
  `PlayerViewModel` (isso exigiria stub pesado do repositório).
- **Esforço:** M · **Depende de:** 0.1.

---

## FASE 1 — Persistência de posição (alto valor, sem Chromecast)

Objetivo: nunca perder onde o usuário parou.

### ISSUE 1.A — ✅ FEITA — Save periódico de posição (§ Diag02 3d)

- **Problema:** só havia save em pause/transição. Se o processo morre no meio da faixa, retoma do
  início do capítulo (perde minutos).
- **Arquivos:** `service/PlaybackService.kt` (auto-save).
- **Critério de aceitação:** posição é persistida em intervalo regular (~10–15s) enquanto toca; após
  kill no meio da faixa, retoma do ponto (tolerância ≤ intervalo).
- **Resultado:** loop de save no serviço, ligado por `onIsPlayingChanged` (só roda com áudio de fato
  saindo), gravando a cada `PERIODIC_SAVE_INTERVAL_MS` (15s) via `saveCurrentState()`. Fica no
  serviço (não na ViewModel) porque ele sobrevive à morte da Activity e cobre reprodução em background.
- **Validação (device, moto g53):** tocando sem pausar, a posição salva avançou fresca entre leituras
  (29.7s → 74.7s); `kill -9` no meio da faixa (posição real ~128s, último save a 119.7s) → reabrir
  retomou em 1:59 (perda ~8.7s, dentro da tolerância de 15s).
- **Nota:** `am kill` não derruba o processo enquanto toca (serviço em foreground); usar `kill -9` no
  pid via `run-as` para simular morte abrupta com áudio ativo.
- **Esforço:** M · **Depende de:** nada.

### ISSUE 1.B — ✅ FEITA — Duração 0 no buffering → barra cheia no cold start (§ Diag02 3c)

- **Problema:** se o save ocorre antes da duração ser conhecida, grava `duration=0`; no cold start a
  barra aparecia 100% (com posição real, ex.: "2:38") até o controller conectar.
- **Causa raiz:** a barra é o `Slider` de `PlayerProgressBar` (`SharedPlayerScreen.kt`), com
  `valueRange = 0f..safeDuration` e `value = currentPosition`. Quando `duration<=0`, `safeDuration`
  vira `1L` (range `0f..1f`) e a posição real (ex.: 158204) estoura o range → thumb satura em cheio.
- **⚠️ Falso positivo corrigido:** a 1ª tentativa mexeu só no getter `PlayerUiState.progress`, que
  **não era usado por ninguém** (dead code). O teste passou testando código morto e a barra real
  continuou cheia. Lição: rastrear quem consome o dado antes de declarar pronto.
- **Arquivos:** `ui/player/SharedPlayerScreen.kt` (nova função pura `sliderProgressValueMs`, usada no
  slider), `ui/player/PlayerViewModel.kt` (cold start propaga `0L`, necessário para o `duration>0`
  do slider detectar "desconhecida"), `ui/player/PlayerUiState.kt` (getter `progress` morto removido).
- **Critério de aceitação:** barra não exibe 100% falso no cold start; progresso correto após conectar.
- **Resultado:** `sliderProgressValueMs(pos, dur)` devolve `0f` quando `dur<=0` (barra vazia), senão a
  posição real. `PlayerUiStateProgressTest` cobre a função **realmente usada** pelo slider.
- **Validação:** regressão do caso normal confirmada em device (barra e tempos corretos, "2:32/2:55",
  ~87%; o ramo `duration>0` é byte-idêntico ao original, então o seek é preservado). O transiente
  `duration=0` não foi reproduzível em device (o `START_STICKY` ressuscita o serviço e reescreve o DB
  injetado; adb-via-WiFi instável; SELinux bloqueia a escrita) — daí o teste unitário sobre a função
  viva como guarda primária.
- **Esforço:** P · **Depende de:** 1.A.

---

## FASE 2 — Convenção de conteúdo (`mediaId`) e tipos

Objetivo: matar a fragilidade de "tudo é Bíblia". Tratar como um pacote.

### ISSUE 2.A — ✅ FEITA — Parser único de `mediaId` (sealed type) (§ Diag02 1, 2.5)

- **Problema:** `mediaId` era formatado/parseado em ~10 lugares com convenções string (`study_`,
  `moment_`, `{bookId}|{idx}`, numérico). Frágil e duplicado.
- **Arquivos:** novo `playback/MediaContentId.kt` (sealed interface); migrados
  `service/PlaybackService.kt`, `ui/player/PlayerViewModel.kt`, `ui/chapters/ChaptersViewModel.kt`.
- **Critério de aceitação:** um único ponto de parse/format; todos os call-sites usam ele; `mediaId`
  inválido tem caminho tratado com log.
- **Resultado:** `sealed interface MediaContentId` com `Bible`/`Study`/`ThemeMoment`/`BookFolder`;
  `.raw` formata, `parse()` interpreta (`null` = malformado, logado nos call-sites do serviço). Todas
  as strings `.raw` são byte-idênticas às antigas → refactor puro (estados salvos/retomada intactos).
  Nós navegáveis da árvore de browse (numericId puro em `onGetChildren`) ficam fora, por serem
  namespace separado. Unificado o parse da Bíblia em `Long` (era `Int` no serviço).
- **Validação:** `MediaContentIdTest` (round-trip das 4 formas + malformados → `null`); smoke em
  device (Bíblia/Estudo/Tema tocam com metadados corretos, retomada ok, sem avisos de malformado).
- **Esforço:** M · **Depende de:** nada (habilita 2.B, 2.C, 2.D).

### ISSUE 2.B — ✅ FEITA — `extractChaptersFromPlayer` assume Bíblia (§ Diag02 1c)

- **Problema:** em Estudos, gerava numeração sintética; a folha de capítulos mostrava "1,2,3" em vez
  dos títulos das aulas.
- **Arquivos:** novo modelo `PlayerTimelineItem` + função pura `timelineItemFor` em
  `ui/player/PlayerUiState.kt`; `ui/player/PlayerViewModel.kt` (`extractTimelineFromPlayer`,
  `uiState.timeline`); `ui/player/components/ChaptersSheet.kt` (render por tipo);
  `ui/player/SharedPlayerScreen.kt` (passa `timeline`).
- **Critério de aceitação:** folha mostra títulos reais por tipo (Bíblia/Estudo).
- **Resultado:** `timeline` é uma projeção de exibição separada de `chapters` (que segue como
  `ChapterWithBookInfo` para Cast/favoritos — não foi tocado, Cast está estacionado/intestável).
  `timelineItemFor` deriva, via `MediaContentId`: Bíblia → número (grid); Estudo → título da aula do
  `subtitle` (lista). Folha adapta layout e cabeçalho ("Escolha o Capítulo" / "Escolha a Aula").
- **Validação:** `TimelineItemForTest` (Bíblia número/fallback, Estudo título/fallbacks); device —
  Estudo mostra nomes das aulas, Bíblia mantém grid de números.
- **Esforço:** M · **Depende de:** 2.A.
- **Descoberta:** o app inclui LeakCanary (debug), que cria um 2º ícone de launcher ("Leaks"); abrir
  via `monkey LAUNCHER` pode cair nele. Abrir com `am start -n <pkg>/.MainActivity`.

### ISSUE 2.C — ✅ FEITA — `restore` aborta sem log em `mediaId` malformado (§ Diag02 1e)

- **Problema:** `buildPlaylistFromState` retornava `null` sem log → "app não retoma" sem rastro.
- **Arquivos:** `service/PlaybackService.kt`.
- **Critério de aceitação:** log de aviso quando o restore aborta por id inválido.
- **Resultado:** todo caminho de abort loga `Log.w` com o `mediaId`: id malformado/tema/pasta (else),
  estudo sem aulas, livro não encontrado p/ capítulo, e livro sem capítulos. (Parte iniciada junto
  do 2.A.)
- **Validação:** inspeção (sem device). **Esforço:** P · **Depende de:** 2.A.

### ISSUE 2.D — ✅ FEITA — Clipping restaurado como absoluto (§ Diag02 3b, latente)

- **Problema:** posição relativa ao recorte seria restaurada como absoluta. Latente: confirmado que
  `onPlayBook` sempre passa `startMs=0` (`NavigationGraph:45,55`), `ChaptersViewModel` não seta
  recorte, e Tema (único que recorta) nunca é persistido — o bug não dispara hoje.
- **Decisão (usuário):** guarda explícita, não corrigir agora.
- **Arquivos:** `service/PlaybackService.kt` (`saveCurrentState`).
- **Critério de aceitação:** decisão documentada / guarda para não ativar por acidente.
- **Resultado:** `saveCurrentState` não persiste item com `clippingConfiguration != UNSET` (loga e
  retorna). Se o recorte for religado, a retomada simplesmente não grava posição errada; a correção
  clip-aware fica atrelada à 3.D.
- **Validação:** inspeção + suíte unitária verde (no-op hoje). **Esforço:** P · **Depende de:** 2.A.

---

## FASE 3 — Dívida técnica de arquitetura

Objetivo: parar a corrosão estrutural. Não urgente, mas paga juros.

### ISSUE 3.A — ✅ FEITA — Camada de domínio / parar de vazar DTO e Entity pra UI (§ Diag01 2)

- **Problema:** `:app` dependia direto de `:data:local`/`:data:remote`; UI consumia `*Entity`,
  `*Dto` e relation-models do Room (`data.local.model`) sem mapeamento.
- **Feito:** pacote `domain/` em `:data:repository` — `model/` (nomes limpos: Book, Chapter,
  Study+Lesson, Theme, Moment, FavoriteLesson, árvore MoreContent/MoreSection/…) + `mapper/`
  (extensões `internal` `toDomain()`). Interface e Impl passam a expor domínio; `:app` migrado
  fatia a fatia (Estudos, Temas, Mais, Favoritos, Bíblia/Home + Player/Service). Removidas as
  deps `:data:local`/`:data:remote` do `app/build.gradle.kts` (Hilt segue agregando os módulos
  DI via classpath transitivo do `:data:repository` — clean build valida).
- **Decisão:** escopo amplo (incluiu `data.local.model`); nomes limpos; entrega fatiada.
- **Validação:** `grep -rE 'data.local.entity|data.remote.dto|data.local.model' app/src` → 0;
  `assembleDebug test` (clean) verde; smoke test no device OK. **Esforço:** G. Commits
  `d7bf413`→`7aa587f`.

### ISSUE 3.B — ✅ FEITA — Orquestração de loading duplicada nas ViewModels (§ Diag01 3b)

- **Problema:** "tem cache? falha de sync silenciosa?" copiada em Home/Themes/Studies VMs.
- **Feito:** `Resource<T>` (Loading/Success/Error) em `domain/`; `syncedListResource(cache, sync)`
  no Impl dispara o sync version-gated 1x dentro do `flow{}` (não em combine) e reflete o cache;
  `getMoreContentResource` é a variante nullable-single. VMs viram
  `refreshTrigger.flatMapLatest { getXResource() }.map { toUiState() }.stateIn(WhileSubscribed(5s))`;
  Retry = `refreshTrigger.update { it+1 }`. MoreSectionDetails ficou intacta (não sincroniza).
- **Comportamento (validado por logcat no device):** 1 GET por load; revisita <5s NÃO re-dispara
  (stream compartilhado); revisita >5s re-dispara 1x (cold-restart do WhileSubscribed — **muda**
  vs. o antigo `init{}` que sincronizava 1x por vida da VM; ainda version-gated); Retry = 1 GET.
- **Validação:** build + testes verdes; logcat + `dumpsys media_session` no device. **Esforço:** M.
  Commit `d9a271d`.

### ISSUE 3.C — ✅ FEITA — Telas fora do padrão LCE/MVI + rename (§ Diag01 5)

- **Escopo escolhido:** completo/consistente.
- **Feito:**
  - `FavoritesViewModel`: `FavoritesUiState` virou LCE selado (Loading/Success/Error) num
    `FavoritesContract.kt` novo (antes era `data class` com flag `isLoading`); removido o
    `Log.d("DEBUG_FAV", …)`; adicionado `FavoritesIntent` (RemoveChapter/RemoveStudy) +
    `handle()`. Tela mantém header+seletor sempre visíveis e troca só a região de conteúdo.
  - `MoreViewModel`/`MoreSectionDetailsViewModel`/`ThemeDetailsViewModel`: `UiState` inline
    extraído para `MoreContract.kt`/`MoreSectionDetailsContract.kt`/`ThemeDetailsContract.kt`.
  - `Intent` + `handle()` adicionados em More (Retry→sync) e ThemeDetails (Retry→loadMoments).
    MoreSectionDetails ficou **sem Intent** (só leitura; retry do erro = voltar) — documentado
    no Contract.
  - `MoreSectionDetailsRoute.k.kt` → `MoreSectionDetailsRoute.kt`.
  - Player permanece como **exceção documentada** (não migrado).
- **Validação:** `:app:assembleDebug` + `:app:testDebugUnitTest` verdes; device pendente.

### ISSUE 3.D — ✅ FEITA — Destino do `ChaptersScreen` morto (§ Diag02 0.2)

- **Decisão do usuário:** **remover** o código morto.
- **Feito:** apagados `ui/chapters/ChaptersScreen.kt` e `ChaptersViewModel.kt`; removidos
  a rota/`composable<Screen.Chapters>` (`AppNavigation.kt`/`NavigationGraph.kt`), o `contains`
  cosmético no `MainScreen.kt`, o tipo `MediaContentId.BookFolder` + separador `"|"` + ramo do
  `parse`, o ramo `isBookFolder` do `onSetMediaItems` e os params de clipping órfãos de
  `createMediaItemsFromChapters`. Testes do `MediaContentIdTest` ajustados (pipe agora → `null`).
- **Nota:** a guarda 2.D em `saveCurrentState` foi mantida como defesa (o clipping de Bíblia em
  `PlayerViewModel.buildBibleMediaItems` segue existindo, mas inalcançável — `playBook` só recebe
  `startMs=0` dos call-sites vivos Home/Favoritos). Limpá-lo é candidato futuro (fora do escopo 3.D).
- **Validação:** `:app:assembleDebug` + `:app:testDebugUnitTest` verdes.

### ISSUE 3.E — ✅ FEITA — Unificar compileSdk/targetSdk (§ Diag01 4a)

- **Feito:** `:app` movido de 35 → 36 (compileSdk e targetSdk), alinhado às libs. Build debug limpo.
- **Validação:** `:app:assembleDebug` verde.

---

## FASE 4 — Polimento & features

### ISSUE 4.A — ✅ FEITA — Opção A: notificação persiste pausada após fechar app

- **Feito:** removida a chamada `super.onTaskRemoved()` em `PlaybackService.onTaskRemoved`.
  O default do `MediaSessionService` faz `if (!isPlaybackOngoing() || !isAnySessionPlaying())
  pauseAllPlayersAndStopSelf()` → com o player **pausado** ele dá `stopSelf()` (= Opção B).
  O comentário do código já afirmava Opção A; a chamada super contradizia. Commit `719e6f1`.
- **Validação no moto g53 (A/B, mesmo swipe pausado):** comportamento **idêntico** entre o
  build com e sem `super`. A Motorola mata o processo *cached* direto no `remove task`
  (adj 915) **sem entregar `onTaskRemoved`**, e a notificação *detached* (`flags=0x8`,
  dismissível) sobrevive à morte do processo. Ou seja, a **Opção A já ocorre por conta do SO**
  neste aparelho — a premissa "hoje some (Opção B)" não reproduz. A mudança é hardening
  correto (garante Opção A onde o `onTaskRemoved` É entregue pausado, ex.: Android stock).
- **Tradeoff consciente (revisão):** sem `stopSelf`, quando o `onTaskRemoved` chega pausado
  o serviço fica *started* e `player.release()` só ocorre no `onDestroy` — que o SO
  normalmente NÃO chama num kill de processo cached. Não é vazamento (a morte do processo
  libera os recursos nativos), mas não há mais caminho que garanta `onDestroy` no task
  removal. Alinhado ao roadmap persistente.
- **Critério de aceitação:** atendido (notificação permanece + dismissível `flags=0x8`).

### 🔭 Roadmap separado — persistência "estilo-Spotify" (CONFLITA com 4.A)

- **Achado no device:** o Spotify mantém `isForeground=true` **mesmo pausado** (notificação
  com `FOREGROUND_SERVICE`+`NO_CLEAR`, **não-dismissível**), por isso o processo dele
  **sobrevive** ao swipe na Motorola. O Media3, por padrão, **rebaixa** o serviço do
  foreground no pause (`stopForeground` → notificação dismissível), então o processo vira
  *cached* e é morto no `remove task`.
- **Tensão:** persistir como o Spotify exige foreground pausado → notificação **não-dismissível**,
  o que **contradiz** o critério da 4.A (dismissível). São objetivos opostos. Decidir à parte
  se o objetivo é "processo sobrevive p/ resume instantâneo" (Spotify, notif. grudada) ou
  "notif. dispensável" (atual). Não implementado.

### ISSUE 4.B — ✅ FEITA — `onPlaybackResumption` `@Deprecated` (§ Diag02 4b)

- **Feito:** migrado para a overload `onPlaybackResumption(MediaSession, ControllerInfo,
  isForPlayback: Boolean)` (Media3 1.7+, presente na 1.9.2); removido o `@Deprecated`.
  O antigo (2 args) delegava e por isso ainda funcionava, só com warning.
- **Semântica do flag:** `false` → sistema quer só metadados (notificação de "continuar"
  no boot), não inicia playback; `true` → devolve playlist+posição e o framework dá play.
  Retornamos o mesmo `MediaItemsWithStartPosition` nos dois casos → comportamento preservado.
- **Verificação:** `./gradlew :app:compileDebugKotlin` OK, sem warning de deprecação;
  nenhuma outra referência à assinatura antiga (grep). Path `isForPlayback=false` só roda
  no boot do device com sessão salva.

### ISSUE 4.C — ✅ FEITA — Android Auto (declarar) / StrictMode / higiene de lint

- **Feito:**
  - **Lint errors (2):** `@OptIn(UnstableApi::class)` no `bitmapLoader`/`onDestroy`
    do `PlaybackService`. Commit `72eb8d9`. ⚠️ **CORREÇÃO (revisão):** a declaração de
    Android Auto neste mesmo passo (`0bc8436`) REINTRODUZIU 2 lint errors
    (`MissingMediaBrowserServiceIntentFilter`, `MissingIntentFilterForMediaSearch`) e o
    lint não foi re-rodado — a afirmação "sem errors" ficou falsa até `96a7dfa`, que
    completou o Auto e zerou o lint (ver 4.D abaixo).
  - **UnusedResources (21):** drawables órfãos, `colors.xml` enxuto, `backup_rules.xml`;
    rastreado 0 refs cada (99→77 warnings). Commit `5b1d985`.
  - **StrictMode:** ligado só em debug no `OuvindoBibliaApp` (thread detectAll + vm
    activity/closable/sqlite leaks, penaltyLog). **Android Auto:** `automotive_app_desc.xml`
    + meta-data `com.google.android.gms.car.application`. Commit `0bc8436`.
  - **Cast DESLIGADO** (fora desta versão): kill-switch único `cast/CastConfig.ENABLED=false`
    corta `initializeCast()`; some a I/O de disco na main do `CastContext.getSharedInstance`
    (validado no device via StrictMode). Botão de Cast já oculto. Commit `a15c846`.
- **Vazamento LeakCanary:** **não reproduz** no build atual — 0 APPLICATION/LIBRARY LEAKS
  após 3 rotações + navegação + heap dump forçado. `PlayerViewModel` usa `@ApplicationContext`
  e `onCleared()` remove listener do Cast/callback/controller. Fechado.
- **Deixado para depois (não-hygiene):** 35 Typos (falso-positivo pt-BR — candidato a
  baseline), 37 bumps de dependência (tarefa à parte, arriscado num pass de higiene).

### ISSUE 4.D — ✅ FEITA — Android Auto de verdade (browse + playback por mediaId)

Surgiu da revisão que pegou o lint quebrado da 4.C. Commit `96a7dfa`.
- **Manifest:** serviço declarado como `MediaBrowserService` (honesto — somos
  `MediaLibraryService`) → zera `MissingMediaBrowserServiceIntentFilter`. Busca por VOZ
  (`MEDIA_PLAY_FROM_SEARCH`) suprimida com `tools:ignore` + doc — Auto por navegação
  funciona sem voz; voz é backlog e não anunciamos capacidade que não temos.
- **Playback por mediaId (era o dealbreaker):** `onAddMediaItems` resolve id→item com URI;
  `onSetMediaItems` expande item único navegado (sem URI) na playlist completa c/ índice
  certo (auto-avanço); `onGetItem` deixou de ser stub (resolve livro browsable ou item
  tocável). Refator `buildPlaylistFromState`→`buildPlaylistFromMediaId`+`resolvePlayableItem`
  compartilhado com o resume. App (telefone) manda itens com URI → segue pelo `super`, sem
  regressão.
- **Bug pego na validação (commit `bcfe4bc`):** `onConnect` restringia os comandos a
  `DEFAULT_SESSION_COMMANDS` (sem os de biblioteca) → `getLibraryRoot/getChildren/getItem`
  davam `PERMISSION_DENIED` para clientes `MediaBrowser`, ou seja **o Auto não navegaria**.
  Corrigido para `DEFAULT_SESSION_AND_LIBRARY_COMMANDS` (default do MediaLibrarySession).
- **Validação:** lint 0 errors, compile+test OK, resume no device sem crash. **Teste
  instrumentado `AndroidAutoBrowseTest`** (roda no emulador AAOS com conteúdo sincronizado):
  conecta um `MediaBrowser` real e prova raiz→livros→capítulos, `onGetItem`, e playback por
  `mediaId` (item cru expande no livro inteiro c/ startIndex correto). ✅ passa.
- **Nota:** no emulador AAOS o app **não é LISTADO** no seletor de fontes de mídia (filtro do
  host p/ apps user-installed/debug — descartado cache e toggle "show debug apps"). Isso é
  independente do contrato de browse+playback, que o teste prova. Validação na UI real fica
  p/ **DHU + Android Auto** (celular), onde a descoberta difere do AAOS.

---

## FASE 5 — Bugs clássicos de app de áudio (varredura 2026-07-15)

Objetivo: fechar os buracos de periferia que faltaram. O núcleo de playback já está sólido —
`setAudioAttributes(handleAudioFocus=true)` + `setHandleAudioBecomingNoisy(true)` +
`setWakeMode(WAKE_MODE_NETWORK)` (`di/MediaModule.kt:94-96`), FGS `mediaPlayback` +
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, e player não-`@Singleton` liberado só no `onDestroy`.
As issues abaixo estão ordenadas por criticidade (5.A → 5.D).

### ISSUE 5.A — ✅ FEITA (2026-07-15) — `POST_NOTIFICATIONS` ausente → sem notificação de mídia no Android 13+

- **Problema:** `targetSdk = 36` (`app/build.gradle.kts:16`), mas o `AndroidManifest.xml` **não
  declarava** `android.permission.POST_NOTIFICATIONS` e o app **nunca a pedia em runtime**
  (`MainActivity.kt`). Em Android 13+ (API 33), sem essa permissão a notificação do foreground
  service de mídia é **suprimida pelo SO**: o áudio toca, mas sem os controles na notificação/lockscreen.
- **Correção:** (1) permissão declarada no manifest (com comentário; ignorada em APIs < 33).
  (2) `MainActivity` registra um `ActivityResultContracts.RequestPermission` como campo e chama
  `requestNotificationPermissionIfNeeded()` no `onCreate`, gated em `SDK_INT >= TIRAMISU`, só pedindo
  se ainda não concedida (`ContextCompat.checkSelfPermission`). Callback é no-op de propósito: negada
  ou concedida, o áudio funciona; negar só esconde os controles.
- **Validado no device (moto g53, Android 14 / SDK 34):**
  - Estado revogado → 1º launch **exibe o diálogo** (`GrantPermissionsActivity`, `REQUEST_PERMISSIONS`).
  - **Concedida** → ao dar play surge a notificação `MediaStyle` (`category=transport`, ações
    "Ir para o item anterior"/"Pausar"/"Ir para o próximo item", título "Efésios 4", state=PLAYING).
  - **Negada** → áudio toca normalmente (mini player em ⏸), **sem crash** (logcat sem FATAL);
    notificação suprimida pelo SO, como esperado.
  - Bônus: a Home real **não** mostra "Continuar Ouvindo"/"Favoritos" (confirma 7.E ao vivo); o
    mini player restaurado faz esse papel.
- **Esforço:** P · **Depende de:** nada.

### ISSUE 5.B — ✅ FEITA (2026-07-15) — Comando de play descartado antes do controller conectar (cold start)

- **Problema:** `playBook`, `playThemePlaylist` e `playStudyPlaylist` começavam com
  `val controller = mediaController ?: return`. No cold start, se o usuário tocava num item **antes**
  de o `MediaController` conectar (o `buildAsync` de `initializeController` leva ~centenas de ms), o
  toque virava **no-op silencioso**: nada tocava e não havia feedback.
- **Correção:** campo `pendingPlayAction: (() -> Unit)?` + helper `isControllerReady()`
  (`mediaController?.isConnected == true`). Os 3 play methods, se `!isControllerReady()`, **guardam
  a intenção** (lambda que re-chama o próprio método com os mesmos args) e retornam; no callback de
  conexão do `controllerFuture` a intenção pendente é executada (o play explícito sobrepõe a sessão
  restaurada). Guardamos só a **última** intenção. `playStudyById` funila em `playStudyPlaylist`,
  então fica coberto.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`pendingPlayAction`, `isControllerReady`,
  `initializeController`, `playBook`/`playThemePlaylist`/`playStudyPlaylist`).
- **Validado no device (moto g53), teste do usuário:** com atraso artificial de 30s na conexão do
  controller (hack temporário, removido depois), o usuário tocou 6× em livros durante a janela →
  logcat mostrou 6× "playBook adiado" (nenhum toque perdido) e, ao conectar, "executando intenção
  pendente"; a **última** intenção (Êxodo) tocou sozinha (`media_session state=PLAYING,
  description=Êxodo 1`). Hack de atraso + logs removidos após validação.
- **Esforço:** M · **device?** sim (corrida de cold start reproduzida com atraso artificial).

### ISSUE 5.C — ✅ FEITA (2026-07-15) — Sleep timer conta wall-clock (não pausa com a reprodução)

- **Problema:** `setSleepTimer` usava `delay(minutes*60*1000L)` — um job de tempo de parede,
  independente do estado real. Se o usuário **pausava**, o timer continuava correndo e disparava
  na hora errada; se a faixa acabava sozinha, seguia contando.
- **Correção:** troca o `delay` único por um laço com tick de 1s que **só desconta enquanto
  `_uiState.value.isPlaying`** — o cronômetro conta tempo de reprodução, pausando junto com o
  áudio e retomando ao voltar a tocar. Ao zerar, pausa (mesma lógica Cast/local de antes).
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`setSleepTimer`).
- **Validado no device (moto g53), teste do usuário:** timer de 5 min + 3 ciclos de pausa/play; o
  logcat (log temporário, removido depois) mostrou `remaining` caindo 1s/s enquanto `playing=true`
  e **congelado** enquanto `playing=false`, retomando a cada play.
- **Esforço:** M · **device?** sim (comportamento de pausa validado ao vivo).

### ISSUE 5.D — ✅ FEITA (2026-07-15) — Progresso otimista de `fastForward`/`rewind` sem limite pela duração

- **Problema:** `fastForward()` somava 30s ao `currentPosition` do `_uiState` sem
  `coerceAtMost(duration)` (só o `rewind` fazia `coerceAtLeast(0)`). Perto do fim da faixa, a barra
  podia ultrapassar 100% por um instante até o loop de progresso (1s) corrigir. Só visual.
- **Correção:** o update otimista do `fastForward` agora faz `coerceAtMost(duration)` (guardado por
  `duration > 0`, para não clampar quando a duração ainda é desconhecida). `rewind` já estava OK.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`fastForward`).
- **Esforço:** P · **device?** não (clamp verificável por inspeção; sem regressão no smoke test).

---

## FASE 6 — Bugs funcionais & robustez (2ª varredura 2026-07-15)

Fora do core de playback (já sólido). Achados verificados nos arquivos reais. Ordem por criticidade.

### ISSUE 6.A — ↪️ REBAIXADA para código morto → ver ISSUE 7.E

- **Reclassificada em 2026-07-15.** O que parecia "feature de UI morta a ligar" é, na verdade,
  **scaffolding de uma Home antiga já substituído por soluções vivas** — não é bug pra corrigir,
  é código morto pra remover. Detalhe e plano de remoção estão na **ISSUE 7.E** (FASE 7).
- **Por quê:** as duas seções duplicam funcionalidade que já existe e funciona:
  - "Continuar Ouvindo" → o **mini player** já restaura a última sessão (pausada) no cold start
    via `PlayerViewModel.kt:262-283` (`getLatestPlaybackState()`), com o subtítulo caindo
    literalmente em `"Continuar Ouvindo"`; o player aparece sempre que `title` não é vazio
    (`MainScreen.kt:126`, `SharedPlayerScreen.kt:97`).
  - "Favoritos" → já há a **tela de Favoritos dedicada** (`ui/favorites/FavoritesViewModel.kt`,
    `getFavorites()` + `getFavoriteStudyLessons()`).

### ISSUE 6.B — ✅ FEITA (2026-07-15) — `ThemeDetails`/`StudyDetails` VMs vazam coletores a cada Retry

- **Problema:** `ThemeDetailsViewModel.loadMoments` e `StudyDetailsViewModel.loadStudyDetails`
  faziam `viewModelScope.launch { flow.collect {} }` sobre Flows de Room que nunca completam, e
  `handle(Retry)` re-chamava a função **sem cancelar** o job anterior. N toques em "Tentar novamente"
  (ou reentradas) acumulavam N coletores permanentes escrevendo no mesmo `_uiState` → leak de
  coroutines + corrida de escrita até `onCleared`.
- **Correção:** migrados para o padrão idiomático das demais VMs —
  `refreshTrigger(MutableStateFlow).flatMapLatest { <flows de Room> }.stateIn(WhileSubscribed(5s))`.
  O `flatMapLatest` **cancela o coletor anterior** a cada emissão do trigger, então `Retry`
  (`refreshTrigger.update{it+1}`) nunca acumula coletores. `onStart { emit(Loading) }` preserva o
  Loading a cada (re)carga; `catch { emit(Error) }` dentro do flatMapLatest isola erros por tentativa.
- **Arquivos:** `ui/themas/ThemeDetailsViewModel.kt`, `ui/studies/StudyDetailsViewModel.kt`.
- **Validado:** compila; smoke test no device (moto g53) — telas de detalhe de Tema
  ("Ansiedade e confiança em Deus") e de Estudo ("Estudos Expositivos em Apocalipse") carregam o
  Success corretamente. O fim do vazamento é garantido pela semântica do `flatMapLatest`.
- **Esforço:** P · **device?** não (era revisável por inspeção; smoke test confirmou o caminho feliz).

### ISSUE 6.C — ✅ FEITA (2026-07-15) — `extractDominantColorFromUrl` cria `ImageLoader` sem o User-Agent do WAF

- **Problema:** `ColorExtension.kt:18` fazia `val loader = ImageLoader(context)` — um loader novo,
  sem o header `User-Agent: "BibliaFaladaApp"` do singleton do `CoilModule.kt` e sem os caches
  compartilhados, instanciado **a cada** mudança de `imageUrl` (`MainScreen.kt:172`).
- **Correção:** troca por `context.imageLoader` (extensão do Coil), que devolve o **singleton** via
  o `ImageLoaderFactory` do `OuvindoBibliaApp` (UA do WAF + cache de memória/disco). Mudança mínima,
  sem tocar em `MainScreen` nem na assinatura da função.
- **Achado honesto no device (moto g53):** a premissa "cor cai sempre no `defaultColor`" **NÃO se
  confirmou** — o CDN de imagens não exige o UA, então a extração **já funcionava** com o loader
  simples. Capas diferentes já davam cores diferentes (Efésios→navy, Josué→laranja, Romanos→dourado).
  Portanto o ganho real da correção é: (1) **cache compartilhado** (não rebaixa a capa a cada abertura
  do player), (2) **sem novo `ImageLoader` por troca de capa** (memória/perf), (3) **hardening** do UA
  caso o CDN passe a exigir. Sem regressão: fundo do player continua refletindo a capa.
- **Arquivos:** `ui/theme/ColorExtension.kt`.
- **Validado no device:** full player de "Romanos Cap. 11" com fundo dourado/âmbar casando com a capa.
- **Esforço:** P · **device?** sim (confirmou: extração mantida, agora via singleton+cache).

### ISSUE 6.D — ✅ FEITA (2026-07-15) — Home presa em Loading eterno com sync bem-sucedido e lista vazia

- **Problema:** `HomeViewModel.toUiState` mapeava `Resource.Success` com `data.isEmpty()` para
  `HomeUiState.Loading`, e o Loading não tem Retry. Se o repo emitisse Success vazio (0 livros), a
  Home ficava em spinner infinito. Inconsistente com Themes (empty→Error) e Studies (empty→Empty).
- **Correção:** `Success(empty)` → `HomeUiState.Error("Nenhum livro disponível. Tente novamente.")`,
  que a `HomeScreen` (`:53`) renderiza com Retry. **Seguro:** `syncedListResource` só emite `Success`
  DEPOIS de o `sync()` concluir (o `sync` é `suspend` e é aguardado antes do `emitAll`), então não
  há "empty transitório" que causaria flash de erro — vazio aqui é estado terminal.
- **Arquivos:** `ui/home/HomeViewModel.kt`.
- **Validado:** smoke test no device — Home carrega o grid de livros normalmente (não cai no Error).
- **Esforço:** P · **device?** o caso vazio em si é difícil de reproduzir (0 livros); confirmado que
  não há regressão no caminho normal.

### ISSUE 6.E — ✅ FEITA (2026-07-15) — `syncMoreContent` sem version-gating (escrita redundante)

- **Problema:** diferente de `syncBibleData/syncThemes/syncStudies`, `syncMoreContent` ignorava o
  campo `version` e fazia fetch + `INSERT REPLACE` no Room a cada coleta de `getMoreContentResource()`,
  mesmo sem mudança. Ineficiência (não perde dados).
- **Correção:** version-gating via `dao.get()?.version` (a versão vive na própria `MoreContentEntity`):
  se `cached != null && cached.version == remote.version`, retorna sem reescrever. (Bônus: usa o
  `dao.get()`, que era listado como código morto na 7.D — sai da lista.)
- **Arquivos:** `data/repository/.../BibleRepositoryImpl.kt` (`syncMoreContent`).
- **Esforço:** P · **device?** não (lógica espelha os outros syncs; verificável por inspeção).

### ISSUE 6.F — ✅ FECHADA (2026-07-16) — NÃO é bug: nenhuma base pré-8 no mundo real

- **Problema (hipótese):** `DatabaseModule.kt:31-33` registra só `MIGRATION_8_9` e mantém apenas
  `fallbackToDestructiveMigrationOnDowngrade()` (o destrutivo geral foi removido na 0.2 para
  preservar favoritos/retomada). Base instalada em schema **< 8** lançaria `IllegalStateException`
  na 1ª abertura → crash em loop.
- **Investigação (2026-07-16):** rastreado o `version` no git deste repo — subiu 1→2→…→8→**9**
  (`d9facb0`→`f48381c`); só a v9 tem migração. A tag **`v1.0.0-rc1` (2026-02-08)** tinha
  `version = 2`. Confirmado com o usuário:
  1. **App antigo da loja (5 anos):** é OUTRA versão/codebase, sem Room `bible_db`. No update o
     Room cria o banco do zero na v9 → sem migração → **sem crash**.
  2. **Builds 1–7 deste repo (rc1 etc.):** rodaram **só no device de dev do usuário (1 aparelho)** —
     NÃO foram para Play nem testadores. Sem base pré-8 no mundo real.
  3. **Relançamento = 1º publish real:** todo mundo instala do zero → `bible_db` nasce na v9.
- **Veredito:** não é bug. O comportamento atual (MIGRATION_8_9 + lança-se-faltar-migração) é o
  desejado daqui pra frente (decisão da 0.2). **Nenhuma mudança de código.**
- **Cuidado operacional (não é código):** antes de instalar o relançamento no device de dev, o
  usuário deve **desinstalar/limpar dados** do app (senão crasha uma vez só nesse aparelho).
- **Blindagem opcional (dispensada):** `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6,7)` recriaria
  o banco só vindo de 1–7 e manteria o "lança" p/ 9→10+; não adotada por afetar só 1 device de dev.
- **Arquivos:** nenhum alterado.
- **Esforço:** P (investigação) · **Fechada sem código.**

### ISSUE 6.G — ✅ FEITA (2026-07-15) — Coração de favorito não atualiza corretamente ao tocar (mini e full player)

- **Bug conhecido (reportado pelo usuário):** tocar no coração executa a ação e **salva no banco**,
  mas o ícone não reflete o novo estado direito. Investigação no device revelou que eram **DOIS
  problemas**, ambos por confiar no metadata do controller como fonte de verdade do favorito:
  1. **Reversão do update otimista** — `syncStateWithController()` (`PlayerViewModel.kt:653`) rodava
     a cada evento do player e **re-derivava** `currentIsFavorite` do
     `currentMediaItem.mediaMetadata.extras["is_favorite"]` do controller, sobrescrevendo o update
     otimista do `toggleFavorite` (`:552`) com o valor antigo até o `replaceMediaItem`/Flow do Room
     convergirem → coração demorava a preencher.
  2. **Nunca desfavoritava (o mais grave)** — `toggleFavorite` lia o `oldStatus` do MESMO metadata
     do controller, que **não reflete os toggles anteriores** (o `replaceMediaItem` não "gruda" na
     releitura do controller — quirk de MediaController/serviço). `oldStatus` vinha **sempre false**
     → `newStatus` **sempre true** → cada tap só "favoritava", nunca desfavoritava. Confirmado por
     logcat no device (moto g53): taps consecutivos logavam `old=false new=true` repetido.
- **Onde a UI lê:** `SharedPlayerScreen.kt:96` liga o ícone a `uiState.currentIsFavorite` (mini
  `:452`, full `:192`). O full tem `enabled = controlsEnabled`; o mini não (irrelevante ao bug).
- **Correção (fonte única de verdade = DB, via `currentIsFavorite`):**
  1. `syncStateWithController` **não escreve mais** `currentIsFavorite` (`:653`+). Quem escreve:
     `toggleFavorite` (otimista), os observadores de DB (`observeCurrentFavorite`/
     `observeCurrentStudyFavorite`) e o handler de `EVENT_MEDIA_ITEM_TRANSITION` (valor imediato
     e correto no instante da troca de faixa, mantido em dia pelo observador de DB).
  2. `toggleFavorite` lê `oldStatus` de `_uiState.value.currentIsFavorite` (DB-backed), não mais do
     metadata do controller.
- **Validado no device (moto g53, Android 14):** favoritar preenche na hora (mini e full),
  persiste em "Meus Favoritos"; taps consecutivos no full player **alternam** corretamente
  (`old=true→false`, `old=false→true`), confirmado por logcat + screenshots + teste do próprio
  usuário. Logs de debug temporários removidos após validação.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (`toggleFavorite`, `syncStateWithController`,
  handler de transição). Nenhuma mudança de UI.
- **Nota de higiene (fora de escopo, achado colateral):** `tryBeginSourceSwitch` (`:194`) reseta só
  a flag interna `isSourceSwitchInFlight` no timeout de 4s, mas **não** reseta `isSwitchingSource`
  no uiState — se `finishSourceSwitch()` nunca disparar, os controles do full player (que usam
  `enabled = controlsEnabled = !isSwitchingSource`) ficam travados desabilitados. Não era a causa da
  6.G (default é false; no restore não é tocado), mas vale corrigir depois (candidato a nova issue).
- **Esforço:** P (real) · **device?** sim (foi essencial pra achar o 2º problema).

---

## FASE 7 — Código morto (2ª varredura 2026-07-15)

Candidatos a remoção confirmados por `grep` em `src/main` (0 refs vivas). **Antes de apagar,
reconfirmar incluindo `src/test`/`androidTest`** e o ROADMAP. Baixa prioridade (não afeta runtime),
mas paga juros de manutenção. Um único commit de limpeza por área é suficiente.

### ISSUE 7.A — ✅ FEITA (2026-07-16) — Clipping da Bíblia inalcançável + parâmetros propagados mortos

- Removidos os params `startMs`/`endMs` (sempre 0 nos call-sites vivos) de toda a cadeia da Bíblia:
  `onPlayBook` (`NavigationGraph`: tipo + 2 call-sites; `MainScreen`: lambda + chamada de
  `playBook`), `PlayerViewModel.playBook` (assinatura + `pendingPlayAction`) e
  `buildBibleMediaItems`. Este último perdeu também `targetChapterIndex` (só servia ao clipping) e
  o bloco `ClippingConfiguration` inteiro — a Bíblia não recorta (agora `map`, sem `setClippingConfiguration`).
- **Mantidos (vivos):** clipping de Tema (`playThemePlaylist`/`moment.startMs`), o `startMs`/`endMs`
  de `ThemeDetailsScreen`, e a guarda 2.D em `saveCurrentState` (comentário atualizado p/ refletir
  que agora protege só o recorte de Tema + futuros). Compila limpo (`:app:compileDebugKotlin`).

### ISSUE 7.B — ✅ FEITA (2026-07-16) — Campos/ações de player nunca lidos

- Removidos de `PlayerUiState`: `repeatMode` (só a declaração, 0 uso), `artist` (write-only) e
  `isShuffleEnabled` (write-only). Removidos de `PlayerViewModel`: a função `toggleShuffle()`
  (0 chamadas) e as escritas de `artist`/`isShuffleEnabled` no `syncStateWithController`. Shuffle
  era feature inteiramente morta (nenhuma leitura na UI). Compila limpo (`:app:compileDebugKotlin`).

### ISSUE 7.C — ✅ FEITA (2026-07-16) — Destinos de navegação órfãos

- Removidos `Screen.Player`, `Screen.About`, `Screen.Copyright` de `AppNavigation.kt` — confirmado
  0 refs em `app/src` (`Screen.Player`/`.About`/`.Copyright` = 0; `composable<>` = 0). O player é
  overlay em `MainScreen`, não destino. Compila limpo (`:app:compileDebugKotlin` BUILD SUCCESSFUL).

### ISSUE 7.D — ✅ FEITA (2026-07-16) — Repositório/DAO/DTO não usados

Confirmado 0 refs em TODOS os módulos (main+test+androidTest; nenhum teste referencia). Build
completo limpo com regeneração do Room (KSP `data:local`). **Removidos:**
- **Repo (interface + impl):** `getBook(bookId)` (morto + bug latente do slug) e
  `getBookIdFromChapter` (o vivo é `getBookNumericIdFromChapter`). Com `getBook` fora, o DAO
  `getBookById` ficou órfão → removido também.
- **DAO (`BibleDao.kt`):** `getChaptersForBook`, `getBookById`, `getChapterWithBookInfoById`
  (JOIN inválido), `updateChapterMetadata`, `insertBooks`, `insertChaptersIgnore`, `clearBooks`,
  `clearChapters`, `insertStudies`, `insertStudyLessons`, `getStudies()`, `clearStudies`,
  `clearStudyLessons`. **Caminho vivo de sync preservado** (`refreshBibleData`/`refreshThemesData`/
  `refreshStudiesData` usam os `*Ignore` singulares + `update*Metadata`). `get()`/`observe()`/`save()`/
  `clear()` de `more_content` mantidos (vivos, 6.E). `api.getStudies()` remoto é outro símbolo (vivo).
- **DTO:** arquivo `PlaybackStateDto.kt` apagado (classe inteira sem refs).
- **Intents no-op:** `HomeIntent.OpenBook`, `ThemesIntent.SelectTheme`, `StudiesIntent.SelectStudy`
  removidos dos contracts + os branches no-op nos 3 `handle()` (navegação é por callback nas Screens).

### ISSUE 7.E — ✅ FEITA (2026-07-16) — Seções mortas da Home ("Continuar Ouvindo" / "Favoritos") (ex-6.A)

**Removido (compila limpo, `:app:compileDebugKotlin` BUILD SUCCESSFUL):**
- `HomeContract.kt` — campos `continueListeningBook` e `favoriteBooks` de `HomeUiState.Success`.
- `HomeScreen.kt` — os dois blocos `item {}` mortos + imports órfãos (`Column`, `LazyRow`,
  `lazy.items`, `ContinueListeningCard`, `FavoriteBookItem`, `SectionHeader`).
- `HomeComponents.kt` — composables `ContinueListeningCard`/`FavoriteBookItem` (vivos e as versões
  antigas comentadas) + 11 imports órfãos. **`SectionHeader` mantido** (usado por
  `MoreSectionDetailsScreen.kt`).
- Grep pós-remoção: 0 refs aos símbolos removidos em `src/main`/`test`/`androidTest`.
- Preservados: mini player (restaura sessão), tela `ui/favorites/`, `getFavorites()`/
  `getLatestPlaybackState()` no repositório.

<!-- detalhamento original abaixo -->


- **Origem:** rebaixada da 6.A (era classificada como bug). Não é bug: são duas seções de uma Home
  antiga que **nunca são populadas** e cuja função já é coberta por soluções vivas — logo, remover.
- **Confirmação em runtime:** `HomeViewModel.toUiState` (`HomeViewModel.kt:82-85`) monta o `Success`
  só com `filteredBooks`+`selectedFilter`; os campos `continueListeningBook`/`favoriteBooks` ficam
  no default → os blocos condicionais da `HomeScreen` (`:102` e `:114`) **nunca renderizam**.
- **Por que é redundante (não ligar):**
  - "Continuar Ouvindo" já é o **mini player** restaurando a sessão no cold start
    (`PlayerViewModel.kt:262-283`; subtítulo default `"Continuar Ouvindo"`).
  - "Favoritos" já tem **tela dedicada** (`ui/favorites/`).
- **Remoção (fazer numa passada só, verificar compilação a cada arquivo):**
  1. `HomeContract.kt:8-9` — remover os campos `continueListeningBook` e `favoriteBooks` de
     `HomeUiState.Success`.
  2. `HomeScreen.kt:102-140` — remover os dois blocos `item { … }` ("Continuar Ouvindo" e a
     `LazyRow` de "Favoritos") e os `import` de `ContinueListeningCard`/`FavoriteBookItem`
     (`:33`,`:35`).
  3. `HomeComponents.kt` — remover os composables `ContinueListeningCard` (`:137`) e
     `FavoriteBookItem` (`:233`) **e** as versões antigas comentadas (`:80`, `:176`). Conferir se
     `SectionHeader` continua usado por outra seção antes de mexer nele.
  4. `HomeViewModel.kt` — nada a mudar (já não referencia os campos); confirmar que nenhum outro
     ponto lê os campos removidos (grep `continueListeningBook`/`favoriteBooks` = 0 fora dos acima).
- **Não confundir:** manter intactos o mini player, a tela de Favoritos e `getFavorites()`/
  `getLatestPlaybackState()` no repositório (usados por PlaybackService e PlayerViewModel).
- **Critério de aceitação:** app compila; Home renderiza header + filtro + grid de livros sem os
  buracos; nenhum símbolo removido referenciado em `src/main`.
- **Esforço:** P · **Depende de:** nada. · **device?** não (remoção mecânica; smoke test da Home basta).

---

## FASE 8 — Publicação na Play Store (produção) — consolidada das 4 auditorias (2026-07-16)

Origem: `docs/archive/AUDITORIA_01..04_*.md` + `docs/archive/CHECKLIST_PUBLICACAO.md`. Issues
autocontidas com ids **PUB-XX**. Etiquetas: 🤖 **CÓDIGO** (Claude Code resolve) · 🧑 **MANUAL**
(dono no Play Console/docs) · 🔬 **TESTE-DEVICE**. Severidade: 🔴 BLOQUEIA · 🟡 CORRIGIR ANTES ·
🟢 PODE ESPERAR. `arquivo:linha` reconfirmados em 2026-07-16 (reconfirmar ao pegar cada uma).

**Ordem:** `PUB-01` → `PUB-02/03/04` → `PUB-10..16` (testes no release). Em paralelo desde já:
`PUB-20..25` (Console/manual, não dependem de código).

### 🔴 BLOQUEIA — Código/Build
- **PUB-01 · Assinar o release (signingConfig)** · 🤖+🧑 · ✅ FEITA (2026-07-16) · *(Audit 01 §2)* —
  `signingConfigs.release` lê `keystore.properties` (fora do git); dono criou a upload keystore
  (`ag.uny.ouvindoabiblia`, alias `ouvindoabiblia`). **`assembleRelease` gera APK assinado** —
  `apksigner` confirma SHA-256 `842d3a33…` (esperado `84:2D:3A:33:…`), esquema v2. `bundleRelease`
  usa o mesmo signingConfig → AAB sairá assinado igual. Instalado no device (vc4/vn3.0).

### 🟡 CORRIGIR ANTES — Código
- **PUB-02 · Erro de playback silencioso** · 🤖 · ✅ FEITA (2026-07-16) · *(Audit 02 §3)* —
  `PlayerViewModel.kt:620` `EVENT_PLAYER_ERROR` agora seta `playbackError` no `PlayerUiState` (campo
  novo); `SharedPlayerScreen` mostra um Toast (consume-once via `consumePlaybackError()`, ligado no
  `MainScreen`). Mensagem genérica pt-BR (não expõe stacktrace). `finishSourceSwitch()` preservado →
  controles não travam. Compila limpo. 🔬 validar em device no PUB-12.
- **PUB-03 · Desligar OkHttp `Level.BODY` no release** · 🤖 · ✅ FEITA (2026-07-16) ·
  *(Audit 01 §4 / 03 §5)* — `NetworkModule.kt`: `provideOkHttpClient` agora recebe
  `@ApplicationContext` e gateia por `FLAG_DEBUGGABLE` → `Level.BODY` em debug, `Level.NONE` em
  release (mesmo critério do `OuvindoBibliaApp`, independe de BuildConfig por módulo). Compila limpo.
- **PUB-04 · Loading eterno na tela "Mais"** · 🤖 · ✅ FEITA (2026-07-16) · *(Audit 02 §1)* —
  `BibleRepositoryImpl.kt` `getMoreContentResource`: o ramo `content==null` + sync OK virou
  `Resource.Error("Nenhum conteúdo disponível. Tente novamente.")` (era `Resource.Loading`).
  `MoreScreen` já mapeia Error→ErrorScreen+Retry. Paridade com a 6.D. Compila limpo.

### 🟡 CORRIGIR ANTES — Testes no APK de RELEASE assinado · 🔬 (dependem de PUB-01)
- **PUB-10 · Smoke do release** *(Audit 01 §3)* — ✅ FEITA (2026-07-16, ver `docs/archive/SMOKE_TEST_02_RELEASE.md`):
  APK R8 assinado instalado (vc4); cold start sem crash; **as 4 sincronizações (Bíblia/Temas/Estudos/Mais)
  parseiam sem erro de serialização/R8**; playback das 3 fontes PLAYING (som confirmado pelo usuário);
  persistência 5.1 (force-stop→restore PAUSED). PUB-14/PUB-15 cobertos no release; restam PUB-11/12
  (feitos no debug), PUB-13, PUB-16.
- **PUB-11 · Falha de rede** *(Audit 02 T1)* — 1º uso offline → Error+Retry nas 4 telas; queda no meio não crasha.
- **PUB-12 · Erro de playback** *(Audit 02 T2)* — 404/rede fora → sem crash; validar feedback do PUB-02.
- **PUB-13 · POST_NOTIFICATIONS negado (Android 13+)** *(Audit 02 T3)* — negar → áudio toca; conceder → notificação com controles.
- **PUB-14 · Persistência sob estresse** *(Audit 02 T4)* — rotação/bg-fg/matar processo → restaura sessão sem auto-tocar; validar R8. ✅ smoke DEBUG OK (background+FGS; force-stop→reabrir restaura PAUSED).
  - **BUGFIX achado aqui (commit `ef96f85`, follow-up 6.G):** favorito do item RESTAURADO não refletia
    no mini/full player (salvo no DB, coração vazio). O observer de favorito só ligava no
    `EVENT_MEDIA_ITEM_TRANSITION`, que não dispara no cold-start restore. Fix:
    `observeFavoriteForCurrentItem()` também no connect (`initializeController`). Validado no device.
- **Smoke DEBUG (2026-07-16):** A/B/C/E ✅ (usuário confirmou som+aparência). D2 (ErrorScreen) e D3
  (Toast PUB-02) ✅ testados manualmente pelo usuário. Restam no APK de release: PUB-10..16 + Android 8/Auto.
- **PUB-15 · StrictMode / ANR** *(Audit 02 T5)* — debug + `adb logcat | grep StrictMode`; sem I/O na main; sem ANR.
- **PUB-16 · Android 8 (API 26)** *(Audit 02 T6)* — smoke em aparelho antigo: notificação+FGS sem `NoSuchMethodError`/`VerifyError`.

### 🔴 BLOQUEIA — Console / Documentos · 🧑 MANUAL (estado real confirmado pelo dono em 2026-07-18)
- **PUB-20 · Política de Privacidade** · ✅ FEITA (2026-07-18) — hospedada em
  `https://ouvindo-a-biblia.ide.app.br/politica.html` e informada no Console.
- **PUB-21 · Data Safety form** · ✅ FEITA (2026-07-18) — "nenhuma coleta de dados", criptografado em trânsito.
  *(Era o que derrubou o app em mai/2024 — resolvido.)*
- **PUB-22 · Content Rating (IARC)** · ✅ FEITA (2026-07-18) — classificação "Livre"/L (herdada).
- **PUB-23 · Declaração de Foreground Service** · 🔶 VÍDEO GRAVADO (2026-07-18) — o dono gravou
  manualmente (com som e tela apagada; a via `adb screenrecord` foi tentada e descartada: não
  capta áudio e para quando a tela apaga). Falta: enviar descrição + caso de uso + o vídeo no
  Console. Código pronto.
- **PUB-24 · Assets da ficha** · ✅ FEITA (2026-07-18; textos + imagens no Console) — EXCETO o screenshot
  `04_estudos`, bloqueado pelos placeholders de Estudos (ver 🟡 abaixo).
- **PUB-25 · Declarar sem login/compras/anúncios** · ✅ FEITA (2026-07-18) — declarado no Console.
- **Conteúdo do app (Console):** ✅ FEITA (2026-07-18) — as 6 declarações concluídas ("Tudo em dia").

### 🔴 BLOQUEIA — "Novela da chave" (Play App Signing, app legado) · 🧑 · ⏳ PENDENTE
- App legado sem App Signing + keystore antiga **1024-bit** (2013), obsoleta. Na tela
  "Assinatura de apps" do Console, caminho: **"usar nova chave (versões duplas)"** — exige o
  AAB novo (chave 2048-bit, PUB-01 ✅) + um **APK assinado com a chave LEGADA de 2013**
  (o dono tem as duas). É o passo mais incerto da publicação. **Plano B aceito:** app novo.

### 🟡 CORRIGIR ANTES — Conteúdo no servidor · 🧑 · 🔶 QUASE
- **Placeholders de Estudos:** ✅ RESOLVIDO no servidor (confirmado no device em 2026-07-18: capas
  reais de "Estudos Expositivos em Apocalipse" e "Quarentena em Jó" no `estudos.json`).
  Falta: **recapturar o screenshot `04_estudos`** e subir na ficha (sugestão: depois do bump de
  `meta.version` da 9.A, para a captura já sair com as descrições das aulas).

### 🟢 PODE ESPERAR — backlog pós-launch (não bloqueia)
- **PUB-30** 🤖 Stripar `Log` no release (`-assumenosideeffects`) / rebaixar `Log.i` de lifecycle. *(Audit 03 §5)*
- **PUB-31** 🤖 Acento "Ouvindo A Biblia" → "Bíblia" (`app_name`). *(Audit 01 §8)*
- **PUB-32** 🤖 77 warnings de lint (typos, bumps, ícone). *(Audit 03 §6)*
- **PUB-33** 🤖 `Player` fora do padrão MVI (consistência). *(Audit 03 §3)*
- **PUB-34** 🤖 i18n: strings de UI → `strings.xml` (só se internacionalizar). *(Audit 03 §7)*
- **PUB-35** 🤖 "Optimize Imports" antes do tag de release. *(Audit 03 §1)*
- **Operacional:** incrementar `versionCode` a cada upload. *(Audit 01 §5)*

**Go/No-Go (estado real 2026-07-18):** código ✅ pronto e assinado; formulários do Console ✅
feitos. O que separa de publicar: (1) **enviar a declaração FGS + vídeo no Console** (PUB-23;
vídeo já gravado manualmente em 2026-07-18), (2) **novela da chave**
(Play App Signing legado), (3) **placeholders de Estudos** no servidor + recaptura do
`04_estudos`, (4) repassar no release assinado os testes **PUB-11/12/13/16** (11/12/13 já
validados no debug). Painel completo em `docs/archive/CHECKLIST_PUBLICACAO.md`.

---

## FASE 9 — Features de conteúdo (aberta 2026-07-18)

### ISSUE 9.A — ✅ FEITA (2026-07-18, commit `1af30dd`) — Descrição por AULA de estudo

- **Resultado:** cadeia completa implementada conforme o plano abaixo (DTO nullable → entity +
  Room 9→10 c/ `MIGRATION_9_10` → `updateStudyLessonMetadata` → domínio/mapper → `LessonListItem`
  recolhida em 2 linhas + expand no toque; null/blank não renderiza). Testes: `StudyDtoParseTest`,
  `MigrationTest` 9→10, `StudyLessonRefreshTest` (update preenche description preservando favorito),
  `VisibleLessonDescriptionTest`. Instrumentados verdes no moto g53; release instalado por cima
  migrou 9→10 sobre dados reais sem crash.
- **⏳ Validação visual pendente do GATE:** o device já tinha sincronizado a `0.0.21` com o app
  antigo → o version-gate pula o sync e as descrições ficam NULL. Falta o dono **bumpar o
  `meta.version` do `estudos.json`** (ex.: 0.0.22); na visita seguinte à aba Estudos o update
  preenche e a UI mostra (caminho provado por teste).
- **Nota de política (dono, 2026-07-18):** enquanto o app não estiver na loja/testadores, novos
  bumps de schema NÃO precisam de migração (o device de dev limpa dados). A 9→10 fica porque já
  estava pronta/validada e sustenta o `MigrationTest`.

### ISSUE 9.B — ✅ FEITA (2026-07-18, commit `4bf2c9c`) — Capa 1:1 no player para Estudos

- **Pedido do dono:** no full player, Estudo deve exibir a capa em 1:1 (arte quadrada), não 7:10.
- **Feito:** `PlayerUiState.isStudyMode` derivado do `MediaContentId` (helper puro
  `isStudyMediaId`, mesmo padrão do `isThemeMode`), escrito em `syncStateWithController`, no
  restore do cold start (mini restaurado já nasce com a forma certa) e nos 3 plays otimistas.
  Geometria da capa em `SharedPlayerScreen` com `coverAspect` **animado**
  (`animateFloatAsState`: troca de fonte faz morph, não salto) e **largura compensada** para
  área visual constante (quadrada ≈78% da tela, retrato 65%); o mini player acompanha
  (56dp quadrado) e o conteúdo abaixo reposiciona sozinho (já derivava de `fullHeight`).
- **Validação:** `IsStudyMediaIdTest`; no release (moto g53): Estudo 1:1 inteiro (sem crop
  lateral), Bíblia mantém 7:10. **Esforço:** P.

### ISSUE 9.C — ✅ FEITA (2026-07-18, commit `29831dd`) — Seta "replay" → pause/retoma (padrão de mercado)

- **Decisão do dono:** seguir o padrão de mercado — item tocando mostra ícone de pause; toque
  pausa/retoma. **Feito:** `LessonListItem` com `isCurrent` (borda/realce) separado de
  `isPlaying` (Pause tocando / Play pausado); ramo same-study do `playStudyPlaylist` faz toggle
  na mesma aula (e mantém o seek p/ aula diferente). Robustez: `finishSourceSwitch()` no
  early-return + timeout do `tryBeginSourceSwitch` limpa também `uiState.isSwitchingSource`
  (fecha o colateral da 6.G — nenhum comando no-op congela mais a UI).
- **Validado no release (moto g53):** toggle pausa sem recarregar (posição preservada, retomou
  de 2.9s), troca para outra aula segue com seek+play, controles nunca travam. De quebra,
  validadas ao vivo as descrições da 9.A (dono bumpou o `meta.version` do estudos.json).
- **↪️ Aplicada ao contexto de TEMA (2026-07-18, pedido do dono):** `MomentListItem` tinha a
  mesma seta Replay, com agravante — `playThemePlaylist` NÃO tinha proteção same-theme, então
  o toque no momento atual RECARREGAVA a playlist e reiniciava do zero. Fix espelhado: ícone
  Pause/Play por `isCurrent`/`isPlaying`, ramo same-theme com toggle (mesmo momento) e seek
  (outro momento, clipping preservado) + `finishSourceSwitch`. Validado no release: pausa
  preserva posição (4.1s), retoma de onde parou (4.1→7.0s).

### ISSUE 9.D — ✅ FEITA (2026-07-18, commit `e4a661e`) — Barra de progresso SÓ-LEITURA no mini player

- **Resultado:** linha de 2dp rente à borda inferior do mini bar, `Box` com
  `fillMaxWidth(fraction)` (sem Slider, sem gesto); `miniProgressFraction` pura com a guarda da
  1.B (`duration<=0` → 0f) + `MiniProgressFractionTest`; some via `miniAlpha`, pontas clipadas
  pelo corner 16dp. Validada no release (linha na fração correta ~68% da posição real, ao vivo).

<!-- plano original abaixo -->
### (plano original) ISSUE 9.D — Barra de progresso SÓ-LEITURA no mini player

- **Pedido do dono (2026-07-18):** mini player deve mostrar o progresso da faixa; **não pode ser
  ajustável** pelo usuário (seek só no full player).
- **Padrão de mercado:** linha fina (~2dp) rente à borda INFERIOR da barra do mini player
  (Spotify/YT Music). Ler `uiState.currentPosition/duration`.
- **Como:** `LinearProgressIndicator` (ou `Box` com `fillMaxWidth(fraction)`) — NUNCA `Slider`
  (Slider é interativo por natureza; um Slider "disabled" fica com cara de quebrado). Alinhar no
  fundo do mini bar (`SharedPlayerScreen`, bloco `--- MINI PLAYER ---`, Row de 64dp), respeitando
  o clip do `RoundedCornerShape(16dp)` do container (`MainScreen.kt` Surface). Cor: branco/
  conteúdo com alpha sobre a `animatedArtworkColor` (contraste em capa clara E escura). Sumir
  junto com o mini (`alpha = miniAlpha`); no morph p/ full player não deve "vazar".
- **Guarda de duração:** reaproveitar a semântica da 1.B — `duration <= 0` → fração 0f (barra
  vazia), NUNCA 100% falso no cold start. Extrair função pura `miniProgressFraction(pos, dur)`
  (mesmo padrão de `sliderProgressValueMs`) + teste unitário.
- **Critério de aceitação:** progresso visível e atualizando no mini (tocando e pausado);
  nenhum gesto no mini faz seek; cold start com duração desconhecida = barra vazia;
  transição mini↔full sem artefato visual.
- **Esforço:** P · **device?** sim (visual + cold start).

### ISSUE 9.E — ✅ FEITA (2026-07-18, commit `e4a661e`) — Corners concêntricos da capa no mini

- **Resultado (v2, commit `c3ef61d`):** folga uniforme de 4dp e raio da capa 12dp (16−4);
  `imageCorner` virou constante 12dp (full já usava 12dp); padding do texto ajustado.
  **⚠️ Correção da v1 (pega pelo olho do dono):** `imageStartX` é relativo ao CONTEÚDO da
  barra (o Surface já entra 8dp da tela) — a v1 usou 12dp e deixou ~10-12dp à esquerda vs 4dp
  vertical ("capa muito distante"). Medido por scanline de pixels no device; v2 = 4dp.
  Validada no release: estudo (quadrada) e Bíblia (retrato) com ~4dp nos 4 lados, cantos
  concêntricos.

<!-- plano original abaixo -->
### (plano original) ISSUE 9.E — Corners concêntricos da capa no mini player (capa de Estudo)

- **Pedido/pergunta do dono (2026-07-18):** capa do estudo no mini com corner na "mesma
  proporção" do mini player e aproximada da esquerda, para os ângulos ficarem simétricos.
- **Avaliação: SIM, faz sentido — é a regra de "concentric corners"** (raio interno = raio
  externo − folga; Apple HIG). Hoje ela é violada: barra com raio **16dp** (`MainScreen.kt`,
  `animatedCorner`), capa com raio **4dp** (`imageCorner` em `SharedPlayerScreen.kt:146`) e
  folgas **assimétricas** — 8dp à esquerda (imageStartX=16dp − 8dp de side padding da barra)
  vs 4dp em cima/embaixo (`imageStartY=4dp`, barra 64dp vs capa 56dp). Com a capa QUADRADA de
  Estudo (9.B) preenchendo a altura da barra, o "encaixe torto" fica evidente.
- **Correção (números fechados):** folga uniforme de **4dp** → `imageStartX` do mini = 12dp
  (8dp da barra + 4dp de folga) e raio da capa no mini = **12dp** (16 − 4). Vale para Estudo
  (quadrada) E Bíblia/Tema (retrato 0.7 — a regra é a mesma, o thumb só é mais estreito).
- **Cuidados (a capa é o elemento COMPARTILHADO do morph mini↔full):**
  1. `imageStartX` é o ponto de partida da animação — mudar 16→12dp exige ajustar o padding do
     texto do mini (`start = 16.dp + miniWidth + 12.dp` → `12.dp + miniWidth + 12.dp`).
  2. `imageCorner` é `lerp(4.dp → 12.dp)`; vira `lerp(12.dp → 12.dp)` (constante) ou mantém
     lerp se o full mudar de raio — conferir o visual do full (12dp lá já é o valor atual).
  3. Conferir sombra (`imageShadow`) com a folga menor (não "vazar" além da barra).
- **Critério de aceitação:** no mini, folga visual uniforme (4dp) nos 4 lados da capa de Estudo
  e cantos visivelmente concêntricos com os da barra; Bíblia/Tema sem regressão; morph
  mini↔full contínuo (sem salto no ponto de partida).
- **Esforço:** P · **device?** sim (é polimento visual — screenshot antes/depois).
- **Sinergia:** fazer junto com a 9.D (mesmo bloco de código, 1 validação visual só).

### ISSUE 9.F — ✅ FEITA (2026-07-18) — Capas de livro 1:1 na tela Mais → "Direitos das capas e imagens"

- **Resultado:** `MoreSheetInfoBlock` ganhou `imageAspect` (default 1f — pessoas/fontes
  inalteradas); call-site do `ASSET_LIST` passa 0.7f → thumb 44.8×64dp, capa inteira em
  retrato. Validado no release (moto g53): sheet mostra Gênesis/Êxodo/Levítico/Deuteronômio/
  Josué sem corte lateral, proporção medida ≈0.7.

<!-- diagnóstico original abaixo -->
### (diagnóstico original) ISSUE 9.F — BUG simples — Capas de livro 1:1 na tela Mais

- **Reportado pelo dono (2026-07-18):** na sheet "Direitos das capas e imagens" (tela Mais), as
  miniaturas das capas dos livros aparecem **1:1**, mas capa de livro é **retrato** — o crop
  quadrado corta as laterais (confirmado no device: Gênesis/Êxodo/Levítico cortados).
- **Onde (renderer VIVO — atenção):** o item do menu abre um **ModalBottomSheet**
  (`MoreScreen.kt:113`), não a rota `MoreSectionDetailsScreen` — o thumb é o
  `MoreSheetInfoBlock` (`MoreScreen.kt:316`): `Box .size(64.dp) .clip(RoundedCornerShape(14.dp))`
  + `AppAsyncImage fillMaxSize` (crop). (O `AssetCard` de `MoreSectionDetailsScreen.kt` NÃO é o
  caminho usado por esse fluxo; não corrigir lá achando que resolveu.)
- **Cuidado:** `MoreSheetInfoBlock` é compartilhado por 3 tipos — `ASSET_LIST` (capas, retrato),
  `RIGHTS_LIST` (fontes) e `PEOPLE_LIST` (pessoas — 1:1 é o CERTO para foto de pessoa). A
  correção deve parametrizar, não trocar global.
- **Correção sugerida:** param `imageAspect: Float = 1f` no `MoreSheetInfoBlock`
  (largura = 64.dp × aspect, altura fixa 64.dp); o call-site do `ASSET_LIST`
  (`MoreScreen.kt:~250`) passa **0.7f** (mesma proporção de capa do Home/mini player).
  Pessoas/fontes ficam 1:1 como hoje. Corner 14dp mantém (thumb pequeno, concentricidade
  não se aplica — não há moldura externa encostada).
- **Critério de aceitação:** na sheet de direitos, capas aparecem inteiras em retrato
  (~45×64dp) sem corte lateral; seções de pessoas/curadoria/fontes inalteradas (1:1).
- **Esforço:** P · **device?** sim (screenshot da sheet antes/depois).

<!-- diagnóstico original abaixo -->
### (diagnóstico original) ISSUE 9.C — BUG — Seta "replay" na aula tocando não faz nada e TRAVA o player em "carregando"

- **Reportado pelo dono (2026-07-18, visto no device):** com uma aula de Estudo tocando, o item
  dela na lista ganha borda e o ícone vira uma seta de "voltar ao início" (`Icons.Default.Replay`,
  contentDescription "Reiniciar Estudo", `StudyDetailsScreen.LessonListItem`). Ao tocar na seta:
  o áudio **não** volta ao início e o mini/full player fica **preso em estado de carregando**
  (controles desabilitados).
- **Causa raiz (confirmada por leitura do código, cadeia completa):**
  1. O toque chama `onPlayStudy` → `MainScreen` → `PlayerViewModel.playStudyById`, que faz
     `tryBeginSourceSwitch()` **antes de tudo** → `uiState.isSwitchingSource = true` (UI
     congela: `controlsEnabled = !isSwitchingSource`).
  2. `playStudyPlaylist` cai na "PROTEÇÃO CONTRA RESTART": mesmo estudo + mesma aula + já
     tocando → `play()` é no-op e **`return`** — o replay nunca foi implementado (só há
     `seekToDefaultPosition` quando a aula é OUTRA). Por isso o áudio não volta.
  3. **Ninguém chama `finishSourceSwitch()`** nesse caminho: o unlock normal depende de
     `EVENT_PLAYBACK_STATE_CHANGED` (READY/IDLE/ENDED), que não dispara porque nada mudou; e o
     timeout de 4s de `tryBeginSourceSwitch` reseta só o flag interno `isSourceSwitchInFlight`,
     **não** o `uiState.isSwitchingSource` — exatamente o achado colateral da 6.G. UI presa até
     trocar de faixa/matar o app.
- **Avaliação de UX (pergunta do dono: "faz sentido essa seta?"):** **não faz.** Nenhum player
  de referência (Spotify/podcasts) oferece "reiniciar do zero" na lista — o affordance padrão
  para o item tocando é um **indicador de reprodução** (equalizer animado) e/ou toque =
  **pausa/retoma**. Reiniciar do zero é ação destrutiva (perde a posição de uma aula de ~1h40)
  a um toque de distância, e aqui nem sequer funciona.
- **Correção recomendada (2 partes):**
  1. **UX:** trocar `Replay` por indicador de "tocando" (ícone `Pause`/equalizer); toque no item
     tocando = pausa/retoma (via toggle no controller). Elimina a promessa falsa de replay.
  2. **Robustez (o achado 6.G junto):** no early-return da "PROTEÇÃO CONTRA RESTART" chamar
     `finishSourceSwitch()`; e no timeout do `tryBeginSourceSwitch` resetar TAMBÉM
     `uiState.isSwitchingSource` (hoje só reseta o flag interno). Assim qualquer comando que
     vire no-op nunca mais deixa a UI presa.
- **Arquivos:** `ui/studies/StudyDetailsScreen.kt` (`LessonListItem`, ícone/onClick),
  `ui/player/PlayerViewModel.kt` (`playStudyById`, `playStudyPlaylist` ramo same-study,
  `tryBeginSourceSwitch`/`finishSourceSwitch`). Reconfirmar linhas ao pegar.
- **Critério de aceitação:** tocar no item da aula em reprodução pausa/retoma (sem recarregar);
  nenhum caminho deixa `isSwitchingSource` preso (timeout limpa a UI); troca para OUTRA aula
  segue funcionando (seek); Bíblia/Tema sem regressão.
- **Esforço:** P/M · **device?** sim (reproduzir o travamento antes, confirmar destravado depois).

<!-- plano original da 9.A abaixo -->
### (plano original) ISSUE 9.A — Descrição por AULA de estudo (novo campo `description` no `estudos.json`)

- **Contexto:** o servidor passou a mandar `description` dentro de cada item de `audios[]` no
  `estudos.json` (texto longo, ex.: "Aula introdutória que estabelece os fundamentos…"). Hoje o
  app só tem descrição no nível do **estudo** (`StudyDto.description` → `Study.description`,
  exibida no header de `StudyDetailsScreen.kt:335`); no nível da **aula** o campo é ignorado no
  parse e não existe em lugar nenhum da cadeia.
- **Cadeia a tocar (na ordem):**
  1. **`:data:remote`** — `StudyDto.kt`: `StudyAudioDto` ganha `val description: String? = null`
     (**nullable com default** — JSON antigo/aulas sem o campo continuam parseando; kotlinx-ser
     só exige o default).
  2. **`:data:local`** — `StudyLessonEntity` ganha `val description: String? = null` →
     **Room `version` 9→10** em `BibleDatabase.kt:28` + `MIGRATION_9_10`
     (`ALTER TABLE study_lessons ADD COLUMN description TEXT`) registrada no
     `DatabaseModule.kt:31` junto da `MIGRATION_8_9`. **NÃO usar destrutivo** — regra da 0.2
     (preserva favoritos/retomada). Lembrar: KSP pode pedir `./gradlew clean`.
  3. **`:data:local`** — `BibleDao.kt`: `updateStudyLessonMetadata` (`:326`) ganha o param
     `description` e o SET correspondente; `refreshStudiesData` (`:251-254`) repassa. O caminho
     de INSERT (aula nova) já cobre via entity. Sem isso, quem JÁ tem as aulas no Room nunca
     recebe as descrições (o insert é IGNORE).
  4. **`:data:repository`** — `domain/model/StudyModels.kt`: `Lesson` ganha
     `description: String?`; `StudyMappers.kt` (`toDomain`) mapeia. Conferir o ponto do
     `BibleRepositoryImpl.syncStudies` que converte DTO→entity (incluir o campo).
  5. **`:app`** — `StudyDetailsScreen.kt`: exibir a descrição no item da aula. Sugestão de UX
     (texto longo): 2–3 linhas com ellipsis + expandir no toque (padrão `maxLines` +
     `animateContentSize`), corpo em `bodySmall`/`onSurfaceVariant`. Aula sem descrição
     (null/blank) não reserva espaço.
- **⚠️ Gate operacional (dono):** o sync de Estudos é **version-gated** — as descrições só
  entram no Room quando `meta.version` do `estudos.json` for **bumpada**. Sem bump, nada muda
  no app mesmo com o código pronto.
- **Fora do escopo (anotar se quiser depois):** mostrar a descrição da aula no player
  (`ChaptersSheet`/full player) e na tela de Favoritos.
- **Critério de aceitação:** JSON novo parseia (com e sem `description`); migração 9→10 preserva
  favoritos/retomada (teste espelhando o da 0.2); update de metadata grava descrição em aula já
  existente após bump de version; `StudyDetailsScreen` mostra/expande a descrição; aula sem
  descrição renderiza como hoje. Release: campo novo não tem superfície de R8 (serializer gerado
  pelo plugin), mas validar o parse no smoke de release.
- **Testes:** DTO parse (com/sem campo), mapper, `MigrationTest` 9→10, unit do
  “null/blank não renderiza”.
- **Esforço:** M · **Depende de:** bump de `meta.version` no servidor (dono) para validar ponta a ponta.

---

## ❌ FORA DE ESCOPO desta versão — Cast (desligado via kill-switch; reativar no futuro)

- Desligado em 2026-07-14 via `CastConfig.ENABLED=false` (código dormente no repo).
  Reativar: flip do flag + descomentar `CastButton` em `SharedPlayerScreen`.
- Backlog quando religar: §6.1 posição não volta Cast→local · §6.2 sem fila (auto-avanço) ·
  §6.3 metadados errados p/ Estudo-Tema · §6.4 alvo divergente na transição. Exigem
  device + Chromecast para validar.
- **Confirmado na varredura 2026-07-15 (código dormente, latente):**
  - §6.2 — `checkCastCompletion()` (`PlayerViewModel.kt:790`) chama `skipToNextChapter()`, que
    opera no **controller local** e não faz `loadMediaOnCast` → o próximo capítulo não carrega no
    dispositivo Cast quando a faixa remota termina.
  - §6.4 — `onChapterSelected()` (`PlayerViewModel.kt:513`) sempre chama `mediaController.seekTo()`
    + `play()` **e** o Cast → tocaria local e remoto ao mesmo tempo (áudio duplo). Corrigir junto
    ao religar o Cast (ramificar por `castSession?.isConnected`).

---

## Ordem sugerida de ataque

`0.1 → 0.3 → 1.A → 1.B → 2.A → (2.B, 2.C, 2.D) → 3.C/3.E (baratos) → 3.A/3.B (grande) → 4.x`
Cast entra quando você tiver uma TV pra testar.

**FASE 5 (nova):** `5.A ✅ → 5.B ✅ → 5.C ✅ → 5.D ✅`. **FASE 5 CONCLUÍDA.**

**FASE 6 (nova):** `6.G ✅ → 6.B ✅ → 6.C ✅ → 6.D ✅ → 6.E ✅ → 6.F ✅`. **FASE 6 CONCLUÍDA.**
6.F fechada sem código (2026-07-16: nenhuma base DB<8 no mundo real). (6.A rebaixada para 7.E.)

**FASE 7 (código morto):** `7.E ✅ → 7.C ✅ → 7.B ✅ → 7.A ✅ → 7.D ✅`. **FASE 7 CONCLUÍDA (2026-07-16).**

**FASE 9 (features de conteúdo):** `9.A ✅` (2026-07-18, `1af30dd`) — descrição por aula
implementada+testada; visual no device destrava com o bump de `meta.version` do `estudos.json`.
`9.B ✅` (2026-07-18, `4bf2c9c`) — capa 1:1 no player para Estudos (validada no release).
`9.C ✅` (2026-07-18, `29831dd`) — aula tocando = pause/retoma na lista (padrão de mercado);
fechou também o colateral da 6.G (timeout agora reseta `isSwitchingSource`). 9.A validada ao
vivo no device (meta.version bumpado; descrições no ar).
`9.D ✅` e `9.E ✅` (2026-07-18, `e4a661e`; v2 da folga em `c3ef61d`) — progresso só-leitura no
mini + corners concêntricos da capa (folga 4dp, raio 12dp). Validadas juntas no release.
`9.F ✅` (2026-07-18) — capas em retrato (0.7) na sheet "Direitos das capas e imagens";
pessoas/fontes seguem 1:1. Validada no release.
Micro-ajustes ✅ (2026-07-18): 9.C estendida a Temas (`629c797`); imagem do card de Temas
alinhada ao topo como em Estudos (style).

**FASE 8 (publicação Play Store):** 🔲 EM ANDAMENTO (atualizada 2026-07-18) — código e Console
quase todos ✅ (PUB-01/02/03/04/10, PUB-20/21/22/24/25, declarações de conteúdo). Restam:
**PUB-23** (vídeo FGS), **novela da chave** (Play App Signing legado 1024-bit → dupla assinatura),
placeholders de Estudos no servidor (+ recaptura `04_estudos`) e testes no release
**PUB-11/12/13/16**. Detalhes na seção FASE 8 acima.

**FASES 0→7 CONCLUÍDAS; FASE 8 (publicação) ABERTA.** Backlog residual só-quando-religar: Cast (§6.1-6.4, abaixo),
persistência estilo-Spotify (conflita c/ 4.A), validar Auto em DHU, busca por voz no Auto, 35 typos
de lint, 37 bumps de dependência, e o achado colateral da 6.G (`tryBeginSourceSwitch` timeout não
reseta `isSwitchingSource` no uiState).

---

## Como o Claude Code pode ajudar a manter isso (opcional)

Se o repo está no GitHub, ele pode transformar cada issue acima em issue real:

```text
Para cada ISSUE deste arquivo, crie uma issue no GitHub via `gh issue create`
com título, corpo (problema + arquivos + critério de aceitação + validação) e
labels por fase (fase-0..fase-4) e por esforço (P/M/G). Não crie as estacionadas
(Cast). Confirme os arquivo:linha contra o ROADMAP.md antes.
```

Se não usar GitHub, este arquivo já serve como backlog — basta colar a issue desejada no Claude Code
quando for executá-la.