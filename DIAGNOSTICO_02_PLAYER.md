# Diagnóstico 02 — Mergulho profundo na camada de reprodução

> Auditoria somente-leitura. Foco exclusivo: playback (`PlaybackService` + `PlayerViewModel` + estado/persistência + Cast). Continuação do `DIAGNOSTICO_01_ARQUITETURA.md`.
> Fonte da verdade: o **código**. Nenhuma correção foi aplicada.
> Data: 2026-06-27 · Branch: `analise-media3`

Arquivos lidos integralmente: `service/PlaybackService.kt`, `ui/player/PlayerViewModel.kt`, `ui/player/PlayerUiState.kt`, `ui/chapters/ChaptersViewModel.kt`, `data/local/entity/PlaybackStateEntity.kt`, `data/local/dao/BibleDao.kt` (queries de playback), `BibleRepositoryImpl` (getBookNumericIdFromChapter, getLatestPlaybackState), `ui/MainScreen.kt` + `ui/navigation/*` (acionamento).

Gravidade: **[CRÍTICO]** crash/perda total · **[ALTO]** perda de dado do usuário ou função quebrada · **[MÉDIO]** falha intermitente/edge · **[BAIXO]** cosmético/latente.

**Legenda de confiança:**
- ✅ **CONFIRMADO** — deduzível só lendo o código.
- 🔬 **PRECISA RODAR** — preciso do emulador/device para confirmar o sintoma real. Passo a passo no fim.

---

## Mapa de fatos estruturais (base para tudo abaixo)

1. **A linha de persistência é uma única linha-singleton.** `PlaybackStateEntity` tem `@PrimaryKey val id: Int = 1` (PlaybackStateEntity.kt:8); `savePlaybackState` usa `OnConflictStrategy.REPLACE` (BibleDao.kt:191) e `getLastPlaybackState` é `SELECT * FROM playback_state WHERE id = 1` (BibleDao.kt:199). Logo **só existe UM estado salvo** — compartilhado entre Bíblia e Estudo. O comentário "É VÁLIDO (garantido pelo SQL)" em `restoreLastSession` (PlaybackService.kt:154) é enganoso: não há garantia nenhuma de integridade, é só a linha fixa.

2. **Existem DOIS caminhos para iniciar a Bíblia, com convenções de índice diferentes:**
   - **Caminho vivo:** `MainScreen → PlayerViewModel.playBook` (MainScreen.kt:328) constrói a playlist **inteira no cliente** (`buildBibleMediaItems`, PlayerViewModel.kt:399) e passa `initialIndex` = **posição na lista**. No serviço, `onSetMediaItems` vê `isBrowsable == false` e cai no `super` (PlaybackService.kt:474) — o branch de expansão não roda.
   - **Caminho morto:** `ChaptersViewModel.playChapter` (ChaptersViewModel.kt:73) cria um item *browsable* com `mediaId = "${bookId}|${chapter.number - 1}"` e dispara o branch de expansão do serviço (PlaybackService.kt:425). **Mas `Screen.Chapters` nunca é navegado** — não existe nenhum `navController.navigate(Screen.Chapters(...))` no projeto (só a referência por nome em MainScreen.kt:271). Confirmado por grep. Ou seja, `ChaptersScreen`/`ChaptersViewModel` e o parser `"|"` são **código morto hoje** pela UI.

3. **O ExoPlayer é `@Singleton` (escopo de processo) mas é `release()`-ado pelo serviço** (PlaybackService.kt:305 e :314). Esse é o eixo do risco mais grave (ver §5.1).

---

## 1. Convenção de `mediaId` — todos os parsers e onde quebra em silêncio

### 1a. Inventário completo dos pontos que **formatam** `mediaId`

