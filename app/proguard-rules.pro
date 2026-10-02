# Moggr release ProGuard/R8 rules.
# Libraries (CameraX, ML Kit, OkHttp, coroutines, Compose) ship their own
# consumer rules which R8 applies automatically; the keeps below are only
# for our own entry points and the two reflection-heavy SDKs.

# ML Kit face detection (bundled model, reflection inside the SDK)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# App entry points referenced by the framework / manifest
-keep class com.kurupdevs.moggr.MainActivity { *; }
-keep class com.kurupdevs.moggr.MoggrApplication { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    *;
}

# Biometric + security-crypto keep their own consumer rules; nothing needed.
