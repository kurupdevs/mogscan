import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kurupdevs.moggr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kurupdevs.moggr"
        minSdk = 26
        targetSdk = 35
        versionCode = 27
        versionName = "3.6"
    }

    // Release signing comes ONLY from environment (GitHub Actions secrets).
    // No key generation here, no hardcoded passwords — a missing keystore
    // fails the build loudly instead of shipping a wrongly-signed APK.
    val ksPath = System.getenv("MOGGR_KEYSTORE_PATH")
    val ksPass = System.getenv("MOGGR_KEYSTORE_PASSWORD")
    val kAlias = System.getenv("MOGGR_KEY_ALIAS")
    val keyPass = System.getenv("MOGGR_KEY_PASSWORD")
    val hasReleaseKey = !ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() &&
        !kAlias.isNullOrBlank() && !keyPass.isNullOrBlank() &&
        File(ksPath).exists()

    signingConfigs {
        create("moggr") {
            if (hasReleaseKey) {
                storeFile = File(ksPath!!)
                storePassword = ksPass
                keyAlias = kAlias
                // The PKCS12 was generated with OpenSSL, which protects the
                // private key with the store password (no separate key password).
                keyPassword = ksPass
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
            signingConfig = signingConfigs.getByName("moggr")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // CameraX
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit on-device face detection (bundled model, no API key, works offline)
    implementation("com.google.mlkit:face-detection:16.1.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // OkHttp for the keyless Moggr Coach chat (free tier, no API key)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Biometric app lock (never hand-roll a PIN)
    implementation("androidx.biometric:biometric:1.1.0")

    // ProcessLifecycleOwner for the app-lock background observer
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")

    // EncryptedSharedPreferences for profile PII
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