| Formato | Local |
|---|---|
| `chapter.id` (numérico) | `buildBibleMediaItems` PlayerViewModel.kt:139 · `createMediaItemsFromChapters` PlaybackService.kt:262 |
| `"moment_${moment.id}"` | `playThemePlaylist` PlayerViewModel.kt:350 |
| `"study_${studyId}_${remoteId}"` | `playStudyPlaylist` PlayerViewModel.kt:891 · `buildPlaylistFromState` PlaybackService.kt:202 |
| `"${bookId}|${chapter.number - 1}"` | `ChaptersViewModel.playChapter` ChaptersViewModel.kt:76 *(caminho morto)* |
| `book.numericId` (folder) | `onGetChildren` PlaybackService.kt:516 |

### 1b. Inventário completo dos pontos que **parseiam** `mediaId`

| Parser | Local | Assume |
|---|---|---|
| `startsWith("moment_")` → pula save | PlaybackService.kt:128 | tema |
| `startsWith("study_")` + `split("_")[1]` | PlaybackService.kt:193-195 | estudo |
| `toIntOrNull()` → capítulo | PlaybackService.kt:228 | Bíblia |
| `split("|")` → bookId, index | PlaybackService.kt:426 | folder |
| `startsWith("study_")/("moment_")` else BIBLE | PlayerViewModel.kt:100-106 | 3 tipos |
| `startsWith("moment_")` → isThemeMode | PlayerViewModel.kt:627 | tema |
| **`toLongOrNull() ?: 0L`** | PlayerViewModel.kt:664 | **Bíblia (assume sempre!)** |
| `toLongOrNull()` → favorito | PlayerViewModel.kt:811, :833, :846 | Bíblia |

### 1c. [MÉDIO] ✅ `extractChaptersFromPlayer` trata TODO item como Bíblia numérica

`extractChaptersFromPlayer` (PlayerViewModel.kt:658-694) roda para **qualquer** fonte (é chamado em todo `syncStateWithController`, linha 649). Para um estudo, `mediaId = "study_12_3"`:
- linha 664: `item.mediaId.toLongOrNull() ?: 0L` → **`chapterId = 0` para toda aula**;
- linhas 668-672: `titleStr.split(" ").last().toInt()` sobre o título do estudo (ex.: "Estudo sobre Fé") → `NumberFormatException` → fallback `i + 1`.

**Cenário do usuário:** abrir a folha de capítulos (`ChaptersSheet`) durante um Estudo mostra a lista com numeração sintética ("1, 2, 3...") em vez dos títulos reais das aulas, e todos os itens com `id=0`. A navegação por índice ainda funciona (`onChapterSelected` usa índice, não id), então não quebra a reprodução — é degradação silenciosa de exibição. Fica frágil: qualquer lógica futura que dependa de `chapter.id` no modo Estudo opera sobre `0`.

### 1d. [MÉDIO] ✅ Divergência de índice entre os dois caminhos da Bíblia

`playBook` passa `initialIndex` = **posição na lista ordenada** (PlayerViewModel.kt:402). `ChaptersViewModel` passa `chapter.number - 1` (ChaptersViewModel.kt:76), que só é igual à posição **se os capítulos forem 1..N contíguos começando em 1**. Se o livro tiver numeração com lacuna ou não começar em 1, o índice aponta para o capítulo errado. Está latente (caminho morto §0.2), mas é uma bomba-relógio: se alguém religar a navegação para `ChaptersScreen`, toca o capítulo errado sem erro nenhum.

### 1e. [BAIXO] ✅ Onde um `mediaId` malformado escapa sem validação

`buildPlaylistFromState` (PlaybackService.kt:189): se `mediaId` não casar com `study_`/numérico (ex.: lixo, ou um futuro `"moment_"` que escapasse do filtro de save), `toIntOrNull()` → `null` → `return null` → restore silenciosamente não acontece. É *fail-safe* (não quebra), mas sem nenhum log (os `Log.w` estão comentados nas linhas 174-180), então uma corrupção de dado vira "o app simplesmente não retoma" sem rastro.

---

## 2. `shouldBlockDatabaseResumption` (janela de 5s)

### 2a. ✅ O que protege, e a relação com `markExplicitPlaybackRequest`

