# AUDITORIA 02 — Robustez em Produção ("o que quebra na mão do usuário")

**Data:** 2026-07-16 · **Branch:** `analise-media3` · **Modo:** leitura de código + análise estática
(itens 🔬 têm passo a passo de device, incluindo build de release).

Legenda: **[BLOQUEIA]** · **[CORRIGIR ANTES]** · **[PODE ESPERAR]** · ✅ verificado OK · 🔬 requer teste
em device.

---

## 1. Falha de rede — ✅ (com 1 edge estreito)

**Timeouts configurados nos dois caminhos:**
- HTTP/JSON (OkHttp): `NetworkModule.kt:50-52` — connect/read/write 15s. ✅
- Áudio (ExoPlayer): `MediaModule.kt:58-59` — `setConnectTimeoutMs`/`setReadTimeoutMs`. ✅

**JSON malformado NÃO derruba o app:**
- `NetworkModule.kt:26-29` — `Json { ignoreUnknownKeys = true; coerceInputValues = true }`: campos
  desconhecidos são ignorados; `null` em campo com default é coagido ao default. ✅
- Erro de desserialização (ex.: campo obrigatório ausente) é capturado: `syncBibleData`/`syncThemes`/
  `syncStudies` usam `try { … } catch (e) { Result.failure(e) }` (`BibleRepositoryImpl.kt:165-219`,
  `230-282`, `301-348`); `syncMoreContent` usa `runCatching` (`:382-403`); o decode do cache do
  "Mais" usa `runCatching{…}.getOrNull()` (`:409-411`). → vira **Error na tela**, não crash. ✅

**Cache vazio + sem internet (1º uso):** `syncedListResource` (`BibleRepositoryImpl.kt:75-95`) emite
`Loading` (cache vazio) → roda `sync()` → como cache continua vazio e `result.isFailure`, emite
`Resource.Error`. As VMs mapeiam para `Error` e a tela mostra `ErrorScreen` com **Retry**. ✅

**Error + Retry confirmados nas 4 telas de sync:**
| Tela | Error→ErrorScreen | Retry |
|------|-------------------|-------|
| Home | `HomeScreen.kt:53` | `HomeIntent.Retry` ✅ |
| Temas | `ThemesScreen.kt:76` | `ThemesIntent.Retry` ✅ |
| Estudos | `StudiesScreen.kt:83-85` | `StudiesIntent.Retry` ✅ |
| Mais | `MoreScreen.kt:94-96` | `MoreIntent.Retry` ✅ |

Retry funciona via `refreshTrigger.update { it + 1 }` → `flatMapLatest` re-coleta → novo sync. ✅

**⚠️ Edge estreito [CORRIGIR ANTES] — Loading eterno na tela "Mais":**
`BibleRepositoryImpl.kt:428` — em `getMoreContentResource`, quando `content == null` **e** o sync teve
sucesso, emite `Resource.Loading` (não Error). É o mesmo padrão que a ISSUE 6.D corrigiu na Home.
Caminhos comuns estão OK (sem cache + falha → Error; sync ok grava conteúdo → Success). O Loading
eterno só ocorre num edge: já existe uma linha em `more_content` cuja versão bate com a remota
(pula reescrita, `:390`) **mas** cujo JSON cacheado falha ao decodificar agora (`getOrNull` → null).
Improvável, mas deixa a tela "Mais" travada em spinner sem Retry. Sugestão: `else -> Error` (como 6.D).

## 2. Estados vazios — ✅

| Tela | Tratamento de lista vazia | Onde |
|------|---------------------------|------|
| Home | vazio → **Error** "Nenhum livro disponível" + Retry (6.D) | `HomeViewModel.kt:60-66` |
| Temas | vazio → **Error** "Nenhum tema encontrado…" + Retry | `ThemesViewModel.kt:53-55` |
| Estudos | vazio → **estado Empty próprio** (`EmptyStudiesScreen`) | `StudiesViewModel.kt`; `StudiesScreen.kt:82` |
| Favoritos | **Empty por seção** (`EmptyFavorites("capítulos…")`/`("lições…")`) | `FavoritesScreen.kt:194-205,582` |
| Mais | objeto único; sempre Success com estrutura | `MoreViewModel.kt` |

