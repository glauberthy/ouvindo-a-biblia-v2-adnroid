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
        install-release devices emulator guard-keystore guard-serial

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
	@echo "  install-release   instala o APK de release  (SERIAL=<serial>)"
	@echo "  devices           lista aparelhos conectados"
	@echo "  emulator          sobe o AVD $(AVD)"
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

emulator:
	$(EMULATOR) -avd $(AVD) -gpu host -no-boot-anim & \
	$(ADB) wait-for-device