`markExplicitPlaybackRequest` grava `lastExplicitPlaybackRequestAt = SystemClock.elapsedRealtime()` (PlaybackService.kt:66) e é chamado em **todo** `onSetMediaItems` (linha 421), ou seja, sempre que a UI manda tocar algo. `shouldBlockDatabaseResumption` (linha 76) retorna `true` se faz menos de 5s desse marco. É consultado **apenas** em `onPlaybackResumption` (linha 344).

**O que protege:** se o usuário acabou de escolher tocar algo (play explícito) e, quase ao mesmo tempo, o sistema dispara `onPlaybackResumption` (botão de mídia, headset), o bloqueio impede que o **estado salvo antigo do banco** seja reconstruído por cima da seleção nova — a "ressurreição" de sessão anterior descrita no doc.

**Ponto positivo de design:** usa `SystemClock.elapsedRealtime()` (monotônico), não relógio de parede — imune a mudança de fuso/hora. O bug clássico de "lógica por tempo" (relógio andando para trás) **não se aplica aqui**.

### 2b. [BAIXO] ✅ Proteção amplamente redundante

`onPlaybackResumption` já tem um segundo guarda: se `player.mediaItemCount > 0` ele devolve os itens atuais (linha 355), nunca o banco. Como um `onSetMediaItems` bem-sucedido deixa `mediaItemCount > 0`, na maioria dos casos o bloqueio por tempo nem é o que decide. A janela só importa no intervalo entre `markExplicitPlaybackRequest` (linha 421) e os itens **de fato** entrarem no player.

### 2c. [MÉDIO] 🔬 A janela pode falhar no branch assíncrono em device lento

No branch de folder (`isBookFolder`), `onSetMediaItems` marca o request (linha 421) mas só popula a playlist **dentro de uma coroutine IO** (linha 439) que lê o Room. Existe uma janela real entre "marcou" e "itens setados" em que `mediaItemCount` ainda é 0. Se, nesse intervalo, (a) passarem 5s **e** (b) `onPlaybackResumption` disparar, o bloqueio cai (`false`) e o `mediaItemCount` ainda é 0 → ele reconstrói do banco (linha 377), sobrepondo a seleção. Em device lento + Room frio + I/O disputado isso é teoricamente possível. É de baixa probabilidade (leitura de Room raramente passa de 5s) e o branch de folder é o caminho morto (§0.2), por isso **MÉDIO** e não maior. Precisa de device lento para reproduzir de verdade.

---

## 3. Resume / persistência — fluxo completo

Fluxo: `onCreate` → `setupAutoSaveListener` (PlaybackService.kt:109) + `restoreLastSession` (linha 106). Save dispara em `onMediaItemTransition` (linha 111) e em `onPlayWhenReadyChanged(false)` (linha 117). Restore lê a linha-singleton e reconstrói via `buildPlaylistFromState`, **sem auto-play** (`playWhenReady = false`, linha 170).

### 3a. ✅ Momentos temáticos são realmente excluídos do save — CONFIRMADO

`saveCurrentState` retorna cedo se `mediaId.startsWith("moment_")` (PlaybackService.kt:128). Confirmado: temas nunca são persistidos. Consequência correta: tocar um tema **não** sobrescreve o "Continuar Ouvindo" da Bíblia/Estudo — o banco mantém o estado anterior. (Efeito colateral aceitável: pausar um tema também não salva nada; após morte do processo a UI volta ao último Bíblia/Estudo, não ao tema.)

### 3b. [ALTO] ✅ Posição salva é relativa ao recorte (clipping), restore trata como absoluta

