# AUDITORIA 01 — Prontidão de Release (Play Store)

**Data:** 2026-07-16 · **Branch:** `analise-media3` · **Modo:** somente leitura + builds (nada corrigido)
**Build executado:** `./gradlew bundleRelease` → **BUILD SUCCESSFUL** (41s)

Legenda severidade: **[BLOQUEIA]** = impede publicar · **[CORRIGIR ANTES]** = arrumar antes de subir ·
**[PODE ESPERAR]** = pós-launch. Símbolos: ✅ verificado OK · 🔬 precisa de verificação adicional.

---

## 1. Formato de publicação (AAB) — ✅

- **`./gradlew bundleRelease` gera AAB corretamente:** `app/build/outputs/bundle/release/app-release.aab`
  (9.5 MB). Tasks `packageReleaseBundle` → `signReleaseBundle` → `bundleRelease` rodaram. ✅
- **APK antigo no disco não vai pro repo:** `app/release/app-release.apk` (18/abr, build antiga) existe
  localmente, mas está coberto por `.gitignore` (`/app/release/`, `*.apk`) e **não é rastreado**
  (`git ls-files` limpo). ✅
- **Conclusão:** o pipeline de release produz AAB (formato exigido pela Play). O APK só serve para
  side-load/teste; nunca deve ser o artefato de publicação.

## 2. Assinatura — 🔴 [BLOQUEIA] 🔬

- **Não há `signingConfigs` em lugar nenhum.** `app/build.gradle.kts:23-31` — o buildType `release`
  define `minify`/`shrink`/`proguardFiles`, mas **nenhum `signingConfig`**. `grep` por
  `signingConfig|storeFile|keyAlias|keystore` em todos os `*.gradle(.kts)`/`*.properties` = **0 refs**.
- **Verificado empiricamente: o AAB saiu NÃO assinado.** `keytool -printcert -jarfile app-release.aab`
  não retornou certificado; não há bloco de assinatura (`*.RSA/.SF/.MF`) no `META-INF`.
- **Impacto:** a Play Console **rejeita** um AAB não assinado — é preciso assiná-lo com uma *upload key*
  antes do upload (mesmo usando Play App Signing, a assinatura de upload é obrigatória).
- **Segredos (o lado bom):** ✅ `.gitignore` cobre `*.jks`, `*.keystore`, `keystore.properties`,
  `local.properties`, `*.aab`, `*.apk`. Nada sensível rastreado. `local.properties` só contém
  `sdk.dir` (sem credenciais).
- **Ação p/ desbloquear:** criar uma upload keystore FORA do git + `signingConfig` lendo de
  `keystore.properties`/variáveis de ambiente; **ou** publicar via wizard *Build > Generate Signed
  Bundle* do Android Studio (assina sem `signingConfig` no Gradle). Em ambos os casos a keystore
  precisa ser criada — hoje ela não existe no projeto.

## 3. R8 / Minify / ProGuard — ✅ (estático) + 🔬 (runtime pendente)

- **Config correta:** `app/build.gradle.kts:25-26` — `isMinifyEnabled = true`,
  `isShrinkResources = true`. ✅ (bom para tamanho/desempenho).
- **Todos os `proguard-rules.pro` e `consumer-rules.pro` estão VAZIOS** (só comentários default) —
  0 keep rules customizadas. **Porém** as libs de reflexão trazem regras embutidas, e confirmei em
  `app/build/outputs/mapping/release/configuration.txt` que foram aplicadas:
  - `kotlinx-serialization-r8.pro` + `kotlinx-serialization-common.pro` ✅
  - `retrofit2.pro` ✅
- **Sem `missing_rules.txt`** → o R8 não pediu `-dontwarn`/keep adicionais.
- **Serializers sobreviveram ao shrink (o risco clássico):** em `mapping.txt` há **48 `$$serializer`**
  gerados mantidos — ex.: `BibleResponseDto$$serializer`, `BookDto$$serializer`,
  `ChapterDto$$serializer`, `MetaDto$$serializer`, todos com `INSTANCE`. É exatamente o que quebraria
  o parsing do JSON do servidor se fosse removido — e está intacto. ✅
