# AUDITORIA 03 — Qualidade de Código para Produção

**Data:** 2026-07-16 · **Branch:** `analise-media3` · **Modo:** somente leitura + `:app:lint`.
**Base:** DIAGNOSTICO_01/02, ROADMAP.md. Foco em manutenção/estabilidade, não estilo.

Legenda: **[BLOQUEIA]** · **[CORRIGIR ANTES]** · **[PODE ESPERAR]** · ✅ · 🔬.
**Nenhum [BLOQUEIA] neste eixo.** Vários pontos do Diag01 já foram sanados (ver notas).

---

## 1. Código morto — ✅ (em grande parte já resolvido)

- **`ChaptersScreen`/`ChaptersViewModel`: JÁ REMOVIDOS.** Não existe mais `ui/chapters/` nem classe
  `Chapters*`. O único arquivo com "chapters" no nome é `ui/player/components/ChaptersSheet.kt` — vivo
  (o bottom-sheet de capítulos do player). O item do Diag01 §5 sobre Chapters está **fechado**. ✅
- **Destinos/telas órfãos:** heurística de "fun *Screen sem chamador" = 0 telas órfãas (todas têm
  chamador; só `PreviewEmptyStudiesScreen` tem 0, mas é `@Preview`). A FASE 7 (commits `372e80b`→
  `6cc694f`) já removeu Screen.Player/About/Copyright, repo/DAO/DTO mortos e seções mortas da Home. ✅
- **Imports não usados:** o lint padrão do AGP **não** checa unused imports (é inspeção de IDE); não
  há sinal de imports órfãos após a FASE 7 (build compila limpo). Recomendação [PODE ESPERAR]: rodar
  "Optimize Imports" no Android Studio antes do tag de release.
- **Cast dormente NÃO é código morto** — é desligado por design (ver item 2), mantido para reativar.

## 2. Cast desligado — ✅ (kill-switch limpo, nada meio-ligado)

- **`initializeCast()` só é chamado atrás do flag:** `PlayerViewModel.kt:245` —
  `if (CastConfig.ENABLED) initializeCast()`, e `CastConfig.ENABLED = false` (`CastConfig.kt:19`).
  → `CastContext.getSharedInstance` (I/O de disco na main) **nunca** roda no fluxo normal. ✅
- **Botão escondido:** `SharedPlayerScreen.kt:399` — `CastButton(...)` comentado. ✅
- **Sem I/O na main** por conta do Cast (confirmado também na AUDITORIA_02 §7). ✅
- **Nada meio-ligado:** `CastOptionsProvider` continua registrado no manifest
  (`OPTIONS_PROVIDER_CLASS_NAME`), mas essa meta-data só é lida **se** o framework inicializar — o que
  não ocorre com `getSharedInstance` nunca chamado. `CastButton.kt`/`CastOptionsProvider.kt` ficam
  dormentes, prontos para religar (flip do flag + descomentar). ✅
- Nota [PODE ESPERAR]: a dependência `play-services-cast-framework` continua no APK (bloat pequeno);
  aceitável enquanto o Cast é backlog.

## 3. Padrão LCE/MVI — ✅ melhorou muito desde o Diag01; resta 1 divergência aceitável

O Diag01 §5 listava 3 telas "fora do padrão" + 2 "parciais". **Estado atual (reverificado):**

| Tela | UiState LCE selado | Intent selado | Contract | Veredito atual |
|------|--------------------|---------------|----------|----------------|
| Home / Themes / Studies / StudyDetails | ✅ | ✅ | ✅ | **No padrão** |
| **More** | ✅ | ✅ (`MoreIntent.Retry`) | `MoreContract.kt` | **No padrão** (era "parcial") |
| **MoreSectionDetails** | ✅ | ✅ | `MoreSectionDetailsContract.kt` | **No padrão** (era "parcial") |
| **Favorites** | ✅ Loading/Success/Error | ✅ (`FavoritesIntent`) | `FavoritesContract.kt` | **No padrão** (era "fora") |
| Player | ❌ flat `PlayerUiState` | ❌ (métodos diretos) | — | **Fora — aceitável** |

