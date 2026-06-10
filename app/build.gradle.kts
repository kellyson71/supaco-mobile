plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

android {
    namespace = "com.example.supacomobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.supacomobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
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
  implementation("androidx.compose.material:material-icons-extended")
  
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

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

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
  implementation("androidx.work:work-runtime-ktx:2.10.0")

  // Splash Screen
  implementation(libs.androidx.core.splashscreen)

  // Profile Installer
  implementation(libs.androidx.profileinstaller)
}
