# Ouvindo a Bíblia

App Android nativo de áudio (Bíblia falada, momentos temáticos e estudos) — Jetpack Compose, Media3/ExoPlayer, Hilt, Room, multi-módulo Gradle. Código e textos em pt-BR.

## Documentação viva

A referência atual do projeto está nestes arquivos (os planos antigos foram arquivados em `docs/archive/`):

- **`CLAUDE.md`** — visão geral de arquitetura, módulos e comandos de build.
- **`DIAGNOSTICO_01_ARQUITETURA.md`** — auditoria de build e integridade arquitetural.
- **`DIAGNOSTICO_02_PLAYER.md`** — auditoria detalhada da camada de reprodução.
- **`ROADMAP.md`** — trabalho restante, priorizado, derivado dos diagnósticos.
- **`docs/RELEASE_NOTES.md`** — changelog por versão **e** o texto pronto de "O que há de novo" do Play Console.

## Build e publicação (`make`)

O `Makefile` na raiz existe porque duas armadilhas deste ambiente já morderam: `ANDROID_HOME`/PATH
**não** estão exportados no shell (daí `adb: command not found`) e um release sem
`keystore.properties` sai **UNSIGNED sem erro nenhum** — o Console só recusa lá na frente, depois
de todo o build. Os alvos resolvem os dois.

```bash
make help              # lista todos os alvos
make version           # versionCode/versionName atuais
make check             # testes JVM + Android lint
make release           # ← o que sobe na loja
```

### Gerar a versão para a loja

```bash
make release
```

Em um comando: bloqueia se faltar `keystore.properties`, roda `clean` (para não validar sobra de
build da identidade antiga `ag.uny.*`, ver `CLAUDE.md`), roda `test` + `:app:lint`, gera o **AAB
assinado** e no fim imprime versão + a impressão digital de quem assinou, comparando com a chave de
**upload** travada no `Makefile` (`EXPECTED_SHA256`, começa em `84:2D:3A:33`). Se não bater, falha —
assinar com a chave errada é o erro que só apareceria no upload.

Artefato: `app/build/outputs/bundle/release/app-release.aab`

**O Console precisa apenas do AAB.** O APK não vai para a loja; serve só para instalar e testar
localmente:

```bash
make aab                                   # só o AAB, sem clean/testes (iteração rápida)
make verify                                # reconfere a assinatura de um AAB já gerado
make apk                                   # APK de release assinado
make install-release SERIAL=emulator-5554  # instala e abre (SERIAL é obrigatório)
```

`SERIAL` é exigido de propósito: costuma haver **dois** aparelhos pareados (emulador + celular
físico), e sem `-s` o `adb` responde `error: more than one device/emulator`. `make devices` lista.

### Enviar no Console

**Teste interno → Criar nova versão** → subir o `.aab` → colar em "O que há de novo" o bloco de
texto de `docs/RELEASE_NOTES.md` (seção da versão, sob *"Texto para o Console — copiar como está"*;
o limite é 500 caracteres por idioma). O resto daquele arquivo é changelog técnico, não vai para a
loja.

A **declaração de foreground service** é do app, não da versão: já foi enviada e não precisa ser
reenviada enquanto `mediaPlayback` seguir sendo o único tipo no `AndroidManifest.xml` (PUB-23).

### Screenshots da ficha

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

make emulator                                                            # sobe o AVD OuvindoBiblia_API36
adb -s emulator-5554 exec-out screencap -p > store-assets/screenshots/NN_nome.png
```

`screencap` lê o framebuffer do Android, não a janela do host — **moldura do emulador, barra de
título e skin do aparelho nunca aparecem no PNG**. Não é preciso nada de especial para o screenshot
sair "chromeless".

Se quiser a **janela** sem a arte do aparelho (mais área útil ao navegar), edite
`~/.android/avd/OuvindoBiblia_API36.avd/config.ini` → `showDeviceFrame=no`.

Para uma status bar limpa antes de capturar (relógio fixo, bateria cheia, sem ícones de debug):

```bash
adb -s emulator-5554 shell settings put global sysui_demo_allowed 1
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command exit   # desfazer
```

⚠️ **Resolução:** os screenshots já publicados em `store-assets/screenshots/` são **720×1600**
(capturados no moto g53 físico); o AVD Pixel 8 entrega **1080×2400**. O Play aceita os dois, mas um
recapturado no emulador fica com proporção diferente dos vizinhos na galeria — para manter o
conjunto uniforme, recapture no celular.

## Player: ciclo de vida e background (estado atual)

O `PlaybackService` (`MediaLibraryService`) roda como **serviço persistente** no modelo "tipo Spotify":

- O `ExoPlayer` **pertence ao serviço** (não é mais `@Singleton`); é liberado uma única vez no `onDestroy` real.
- Ao começar a tocar, o serviço é promovido a **started + foreground** (`startForegroundService`), então **sobrevive** ao app ir para segundo plano ou à `MainActivity` ser destruída — o áudio continua e a notificação permanece.
- **Remover o app dos recentes (swipe) ENCERRA o playback quando está tocando**: `onTaskRemoved` persiste a posição de forma síncrona, pausa, libera player + sessão, remove a notificação e dá `stopSelf()`. Swipe é intenção explícita de fechar (comportamento tipo Spotify). Se estiver **pausado** após ter tocado, a notificação (dismissível) permanece para retomar por ela/headset — ISSUE 4.A.
- A posição de reprodução é salva no Room (`PlaybackStateEntity`) e restaurada no `onCreate` **sem auto-play**.

Detalhes e itens ainda abertos do player (Cast, save periódico, notificação no estado pausado, `onPlaybackResumption`) estão em `DIAGNOSTICO_02_PLAYER.md` e `ROADMAP.md`.

> Nota: "persistente" aqui significa que o serviço sobrevive à destruição da `MainActivity` e ao app em segundo plano — **não** ao swipe nos recentes. A versão anterior mantinha o áudio tocando após o swipe; isso foi corrigido (a decisão está travada por `TaskRemovalPolicyTest`).