- **`MoreSectionDetailsRoute.k.kt` (nome quebrado): JÁ RENOMEADO** para `MoreSectionDetailsRoute.kt`
  (`find *.k.kt` = 0). Item do Diag01 §5 **fechado**. ✅
- **Único fora do padrão:** `Player` — `PlayerUiState` é data class plana dirigida por chamadas de
  método no VM (`PlayerUiState.kt:50`), não por `Intent`. É **[PODE ESPERAR]**: um player com muitos
  comandos transacionais (play/pause/seek/speed/sleep) diverge naturalmente do MVI; refatorar para
  Intent é dívida de consistência, não de estabilidade.

## 4. TODO / FIXME / HACK — ✅

- Grep por `TODO|FIXME|HACK|XXX|TBD|WIP` em `app` + `data/*` (`src/main`): **0 marcadores reais**.
  As únicas ocorrências de "todo" são a palavra portuguesa ("todo novo schema", "todo o App"). ✅
- Nenhum atalho crítico deixado no caminho de produção.

## 5. Logs — ⚠️ [CORRIGIR ANTES] (1) + [PODE ESPERAR] (1)

- **OkHttp em `Level.BODY` sem gate (vai pra produção):** `NetworkModule.kt:37` —
  `HttpLoggingInterceptor().apply { level = Level.BODY }` incondicional. Loga corpo/headers de toda
  requisição no logcat também em release. **[CORRIGIR ANTES]** (também citado na AUDITORIA_01): usar
  `Level.NONE` (ou `BASIC`) em release, gated por `BuildConfig.DEBUG`/flag injetada. Conteúdo aqui é
  JSON público (risco de vazamento baixo), mas é higiene de release e custo de desempenho.
- **20 `Log.*` em `src/main`** — sem dados sensíveis (logam `mediaId`, ids de conteúdo, estados de
  lifecycle; **nenhum token/PII**). ✅ quanto a vazamento. Porém há **`Log.i` verbosos de lifecycle**
  (`PlaybackService.kt:126,380,391,398,406,441,449,452` tag `PLAYBACK_LC`; `PlayerViewModel.kt:309`)
  que são instrumentação de debug e **não são removidos no release** (R8 não strippa `Log` por
  padrão). **[PODE ESPERAR]:** adicionar `-assumenosideeffects class android.util.Log { *; }` ao
  `proguard-rules.pro` para stripar no release, mantendo `Log.w/Log.e` úteis para diagnóstico se
  desejar (ou rebaixar os `Log.i` de lifecycle). Nada disso bloqueia.

## 6. Lint (`./gradlew :app:lint`) — ✅ 0 errors, 77 warnings (todos higiene)

Nenhum warning de **correção** ou **segurança** — só higiene/manutenção:

| Warning | Qtd | Categoria | Severidade |
|---------|-----|-----------|------------|
| `Typos` | 35 | comentários pt-BR flagrados como typo | [PODE ESPERAR] |
| `GradleDependency` | 23 | libs com versão mais nova | [PODE ESPERAR] |
| `NewerVersionAvailable` | 11 | idem | [PODE ESPERAR] |
| `AndroidGradlePluginVersion` | 3 | AGP mais novo disponível | [PODE ESPERAR] |
| `MonochromeLauncherIcon` | 2 | ícone adaptativo sem tag monochrome (themed icons Android 13+) | [PODE ESPERAR] |
| `ObsoleteSdkInt` | 1 | pasta `mipmap-anydpi-v26` redundante (minSdk já é 26) | [PODE ESPERAR] |
| `IconLocation` | 1 | `ic_splash_logo.webp` em pasta sem densidade | [PODE ESPERAR] |

