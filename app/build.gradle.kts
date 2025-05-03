plugins {
    id("com.android.application")
    // id("com.google.gms.google-services") // Décommente si tu utilises Firebase
}

android {
    namespace = "com.example.tomatosapp" // Adapte si ton namespace est différent
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tomatosapp" // Adapte si ton ID est différent
        minSdk = 24
        targetSdk = 34 // Cible une version récente d'Android
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Ajout de x86 pour la compatibilité avec les émulateurs x86
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // IMPORTANT: Empêche la compression des modèles TFLite
    aaptOptions {
        noCompress("tflite")
    }

    // Ajout pour résoudre le problème de compatibilité native
    packagingOptions {
        pickFirst("lib/*/libtensorflowlite_jni.so")
        pickFirst("lib/*/libtensorflowlite_gpu_jni.so")
    }
}

dependencies {
    // Dépendances AndroidX standards
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Dans le bloc dependencies { ... }

// --- TensorFlow Lite - Versions Récentes + Flex ---
    val tflite_version = "2.16.1" // Version récente
    val tflite_support_version = "0.4.4" // Support récent

// Runtime TFLite principal
    implementation("org.tensorflow:tensorflow-lite:${tflite_version}")
// Support Library (traitement image/tensor)
    implementation("org.tensorflow:tensorflow-lite-support:${tflite_support_version}")
// Dépendance Flex (Fallback pour ops non trouvées)
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:${tflite_version}") // DOIT être la même version que le runtime principal
// -------------------------------------------------

// Assure-toi que les autres dépendances tflite (-gpu, -metadata, etc.) sont commentées ou supprimées pour ce test
    // CameraX (si tu l'utilises)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Glide (pour le chargement d'images)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}