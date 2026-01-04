plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "br.app.ide.ouvindoabiblia.data.repository"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // --- MÓDULOS DE DADOS (O Segredo da Orquestração) ---
    implementation(project(":data:local"))
    implementation(project(":data:remote"))

    // Core
    implementation(libs.androidx.core.ktx)

    // Coroutines (Para gerenciar as threads de sync)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt (Para injetar e ser injetado)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}