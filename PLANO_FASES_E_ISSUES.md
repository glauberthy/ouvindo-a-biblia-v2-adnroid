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
- 🏁 **Plano concluído** (0→4 + 4.D). Backlog opcional: validação DHU do Auto, busca
  por voz, persistência estilo-Spotify, 35 Typos (baseline), 37 bumps de dependência.

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
- **Validação:** lint 0 errors (77 warns), compile+test OK, resume no device sem crash.
  ⚠️ **Browse+playback end-to-end no Auto NÃO validados** — exigem DHU (USB + app Android
  Auto). Pendente de validação em DHU.

---

## ❌ FORA DE ESCOPO desta versão — Cast (desligado via kill-switch; reativar no futuro)

- Desligado em 2026-07-14 via `CastConfig.ENABLED=false` (código dormente no repo).
  Reativar: flip do flag + descomentar `CastButton` em `SharedPlayerScreen`.
- Backlog quando religar: §6.1 posição não volta Cast→local · §6.2 sem fila (auto-avanço) ·
  §6.3 metadados errados p/ Estudo-Tema · §6.4 alvo divergente na transição. Exigem
  device + Chromecast para validar.

---

## Ordem sugerida de ataque

`0.1 → 0.3 → 1.A → 1.B → 2.A → (2.B, 2.C, 2.D) → 3.C/3.E (baratos) → 3.A/3.B (grande) → 4.x`
Cast entra quando você tiver uma TV pra testar.

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