- **Favoritos tem estado vazio próprio** ✅ e não depende de rede (lê só do Room, com `.catch`→Error:
  `FavoritesViewModel.kt:25-27`). Vazio ≠ bug: mostra as duas seções com mensagem dedicada.
- Nota [PODE ESPERAR]: `MoreScreen` não tem estado `Empty` explícito; se o servidor devolvesse
  conteúdo com seções vazias, a tela renderizaria esparsa (não quebra). Baixo risco (o servidor
  sempre devolve a estrutura).

## 3. Erro de playback — ⚠️ [CORRIGIR ANTES]

**Retry automático existe (bom):** `MediaModule.kt:71-87` — `DefaultLoadErrorHandlingPolicy` com
**3 tentativas** e backoff (1s/2s/4s). Quedas transitórias de rede no meio da faixa são reabsorvidas
antes de falhar. ✅ Também `setAllowCrossProtocolRedirects(true)` (`:57`), `WAKE_MODE_NETWORK` (`:96`).

**O erro É detectado, mas NÃO é mostrado ao usuário:** `PlayerViewModel.kt:620-622` — em
`onEvents`, `EVENT_PLAYER_ERROR` só chama `finishSourceSwitch()` (reseta o flag de troca de fonte).
Não há atualização do `uiState` com mensagem de erro, toast ou estado de falha. O `PlayerUiState`
(`PlayerUiState.kt:50+`) **não tem campo de erro**.
- **Efeito na mão do usuário:** URL quebrada/404, ou rede fora após esgotar os 3 retries → o player
  simplesmente **para em silêncio** (vai a `STATE_IDLE`, `isPlaying=false`). Sem crash, sem
  travamento do flag (o `finishSourceSwitch` destrava os controles), mas **sem nenhum feedback**. O
  usuário toca play, nada acontece, e não sabe por quê.
- **Severidade:** não bloqueia (não crasha), mas para um app cujo core é tocar áudio, falha silenciosa
  é uma lacuna real de UX. **[CORRIGIR ANTES]:** adicionar campo de erro ao `PlayerUiState` +
  mensagem/toast ("Não foi possível reproduzir. Verifique sua conexão.") no ramo de `EVENT_PLAYER_ERROR`.

## 4. Android antigo (minSdk 26 / Android 8) — ✅

- **Único guard de versão no app:** `MainActivity.kt:65` (`POST_NOTIFICATIONS` gated em TIRAMISU) — e
  é suficiente, porque não há chamadas cruas de API nova sem proteção:
  - **Notificação/FGS são do Media3** (`MediaLibraryService`): não há `NotificationChannel`,
    `NotificationManager` nem `startForeground` manuais no app (grep = 0). O Media3 cuida das
    diferenças por nível de API e do `foregroundServiceType`. ✅
  - `PendingIntent.FLAG_IMMUTABLE` (`PlaybackService.kt:420`) — existe desde API 23, seguro no
    minSdk 26 e satisfaz a exigência de mutabilidade do Android 12+. ✅
  - `ContextCompat.startForegroundService` (`PlayerViewModel.kt:308`), `enableEdgeToEdge`,
    `installSplashScreen` — todos wrappers androidx que tratam a diferença de versão. ✅
- 🔬 **A confirmar em device Android 8 real** (ver Testes) — o caminho de FGS `mediaPlayback` sob as
  regras do Android 14+ (targetSdk 36) é gerido pelo Media3, mas vale um smoke num aparelho antigo.

## 5. POST_NOTIFICATIONS (Android 13+) — ✅ (código) + 🔬 (comportamento se negado)

- Pedido em runtime, gated e registrado corretamente: `MainActivity.kt:30-31` (launcher como campo),
  `:41` (chamado no `onCreate`), `:64-72` (só SDK≥33, só se ainda não concedida). ✅
