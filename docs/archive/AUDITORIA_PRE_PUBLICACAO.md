# Auditoria Pré-Publicação — Play Store (Produção)

Checklist rigoroso para publicar o "Ouvindo a Bíblia" em produção pública. Execute **uma auditoria
por vez** no Claude Code, na ordem abaixo. Cada uma gera um relatório próprio.

**Regra de severidade (use em todas):**
`[BLOQUEIA]` impede publicar ou reprova na revisão · `[CORRIGIR ANTES]` publica, mas prejudica
usuário/nota · `[PODE ESPERAR]` melhoria pós-launch.

**Regra de confiança (use em todas):** marque cada achado como ✅ confirmado-no-código ou 🔬
precisa-rodar-no-device. Não afirme comportamento de runtime a partir de leitura de código.

**Importante:** algumas exigências da Play Store **não são código** (política de privacidade, Data
Safety, ficha da loja) — o Claude Code identifica o que falta, mas parte é tarefa sua.

---

## AUDITORIA 1 — Prontidão de release (a que mais bloqueia)

```text
Auditoria de PRONTIDÃO DE RELEASE para publicar em produção na Play Store.
Somente leitura + builds; não corrija nada ainda. Cada achado com arquivo:linha,
severidade [BLOQUEIA]/[CORRIGIR ANTES]/[PODE ESPERAR] e ✅/🔬.

Verifique:

1. FORMATO DE PUBLICAÇÃO: a Play Store exige Android App Bundle (.aab), não APK.
   Rode ./gradlew bundleRelease e confirme que gera .aab. Há app/release/app-release.apk
   no repo (build antiga) — confirme que o pipeline de release produz AAB.

2. ASSINATURA: o build de release está assinado com uma keystore de release (NÃO a
   debug)? Onde está a config de signingConfigs? A keystore está FORA do git
   (checar .gitignore; segredos não podem estar versionados)? Confirme que
   local.properties / keystore não estão commitados.

3. R8 / MINIFY / PROGUARD (risco alto neste app): minifyEnabled e shrinkResources
   estão como no release? Se minify=true, o app usa reflexão pesada (Hilt, Room,
   Media3, kotlinx-serialization) — verifique se há keep rules suficientes nos
   proguard-rules.pro (app + consumer-rules dos módulos). Rode bundleRelease e faça
   um smoke no build de release (não debug): risco clássico é o release ofuscado
   quebrar serialização do JSON do servidor ou o Media3. Se minify=false, registre
   como [CORRIGIR ANTES] (app maior/mais lento, mas não bloqueia).

4. debuggable: o buildType release tem debuggable=false? LeakCanary/StrictMode e
   qualquer logging de debug estão fora do release (checar dependências debugImplementation
   vs implementation)?

5. versionCode / versionName: estão definidos e coerentes para primeira publicação?
   versionCode é inteiro incremental.

6. targetSdk: a Play Store exige um targetSdk mínimo recente (a exigência muda a cada
   ano). O app está em targetSdk=35 e compileSdk divergente (35 app / 36 libs). VERIFIQUE
   qual é a exigência ATUAL da Play Store hoje (pesquise se tiver acesso) e diga se 35
   atende ou se precisa subir. Unifique compileSdk se necessário.

7. PERMISSÕES: liste todas as permissions do AndroidManifest (app + libs merged).
   Alguma não é usada / não justificável? (Play cobra justificativa, ex.: FOREGROUND_SERVICE_MEDIA_PLAYBACK
   ok; qualquer permissão sensível sem uso é [BLOQUEIA] ou [CORRIGIR ANTES]).

8. applicationId e nome: applicationId final de produção (não .debug), label e ícone
   corretos.

Grave em AUDITORIA_01_RELEASE.md. No fim, liste os [BLOQUEIA] em destaque.
```

---

## AUDITORIA 2 — Robustez em runtime

```text
Auditoria de ROBUSTEZ para produção. Foco: o que quebra na mão do usuário real.
Leitura + device onde necessário. Cada achado com arquivo:linha, severidade e ✅/🔬.

O app busca conteúdo de um servidor próprio (biblia_index.json etc.) e faz playback
de áudio HTTP. Verifique:

1. FALHA DE REDE: o que acontece SEM internet no primeiro uso (cache vazio)? E com
   internet caindo no meio? Timeout do OkHttp está configurado? JSON malformado do
   servidor derruba o app ou é tratado? (a orquestração Resource<> foi feita na 3.B —
   confirme que Error aparece na tela e o Retry funciona em todas as 4 telas de sync).

2. ESTADOS VAZIOS: cada tela (Home, Estudos, Temas, Mais, Favoritos) trata lista
   vazia sem parecer bug? Favoritos vazio tem estado próprio?

3. ERRO DE PLAYBACK: áudio com URL quebrada / 404 / rede caindo durante a reprodução
   — o player mostra erro ou trava? Há listener de Player.Listener.onPlayerError?

4. ANDROID ANTIGO: minSdk=26 (Android 8). Alguma API usada exige nível maior sem
   guarda? Rode/inspecione caminhos sensíveis (notificação de mídia, foreground
   service type, permissão POST_NOTIFICATIONS no Android 13+ está sendo pedida em runtime?).

5. POST_NOTIFICATIONS (Android 13+): app de player PRECISA da notificação. O runtime
   permission de notificação é solicitado? Se o usuário negar, o foreground service
   ainda funciona? (🔬 testar).

6. CICLO DO PLAYER SOB ESTRESSE: tocar, girar tela, background/foreground repetido,
   matar processo e retomar. Reconfirmar que o 5.1 (persistência) não regride.

7. ANR / main thread: com StrictMode, alguma I/O ou trabalho pesado na main além do
   que já foi tratado? Startup trava?

Para itens 🔬, escreva o passo a passo de teste (inclusive testar no BUILD DE RELEASE,
não só debug). Grave em AUDITORIA_02_ROBUSTEZ.md.
```

