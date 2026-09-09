import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ng.pikop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ng.pikop"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val mapsKey = project.rootProject.file("local.properties").let {
            if (it.exists()) {
                val props = Properties()
                props.load(it.inputStream())
                val key = props.getProperty("googleMapsApiKey") ?: ""
                println(">>> Pikop Build: Using Google Maps API Key: ${key.take(8)}...")
                key
            } else ""
        }
        manifestPlaceholders["googleMapsApiKey"] = mapsKey
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${mapsKey}\"")

        val paystackKey = project.rootProject.file("local.properties").let {
            if (it.exists()) {
                val props = Properties()
                props.load(it.inputStream())
                props.getProperty("paystackPublicKey") ?: ""
            } else ""
        }
        buildConfigField("String", "PAYSTACK_PUBLIC_KEY", "\"${paystackKey}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("co.paystack.android:paystack:3.1.3")
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Jetpack DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Lifecycle & Navigation
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.socket.io.client)
    implementation(libs.google.places)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.coil.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.dojah.sdk)
    
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