- **`ObsoleteSdkInt`** é sobre um qualificador de recurso (`-v26`) redundante, **não** um branch de
  código morto — sem impacto de runtime.
- **Nenhum** warning de `ExportedService`/`Security`/`HardcodedText` de correção (o serviço exportado
  é `tools:ignore` justificado; ver AUDITORIA_01 §7). ✅
- Report: `app/build/reports/lint-results-debug.{txt,html}`.

## 7. Hardcoded — ✅ (URLs) + [PODE ESPERAR] (strings de UI)

- **URLs hardcoded no código são todas de `@Preview`:** `MoreScreen.kt:647-767` está dentro de
  `previewMoreContent()` (`:603`), usada só pelos `@Preview` (`:784+`); `ThemesScreen.kt:288` está em
  `previewThemes()` (`:283`). São fixtures de dev ("example.com", unsplash, "João da Silva") — **não**
  aparecem em produção (o conteúdo real vem do JSON do servidor). ✅
- **`BASE_URL` hardcoded** (`NetworkModule.kt:21`) — é config, não string de UI; aceitável (única
  fonte, fácil de achar). ✅
- **Strings de UI:** `strings.xml` tem **1 entrada** (`app_name`); praticamente **todo** texto de UI é
  literal pt-BR no código (ex.: `HomeViewModel.kt:65` "Nenhum livro disponível…", `ThemesViewModel.kt:54`),
  e **0 usos de `stringResource`**. Para um app **pt-BR-only** (por design, ver CLAUDE.md) isto é
  **[PODE ESPERAR]** — só vira problema se for internacionalizar. É dívida de i18n, não de estabilidade.

---

## Sumário

| # | Item | Veredito |
|---|------|----------|
| 1 | Código morto | ✅ (Chapters já removido; FASE 7 limpou o resto) |
| 2 | Cast desligado | ✅ kill-switch limpo |
| 3 | LCE/MVI | ✅ melhorou muito; só Player fora (aceitável); `.k.kt` já renomeado |
| 4 | TODO/FIXME/HACK | ✅ nenhum |
| 5 | Logs | ⚠️ OkHttp BODY [CORRIGIR ANTES]; Log.i lifecycle [PODE ESPERAR]; sem dados sensíveis |
| 6 | Lint | ✅ 0 errors / 77 warnings, todos higiene |
| 7 | Hardcoded | ✅ URLs só em @Preview; strings de UI = dívida de i18n [PODE ESPERAR] |

## 🟡 CORRIGIR ANTES
- **[#5] OkHttp `Level.BODY` em release** (`NetworkModule.kt:37`) — gate por debug (→ `NONE`/`BASIC`).
  (Mesmo achado da AUDITORIA_01; é o único item de qualidade que vale mexer antes de publicar.)

## 🟢 PODE ESPERAR (dívida de manutenção, não de estabilidade)
- Stripar `Log` no release (`-assumenosideeffects`) ou rebaixar os `Log.i` de lifecycle (`PLAYBACK_LC`).
- 77 warnings de lint (35 typos, 37 bumps de dependência/AGP, 2 ícone, 1 folder v26, 1 icon location).
- `Player` fora do padrão MVI (flat + método-driven) — consistência, não risco.
- i18n: mover strings de UI para `strings.xml` se for internacionalizar.
- "Optimize Imports" antes do tag de release.

**Veredito geral:** a base de código está **madura para publicar** do ponto de vista de qualidade —
código morto já foi removido (Chapters + FASE 7), Cast tem kill-switch limpo, sem TODO/HACK crítico,
lint sem erros, e o padrão LCE/MVI melhorou bastante desde o Diag01 (só o Player diverge, o que é
aceitável). O único ajuste recomendado antes do rollout é desligar o log BODY do OkHttp em release
(#5); todo o resto é dívida de manutenção pós-launch.
