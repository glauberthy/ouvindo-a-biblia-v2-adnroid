# SMOKE TEST 02 — Build de RELEASE (R8, assinado) no device

**Data:** 2026-07-16 · **Device:** moto g53 5G, Android 14 (SDK 34) · **Build:** `assembleRelease`
(APK **ofuscado por R8** e **assinado**) · **applicationId:** `ag.uny.ouvindoabiblia` · vc **4** / vn **3.0**.
Mesma regra de honestidade do SMOKE_TEST_01: 👂 (som) confirmado pelo usuário; ✅ objetivo vem de
`dumpsys`/`logcat`/screenshot.

> Este é o teste que valida a publicação de verdade — o R8 poderia ter quebrado serialização/Media3.

## Instalação / assinatura — ✅
- **APK assinado com a upload key do dono:** `apksigner` → Signer DN `CN=Ouvindo a Biblia`,
  **SHA-256 `842d3a33…`** (= impressão `84:2D:3A:33:…` esperada), **APK Signature Scheme v2 = true**.
- Desinstalado o debug antigo (`br.app.ide.ouvindoabiblia`); instalado o release
  (`ag.uny.ouvindoabiblia`). `dumpsys package` → **versionCode=4, versionName=3.0**, minSdk 26,
  targetSdk 36.

## A. Cold start — ✅
- App abre; `ResumedActivity = ag.uny.ouvindoabiblia/br.app.ide.ouvindoabiblia.MainActivity`.
- **Sem FATAL/crash** no logcat.

## B. Sync/parse do JSON no release (risco nº1 do R8) — ✅
- **As 4 telas carregaram o conteúdo do servidor** (instalação limpa → sincronizou do zero):
  Home (grade de livros), Temas (lista de temas), Estudos (lista de séries), Mais (seções).
- **Zero** `SerializationException` / `kotlinx.serialization` / `MissingFieldException` /
  `JsonDecodingException` / `VerifyError` / `ClassNotFound` no logcat. → **R8 NÃO quebrou a
  serialização** (as consumer rules de kotlinx-serialization/Retrofit + os `$$serializer` preservados,
  já vistos na AUDITORIA_01, se confirmam em runtime).

## C. Playback Media3 nas 3 fontes — ✅ (estado) + 👂 (som confirmado pelo usuário)
- Bíblia (3 João), Estudo ("Estudos Expositivos em Apocalipse"), Tema ("Mateus 6 / Ansiedade e
  confiança") → todos `state=PLAYING` no `dumpsys media_session`.
- 👂 Usuário confirmou: **as 3 tocaram com som limpo** no build de release.

## D. Persistência 5.1 — ✅
- Tocando → HOME (background) segue tocando → **force-stop** → reabrir → restaura "3 João 3" em
  **`PAUSED`** (sem auto-tocar), sem FATAL.

## E. Estabilidade — ✅
- Nenhum FATAL/crash na sessão inteira do release. (StrictMode não roda no release, por design.)
- Ruído benigno: `NoSuchMethodException` do tag **MQD** (diagnóstico de MessageQueue da Motorola) —
  componente do sistema, não do app.

## Observações / não coberto nesta passada
- **Uso simultâneo:** durante o teste o usuário usou o WhatsApp no mesmo aparelho; alguns screenshots
  pegaram o WhatsApp (não houve dano — foi o usuário). A automação por toque exige o aparelho ocioso.
- **PUB-11 (falha de rede) e PUB-12 (erro de playback/PUB-02)** foram validados **manualmente pelo
  usuário no build debug** (SMOKE_TEST_01); não é caminho sensível ao R8, mas vale um passe no release.
- **PUB-13 (POST_NOTIFICATIONS negado)** e **PUB-16 (Android 8/API 26)**: não testados aqui.

## Veredito
O **build de release ofuscado está funcional e publicável do ponto de vista técnico**: assina com a
upload key correta, instala como `ag.uny.ouvindoabiblia` vc4/vn3.0, abre sem crash, **sincroniza e
parseia os 4 JSONs sem erro de R8/serialização** (o maior risco), toca as 3 fontes (som confirmado) e
restaura a sessão. Fecha o **PUB-10** e confirma o núcleo do **PUB-01** (assinatura de release OK).
