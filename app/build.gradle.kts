import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ISSUE PUB-01: assinatura de release lida de keystore.properties (FORA do git — ver
// keystore.properties.example). Se o arquivo não existir (CI sem segredos, dev rodando só debug),
// o release fica sem signingConfig e o assembleDebug segue funcionando normalmente.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasReleaseKeystore) FileInputStream(keystorePropertiesFile).use { load(it) }
}

android {
    namespace = "br.app.ide.ouvindoabiblia"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.app.ide.ouvindoabiblia"
        minSdk = 26
        targetSdk = 36
        // Regra operacional (Audit 01 §5): INCREMENTAR a cada upload no Console, mesmo em
        // trilha de teste — o Play rejeita versionCode repetido. E o número QUEIMA no upload:
        // basta o AAB ter subido uma vez, mesmo que a versão seja depois DESCARTADA, para o
        // Console recusar ("O código de versão 3 já foi usado"). Foi o que aconteceu com o vc3.
        // Histórico: vc1/1.0 = AAB validado localmente na decisão de publicar como app novo
        // (dcbee84); vc2/1.1 nunca subiu; vc3/1.2 subiu e foi descartado; vc4/1.3 subiu e ficou
        // EM ANÁLISE (tag `v1.3-vc4`), queimando o 4. Nenhum deles chegou a usuário.
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Só cria a config se a keystore de upload estiver presente (senão release fica unsigned,
        // sem quebrar o build de quem não tem os segredos).
        if (hasReleaseKeystore) {
            create("release") {
                // storeFile é resolvido a partir da RAIZ do projeto (rootProject).
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // SEM bloco `ndk { debugSymbolLevel = ... }`, de propósito — TESTADO em 2026-07-29.
            // O Console avisa que "o App Bundle contém código nativo e você não fez upload dos
            // símbolos de depuração". O código nativo não é nosso: são dois .so que entram por
            // dependências AndroidX — libandroidx.graphics.path.so (Compose UI) e
            // libdatastore_shared_counter.so (DataStore), em 4 ABIs.
            //
            // Com `debugSymbolLevel = "SYMBOL_TABLE"` o AGP roda de fato o
            // `extractReleaseNativeSymbolTables` sobre os 8 .so, e a saída sai VAZIA: eles
            // chegam já stripped da AndroidX (`readelf -SW` mostra só `.dynsym`, sem `.symtab`
            // nem `.debug_info`), então não há tabela para extrair e NADA é adicionado ao
            // bundle — o aviso do Console continua igual. Manter o bloco só daria a impressão
            // falsa de que o assunto foi resolvido.
            //
            // Ou seja: aviso NÃO acionável enquanto o app não tiver código nativo próprio. Se
            // um dia tiver (CMake/NDK aqui dentro), aí sim adicione o bloco — a partir daí ele
            // passa a produzir símbolos de verdade.
            // Assina o release só quando a keystore existe (ver bloco signingConfigs acima).
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        // O rodapé da tela Mais mostra a versão do APP (BuildConfig.VERSION_NAME/VERSION_CODE).
        // O AGP 8 não gera o BuildConfig por padrão, então precisa ser habilitado aqui.
        buildConfig = true
    }

    lint {
        // `./gradlew :app:lint` MORRIA inteiro com NoClassDefFoundError dentro do
        // UnrememberedGetBackStackEntryDetector, do navigation-compose 2.8.5, cujo jar de
        // lint foi compilado contra uma API de lint anterior à do AGP 8.13.2. Não era um
        // aviso sobre o código: o driver abortava antes de reportar qualquer coisa, ou
        // seja, o lint estava CEGO — nenhum problema real chegava até nós. Verificado que
        // é anterior às mudanças da barra (reproduz no commit 4e7c2c0, num worktree).
        //
        // Isto desliga UMA regra, não o lint. A regra pega `getBackStackEntry()` chamado
        // sem `remember` — este app usa destinos type-safe e não chama esse método em
        // lugar nenhum (conferido por busca), então a cobertura perdida é zero.
        //
        // A correção de raiz é subir o navigation-compose (o jar de lint da 2.9.x já casa
        // com o AGP 8.13). Ficou de fora de propósito: trocar dependência de navegação às
        // vésperas de publicar troca um problema conhecido por um desconhecido. Ao subir,
        // REMOVA esta linha e confirme que o lint roda limpo.
        disable += "UnrememberedGetBackStackEntry"
    }
}

dependencies {
    implementation(project(":data:repository"))

    // --- Core & Compose ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material3.window.size) // Importante para tablet
    implementation(libs.androidx.compose.foundation)
    // --- Navigation & Serialization ---
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // --- Hilt (Injeção de Dependência) ---
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.runtime.saveable)
    ksp(libs.hilt.compiler) // OBRIGATÓRIO: Processador de anotações via KSP

    // --- Room (Banco de Dados) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // OBRIGATÓRIO: Processador do Room

    // --- Networking (Retrofit/OkHttp) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // --- Media3 (ExoPlayer) ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    // --- Image Loading ---
    implementation(libs.coil.compose)

    // --- DataStore ---
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.material)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.concurrent.futures)
    // --- Google Cast ---
    implementation(libs.play.services.cast.framework)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)


    // --- Testing & Debug ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)

}