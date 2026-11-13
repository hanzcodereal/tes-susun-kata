plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Penandatanganan rilis: dibaca dari environment (GitHub Actions menyiapkannya).
val ksPath: String? = System.getenv("KEYSTORE_PATH")
val ksPass: String? = System.getenv("KEYSTORE_PASSWORD")
val ksAlias: String? = System.getenv("KEY_ALIAS")
val ksKeyPass: String? = System.getenv("KEY_PASSWORD")
val canSign = !ksPath.isNullOrBlank() && file(ksPath).exists() &&
    !ksPass.isNullOrBlank() && !ksAlias.isNullOrBlank() && !ksKeyPass.isNullOrBlank()

android {
    namespace = "com.hanzdev.susunkata"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hanzdev.susunkata"
        minSdk = 24
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.0"
    }

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