---

## AUDITORIA 3 — Qualidade de código e padrões

```text
Auditoria de QUALIDADE DE CÓDIGO para produção. Use DIAGNOSTICO_01/02 e ROADMAP.md
como base do que já é conhecido. Somente leitura. Cada achado com arquivo:linha,
severidade e ✅/🔬. Foque no que impacta manutenção/estabilidade, não em preferência
de estilo.

1. CÓDIGO MORTO: o ChaptersScreen/ChaptersViewModel ainda está morto (nunca navegado)?
   Decidir remover antes de publicar. Outros arquivos/funções órfãos? Imports não usados?

2. CAST DESLIGADO: confirme que o kill-switch está limpo — nenhum caminho chama
   initializeCast no fluxo normal, botão escondido, sem I/O na main. Nada meio-ligado.

3. PADRÃO LCE/MVI: as telas fora do padrão (identificadas no Diag01 §5) — é [PODE ESPERAR]
   pra publicar, mas liste. Renomear MoreSectionDetailsRoute.k.kt (nome quebrado).

4. TODO/FIXME/HACK: grep no código. Algum crítico deixado no caminho de produção?

5. LOGS: há Log.d/Log.i verbosos que vão pra produção? (idealmente strip no release).
   Dados sensíveis em log? O okhttp logging interceptor está em nível BODY no release
   (vaza payload) — deve ser NONE/BASIC no release.

6. LINT: rodar ./gradlew :app:lint. Dos warnings restantes, algum é de
   correção/segurança (não só higiene)? Classifique.

7. HARDCODED: URLs, strings de UI hardcoded que deveriam estar em strings.xml
   (importa se for internacionalizar; senão [PODE ESPERAR]).

Grave em AUDITORIA_03_CODIGO.md.
```

---

## AUDITORIA 4 — Conformidade Play Store (parte não-código)

Auditoria de CONFORMIDADE com as políticas da Play Store para produção. Identifique
o que falta; deixe claro o que é CÓDIGO e o que é tarefa MANUAL do dono do app
(ficha da loja, documentos). Pesquise as exigências atuais da Play se tiver acesso.

1. POLÍTICA DE PRIVACIDADE: o app coleta algum dado? (se adicionar Firebase
   Analytics/Crashlytics no futuro, coleta). Hoje, o que sai do device? A Play exige
   URL de política de privacidade se coletar QUALQUER dado ou pedir permissões
   sensíveis. Dizer se é obrigatória no estado atual.

2. DATA SAFETY (formulário da Play): baseado no que o app realmente coleta/envia
   (requisições ao servidor próprio, IP, etc.), o que declarar? Listar o que o código
   de fato envia.

3. CONTENT RATING: conteúdo religioso/bíblico — qual faixa etária provável e o que o
   questionário da Play pediria.

4. FOREGROUND SERVICE / permissões sensíveis: a Play pede declaração de uso para
   FOREGROUND_SERVICE_MEDIA_PLAYBACK e afins. Confirmar que o uso é justificável e
   qual a declaração.

5. FICHA DA LOJA (tarefa manual — só listar o que é preciso ter): ícone hi-res,
   feature graphic, screenshots (celular obrigatório), título, descrição curta/longa,
   categoria.

6. CONTAS/LOGIN, COMPRAS, ANÚNCIOS: o app tem algum? (parece que não). Se não,
   registrar que não se aplica (simplifica o Data Safety).

Grave em AUDITORIA_04_PLAYSTORE.md, separando claramente CÓDIGO vs MANUAL.

```text

```

---

## Ordem de execução e consolidação

1. Rode **Auditoria 1** → resolva os `[BLOQUEIA]` antes de tudo (sem release assinado/AAB não há
   publicação).
2. **Auditoria 2** → robustez, testando no **build de release**, não debug.
3. **Auditoria 3** → limpeza de código (remover morto, limpar logs).
4. **Auditoria 4** → conformidade; comece cedo as tarefas manuais (política de privacidade leva
   tempo).

Ao fim das quatro, peça ao Claude Code um **CHECKLIST_PUBLICACAO.md** consolidado: uma lista única
de tudo `[BLOQUEIA]` + `[CORRIGIR ANTES]` das quatro auditorias, marcável, que vira seu roteiro
final de "go/no-go" para apertar publicar.

## Lembrete honesto

Build de release passar ≠ app bom na mão do usuário. Os itens 🔬 (principalmente robustez de rede e
player no build de release, em Android 8) são os que decidem sua nota na loja. Teste no APK/AAB de *
*release**, idealmente num device real além do seu — nunca só no debug.
