plugins {
    id("com.android.application")
    id("com.google.gms.google-services")

    //alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.vibefitapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vibefitapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val unsplashApiKey: String = project.findProperty("UNSPLASH_API_KEY") as String
        buildConfigField("String", "UNSPLASH_API_KEY", "\"$unsplashApiKey\"")
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
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-storage")
    // Add the dependency for the Vertex AI in Firebase library
    implementation("com.google.firebase:firebase-vertexai")
    // Required for one-shot operations (to use `ListenableFuture` from Guava Android)
    implementation("com.google.guava:guava:32.1.3-android")
    // Required for streaming operations (to use `Publisher` from Reactive Streams)
    implementation("org.reactivestreams:reactive-streams:1.0.4")
    implementation("com.google.firebase:firebase-appcheck:18.0.0") // Use the latest version
    implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")

    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.google.android.material:material:1.12.0")

    implementation ("com.github.chrisbanes:PhotoView:2.3.0") // Picture Zooming

    implementation ("com.github.MasayukiSuda:Mp4Composer-android:v0.4.1")

    implementation ("com.github.yalantis:ucrop:2.2.10") // UCrop library for cropping
}