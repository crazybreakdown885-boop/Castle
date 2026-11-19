plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.castlecrashers.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.castlecrashers.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.documentfile:documentfile:1.0.1")
}