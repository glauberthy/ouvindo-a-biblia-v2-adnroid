# Makefile — ciclo de build/release do "Ouvindo a Bíblia".
#
# Existe por dois motivos práticos, os dois documentados no CLAUDE.md:
#  1. `ANDROID_HOME`/PATH NÃO estão exportados no shell deste ambiente, então `adb` e
#     `apksigner` só funcionam por caminho absoluto — o Makefile resolve isso sozinho.
#  2. O release tem armadilhas que já morderam: build sem `keystore.properties` sai
#     UNSIGNED sem erro, e sobras em `app/build/outputs/` podem ser de uma identidade
#     antiga (`ag.uny.*`, ver CLAUDE.md). Por isso `release` sempre limpa antes.
#
# Uso rápido:
#   make check              # testes + lint
#   make release            # limpa, testa e gera o AAB ASSINADO para o Console
#   make verify             # imprime a impressão digital de quem assinou o AAB
#   make install-release    # instala o APK de release num aparelho (smoke test)
#
# Aparelho: costuma haver DOIS (emulador + celular físico), então todo alvo de adb exige
# SERIAL explícito. `make devices` lista os disponíveis.

SHELL := /bin/bash
.DEFAULT_GOAL := help

ANDROID_HOME ?= $(HOME)/Android/Sdk
ADB          := $(ANDROID_HOME)/platform-tools/adb
EMULATOR     := $(ANDROID_HOME)/emulator/emulator
BUILD_TOOLS   = $(lastword $(sort $(wildcard $(ANDROID_HOME)/build-tools/*)))
APKSIGNER     = $(BUILD_TOOLS)/apksigner

GRADLE   := ./gradlew
PKG      := br.app.ide.ouvindoabiblia
ACTIVITY := $(PKG)/$(PKG).MainActivity
AVD      ?= OuvindoBiblia_API36

# Serial(es) de emulador rodando o AVD $(AVD) — vazio se nenhum. Pergunta ao adb qual AVD
# cada emulador carrega, em vez de procurar o processo.
# NÃO troque por `pgrep -f "qemu-system.*-avd $(AVD)"`: esse padrão entra na linha de comando
# do shell da PRÓPRIA receita, o pgrep casa consigo mesmo e o alvo passa a jurar que o
# emulador está de pé mesmo com ele morto. Já aconteceu.
AVD_SERIALS = $(ADB) devices | grep '^emulator-' | cut -f1 | while read s; do [ "$$($(ADB) -s $$s emu avd name 2>/dev/null | head -1 | tr -d '\r')" = "$(AVD)" ] && echo $$s; done

AAB          := app/build/outputs/bundle/release/app-release.aab
APK_RELEASE  := app/build/outputs/apk/release/app-release.apk
KEYSTORE_CFG := keystore.properties

# Impressão digital da chave de UPLOAD (a nova, criada quando se decidiu publicar como app
# novo). Travada aqui de propósito: assinar com a chave errada é o erro que a "novela da
# chave" tornou plausível, e ele só aparece no upload, depois de todo o build. Se algum dia
# a chave girar, este alvo falha alto — aí atualize o valor.
# Para só imprimir sem comparar: make verify EXPECTED_SHA256=
EXPECTED_SHA256 ?= 84:2D:3A:33:F3:FE:24:05:66:2A:CF:4A:73:F9:54:7D:1C:A2:B5:3F:5D:A8:77:1D:99:FF:48:72:2C:01:A0:E5

.PHONY: help version check test lint clean aab apk release verify \
        install-release run uninstall devices emulator emulator-kill guard-keystore guard-serial

help:
	@echo "Alvos disponíveis:"
	@echo "  version           versionCode/versionName atuais"
	@echo "  check             test + lint"
	@echo "  test              testes unitários JVM (todos os módulos)"
	@echo "  lint              Android lint do :app"
	@echo "  release           clean + check + AAB assinado (é o que sobe no Console)"
	@echo "  aab               só o AAB assinado (sem clean/check)"
	@echo "  apk               APK de release assinado (para instalar e testar)"
	@echo "  verify            imprime quem assinou o AAB"
	@echo "  run               emulador + installDebug + abre o app (ciclo de dev)"
	@echo "  uninstall         remove o app do emulador (APAGA favoritos/posição)"
	@echo "  install-release   instala o APK de release  (SERIAL=<serial>)"
	@echo "  devices           lista aparelhos conectados"
	@echo "  emulator          sobe o AVD $(AVD) (no-op se já estiver rodando)"
	@echo "  emulator-kill     derruba o emulador em execução"
	@echo "  clean             ./gradlew clean"

version:
	@grep -E "^[[:space:]]*version(Code|Name) =" app/build.gradle.kts | sed 's/^[[:space:]]*/  /'

# ---------------------------------------------------------------------------
# Qualidade
# ---------------------------------------------------------------------------

check: test lint

test:
	$(GRADLE) test

lint:
	$(GRADLE) :app:lint

clean:
	$(GRADLE) clean

# ---------------------------------------------------------------------------
# Release
# ---------------------------------------------------------------------------

# Sem keystore.properties o Gradle NÃO falha: ele só omite o signingConfig e entrega um
# artefato unsigned, que o Console recusa lá na frente. Falhar aqui é mais barato.
guard-keystore:
	@if [ ! -f $(KEYSTORE_CFG) ]; then \
		echo "ERRO: $(KEYSTORE_CFG) não encontrado na raiz do projeto."; \
		echo "      Sem ele o release sai UNSIGNED. Ver keystore.properties.example."; \
		exit 1; \
	fi

release: guard-keystore clean check aab
	@echo
	@echo "== Pronto para o Console =="
	@$(MAKE) --no-print-directory version
	@$(MAKE) --no-print-directory verify

aab: guard-keystore
	$(GRADLE) bundleRelease
	@test -f $(AAB) || { echo "ERRO: $(AAB) não foi gerado."; exit 1; }
	@echo "AAB: $(AAB) ($$(du -h $(AAB) | cut -f1))"

apk: guard-keystore
	$(GRADLE) assembleRelease
	@test -f $(APK_RELEASE) || { echo "ERRO: $(APK_RELEASE) não foi gerado."; exit 1; }
	@echo "APK: $(APK_RELEASE) ($$(du -h $(APK_RELEASE) | cut -f1))"

# `apksigner` não lê .aab; o keytool lê a assinatura do jar/bundle e é o que existe aqui
# (jarsigner não está instalado nesta máquina).
verify:
	@test -f $(AAB) || { echo "ERRO: rode 'make aab' primeiro."; exit 1; }
	@echo "Assinatura do AAB:"
	@fp=$$(keytool -printcert -jarfile $(AAB) | grep -m1 "SHA256:" | sed 's/.*SHA256: *//'); \
	echo "  SHA-256: $$fp"; \
	if [ -n "$(EXPECTED_SHA256)" ]; then \
		if [ "$$fp" = "$(EXPECTED_SHA256)" ]; then \
			echo "  OK: bate com EXPECTED_SHA256."; \
		else \
			echo "  ERRO: esperado $(EXPECTED_SHA256)"; exit 1; \
		fi; \
	else \
		echo "  (compare com a chave de UPLOAD do Console; use EXPECTED_SHA256= para travar)"; \
	fi

# ---------------------------------------------------------------------------
# Aparelhos
# ---------------------------------------------------------------------------

devices:
	@$(ADB) devices -l

guard-serial:
	@if [ -z "$(SERIAL)" ]; then \
		echo "ERRO: informe o aparelho — costuma haver mais de um conectado."; \
		echo "      Ex.: make $(MAKECMDGOALS) SERIAL=emulator-5554"; \
		echo; $(ADB) devices; exit 1; \
	fi

install-release: guard-serial apk
	$(ADB) -s $(SERIAL) install -r $(APK_RELEASE)
	@echo "Abrindo com componente explícito (o monkey abriria a LeakLauncherActivity do LeakCanary):"
	$(ADB) -s $(SERIAL) shell am start -n $(ACTIVITY)

# Ciclo de dev numa tacada: sobe o AVD (no-op se já estiver de pé), compila+instala o debug
# e abre o app. Não pede SERIAL — ele mesmo descobre o do emulador.
#
# Dois cuidados embutidos:
#  - `./gradlew installDebug` cru instala em TODO aparelho conectado, e aqui costuma haver o
#    celular físico junto — que não deve receber build local (a instalação pela Play quebra
#    depois, ver CLAUDE.md). `ANDROID_SERIAL` prende o Gradle no emulador.
#  - abre por componente EXPLÍCITO: com `monkey -c LAUNCHER` o debug abriria a
#    LeakLauncherActivity do LeakCanary, não a MainActivity.
run: emulator
	@serial=$$($(AVD_SERIALS) | head -1); \
	if [ -z "$$serial" ]; then echo "ERRO: o emulador não subiu."; exit 1; fi; \
	echo "==> Instalando o debug em $$serial"; \
	if ! ANDROID_SERIAL=$$serial $(GRADLE) installDebug; then \
		if $(ADB) -s $$serial shell pm list packages 2>/dev/null | grep -q '$(PKG)'; then \
			echo; \
			echo "-- O pacote $(PKG) JÁ está instalado neste emulador."; \
			echo "   Se o erro acima é INSTALL_FAILED_UPDATE_INCOMPATIBLE, o que está lá foi"; \
			echo "   assinado com OUTRA chave — tipicamente o APK de release do"; \
			echo "   'make install-release'. O debug usa a chave de debug e o Android recusa."; \
			echo "   Saída: 'make uninstall' e rodar de novo."; \
			echo "   ATENÇÃO: apaga favoritos e a posição de 'continuar ouvindo' — são dados"; \
			echo "   LOCAIS, sem cópia no servidor."; \
		fi; \
		exit 1; \
	fi; \
	echo "==> Abrindo $(ACTIVITY)"; \
	$(ADB) -s $$serial shell am start -n $(ACTIVITY)

# DESTRUTIVO. Favoritos (capítulos e aulas) e a posição de "continuar ouvindo" só existem no
# Room do aparelho — não há cópia no servidor, desinstalar apaga de vez. Só toca no emulador:
# no celular físico o alvo se recusa, porque lá build local conflita com a instalação da Play
# (assinadores diferentes, ver CLAUDE.md).
uninstall:
	@serial=$$($(AVD_SERIALS) | head -1); \
	if [ -z "$$serial" ]; then echo "Nenhum emulador com o AVD $(AVD) rodando."; exit 1; fi; \
	echo "Desinstalando $(PKG) de $$serial (apaga favoritos e posição salva)..."; \
	$(ADB) -s $$serial uninstall $(PKG)

# Três armadilhas, todas já vividas:
#  1. O MESMO AVD não roda em duas instâncias — a segunda morre com "Running multiple
#     emulators with the same AVD ... Please use -read-only flag". E o Android Studio sobe
#     este AVD com `-qt-hide-window` (a aba "Running Devices", embutida na IDE), ou seja ele
#     pode estar DE PÉ sem janela nenhuma na tela. Por isso o alvo checa antes de tentar.
#  2. `adb wait-for-device` SEM escopo volta no primeiro aparelho qualquer: com o celular
#     físico pareado ele retornava imediatamente, sem o emulador ter subido. `-e` restringe
#     ao emulador.
#  3. E "device" != "pronto": o adb enxerga o aparelho muito antes do boot terminar, então
#     um `installDebug` logo depois falhava. Quem libera é `sys.boot_completed`.
emulator:
	@running=$$($(AVD_SERIALS)); \
	if [ -n "$$running" ]; then \
		echo "O AVD $(AVD) JÁ está rodando em $$running — nada a fazer."; \
		echo "(Sem janela na tela? O Android Studio o abre embutido, com -qt-hide-window.)"; \
		exit 0; \
	fi; \
	log=/tmp/emulator-$(AVD).log; \
	echo "Subindo $(AVD)... (log em $$log)"; \
	$(EMULATOR) -avd $(AVD) -gpu host -no-boot-anim >$$log 2>&1 & \
	$(ADB) -e wait-for-device; \
	printf "Aguardando boot"; \
	for i in $$(seq 1 60); do \
		[ "$$($(ADB) -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break; \
		printf "."; sleep 2; \
	done; \
	if [ "$$($(ADB) -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; then \
		echo " TIMEOUT (120s). Ver $$log"; exit 1; \
	fi; \
	echo " pronto."; \
	$(ADB) devices | grep '^emulator-'

# `adb emu kill` derruba a própria conexão ao matar o emulador e sai NÃO-ZERO mesmo tendo
# funcionado — ler o exit code como "não havia emulador" reporta o oposto do que aconteceu.
# Por isso aqui se decide pelo ANTES e se confirma pelo DEPOIS, nunca pelo código de saída.
emulator-kill:
	@running=$$($(AVD_SERIALS)); \
	if [ -z "$$running" ]; then echo "Nenhum emulador rodando com o AVD $(AVD)."; exit 0; fi; \
	for s in $$running; do $(ADB) -s $$s emu kill >/dev/null 2>&1 || true; done; \
	for i in $$(seq 1 15); do \
		[ -z "$$($(AVD_SERIALS))" ] && break; sleep 1; \
	done; \
	if [ -n "$$($(AVD_SERIALS))" ]; then \
		echo "ERRO: $(AVD) ainda responde depois de 15s."; exit 1; \
	fi; \
	echo "Emulador derrubado ($$running)."
