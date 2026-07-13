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
- 🅿️ **Cast** (§6.1–6.4) — **estacionado** por decisão (sem Chromecast pra validar).
- ⏭️ **Próximo:** FASE 3 (3.C/3.E baratos → 3.A/3.B grande) ou 4.C (higiene + vazamento do LeakCanary).
- 📝 **Nota:** LeakCanary (debug) acusou um vazamento — investigar na 4.C (higiene).

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

### ISSUE 3.A — Camada de domínio / parar de vazar DTO e Entity pra UI (§ Diag01 2)

- **Problema:** `:app` depende direto de `:data:local` e `:data:remote`; UI consome `*Entity` e
  `*Dto` sem mapeamento. Mudança no JSON/schema quebra a UI direto.
- **Arquivos:** `BibleRepository.kt`, ViewModels de `more/themas/studies/favorites/player`,
  `build.gradle.kts` do `:app`.
- **Critério de aceitação:** repositório expõe modelos de domínio; `:app` deixa de importar
  `data.remote.dto` e `data.local.entity`.
- **Validação:** build + grep dos imports. **Esforço:** G · **Depende de:** nada (grande; pode ser
  fatiado por feature).

### ISSUE 3.B — Orquestração de loading duplicada nas ViewModels (§ Diag01 3b)

- **Problema:** a lógica "tem cache? falha de sync é silenciosa" está copiada em 4 VMs.
- **Arquivos:** `Home/Themes/Studies/More ViewModel`, `BibleRepositoryImpl.kt`.
- **Critério de aceitação:** repositório expõe `Flow<Resource<T>>` (Loading/Success/Error); VMs
  param de reimplementar.
- **Validação:** build + telas funcionando. **Esforço:** M · **Depende de:** 3.A (ideal junto).

### ISSUE 3.C — Telas fora do padrão LCE/MVI + rename de arquivo (§ Diag01 5)

- **Problema:** Favorites/Chapters/Player sem UiState selado; More/MoreSectionDetails sem
  Intent/Contract; arquivo `MoreSectionDetailsRoute.k.kt` (nome quebrado).
- **Arquivos:** telas citadas + renomear `MoreSectionDetailsRoute.k.kt` → `.kt`.
- **Critério de aceitação:** padrão consistente nas telas (ou exceção documentada para o Player);
  arquivo renomeado.
- **Validação:** build. **Esforço:** M · **Depende de:** nada.

### ISSUE 3.D — Decidir destino do `ChaptersScreen` morto (§ Diag02 0.2)

- **Problema:** `ChaptersScreen`/`ChaptersViewModel` nunca são navegados; parser `"|"` órfão.
  Religar acorda os bugs 1d/2.D.
- **Arquivos:** `ui/chapters/*`, `NavigationGraph.kt`, `MainScreen.kt`.
- **Critério de aceitação:** decisão de produto — remover o código morto OU religar a tela (e então
  corrigir 1d/2.D junto).
- **Validação:** sem device (decisão) + build. **Esforço:** P (remover) / M (religar) · **Depende
  de:** 2.A se religar.

### ISSUE 3.E — Unificar compileSdk/targetSdk (§ Diag01 4a)

- **Problema:** `:app` em 35, libs em 36; lint sinaliza `targetSdk=35`.
- **Arquivos:** `build.gradle.kts` de todos os módulos.
- **Critério de aceitação:** versões coerentes; build limpo.
- **Validação:** build. **Esforço:** P · **Depende de:** nada.

---

## FASE 4 — Polimento & features

### ISSUE 4.A — Opção A: notificação persiste no estado pausado após fechar app

- **Problema:** decisão de produto adiada. Hoje pausado+swipe remove a notificação (efetivamente
  opção B). Para opção A, a notificação precisa virar dismissível em vez de sumir sozinha.
- **Arquivos:** `service/PlaybackService.kt` (notificação/foreground).
- **Critério de aceitação:** pausado + fechar app → notificação permanece; usuário pode dispensá-la
  manualmente.
- **Validação:** device. **Esforço:** M · **Depende de:** nada (baixa prioridade).

### ISSUE 4.B — `onPlaybackResumption` `@Deprecated` (§ Diag02 4b)

- **Problema:** sobrescrita marcada como deprecated no Media3; risco em upgrade.
- **Critério de aceitação:** alinhar com a API atual recomendada do Media3.
- **Validação:** device. **Esforço:** M · **Depende de:** nada.

### ISSUE 4.C — Android Auto (declarar) / StrictMode / higiene de lint (100 warnings)

- Itens menores de polimento; agrupar conforme conveniência. **Esforço:** P cada.

---

## 🅿️ ESTACIONADO — Cast (reativar quando houver Chromecast)

- §6.1 posição não volta Cast→local · §6.2 sem fila (auto-avanço) · §6.3 metadados errados p/
  Estudo-Tema · §6.4 alvo divergente na transição. Todos exigem device + Chromecast para validar.

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