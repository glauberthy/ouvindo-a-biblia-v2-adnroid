# AUDITORIA 04 — Conformidade com as Políticas da Play Store

**Data:** 2026-07-16 · **Branch:** `analise-media3` · **Modo:** leitura de código + pesquisa das
políticas atuais da Play.

**Convenção:** 🧩 **CÓDIGO** = algo verificável/ajustável no repositório · 📋 **MANUAL** = tarefa do
dono do app no Play Console / documentos (fora do código). Severidade:
**[BLOQUEIA]** (Play barra a publicação) · **[CORRIGIR ANTES]** · **[PODE ESPERAR]** · ✅ · 🔬.

---

## Fatos do código (base para tudo abaixo) — 🧩

O que o app **de fato** faz com dados (verificado no código):
- **Rede = 4 GET de JSON estático** (`BibleApi.kt`): `biblia_index.json`, `themes.json`,
  `estudos.json`, `mais.json` em `https://ouvindo-a-biblia.ide.app.br/` + **streaming HTTP de áudio**
  (URLs vindas do JSON). **Nenhum POST, nenhum corpo de requisição, nenhum dado de usuário enviado.**
- **O que sai do device:** apenas o inerente a qualquer HTTP — **endereço IP** e o header
  `User-Agent: BibliaFaladaApp` (`NetworkModule.kt:45`, `MediaModule.kt:56`). Nada mais.
- **Armazenamento é 100% LOCAL:** Room (favoritos, posição de "continuar ouvindo") e DataStore
  (versões de sync) — **não são transmitidos**. Logo não contam como "coleta" no Data Safety.
- **Sem SDK de analytics/crash/ads/auth/billing:** grep em `libs.versions.toml` + todos os
  `build.gradle.kts` → só `play-services-cast-framework` (Cast, **desligado** por `CastConfig.ENABLED=false`).
  Sem Firebase/Crashlytics/Analytics/AdMob/Billing/login.
- **Sem permissão de Advertising ID:** o manifest merged do release **não** contém `AD_ID` nem
  `com.google.android.gms.permission.AD_ID` (verificado). ✅ Simplifica o Data Safety.
- **Permissões (merged release):** `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, + a auto-gerada
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. **Nenhuma sensível** (sem localização, contatos, SMS,
  câmera, microfone, telefone).

---

## 1. Política de Privacidade — 📋 [BLOQUEIA] (obrigatória, não existe ainda)

- 🧩 **Código:** o app não gera identificadores nem coleta dados pessoais; só envia IP+UA inerentes.
- 📋 **Regra atual da Play (pesquisada):** **TODOS os apps precisam de uma URL de política de
  privacidade**, mesmo os que não coletam dados. Não é opcional. → **É obrigatória neste app.**
- 📋 **Tarefa do dono:**
  1. Redigir uma política de privacidade (pode ser simples: "o app não cria conta, não coleta dados
     pessoais; faz requisições ao servidor X para baixar conteúdo e streaming de áudio; dados locais
     [favoritos/posição] ficam só no device"). Descrever o que o **servidor** faz com o IP/logs.
  2. Hospedar numa URL pública estável (ex.: `https://ouvindo-a-biblia.ide.app.br/privacidade`).
  3. Informar a URL no **Play Console** (App content → Privacy policy) e, recomendado, linkar
     **dentro do app** (a tela "Mais" já tem seções tipo `LONG_TEXT`/`RIGHTS_LIST` — dá pra
     adicionar uma seção "Política de Privacidade" via JSON do servidor, **sem mudar código**).
- **Severidade [BLOQUEIA]:** sem essa URL a Play não publica.

## 2. Data Safety (formulário) — 📋 obrigatório · declaração enxuta

- 🧩 **Código diz:** cliente não coleta dados de usuário; só transmite IP+UA para o servidor do dono
  e para o host de áudio. Sem identificadores, sem localização, sem analytics.
- 📋 **Regra:** o formulário Data Safety é **obrigatório para todos os apps**. "Coletar" = transmitir
  dado para fora do device. **Exceção "ephemeral":** dado só em memória, usado para servir a
  requisição em tempo real e **não armazenado/logado**, não precisa ser declarado como coletado.
