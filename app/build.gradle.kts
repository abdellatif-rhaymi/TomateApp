plugins {
    id("com.android.application")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.tomatosapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tomatosapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

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

    //aaptOptions {
        //noCompress("tflite")
    //}

    //packagingOptions {
        //pickFirst("lib/*/libtensorflowlite_jni.so")
        //pickFirst("lib/*/libtensorflowlite_gpu_jni.so")
        //pickFirst("lib/*/libtensorflowlite_flex_jni.so")
        // Exclusions pour résoudre les conflits Firebase
        //exclude("META-INF/*.kotlin_module")
       // exclude("META-INF/DEPENDENCIES")
       // exclude("META-INF/LICENSE*")
       // exclude("META-INF/NOTICE*")
    //}
}

dependencies {
    // AndroidX Core
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase (avec BOM pour la gestion des versions)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // TensorFlow Lite
    val tflite_version = "2.14.0"
    val tflite_support_version = "0.4.4"
    implementation("org.tensorflow:tensorflow-lite:$tflite_version")
    implementation("org.tensorflow:tensorflow-lite-support:$tflite_support_version")
    implementation("org.tensorflow:tensorflow-lite-gpu:$tflite_version")
    implementation("org.tensorflow:tensorflow-lite-metadata:$tflite_support_version")

    // Retrofit pour les appels réseau
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Ou une version plus récente si disponible
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Pour parser JSON avec Gson
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // Pour le logging des requêtes (très utile)
    // Gson (si tu l'utilises explicitement ou si converter-gson ne l'inclut pas transitivement)
    implementation("com.google.code.gson:gson:2.10.1") // Souvent inclus par converter-gson

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.navigation:navigation-fragment:2.5.0")
    implementation("androidx.navigation:navigation-ui:2.5.0")
    implementation("com.google.firebase:firebase-auth:21.0.1")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
}