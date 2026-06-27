plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "br.app.ide.ouvindoabiblia.data.local"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    // Schemas exportados do Room — necessários para o MigrationTestHelper validar migrações.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// Exporta os schemas do Room para $projectDir/schemas (usado nos testes de migração).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)

    // --- Room (O Banco de Dados) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // OBRIGATÓRIO: Gera o código do SQL

    // --- Hilt (Injeção de Dependência) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // OBRIGATÓRIO: Gera o código dos Módulos DI

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.android)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing) // MigrationTestHelper

    implementation(libs.androidx.datastore.preferences)
}