- 📋 **Determinação do dono (depende do servidor):**
  - **Se o servidor NÃO registra logs de acesso** (serve JSON/áudio estático sem gravar IP) → o IP é
    ephemeral → **declarar "Nenhum dado coletado / compartilhado"** (o cenário mais provável para
    conteúdo estático).
  - **Se o servidor grava access logs** (Nginx/CDN típicos gravam IP) → declarar **IP address** em
    Data Safety, uso "App functionality/Analytics", não compartilhado, e refletir isso na política.
  - **Recomendação:** confirmar a config do servidor. Na dúvida, o mais seguro é declarar IP como
    coletado para *App functionality* e não compartilhado.
- 📋 **Preenchimento sugerido (assumindo servidor sem logging de PII):**
  - Coleta de dados: **Não** · Compartilhamento: **Não** · Dados criptografados em trânsito: **Sim**
    (HTTPS, `BASE_URL` é `https://`) · Mecanismo de exclusão de dados: N/A (não há conta).
- ⚠️ **Reavaliar SE** no futuro adicionar Firebase/Crashlytics/Analytics **ou religar o Cast** (o
  Cast pode introduzir identificadores) — hoje não se aplica.

## 3. Content Rating (classificação) — 📋 questionário IARC

- 🧩 **Conteúdo:** áudio/textos bíblicos e estudos. **Sem** violência, sexo, linguagem imprópria,
  drogas, jogos de azar, conteúdo gerado por usuário, compras ou anúncios.
- 📋 **Tarefa do dono:** responder o **questionário IARC** no Console. Com "não" para todas as
  categorias sensíveis, a classificação provável é a **mais baixa: "Livre" / Everyone / PEGI 3 /
  ESRB Everyone**. Conteúdo religioso por si só não eleva a faixa.
- 📋 O questionário perguntará também: o app é primariamente para crianças? (**Não** — público geral),
  tem interação/compartilhamento de usuários? (**Não**), coleta localização? (**Não**).

## 4. Foreground Service / permissões sensíveis — 🧩 ✅ (código pronto) + 📋 declaração obrigatória

- 🧩 **Código pronto e correto:** `FOREGROUND_SERVICE_MEDIA_PLAYBACK` declarada
  (`AndroidManifest.xml:11`) **junto** com `foregroundServiceType="mediaPlayback"` no serviço
  (`AndroidManifest.xml:42`) — exatamente o par exigido para Android 14+. `POST_NOTIFICATIONS` e
  `WAKE_LOCK` são permissões **normais** (não exigem formulário). ✅
- 📋 **Regra (obrigatória p/ targetSdk 36 = Android 14+, em vigor desde 22/01/2025):** declarar o uso
  do Foreground Service em **App content → Foreground service permissions**, fornecendo:
  1. **Descrição** da funcionalidade que usa o FGS `mediaPlayback`.
  2. **Vídeo curto** demonstrando o passo a passo do usuário até acionar o recurso.
  3. **Caso de uso** específico.
- 📋 **Declaração sugerida (texto):** *"O app é um player de áudio (Bíblia falada, estudos e momentos
  temáticos). O foreground service do tipo mediaPlayback mantém a reprodução ativa com a tela
  desligada e em segundo plano, exibindo controles (play/pause/próximo) na notificação de mídia e na
  tela de bloqueio."* — uso central e aprovado; sem risco de reprovação.
- 📋 **Vídeo:** gravar tela mostrando: abrir o app → tocar um capítulo → sair do app / apagar a tela →
  áudio continua + notificação com controles. **[CORRIGIR ANTES]** (é pré-requisito do envio).

## 5. Ficha da Loja — 📋 (100% MANUAL) — checklist de assets

Nada disso é código; é o que o dono precisa ter pronto no Console:
- **Ícone hi-res:** 512×512 PNG (32-bit, com alfa). *(O ícone in-app já existe e é custom — este é o
  asset separado da ficha.)*