`buildBibleMediaItems` aplica `ClippingConfiguration` com `setStartPositionMs(startMs)` no capítulo-alvo (PlayerViewModel.kt:127-147). Com clipping, `player.currentPosition` é **relativo ao início do recorte**. `saveCurrentState` grava esse `currentPosition` (PlaybackService.kt:132). No restore, `buildPlaylistFromState` (Bíblia) reconstrói **sem** clipping (`createMediaItemsFromChapters` com `targetChapterIndex = -1`, linha 232) e aplica `state.positionMs` como posição **absoluta**. Se o item original era recortado, a posição restaurada fica deslocada pelo offset do recorte.
**Atenuante:** hoje a navegação sempre chama `playBook` com `startMs = 0L, endMs = 0L` (NavigationGraph.kt:45, :55), então o clipping da Bíblia não é exercido pela UI atual → latente. Por isso ALTO-latente, não CRÍTICO. Vira ativo no instante em que recorte de Bíblia for usado.

### 3c. [MÉDIO] ✅ Duração salva como 0 durante buffering → barra de progresso "cheia" no cold start

Se o save ocorre antes da duração ser conhecida, `saveCurrentState` grava `duration = 0` (PlaybackService.kt:140). Na volta, o `init` do `PlayerViewModel` faz `duration = if (lastState.duration > 0) ... else 1L` (PlayerViewModel.kt:273) e `progress = position/1` → clamp em `1.0`. **Cenário:** mini-player no cold start aparece com a barra cheia/100% até o controller conectar e corrigir. Cosmético, mas visível.

### 3d. [MÉDIO] ✅ `onMediaItemTransition` salva o item NOVO na posição ~0, perdendo a granularidade do anterior

`onMediaItemTransition` chama `saveCurrentState` **depois** de o item novo virar o corrente, então grava `mediaId` novo com `currentPosition ≈ 0` (PlaybackService.kt:111-114). Em auto-avanço normal de capítulo isso é o desejado. Mas não há save periódico (a cada N s): se o processo morre **no meio** de uma faixa sem ter havido pause nem transição, a última posição salva é a do início da faixa atual. **Cenário:** ouvindo há 8 min do capítulo, sem pausar, o sistema mata o processo por memória → ao reabrir, retoma do início do capítulo (perde os 8 min). Confirmado pela ausência de qualquer save por intervalo (o `startProgressLoop` em PlayerViewModel.kt:698 só atualiza a UI, não persiste).

### 3e. ✅ Restore não toca playlist incompleta — OK

`restoreLastSession` só seta itens se `player.mediaItemCount == 0` (PlaybackService.kt:163) e `buildPlaylistFromState` devolve `null` se livro/capítulos sumiram (linhas 229-231) → restore abortado sem crash. Esse ponto está correto.

---

## 4. `onPlaybackResumption` — o que existe, o que falta

### 4a. ✅ O que existe (PlaybackService.kt:337-403)

1. Se `shouldBlockDatabaseResumption()` → devolve lista vazia (no-op).
2. Senão, se `mediaItemCount > 0` → devolve os itens já carregados, no índice/posição atuais.
3. Senão → lê a linha-singleton e reconstrói via `buildPlaylistFromState`; se `null`/erro → lista vazia.

### 4b. [MÉDIO] ✅ Lacunas concretas

- **Está marcado `@Deprecated("Deprecated in Media3")`** (linha 336) — é a sobrescrita de um callback que o autor já sinalizou como instável; em upgrade do Media3 isso pode deixar de ser chamado.
- **Sempre devolve playlist sem afirmar intenção de tocar:** retorna `MediaItemsWithStartPosition`, mas o conjunto de comandos em `onConnect` é o `DEFAULT_SESSION_COMMANDS` cru (linha 329-332), sem customização. A retomada depende inteiramente do default do Media3.
- **Corrida com `restoreLastSession`:** no cold start por botão de mídia, `onCreate` dispara `restoreLastSession` (IO assíncrono) e o sistema pode chamar `onPlaybackResumption` quase junto. Os dois leem o mesmo banco e reconstroem; quem chegar primeiro decide. Resultado costuma ser equivalente, mas é trabalho duplicado e dependente de timing.

### 4c. 🔬 O que acontece HOJE na prática (precisa rodar)

