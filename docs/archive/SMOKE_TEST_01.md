# SMOKE TEST 01 — Build DEBUG no device

**Data:** 2026-07-16 · **Device:** moto g53 5G, Android 14 (SDK 34) · **Build:** `installDebug`
(não é o release assinado) · **Conexão:** adb-over-wifi · **Branch:** `analise-media3`.

**Regra de honestidade:** o testador automatizado (Claude) **não ouve áudio nem julga aparência**.
Itens 👂 (som) e 👁️ (visual) foram **confirmados pelo usuário**. O que está como ✅ objetivo veio de
`dumpsys media_session` / `logcat` / screenshot. Screenshots em `scratchpad/smoke/` (temporário, não
versionado).

Legenda: ✅ passou · ⚠️ parcial · ⛔ não testável nesta configuração · 👂/👁️ confirmado pelo usuário.

---

## A. Navegação — ✅
- Home, Temas, Estudos, Mais, Favoritos **abrem sem nenhum FATAL** (logcat limpo). Detalhe de Estudo
  (Apocalipse) e de Tema (Ansiedade e confiança) abrem.
- 👁️ Usuário confirmou: grade de livros e telas **visualmente corretas** ("tudo certo").
- Bônus: **empty state de Favoritos** funciona ("Sua lista de capítulos bíblicos está vazia").

## B. Playback — ✅ (estado) + 👂 (som confirmado pelo usuário)
- **Bíblia (Gênesis 1):** `state=PLAYING`, **posição avança** (38786→41794 ms em ~3 s).
- **Transporte:** pause→`PAUSED`; resume→`PLAYING`; next→**Gênesis 2** (pos 0); prev→**Gênesis 1**.
- **Estudo:** troca de fonte OK — metadata → "Estudos Expositivos em Apocalipse", `PLAYING`.
- **Tema:** troca de fonte OK — metadata → "Mateus 6 / Ansiedade e confiança em Deus", `PLAYING`.
- 👂 Usuário confirmou: **som limpo** na Bíblia; **Estudo e Tema também tocaram**.

## C. Features
- **Velocidade — ✅:** SpeedSheet abre (0.5x–2.0x); aplicar 1.5x → `speed=1.0→1.5` no dumpsys.
- **Sleep timer — ⚠️ parcial:** SleepTimerSheet abre (5 min–1 h); seleção aplica (sheet fecha). O
  **arme/contagem não é objetivamente verificável** por adb (não exposto no `media_session`, sem badge
  visível; exigiria observar ~5 min). Comportamento em si já foi validado na ISSUE 5.C anteriormente.
- **Favorito (capítulo) — ✅:** desfavoritar Gênesis → **some de Favoritos**; refavoritar → **reaparece**
  no topo + coração enche (code path da 6.G).
- **Favorito (aula de estudo) — não exercitado** (mesmo code path do capítulo, que passou).

## D. Robustez
- **5.1 Persistência — ✅:** em background segue `PLAYING` (serviço `isForeground=true`, notificação de
  mídia `category=transport` com 3 ações). `am kill` **não** mata o FGS (esperado); via **force-stop** e
  reabrir → restaura **`PAUSED`**, "Gênesis 2" na posição salva, **sem auto-tocar**.
- **D2 Falha de rede — ✅ (testado manualmente pelo usuário):** 1º uso sem rede/cache vazio →
  **ErrorScreen + Retry**; religar rede → Retry carrega.
- **D3 Erro de playback / valida PUB-02 — ✅ (testado manualmente pelo usuário):** rede fora no meio →
  após os retries do ExoPlayer, aparece o **Toast** "Não foi possível reproduzir…".

## E. StrictMode / ANR — ✅
- Cold start + navegação por todas as abas + playback: **nenhuma violação** `StrictMode policy
  violation` no logcat; **sem ANR/FATAL**.

---

## 🐞 Bug encontrado durante o smoke (e corrigido)

**Favorito do item restaurado não refletia no player** — commit **`ef96f85`** (follow-up da 6.G).
- **Sintoma:** favoritar um capítulo → coração enche e entra em Favoritos; **matar o app e reabrir** →
  favorito continua salvo (aparece em Favoritos), mas o coração no **mini/full player volta vazio**.
- **Causa:** o observador de favorito do item atual só era ligado no `EVENT_MEDIA_ITEM_TRANSITION`, que
  **não dispara no cold-start restore** (o item já é o atual quando o `MediaController` conecta) → o
  `currentIsFavorite` ficava no default (false).
- **Fix:** extraído `observeFavoriteForCurrentItem()` (valor otimista do metadata + observador de DB) e
  passou a ser chamado **também no connect** (`initializeController`), além do handler de transição.
- **Validado no device** (favoritar → force-stop → reabrir → **coração preenchido**), confirmado pelo
  usuário e por screenshot.

---

## ⛔ Não testável nesta configuração / pendente
- **D2/D3 automatizados por adb:** desligar wifi/dados derrubaria o **adb-over-wifi** (houve 1 drop no
  meio, reconectado). Foram feitos **manualmente pelo usuário** (acima). Para automação → **USB**.
- **Bateria no APK de RELEASE assinado** (PUB-10..16): depende do **PUB-01** (keystore do dono).
- **Android 8 (API 26), Android Auto, ligação real:** fora do alcance desta passada.
- **Sleep timer (contagem) e favorito de aula de estudo:** não exercitados objetivamente aqui.

## Gotchas de device reconfirmados
- `adb` não está no PATH → usar `/home/glauberthy/Android/Sdk/platform-tools/adb`.
- `am kill` **não** mata foreground service → usar `am force-stop`.
- Full player **só expande por SWIPE** up (não por tap).
- Sem `sqlite3` no device → usar a **tela de Favoritos como verdade** (ou `pull` do `bible_db`+`wal`).
- `sleep` de host bloqueado → usar `adb shell sleep N`.

## Veredito
No build **DEBUG**, o núcleo funcional está **sólido**: navegação sem crashes, playback e transporte
corretos nas 3 fontes (Bíblia/Estudo/Tema), velocidade, favorito de capítulo, persistência de sessão,
falha de rede (Error+Retry), erro de playback com feedback (PUB-02) e StrictMode limpo. O único defeito
encontrado (favorito no restore) **já foi corrigido**. Falta a validação no **APK de release assinado**
(pós PUB-01) e os cenários de device antigo/Auto.
