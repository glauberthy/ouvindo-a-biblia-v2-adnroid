# ✅ CHECKLIST DE PUBLICAÇÃO — Go/No-Go (Play Store)

**Consolidado das 4 auditorias** (`docs/archive/AUDITORIA_01_RELEASE.md`, `…_02_ROBUSTEZ.md`,
`…_03_CODIGO.md`, `…_04_PLAYSTORE.md`) · Gerado em 2026-07-16 · Branch `analise-media3`.

> Roteiro final para "apertar publicar". Só entram itens **[BLOQUEIA]** e **[CORRIGIR ANTES]**.
> Marque `[x]` ao concluir. **Regra de GO:** todos os 🔴 marcados + todos os 🟡 marcados (ou aceitos
> conscientemente). Itens [PODE ESPERAR] ficam no rodapé como backlog pós-launch (não bloqueiam).

---

## 🔴 BLOQUEIA — sem isto a Play NÃO publica

### A. Código / Build
- [ ] **A1. Assinar o release** *(Audit 01 §2)* — hoje `./gradlew bundleRelease` gera um **AAB não
  assinado** (não há `signingConfig` em `app/build.gradle.kts:23-31`; confirmado por `keytool`).
  → Criar upload keystore **fora do git** + `signingConfig` lendo de `keystore.properties`/env
  (ou publicar via wizard *Build > Generate Signed Bundle*). `.gitignore` já cobre `*.jks`/
  `keystore.properties` ✅.
- [ ] **A2. Confirmar que o AAB assinado sobe no Console** — gerar o `.aab` **assinado** e validar o
  upload (rejeita não assinado).

### B. Console / Documentos (tarefa MANUAL do dono — não é código)
- [ ] **B1. Política de Privacidade** *(Audit 04 §1)* — **obrigatória para todos os apps**, mesmo sem
  coleta. Redigir, hospedar numa URL pública e informar em **App content → Privacy policy**.
  *(Pode ser linkada na tela "Mais" via JSON do servidor, sem mexer no código.)*
- [ ] **B2. Data Safety form** *(Audit 04 §2)* — obrigatório. Provável **"nenhum dado coletado"**
  (app só envia IP+UA); **confirmar se o servidor grava access logs de IP** — se gravar, declarar IP.
  Marcar "criptografado em trânsito: sim" (HTTPS).
- [ ] **B3. Content Rating (IARC)** *(Audit 04 §3)* — responder o questionário (bíblico, sem
  violência/sexo/ads/UGC → provável **"Livre"**).
- [ ] **B4. Declaração de Foreground Service** *(Audit 04 §4)* — obrigatória p/ Android 14+
  (targetSdk 36). Fornecer descrição + **caso de uso** + **vídeo demo** (tocar → apagar tela →
  áudio segue com controles na notificação). Código já pronto (manifest correto ✅).
- [ ] **B5. Assets da ficha** *(Audit 04 §5)* — ícone 512×512, feature graphic 1024×500,
  **screenshots de celular** (mín. 2), título (≤30), descrição curta (≤80) e longa (≤4000),
  categoria, e-mail de contato.
- [ ] **B6. Declarar sem login / sem compras / sem anúncios** *(Audit 04 §6)* — trivial (N/A no
  código); simplifica Data Safety e rating.

---

## 🟡 CORRIGIR ANTES — não barra o envio, mas arrumar antes do rollout

### Código
- [ ] **C1. Erro de playback silencioso** *(Audit 02 §3)* — `PlayerViewModel.kt:620-622`: o
  `EVENT_PLAYER_ERROR` só chama `finishSourceSwitch()`; o usuário não recebe feedback quando a faixa
  falha (URL 404 / rede fora após 3 retries). Adicionar campo de erro ao `PlayerUiState` + mensagem/
  toast ("Não foi possível reproduzir. Verifique sua conexão."). **É a lacuna de UX de maior peso.**
- [ ] **C2. OkHttp `Level.BODY` em release** *(Audit 01 §4 / Audit 03 §5)* — `NetworkModule.kt:37`:
  logging incondicional loga corpo/headers em produção. Gate por debug → `Level.NONE`/`BASIC`.