A lógica de código sugere o seguinte, mas o comportamento real de retomada **só se confirma em device**:
- **App fechado com playback PAUSADO (swipe):** `onTaskRemoved` faz stop/release/stopSelf (§5) → notificação removida → **não há de onde retomar**. A retomada por headset/Auto provavelmente falha porque o serviço se autodestruiu.
- **App fechado TOCANDO (swipe):** `onTaskRemoved` **não** para (condição da linha 303 é falsa) → serviço sobrevive, notificação persiste, retomada deve funcionar pela sessão viva.
- **Processo morto pelo sistema (memória):** `START_STICKY` (linha 278) pode recriar o serviço → `onCreate` → restore com `playWhenReady=false`. Um play do headset deveria retomar — **se** o ExoPlayer singleton não estiver liberado (ver §5.1).

Passo a passo de teste no fim do documento.

---

## 5. Ciclo de vida — `onTaskRemoved` / release

### 5.1. [CRÍTICO] 🔬 ExoPlayer `@Singleton` liberado + `START_STICKY` = uso de player liberado

Encadeamento confirmado no código:
1. ExoPlayer é `@Singleton` no `SingletonComponent` (MediaModule, ver Diag 01) — **a mesma instância** é injetada a cada criação do serviço (PlaybackService.kt:48-49).
2. `onTaskRemoved` (com pausado/idle) chama `player.release()` + `stopSelf()` (PlaybackService.kt:304-306); `onDestroy` chama `player.release()` de novo (linha 314).
3. `onStartCommand` devolve `START_STICKY` (linha 278) → o sistema pode **recriar** o serviço.
4. Na recriação, `onCreate` injeta o **mesmo** ExoPlayer já liberado e o passa para `MediaLibrarySession.Builder(this, player, ...)` (linha 94) → operar um ExoPlayer liberado lança `IllegalStateException`.

**Cenário do usuário:** fechar o app (pausado) e depois reabrir/reconectar via botão de mídia ou nova `MediaController`, dentro do mesmo processo, pode estourar exceção e o player não volta. **Marco como CRÍTICO pela gravidade, mas 🔬 porque depende do scheduler do Android decidir recriar o serviço com a instância retida** — precisa de repro em device (passo a passo no fim). É a confirmação direta do risco #1 levantado no Diag 01.

### 5.2. [MÉDIO] ✅ Corrida entre o auto-save e o `release()/stopSelf` em `onTaskRemoved`

`onTaskRemoved` chama `saveCurrentState()` (PlaybackService.kt:301), que **lança uma coroutine IO** (linha 136) para escrever no Room — e em seguida, **sincronamente**, faz `stop()/release()/stopSelf()` (linhas 304-306). `stopSelf` → `onDestroy` → `serviceJob.cancel()` (linha 318) cancela o escopo onde a escrita foi lançada. Se a coroutine ainda não tiver gravado, **o save é perdido**.
**Atenuante:** a posição (`player.currentPosition`) é lida **antes** do release (linha 132), e no caminho pausado a posição já costuma ter sido salva pelo listener de pause. Por isso MÉDIO. Mesmo assim, o save "final" do `onTaskRemoved` é não-confiável por construção.

### 5.3. [BAIXO] ✅ `release()` duplo

`onTaskRemoved` libera o player (linha 305) e, na sequência, `onDestroy` libera de novo (linha 314). `ExoPlayer.release()` é idempotente, então não quebra, mas evidencia que o gerenciamento de ciclo de vida está espalhado e sem dono claro.

### 5.4. [MÉDIO] ✅ `CoilBitmapLoader` cria um threadpool que nunca é encerrado

`CoilBitmapLoader` instancia `Executors.newCachedThreadPool()` (PlaybackService.kt:553) e nada chama `shutdown()` no `onDestroy`. A cada ciclo de vida do serviço, um novo pool fica órfão. Vazamento pequeno mas real.

---

## 6. Cast — troca local ↔ Cast

### 6.1. [ALTO] ✅ Posição NÃO volta do Cast para o local ao desconectar