- Comentário do código afirma que negar só esconde a notificação; o áudio toca igual
  (`MainActivity.kt:27-29`). **Precisa de confirmação empírica** → 🔬 (ver Testes).

## 6. Ciclo do player sob estresse / persistência (5.x) — ✅ (hooks) + 🔬 (device)

Mecanismo de persistência intacto (FASE 7 não mexeu nele):
- `restoreLastSession()` chamado no `onCreate` do serviço (`PlaybackService.kt:143`) — reconstrói a
  playlist mas **não** auto-toca.
- `saveCurrentState()` em transições e pause (`:150,154,175`).
- `shouldBlockDatabaseResumption(5s)` (`:93`) + `markExplicitPlaybackRequest` (`:82`) — janela anti
  "ressurreição" da sessão salva sobre um play explícito.
- 🔬 **Reconfirmar sob estresse em device** (rotação, bg/fg repetido, matar processo e retomar) — ver
  Testes. Estático OK; o comportamento sob morte de processo depende do SO (ver
  `[[device-testing-gotchas]]`: no moto g53 o SO mata o processo *cached* sem entregar `onTaskRemoved`).

## 7. ANR / main thread — ✅ (estático) + 🔬 (StrictMode em device)

- **Startup enxuto:** `OuvindoBibliaApp.onCreate` (`:17-20`) só faz `super` + StrictMode (debug).
  `MainActivity.onCreate` (`:34-55`) só splash/edge-to-edge/permissão/`setContent`. Sem I/O. ✅
- **I/O fora da main:** sync em `withContext(Dispatchers.IO)` (`BibleRepositoryImpl.kt:165` etc.);
  favoritos/estado com `flowOn(Dispatchers.IO)` (`:372,150`); Room expõe `Flow` (coleta fora da main);
  Coil é assíncrono.
- **Fonte conhecida de I/O na main está desligada:** Cast — `CastConfig.ENABLED = false`
  (`CastConfig.kt:19`), então `CastContext.getSharedInstance` (I/O de disco na main) nunca é chamado;
  `initializeCast` gated (`PlayerViewModel.kt:242`). ✅
- StrictMode roda só em debug com `penaltyLog` (não `penaltyDeath`) → não crasha; serve para flagrar
  regressões. 🔬 **Rodar o debug e observar o logcat** por `StrictMode policy violation` (ver Testes).

---

## 🔬 Passo a passo dos testes de device

> Onde indicado, **testar também no BUILD DE RELEASE** (ofuscado por R8), não só debug — o release
> pode se comportar diferente (ver AUDITORIA_01: hoje o AAB/APK de release ainda não está assinado;
> gerar um APK de release assinado — mesmo com keystore de teste — para estes testes).

### T1 — Falha de rede (item 1)
1. Desinstale o app (garante cache/Room vazios). Ative **modo avião**.
2. Abra o app → Home deve mostrar **ErrorScreen** ("Nenhum livro disponível"/erro de conexão) com
   botão **Tentar Novamente** (não spinner infinito, não crash).
3. Navegue por Temas, Estudos, Mais → cada uma deve mostrar Error/Empty adequado.
4. Ligue a internet → toque **Tentar Novamente** em cada tela → deve carregar o conteúdo.
5. **Queda no meio:** com conteúdo carregado, comece a rolar/abrir detalhe e ative modo avião →
   confirme que não trava nem crasha; telas já carregadas seguem exibindo cache.
6. Repita T1 no **APK de release**.

### T2 — Erro de playback (item 3) — o mais importante
1. Toque um capítulo com internet OK (confirma baseline).
2. **Rede fora no meio:** durante a reprodução, ative modo avião. Observe: deve haver ~3 tentativas
   (1s/2s/4s) e então a faixa para. **Verifique se aparece alguma mensagem de erro** — hoje a
   expectativa é que **não apareça** (lacuna do item 3). Anote o comportamento.
3. **URL 404/quebrada:** aponte um item para uma URL inválida (ou simule via proxy/hosts) e toque →
   confirme que o app não crasha e observe a (ausência de) mensagem.
