plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.winlator"
    compileSdk = 35
    ndkVersion = "24.0.8215888"

    defaultConfig {
        applicationId = "com.crazybreakdown.castlecrashers.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.0"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake { cppFlags += listOf("-std=c++17") }
        }
    }

    signingConfigs {
        create("castleRelease") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("castleRelease")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }

    sourceSets {
        getByName("main") {
            java.srcDir("../vendor/winlator-app/app/src/main/java")
            res.srcDir("../vendor/winlator-app/app/src/main/res")
            assets.srcDir("../vendor/winlator-app/app/src/main/assets")
            jniLibs.srcDir("../vendor/winlator-app/app/src/main/jniLibs")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../vendor/winlator-app/app/src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.github.luben:zstd-jni:1.5.2-3@aar")
    implementation("org.tukaani:xz:1.12")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
}
