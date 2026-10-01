plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kurupdevs.mogscan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kurupdevs.mogscan"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Sign every build (including debug) with a proper release key instead of the
    // well-known Android debug key — that's what makes Play Protect hard-block
    // sideloaded installs. The key is generated once per build machine.
    val ksFile = java.io.File(System.getProperty("user.home"), ".mogscan-release.p12")
    if (!ksFile.exists()) {
        try {
            ProcessBuilder(
                "keytool", "-genkeypair",
                "-keystore", ksFile.absolutePath, "-storetype", "PKCS12",
                "-alias", "mogscan", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "10950",
                "-storepass", "mogscan", "-keypass", "mogscan",
                "-dname", "CN=kurupdevs, O=kurupdevs, C=IN"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) { /* fall back to default signing */ }
    }
    val hasReleaseKey = ksFile.exists()

    signingConfigs {
        create("mogscan") {
            if (hasReleaseKey) {
                storeFile = ksFile
                storePassword = "mogscan"
                keyAlias = "mogscan"
                keyPassword = "mogscan"
            }
        }
    }

    buildTypes {
        debug {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("mogscan")
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("mogscan")
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
}