- **Hilt/Room/Media3:** geram código (não dependem de reflexão frágil em runtime) e trazem consumer
  rules próprias; sem sinais de remoção indevida.
- 🔬 **Pendente:** *smoke test no build de release real* (não debug) — exercer sync do JSON
  (`biblia_index/themes/estudos/mais`) + playback Media3. **Bloqueado pela ausência de assinatura**
  (item 2): sem AAB/APK assinado não dá pra instalar o release no device. Fazer assim que a assinatura
  estiver configurada. Evidência estática acima reduz muito o risco, mas não substitui o runtime.

## 4. debuggable / logging de debug — ⚠️ [CORRIGIR ANTES] (1 achado) + resto ✅

- **`debuggable` do release:** não é declarado → default do AGP para `release` é `debuggable=false`. ✅
- **StrictMode:** `OuvindoBibliaApp.kt:19,26-27` — `if (isDebuggable()) enableStrictMode()`, gated por
  `FLAG_DEBUGGABLE`. Em release (não-debuggable) **não roda**. ✅
- **LeakCanary e tooling de debug:** `app/build.gradle.kts:110-112` — `leakcanary.android`,
  `compose.ui.tooling` e `compose.ui.test.manifest` são `debugImplementation` → **fora do release**. ✅
- ⚠️ **OkHttp logging em nível BODY sem gate:** `NetworkModule.kt:36-41` —
  `HttpLoggingInterceptor().apply { level = Level.BODY }` é **incondicional**, logando corpo/headers
  de toda requisição no logcat também em release. Conteúdo aqui é JSON público (risco de vazamento
  baixo) e o header `User-Agent: BibliaFaladaApp` do WAF, mas há custo de desempenho e ruído.
  **Recomendação:** `Level.NONE` em release (gate por `BuildConfig.DEBUG`/flag injetada).
  **[CORRIGIR ANTES]** (não bloqueia, mas é higiene de release).

## 5. versionCode / versionName — ✅

- `app/build.gradle.kts:17-18` — `versionCode = 1`, `versionName = "1.0"`. Coerente para a 1ª
  publicação; `versionCode` é inteiro. ✅
- **Lembrete operacional:** cada novo upload à Play exige `versionCode` estritamente maior.

## 6. targetSdk / compileSdk — ✅ (e à prova da próxima exigência)

- **Valores reais (a premissa do prompt está desatualizada):** `app/build.gradle.kts:11,16` —
  `compileSdk = 36`, `targetSdk = 36`. Os 3 módulos lib (`data/local`, `data/remote`,
  `data/repository`) também estão em `compileSdk = 36`. **compileSdk unificado em 36.** ✅
  (O `CLAUDE.md` que diz "app compila em compileSdk 35" está stale.)
- **Exigência atual da Play (pesquisado hoje):** até **31/08/2026**, apps novos e updates precisam ter
  `targetSdk ≥ 35` (Android 15). A partir de **31/08/2026**, passa a exigir `targetSdk ≥ 36`
  (Android 16). **O app já está em 36** → atende hoje **e** já cumpre a próxima exigência. ✅
- `minSdk`: app = 26, libs = 24 (efetivo do app = 26). OK.

## 7. Permissões (manifest merged do release) — ✅

Fonte: `app/build/intermediates/merged_manifests/release/.../AndroidManifest.xml`.