`onSessionStarted` (local→Cast) transfere a posição: `loadMediaOnCast(currentChapter, _uiState.value.currentPosition)` → `setPlayPosition` (PlayerViewModel.kt:223, :770). **Esse sentido funciona.**
O sentido inverso **não**: `onSessionEnded` só chama `syncStateWithController()` (linha 237), que lê a posição do **controller local** — congelada em onde estava quando o Cast começou (o local foi `pause()`-ado na linha 225). Nada lê `remoteMediaClient.approximateStreamPosition` para dar `seekTo` no local antes de assumir.
**Cenário do usuário:** começa a ouvir no celular aos 2 min, conecta na TV, ouve até os 25 min, desconecta → o celular retoma dos **2 min**, perdendo 23 min de progresso. Perda de estado claramente perceptível.

### 6.2. [ALTO] ✅ Cast recebe só a faixa atual, sem fila → auto-avanço quebrado

`loadMediaOnCast` carrega **um** `MediaInfo` (a faixa corrente), nunca a playlist (PlayerViewModel.kt:744-773). Quando a faixa termina no Cast, `checkCastCompletion` (linha 735) detecta `IDLE_REASON_FINISHED` e chama `skipToNextChapter()` — que opera no **controller local** (`seekToNextMediaItem`, linha 451) e **não** recarrega nada no Cast (`loadMediaOnCast` só é chamado em `onSessionStarted` e `onChapterSelected`).
**Cenário do usuário:** no Chromecast, ao fim de um capítulo, o áudio para; o índice local até avança, mas a TV não recebe a próxima faixa. Reprodução contínua na TV não funciona.

### 6.3. [MÉDIO] ✅ Cast assume Bíblia: metadados errados para Estudo/Tema

`loadMediaOnCast` monta `subtitle = "Capítulo ${chapter.chapter.number}"` e usa `chapter.chapter.audioUrl` (PlayerViewModel.kt:755, :762), tirados de `_uiState.value.chapters` — que para Estudos vêm de `extractChaptersFromPlayer` com `number` sintético e `id=0` (§1c). Content-type fixo `"audio/ogg"` (linha 764).
**Cenário:** ao castar um Estudo, a TV mostra "Capítulo 1/2/3..." em vez do título da aula; se o áudio não for OGG, o `contentType` fica incorreto. Funcional-degradado.

### 6.4. [MÉDIO] 🔬 `togglePlayPause`/`seekTo` divergem de fonte durante a transição de sessão

Os controles checam `castSession?.isConnected == true` para escolher alvo (PlayerViewModel.kt:418, :428). Durante `onSessionStarting`/`onSessionResuming` (que estão vazios, linhas 214, 240), `castSession` pode já estar setado mas o `remoteMediaClient` ainda não pronto, ou vice-versa. Há janela em que um toque em play/seek vai para o alvo errado (local enquanto migra para Cast). Precisa de device com Chromecast para confirmar o sintoma.

---

## Resumo por gravidade

| # | Achado | Gravidade | Confiança |
|---|---|---|---|
| 5.1 | ExoPlayer `@Singleton` liberado + START_STICKY → player liberado | **CRÍTICO** | 🔬 |
| 3b | Posição relativa ao clipping restaurada como absoluta | ALTO (latente) | ✅ |
| 6.1 | Posição não volta Cast→local ao desconectar | ALTO | ✅ |
| 6.2 | Cast sem fila → auto-avanço quebrado | ALTO | ✅ |
| 1c | `extractChaptersFromPlayer` assume Bíblia (id=0 p/ estudo) | MÉDIO | ✅ |
| 1d | Índice divergente entre os 2 caminhos da Bíblia | MÉDIO (latente) | ✅ |
| 2c | Janela de 5s pode cair no branch assíncrono em device lento | MÉDIO | 🔬 |
| 3c | Duração 0 no buffering → barra cheia no cold start | MÉDIO | ✅ |
| 3d | Sem save periódico → perde posição se morto no meio da faixa | MÉDIO | ✅ |
| 4b | `onPlaybackResumption` `@Deprecated` + corrida com restore | MÉDIO | ✅ |
| 5.2 | Save final do `onTaskRemoved` corre com release/cancel | MÉDIO | ✅ |
| 5.4 | Threadpool do `CoilBitmapLoader` nunca encerrado | MÉDIO | ✅ |
| 6.3 | Cast assume Bíblia (metadados errados p/ Estudo/Tema) | MÉDIO | ✅ |
| 6.4 | Alvo de controle divergente durante transição de sessão Cast | MÉDIO | 🔬 |
| 1e | `mediaId` malformado aborta restore sem log | BAIXO | ✅ |
| 5.3 | `release()` duplo | BAIXO | ✅ |

