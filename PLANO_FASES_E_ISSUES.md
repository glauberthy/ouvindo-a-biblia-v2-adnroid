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
  - 🔲 **5.B** (MÉDIA) — comando de play descartado antes do `MediaController` conectar (cold start).
  - 🔲 **5.C** (MÉDIA) — sleep timer conta wall-clock (não pausa junto com a reprodução).
  - 🔲 **5.D** (BAIXA) — progresso otimista de `fastForward`/`rewind` sem `coerceAtMost(duration)`.
  - (2 achados de Cast entraram no backlog de Cast, abaixo — estão fora de escopo desta versão.)
- 🆕 **FASES 6 e 7 abertas** (2026-07-15) — 2ª varredura, agora fora do core de playback
  (repositório, DAO, ViewModels de conteúdo, app-level, código morto). Detalhes nas seções
  FASE 6 (bugs funcionais/robustez) e FASE 7 (código morto). Destaques:
  - ↪️ **6.A REBAIXADA → 7.E** (2026-07-15) — as seções "Continuar Ouvindo"/"Favoritos" da Home
    NÃO são bug: são scaffolding morto, já substituído por soluções vivas (mini player restaura a
    sessão no cold start; tela de Favoritos dedicada). Virou remoção de código morto (ver 7.E).
  - 🔲 **6.B** (MÉDIA) — `ThemeDetails`/`StudyDetails` VMs vazam coletores a cada Retry.
  - 🔲 **6.C** (MÉDIA) — `extractDominantColorFromUrl` cria `ImageLoader` sem o User-Agent do WAF.
  - 🔲 **6.D/6.E/6.F** (BAIXA/RISCO) — Home preso em Loading com lista vazia; `syncMoreContent`
    sem version-gating; risco condicional de migração Room < 8.
  - ✅ **6.G** (MÉDIA, user-facing, FEITA 2026-07-15) — coração de favorito. Eram 2 bugs (metadata do
    controller como falsa fonte de verdade): (1) `syncStateWithController` revertia o otimista;
    (2) `toggleFavorite` lia `oldStatus` do metadata → nunca desfavoritava. Fix: `currentIsFavorite`
    (DB-backed) vira a fonte única. Validado no device (favorita/desfavorita alternando).
  - 🔲 **FASE 7** — código morto confirmado por grep (clipping da Bíblia, `repeatMode`, shuffle,
    `artist`, destinos de navegação órfãos, queries de DAO e DTO não usados).

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

### ISSUE 5.B — 🔲 TODO — Comando de play descartado antes do controller conectar (cold start)

- **Problema:** `playBook` (`PlayerViewModel.kt:406`), `playThemePlaylist` (`:354`) e
  `playStudyPlaylist` (`:914`) começam com `val controller = mediaController ?: return`. No cold
  start, se o usuário toca num item **antes** de o `MediaController` conectar (o `buildAsync` de
  `initializeController`, `:323`, leva ~centenas de ms), o toque vira **no-op silencioso**: nada
  toca e não há feedback. `playStudyById`/`playBook` disparam coroutines que também dependem do
  controller já resolvido.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (guardar a última intenção de play e executá-la no
  callback de conexão do `controllerFuture`, ou expor estado de "conectando" para a UI desabilitar/
  enfileirar o toque).
- **Critério de aceitação:** tocar num livro/estudo/tema imediatamente após abrir o app (frio)
  inicia a reprodução assim que o controller conecta, sem toque perdido.
- **Esforço:** M · **Depende de:** nada. · **device?** sim (reproduzir a corrida de cold start).

### ISSUE 5.C — 🔲 TODO — Sleep timer conta wall-clock (não pausa com a reprodução)

- **Problema:** `setSleepTimer` (`PlayerViewModel.kt:833-848`) usa `delay(minutes*60*1000L)` num
  job de tempo de parede, independente do estado real. Se o usuário **pausa**, o timer continua
  correndo e "pausa" algo já pausado; se a faixa **acaba** sozinha, o timer segue contando; e ele
  não sobrevive à morte do processo. Comportamento esperado num app de áudio: pausar a contagem
  quando a reprodução para e retomá-la ao voltar a tocar.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (ancorar a contagem no tempo de reprodução —
  descontar em `onIsPlayingChanged`, ou recalcular deadline a cada retomada). Opcional: opção
  "fim do capítulo atual".
- **Critério de aceitação:** com o timer ativo, pausar a reprodução congela a contagem; retomar
  continua de onde parou; ao zerar, a reprodução é pausada.
- **Esforço:** M · **Depende de:** nada. · **device?** recomendável.

### ISSUE 5.D — 🔲 TODO — Progresso otimista de `fastForward`/`rewind` sem limite pela duração

- **Problema:** `fastForward()` e `rewind()` (`PlayerViewModel.kt:501-511`) somam 30s/10s ao
  `currentPosition` do `_uiState` sem `coerceAtMost(duration)` (só o `rewind` faz `coerceAtLeast(0)`).
  Perto do fim da faixa, a barra pode ultrapassar 100% por um instante até o loop de progresso
  (`startProgressLoop`, 1s) corrigir com a posição real do player. Só visual, mas é jitter perceptível.