| Permissão | Origem | Justificativa | Status |
|-----------|--------|---------------|--------|
| `INTERNET` | app (`AndroidManifest.xml:5`) | baixar JSON + streaming de áudio | ✅ |
| `ACCESS_NETWORK_STATE` | app (`:6`) | checar conectividade | ✅ |
| `FOREGROUND_SERVICE` | app (`:9`) | serviço de playback | ✅ |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | app (`:11`) | FGS tipado (Android 14+) | ✅ |
| `POST_NOTIFICATIONS` | app (`:19`) | controles de mídia na notificação (ISSUE 5.A) | ✅ |
| `WAKE_LOCK` | merge do Media3/ExoPlayer | manter CPU no playback com tela apagada | ✅ |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | auto-gerada (androidx.core) | permissão *signature-level* interna p/ receivers não-exportados; não é user-facing | ✅ |

- **Nenhuma permissão sensível** (sem localização, contatos, storage, microfone, câmera, telefone). ✅
- **A declarar no Console (não é bug de código):** o formulário de Foreground Service da Play pedirá
  justificativa do `mediaPlayback` — trivial de justificar (app de áudio). `PlaybackService` já
  declara `foregroundServiceType="mediaPlayback"` (`AndroidManifest.xml:42`). ✅

## 8. applicationId / label / ícone — ✅

- **applicationId:** `br.app.ide.ouvindoabiblia` (`app/build.gradle.kts:14`) — id de **produção**, sem
  sufixo `.debug`. ✅
- **label:** `@string/app_name = "Ouvindo A Biblia"` (`res/values/strings.xml`). ✅
  - Nota cosmética **[PODE ESPERAR]:** "Biblia" sem acento; o nome próprio é "Bíblia". Ajustar se
    quiser o acento no launcher.
- **Ícone:** custom (livro com botão de play, fundo creme), adaptativo (`mipmap-anydpi-v26` +
  densidades webp + `ic_launcher_foreground`). **Não** é o robô verde do template. ✅

---

## 🔴 BLOQUEIA (resolver antes de publicar)

1. **[#2] Assinatura ausente** — `bundleRelease` produz um **AAB não assinado** (sem `signingConfig`
   em `app/build.gradle.kts`; nenhuma keystore no projeto; confirmado por `keytool` que o AAB não tem
   certificado). A Play rejeita AAB não assinado. **Criar upload keystore (fora do git) + signingConfig**
   (ou usar o wizard *Generate Signed Bundle*) antes de subir.

2. **[#3, dependente do #1] Smoke test do release não executado** 🔬 — a verificação runtime do build
   ofuscado (sync do JSON + Media3) está **bloqueada** porque não há artefato de release instalável sem
   assinatura. A evidência estática (serializers preservados no `mapping.txt`, consumer rules aplicadas)
   é forte, mas o smoke no device deve ser feito assim que o #1 for resolvido, antes do rollout.

## 🟡 CORRIGIR ANTES (não bloqueia, mas arrumar antes do rollout)

- **[#4] OkHttp `Level.BODY` sem gate** (`NetworkModule.kt:37`) — desligar em release
  (`Level.NONE` / gate por debug).

## 🟢 PODE ESPERAR

- **[#8]** Acento em "Ouvindo A Biblia" → "Bíblia" no `app_name`.
- **Lembrete:** incrementar `versionCode` a cada upload.

## Resumo

| # | Item | Severidade |
|---|------|-----------|
| 1 | Formato AAB | ✅ |
| 2 | Assinatura | 🔴 BLOQUEIA |
| 3 | R8/minify (estático OK; runtime pendente) | ✅ / 🔬 |
| 4 | debuggable ✅ · OkHttp logging | 🟡 CORRIGIR ANTES |
| 5 | versionCode/Name | ✅ |
| 6 | targetSdk/compileSdk (36, unificado) | ✅ |
| 7 | Permissões | ✅ |
| 8 | applicationId/label/ícone | ✅ |

**Veredito:** o projeto está tecnicamente muito perto de publicável — build gera AAB, targetSdk já à
frente da exigência, permissões limpas, R8 com serializers preservados. **O único bloqueio real é a
assinatura** (#2); resolvido isso, fazer o smoke do release (#3) e desligar o log do OkHttp (#4).