- **Feature graphic:** 1024×500 PNG/JPG (obrigatório para publicar).
- **Screenshots de celular:** **obrigatório** (mín. 2; recomendado 4–8), 16:9/9:16, lado maior
  320–3840px. *(Telas boas: Home com grade de livros, player expandido, Temas, Estudos, Favoritos.)*
- **Screenshots de tablet:** opcional (o app é responsivo via `WindowSizeClass` — vale incluir).
- **Título:** ≤ 30 caracteres (ex.: "Ouvindo a Bíblia").
- **Descrição curta:** ≤ 80 caracteres.
- **Descrição completa:** ≤ 4000 caracteres.
- **Categoria:** provável **"Livros e referências"** ou **"Música e áudio"** (escolha do dono) + tags.
- **E-mail de contato** (obrigatório) + site/telefone opcionais.
- **Público-alvo e conteúdo** (faixa etária) + a URL de política de privacidade (item 1).
- 📋 Nota: revisar o acento em "Ouvindo A Biblia" → "Bíblia" no `app_name` se quiser consistência com
  o título da ficha (também citado na AUDITORIA_01 §8).

## 6. Contas/Login, Compras, Anúncios — 🧩 ✅ N/A (simplifica tudo)

- 🧩 **Verificado no código:** **não há** login/conta (sem OAuth/token/senha/e-mail), **não há**
  compras (sem Play Billing/IAP) e **não há** anúncios (sem AdMob/SDK de ads). Os "token" no código
  são do parser de rich text (`MoreRichText.kt`), não autenticação.
- 📋 **Efeito no Console:** declarar **"não"** para app com login, compras no app e anúncios →
  simplifica o Data Safety (sem "informações financeiras", sem "info de conta"), o content rating
  (sem "compras digitais") e dispensa a política de anúncios/famílias-com-ads. ✅

---

## Sumário: o que falta para publicar

### 🧩 CÓDIGO — situação
| Item | Estado |
|------|--------|
| FGS `mediaPlayback` + permissão declaradas no manifest | ✅ pronto |
| Sem AD_ID / sem SDK de tracking / sem ads / sem login / sem IAP | ✅ pronto |
| HTTPS em trânsito (`BASE_URL` https) | ✅ |
| (fora deste eixo, mas lembrar) OkHttp `Level.BODY` em release | ⚠️ ver AUDITORIA_01/03 |

### 📋 MANUAL — pendências do dono (nenhuma é código)
| # | Tarefa | Severidade |
|---|--------|-----------|
| 1 | **Criar + hospedar Política de Privacidade** e informar a URL no Console | **[BLOQUEIA]** |
| 2 | Preencher o **Data Safety form** (provável "nenhum dado coletado"; confirmar logs do servidor) | **[BLOQUEIA]** (form é obrigatório) |
| 3 | Responder o **questionário de Content Rating** (IARC → provável "Livre") | **[BLOQUEIA]** (rating é obrigatório) |
| 4 | **Declaração de Foreground Service** (descrição + **vídeo demo** + caso de uso) | **[BLOQUEIA]** p/ Android 14+ |
| 5 | **Assets da ficha:** ícone 512², feature graphic 1024×500, screenshots de celular, textos, categoria, e-mail | **[BLOQUEIA]** (obrigatórios p/ publicar) |
| 6 | Declarar **sem login / sem compras / sem anúncios** | ✅ trivial (N/A) |

> As pendências manuais são **procedimentais** (todo app passa por elas), não defeitos do app. O
> código está conforme: permissões limpas, FGS corretamente pareado, sem tracking, sem AD_ID, HTTPS.

**Veredito:** do lado do **código**, o app está **conforme** as políticas — nada a corrigir para
compliance (o único ajuste técnico correlato, o log BODY do OkHttp, está nas Auditorias 01/03). O que
falta é **100% trabalho manual de Console/documentos**: política de privacidade (a pendência de maior
peso, obrigatória), Data Safety, content rating, declaração de FGS com vídeo, e os assets da ficha.

---

### Fontes (políticas atuais da Play)
- [Play Console — Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Play Console — Foreground service & full-screen intent requirements](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en)
- [Android Developers — Foreground service types are required (Android 14)](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Android privacy policy requirements (Termly)](https://termly.io/resources/articles/android-privacy-policy/)