- **Arquivos:** `ui/player/PlayerViewModel.kt` (clampar o update otimista em `0..duration`).
- **Critério de aceitação:** avançar/retroceder perto das bordas não faz a barra estourar/ficar
  negativa; posição converge com o player.
- **Esforço:** P · **Depende de:** nada. · **device?** não (visual, verificável no emulador).

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

### ISSUE 6.B — 🔲 TODO — `ThemeDetails`/`StudyDetails` VMs vazam coletores a cada Retry

- **Problema:** `ThemeDetailsViewModel.loadMoments` e `StudyDetailsViewModel.loadStudyDetails`
  fazem `viewModelScope.launch { flow.collect {} }` sobre Flows de Room que nunca completam, e
  `handle(Retry)` re-chama a função **sem cancelar** o job anterior. N toques em "Tentar novamente"
  (ou reentradas) acumulam N coletores permanentes escrevendo no mesmo `_uiState` → leak de
  coroutines + corrida de escrita até `onCleared`.
- **Arquivos:** `ui/themas/ThemeDetailsViewModel.kt`, `ui/studies/StudyDetailsViewModel.kt`.
- **Critério de aceitação:** só um coletor ativo por vez (guardar/cancelar `Job`, ou migrar para
  `trigger.flatMapLatest{...}.stateIn(WhileSubscribed)` como Home/Themes/Studies/More).
- **Esforço:** P · **Depende de:** nada. · **device?** não (revisável por inspeção/teste).

### ISSUE 6.C — 🔲 TODO — `extractDominantColorFromUrl` cria `ImageLoader` sem o User-Agent do WAF

- **Problema:** `ColorExtension.kt:18` faz `val loader = ImageLoader(context)` — um loader novo,
  sem o header `User-Agent: "BibliaFaladaApp"` que o `ImageLoader` singleton do `CoilModule.kt`
  injeta ("segredo do WAF") e sem os caches compartilhados. Se o host de imagens exige o UA (mesma
  premissa do resto do app), `execute` não retorna `SuccessResult` → a cor dominante do player cai
  **sempre** no `defaultColor` (`MainScreen.kt:175`). Independente do WAF, ainda instancia um
  `ImageLoader` novo **a cada** mudança de `imageUrl` (`MainScreen.kt:172`), ignorando o cache.
- **Arquivos:** `ui/theme/ColorExtension.kt` (receber o `ImageLoader` singleton por parâmetro),
  `ui/MainScreen.kt` (passar o loader do `OuvindoBibliaApp`/DI).
- **Critério de aceitação:** extração usa o loader compartilhado (com UA + cache); a cor do player
  reflete a capa em device; sem novos `ImageLoader` por frame.
- **Esforço:** P · **Depende de:** nada. · **device?** sim (confirmar se a cor passa a extrair).

### ISSUE 6.D — 🔲 TODO — Home presa em Loading eterno com sync bem-sucedido e lista vazia

- **Problema:** `HomeViewModel.kt:61-63` mapeia `Resource.Success` com `data.isEmpty()` para
  `HomeUiState.Loading`, e o estado Loading não tem Retry (`HomeScreen.kt`). Se o repositório
  emitir Success vazio (0 livros), a Home fica em spinner infinito sem saída. Inconsistente com
  Themes (empty→Error) e Studies (empty→Empty).
- **Arquivos:** `ui/home/HomeViewModel.kt` (tratar vazio como Empty/Error com Retry).
- **Critério de aceitação:** lista vazia após sync não prende em Loading; usuário tem como re-tentar.
- **Esforço:** P · **Depende de:** nada. · **device?** difícil de reproduzir (especulativo).

### ISSUE 6.E — 🔲 TODO — `syncMoreContent` sem version-gating (escrita redundante)

- **Problema:** diferente de `syncBibleData/syncThemes/syncStudies`, `syncMoreContent`
  (`BibleRepositoryImpl.kt:~399`) ignora o campo `version` (que é até persistido em
  `MoreContentEntity.version`). Toda coleta de `getMoreContentResource()` faz fetch de rede +
  `INSERT REPLACE` no Room mesmo sem mudança. Ineficiência (não perde dados).
- **Arquivos:** `data/repository/.../BibleRepositoryImpl.kt`.
- **Critério de aceitação:** sync do "Mais" só reescreve o Room quando `meta.version` muda,
  como os outros três.
- **Esforço:** P · **Depende de:** nada.

### ISSUE 6.F — 🔲 VERIFICAR (risco condicional) — Migração Room de schema < 8 → crash no launch

- **Problema:** `DatabaseModule.kt:31-33` registra só `MIGRATION_8_9` e mantém apenas
  `fallbackToDestructiveMigrationOnDowngrade()` (o destrutivo geral foi removido na 0.2 para
  preservar favoritos/retomada). Se existir base instalada em schema **< 8**, a atualização lança
  `IllegalStateException` na 1ª abertura → **crash em loop**. **Falhar alto em bumps futuros sem
  migração é intencional** (decisão da 0.2); o risco é só o histórico pré-8.
