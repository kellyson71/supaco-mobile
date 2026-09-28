import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

android {
    namespace = "io.github.kellyson71.supaco"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.kellyson71.supaco"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "2.0"
    }

    // Assinatura de release: keystore.properties na raiz (fora do git) ou
    // variáveis de ambiente (CI). Sem nenhum dos dois, o APK de release sai
    // sem assinatura — nunca há senha padrão no código.
    val keystoreProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    }
    fun signingValue(prop: String, env: String): String? =
        keystoreProps.getProperty(prop) ?: System.getenv(env)

    val releaseStoreFile = signingValue("storeFile", "SUPACO_KEYSTORE_PATH")?.let { rootProject.file(it) }
    val hasReleaseSigning = releaseStoreFile?.exists() == true

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingValue("storePassword", "SUPACO_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SUPACO_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SUPACO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

// Schemas do Room versionados em app/schemas — base para migrações futuras.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  // Esquemas de cor dinâmicos a partir de uma cor-semente (temas + OLED monocromático)
  implementation(libs.material.kolor)
  
  // Glance App Widget
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Dependency Injection (Koin)
  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)

  // Network & Serialization
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx.serialization)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.kotlinx.serialization.json)

  // Security (EncryptedSharedPreferences)
  implementation(libs.security.crypto)

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Biometric
  implementation(libs.androidx.biometric)

  // WorkManager — alertas locais de faltas (sem Firebase)
  implementation(libs.androidx.work.runtime)

  // Splash Screen
  implementation(libs.androidx.core.splashscreen)

  // Profile Installer
  implementation(libs.androidx.profileinstaller)
}