---

## Passo a passo de teste manual (itens 🔬)

### T1 — ExoPlayer liberado / START_STICKY (§5.1) — o mais importante
1. `./gradlew installDebug`; abrir o app e tocar um capítulo da Bíblia.
2. **Pausar** o player.
3. Ir para a lista de recentes e **fechar o app (swipe)** → notificação deve sumir (Clean Exit).
4. Sem matar mais nada, **reabrir o app pelo ícone** e tocar play em qualquer conteúdo.
5. Repetir 1–4 algumas vezes, alternando: reabrir via ícone, via botão de play do headset Bluetooth, e via Android Auto.
6. **Observar Logcat** filtrando `IllegalStateException` e `Player is released` / `Player is accessed on the wrong thread`. Crash ou player que não responde = §5.1 confirmado.
7. Cross-check: rodar `adb shell am kill br.app.ide.ouvindoabiblia` enquanto pausado e depois acionar play pelo headset (força a recriação por START_STICKY).

### T2 — Retomada após morte do processo (§4c, §3d)
1. Tocar Bíblia, deixar ~5 min **sem pausar**.
2. `adb shell am kill br.app.ide.ouvindoabiblia` (simula morte por memória).
3. Reabrir o app. **Esperado pelo código:** retoma do **início do capítulo** (não dos 5 min) → confirma §3d.
4. Repetir tentando retomar pelo **botão do headset** antes de abrir o app → confirma se §4c realmente retoma ou falha.

### T3 — Cast: posição e fila (§6.1, §6.2, §6.4)
1. Tocar Bíblia no celular, avançar até ~2 min.
2. Conectar a um Chromecast → confirmar que a TV começa **dos 2 min** (sentido local→Cast OK).
3. Na TV, ouvir até o **fim do capítulo** → observar se avança sozinho para o próximo (esperado: **não** avança → §6.2).
4. Avançar manualmente na TV até ~20 min e **desconectar** o Cast → observar de onde o celular retoma (esperado: **dos 2 min** → §6.1).
5. Castar um **Estudo** e olhar título/subtítulo na TV (esperado: "Capítulo N" em vez do título da aula → §6.3).
6. Tocar play/seek **durante** o "Conectando…" do Cast e ver se age no alvo certo (§6.4).

### T4 — Barra de progresso no cold start (§3c)
1. Tocar Bíblia, pausar durante o **buffering inicial** (rede lenta / liga modo avião por 1s ao dar play).
2. Fechar tocando (ou matar) e reabrir.
3. Observar o mini-player no cold start: barra cheia/100% momentânea antes do controller conectar = §3c confirmado.

---

## Recomendação de sequência para a passada de correção (fora do escopo desta leitura)
Prioridade pela relação gravidade × esforço: **5.1** (ciclo de vida do ExoPlayer — provavelmente trocar o `@Singleton` por instância dona do serviço, ou recriar no `onCreate`) → **6.1/6.2** (estado do Cast) → **3d** (save periódico) → **1c/1d** (unificar a convenção de `mediaId` num único parser/sealed type). Os itens latentes 3b e 1d devem ser corrigidos junto com 1c para não voltarem a morder quando recorte/`ChaptersScreen` forem religados.
