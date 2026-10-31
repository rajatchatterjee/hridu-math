plugins {
    id("com.android.application")
}

android {
    namespace = "com.hridu.math"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hridu.math"
        minSdk = 23
        targetSdk = 34
        // CI run number -> every Release installs over the previous one.
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    signingConfigs {
        // Fixed debug key committed to the repo so each new build installs over
        // the old one (a random CI key causes "App not installed").
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // The game itself lives in /web (also served on GitHub Pages);
    // the APK just bundles that folder as its assets.
    sourceSets {
        getByName("main") {
            assets.srcDirs("../web")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