- **Ação:** confirmar se **alguma versão publicada** rodou com `BibleDatabase.version < 8`. Se não,
  fechar como "não é bug". Se sim, adicionar `MIGRATION_x_8` (ou destrutivo só para esse caminho).
- **Arquivos:** `data/local/.../di/DatabaseModule.kt`, `database/BibleDatabase.kt`.
- **Esforço:** P (investigação) · **Depende de:** histórico de releases.

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

### ISSUE 7.A — 🔲 TODO — Clipping da Bíblia inalcançável + parâmetros propagados mortos

- Cadeia `onPlayBook(...,0L,0L)` (únicos 2 call-sites vivos: `NavigationGraph.kt:43,53`) →
  `playBook` → `buildBibleMediaItems` torna `startMs`/`endMs` **sempre 0**. Os ramos
  `if (startMs>0)` / `if (endMs>startMs)` em `PlayerViewModel.buildBibleMediaItems` são
  inalcançáveis e o `ClippingConfiguration` da Bíblia sai sempre vazio. Remover os params
  `startMs`/`endMs` de `playBook`, `buildBibleMediaItems` e do lambda `onPlayBook`
  (`NavigationGraph`/`MainScreen`). **Manter** o clipping de Tema (`playThemePlaylist`, vivo) e a
  guarda 2.D em `saveCurrentState` (defesa). Já era "limpeza futura" citada na 3.D.

### ISSUE 7.B — 🔲 TODO — Campos/ações de player nunca lidos

- `PlayerUiState.repeatMode` (grep=1, só a declaração), `PlayerUiState.artist` (write-only:
  escrito em `PlayerViewModel.kt:675`, 0 leituras), `isShuffleEnabled` (write-only) +
  `toggleShuffle()` (0 chamadas) — shuffle é feature inteiramente morta. Remover ou implementar.

### ISSUE 7.C — 🔲 TODO — Destinos de navegação órfãos

- `Screen.Player`, `Screen.About`, `Screen.Copyright` (`AppNavigation.kt`) — 0 `composable<>`/
  `navigate()`. O player é overlay em `MainScreen`, não destino. Remover os 3 tipos.

### ISSUE 7.D — 🔲 TODO — Repositório/DAO/DTO não usados

- **Repo (+ interface):** `getBook(bookId)` (morto **e** com bug latente — chama `getBookById`
  que filtra pelo slug, nunca casaria com numericId) e `getBookIdFromChapter` (o usado é
  `getBookNumericIdFromChapter`).
- **DAO (`BibleDao.kt`):** `getChaptersForBook`, `getChapterWithBookInfoById` (morto **e** com JOIN
  inválido: cruza `chapters.book_id` numérico com `books.book_id` slug), `updateChapterMetadata`,
  `insertBooks`, `insertChaptersIgnore`, `clearBooks`, `clearChapters`, `clearStudies`,
  `clearStudyLessons`, `insertStudies`, `insertStudyLessons`, `getStudies()`, `get()` e `clear()`
  (de `more_content`).
- **DTO:** `data/local/.../model/PlaybackStateDto.kt` — classe inteira sem referências.
- **Intents no-op nunca despachadas:** `HomeIntent.OpenBook`, `ThemesIntent.SelectTheme`,
  `StudiesIntent.SelectStudy` (navegação é feita direto por callback nas Screens).
- ⚠️ Alguns `clear*`/`insert*` podem ser úteis como API reservada; confirmar contra testes antes.

### ISSUE 7.E — 🔲 TODO — Seções mortas da Home ("Continuar Ouvindo" / "Favoritos") (ex-6.A)

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

**FASE 5 (nova):** `5.A (crítica, primeiro) → 5.B → 5.C → 5.D`. A 5.A é a de maior impacto e a
mais autocontida (permissão + request). 5.B e 5.C precisam de device para validar a corrida de
cold start e o comportamento de pausa; 5.D dá pra fechar no emulador.

**FASE 6 (nova):** `6.G ✅ → 6.B → 6.C → 6.D → 6.E → 6.F`. 6.B/6.C são baratas e de bom retorno.
6.F é só investigação (pode virar no-op). (6.A saiu daqui: rebaixada para 7.E — código morto.)

**FASE 7 (código morto):** baixa prioridade, fazer depois das 5/6 ou em janela de limpeza. Sequência
sugerida: `7.E → 7.C → 7.B → 7.A → 7.D`. 7.E (seções mortas da Home) e 7.C (destinos de navegação
órfãos) são as remoções mais autocontidas e sem risco; 7.A (clipping) já estava mapeada desde a
3.D; 7.D (repo/DAO/DTO) por último, confirmando contra `src/test`/`androidTest` antes de apagar.

**Sugestão global de prioridade:** `5.A ✅ → 6.G ✅ → 6.B → 6.C → 5.B → 5.C → (5.D, 6.D, 6.E) → 6.F → FASE 7 (7.E → 7.C → 7.B → 7.A → 7.D)`.

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