- [ ] **C3. Loading eterno na tela "Mais"** *(Audit 02 §1)* — `BibleRepositoryImpl.kt:428`:
  `content==null` + sync OK → `Resource.Loading` sem Retry (edge estreito). Trocar por `Error`
  (paridade com a ISSUE 6.D).

### Verificação em device — 🔬 (rodar no **APK de RELEASE assinado**, depende de A1)
- [ ] **C4. Smoke do build de release** *(Audit 01 §3)* — instalar o release ofuscado (R8) e exercer
  sync do JSON + playback Media3 (serializers preservados no `mapping.txt`, mas validar em runtime).
- [ ] **C5. Falha de rede** *(Audit 02 T1)* — 1º uso sem internet → Error+Retry nas 4 telas; queda no
  meio não crasha.
- [ ] **C6. Erro de playback** *(Audit 02 T2)* — URL 404 / rede fora no meio → sem crash; validar o
  feedback do C1.
- [ ] **C7. POST_NOTIFICATIONS negado** *(Audit 02 T3, Android 13+)* — negar → áudio toca mesmo assim,
  sem crash; conceder depois → notificação com controles aparece.
- [ ] **C8. Persistência sob estresse** *(Audit 02 T4)* — rotação, bg/fg repetido, matar processo →
  restaura sessão (faixa+posição) sem auto-tocar; validar que R8 não quebrou o estado salvo.
- [ ] **C9. StrictMode / ANR** *(Audit 02 T5)* — rodar debug com `adb logcat | grep StrictMode`;
  nenhuma violação de I/O na main; sem ANR nas transições pesadas.
- [ ] **C10. Android 8 (API 26)** *(Audit 02 T6)* — smoke em aparelho/emulador antigo: notificação de
  mídia + FGS sem `NoSuchMethodError`/`VerifyError`.

---

## 🟢 PODE ESPERAR — backlog pós-launch (NÃO bloqueia; aqui só para não esquecer)
- Stripar `Log` no release (`-assumenosideeffects`) ou rebaixar os `Log.i` de lifecycle (`PLAYBACK_LC`). *(Audit 03 §5)*
- 77 warnings de lint: 35 typos, 37 bumps de dependência/AGP, 2 ícone, 1 folder `-v26`, 1 icon location. *(Audit 03 §6)*
- Acento em "Ouvindo A Biblia" → "Bíblia" (`app_name`). *(Audit 01 §8 / 04 §5)*
- `Player` fora do padrão MVI (flat/método-driven) — consistência, não risco. *(Audit 03 §3)*
- i18n: mover strings de UI para `strings.xml` (hoje só 1 entrada; app é pt-BR-only). *(Audit 03 §7)*
- "Optimize Imports" antes do tag de release. *(Audit 03 §1)*
- Lembrete operacional: incrementar `versionCode` a cada upload. *(Audit 01 §5)*

---

## 🚦 DECISÃO GO / NO-GO

| Bloco | Itens | Status |
|-------|-------|--------|
| 🔴 Código/Build (A1–A2) | 2 | ☐ |
| 🔴 Console/Docs (B1–B6) | 6 | ☐ |
| 🟡 Código (C1–C3) | 3 | ☐ |
| 🟡 Verificação device (C4–C10) | 7 | ☐ |

- [ ] **GO** — todos os 🔴 concluídos **e** todos os 🟡 concluídos (ou explicitamente aceitos).

> **Ordem sugerida:** A1 (assinatura) desbloqueia C4–C10 (testes no release). C1/C2/C3 são rápidos e
> devem entrar antes do C4 (para o smoke já validar as correções). Os B* (Console) correm em paralelo,
> por serem manuais e independentes do código.

**Resumo honesto:** o **código está tecnicamente pronto** — nada de conformidade a corrigir, targetSdk
à frente da exigência, permissões limpas, R8 com serializers preservados. Os bloqueios reais são
**1 técnico (assinatura, A1)** e **os procedimentais de Console (B1–B6)**; os 🟡 de código (C1–C3) são
polimento de robustez/higiene, sendo o **erro de playback silencioso (C1)** o mais importante.
