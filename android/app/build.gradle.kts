plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    // Firebase, ARCore, Filament, TFLite etc.
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.ar:core:1.46.0")
    implementation("com.google.android.filament:filament-android:1.65.2")
    implementation("com.google.android.filament:filament-utils-android:1.65.2")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")


    // AndroidX AppCompat and UI components
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")


    // ✅ Modern ARCore + Sceneform (SceneView) SDK
//    implementation("com.google.ar:core:1.41.0")
//    implementation("io.github.sceneview:arsceneview:1.2.3")

    // Exclude Play Core duplicates if they appear transitively
    // (you can remove these if you explicitly add a single play-core dependency)
    configurations.all {
        exclude(group = "com.google.android.play", module = "core")
        exclude(group = "com.google.android.play", module = "core-common")
    }
}

android {
    namespace = "com.solemate.app.solemate_app"
    compileSdk = flutter.compileSdkVersion
    // keep ndkVersion if you pinned one in flutter (optional)
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.solemate.app.solemate_app"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // --- ABI filters: include the ABIs you want packaged into the APK ---
        // armeabi-v7a (32-bit ARM phones), arm64-v8a (64-bit phones), x86_64 (emulator / some devices)
        ndk {
            abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }

        // If you use CMake/native libs ensure externalNativeBuild is configured below
    }

    // If you have native code built via CMake, configure it here
    externalNativeBuild {
        cmake {
            // update the path if your CMakeLists is elsewhere
            path = file("CMakeLists.txt")
            // version is optional: only include if the SDK Manager has this CMake version installed
            // version = "3.22.1"
        }
    }

    // Optional: produce per-ABI APKs instead of one fat APK.
    // Use either this (splits) or the ndk.abiFilters above for packaging control.
//    splits {
//        abi {
//            isEnable = true
//            reset()
//            include("armeabi-v7a", "arm64-v8a", "x86_64")
//            isUniversalApk = false // if true, builds a universal APK including all ABIs
//        }
//    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    packaging {
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE-FIREBASE.txt",
                "META-INF/NOTICE"
            )
        }
    }


}

flutter {
    source = "../.."
}