4. Confirme que os controles do player não ficam travados (o `finishSourceSwitch` deve reabilitar).
5. Repita no **APK de release**.

### T3 — POST_NOTIFICATIONS negado (item 5) — Android 13+
1. Instale em aparelho Android 13+ (limpo). Ao abrir, **negue** o diálogo de notificações.
2. Toque um capítulo → **o áudio deve tocar normalmente**.
3. Confirme que **não há** notificação de mídia (esperado) e que **não há crash**.
4. Vá em Ajustes do sistema → conceda a permissão → volte e toque de novo → a notificação de mídia
   (com play/pause/next) deve aparecer.
5. Repetir no **APK de release**.

### T4 — Ciclo/persistência sob estresse (item 6)
1. Toque um capítulo, avance a posição ~1min, **pause**.
2. **Gire a tela** várias vezes → o mini player mantém faixa/posição, sem duplicar áudio.
3. Mande para **background** e volte ao **foreground** repetidamente (5×) → estado consistente.
4. **Mate o processo** (swipe na recents ou `adb shell am kill`) e reabra → o mini player deve
   **restaurar** a última sessão (faixa + posição) **sem auto-tocar** (5.1).
5. Toque play → deve retomar de onde parou.
6. Repetir no **APK de release** (R8 pode afetar serialização do estado salvo — validar).

### T5 — StrictMode / ANR (item 7)
1. Instale o **debug** e rode `adb logcat | grep -i "StrictMode policy violation"`.
2. Exercite: cold start, abrir cada aba, tocar, favoritar, abrir player, girar. Nenhuma violação de
   I/O de disco/rede na main deve aparecer (penaltyLog só loga).
3. Observe se há ANR (tela congelada >5s) em qualquer transição pesada (1º sync, troca de faixa).

### T6 — Android 8 (item 4)
1. Rodar T1–T4 num emulador/aparelho **API 26 (Android 8)**.
2. Foco: notificação de mídia aparece, FGS inicia sem crash, sem `NoSuchMethodError`/`VerifyError`
   (sinais de API nova sem guarda).

---

## Sumário de severidade

| # | Item | Veredito |
|---|------|----------|
| 1 | Falha de rede (timeouts, JSON, Error+Retry 4 telas) | ✅ · 1 edge [CORRIGIR ANTES] (Loading eterno "Mais", `:428`) |
| 2 | Estados vazios (incl. Favoritos com Empty próprio) | ✅ |
| 3 | **Erro de playback não é mostrado ao usuário** | ⚠️ **[CORRIGIR ANTES]** |
| 4 | Android 8 / guards de API | ✅ · 🔬 smoke em device antigo |
| 5 | POST_NOTIFICATIONS runtime (código correto) | ✅ · 🔬 testar negação |
| 6 | Persistência/ciclo do player (hooks intactos) | ✅ · 🔬 stress em device |
| 7 | ANR/main thread (startup enxuto, Cast off, I/O em IO) | ✅ · 🔬 StrictMode logcat |

## 🟡 CORRIGIR ANTES (priorizado)
1. **[#3] Erro de playback silencioso** — `PlayerViewModel.kt:620-622` só reseta o flag; adicionar
   campo de erro ao `PlayerUiState` e feedback ao usuário. É a lacuna de robustez mais visível.
2. **[#1] Loading eterno na tela "Mais"** — `BibleRepositoryImpl.kt:428`: `content==null` + sync OK →
   Loading sem Retry. Trocar por `Error` (paridade com a ISSUE 6.D).

## 🟢 PODE ESPERAR
- `MoreScreen` sem estado `Empty` explícito (baixo risco).

**Veredito geral:** a base de robustez é sólida — timeouts nos dois canais, JSON defensivo, Error+Retry
nas 4 telas, retry automático de playback (3×), estados vazios tratados, startup sem I/O e Cast (I/O na
main) desligado. **A única lacuna de peso é o erro de playback não chegar ao usuário (#3)**; o resto são
verificações de device (🔬) e um edge estreito de Loading na tela "Mais".
