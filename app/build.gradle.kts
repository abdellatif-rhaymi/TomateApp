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

    buildFeatures {
        buildConfig = true
    }
    // --------------------

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://tomato-disease-service-299287005031.europe-west1.run.app/\"")
            isMinifyEnabled = false
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://tomato-disease-service-299287005031.europe-west1.run.app/\"")
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

    // L'option noCompress "tflite" n'est plus nécessaire si tu n'as plus de .tflite dans les assets
    // aaptOptions {
    //     noCompress("tflite")
    // }

    // Les packagingOptions ne sont plus nécessaires si tu n'utilises plus TFLite localement
    // packagingOptions {
    //     pickFirst("lib/*/libtensorflowlite_jni.so")
    //     pickFirst("lib/*/libtensorflowlite_gpu_jni.so")
    // }
}

dependencies {
    // Dépendances AndroidX standards
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0") // Utilise une version plus récente
    implementation("androidx.activity:activity:1.8.2") // activity-ktx si tu utilises Kotlin pour l'activité
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Retrofit pour les appels réseau
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Ou une version plus récente si disponible
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Pour parser JSON avec Gson
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // Pour le logging des requêtes (très utile)
    // Gson (si tu l'utilises explicitement ou si converter-gson ne l'inclut pas transitivement)
    // implementation("com.google.code.gson:gson:2.10.1") // Souvent inclus par converter-gson

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Ajoute les dépendances Firebase si tu les utilises (avec BoM)
    // implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // implementation("com.google.firebase:firebase-auth")
    // ... etc ...
}