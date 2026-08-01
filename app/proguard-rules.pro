# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------------
# ÚNICA regra ativa do arquivo (FASE 14 / avaliação in-app, 2026-08-01).
#
# NÃO é keep rule: `-dontwarn` só cala um aviso, não impede o R8 de encolher nem
# de otimizar nada. A checklist da ISSUE 12.B ("proguard-rules.pro sem nenhuma
# regra ativa" como prova de que o R8 está solto) segue valendo no que importa.
#
# O que acontece sem ela: `minifyReleaseWithR8` FALHA (não avisa — falha) com
#   Missing class com.google.android.gms.common.annotation.NoNullnessRewrite
#   (referenced from ReviewManagerKtxKt$sam$..OnSuccessListener$0.onSuccess)
#
# Causa: o `review-ktx:2.0.2` foi compilado contra um `play-services-tasks` mais
# velho; o Cast puxa o `tasks` para 18.3.2, onde `OnSuccessListener.onSuccess`
# passou a ser anotado com `@NoNullnessRewrite`. Essa anotação tem retenção
# CLASS — não existe em runtime e não é empacotada —, então descartá-la é seguro
# por definição; o R8 só tropeça ao montar o wrapper SAM. É exatamente a regra
# que o próprio AGP gera em `app/build/outputs/mapping/release/missing_rules.txt`.
#
# Some sozinha quando o `review-ktx` for recompilado contra o tasks novo. Ao
# subir a dependência, tente remover esta linha e rode `make release`: o debug
# NÃO acusa nada, porque o R8 só roda no release.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite