plugins {
    id("com.android.library")
}

android {
    namespace = "com.winlator"
    compileSdk = 35
    ndkVersion = "24.0.8215888"

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake { cppFlags += listOf("-std=c++17") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { buildConfig = true }

    sourceSets {
        getByName("main") {
            val upstream = "../vendor/winlator-app/app/src/main"
            manifest.srcFile("$upstream/AndroidManifest.xml")
            java.setSrcDirs(listOf("$upstream/java"))
            res.setSrcDirs(listOf("$upstream/res"))
            assets.setSrcDirs(listOf("$upstream/assets"))
            jniLibs.setSrcDirs(listOf("$upstream/jniLibs"))
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
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.github.luben:zstd-jni:1.5.2-3@aar")
    implementation("org.tukaani:xz:1.12")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
}
