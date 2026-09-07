package com.ng.pikop

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.dojah.kyc_sdk_kotlin.DojahSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@HiltAndroidApp
class PikopApp : Application() {
    init {
        android.util.Log.e("PikopApp", "!!! PIKOP APP CLASS LOADED !!!")
    }
    override fun onCreate() {
        super.onCreate()
        
        android.util.Log.e("PikopApp", "!!! PIKOP APP ONCREATE START !!!")

        // Immediate Init (Main Thread required for native SDKs & Places)
        try {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
            }
        } catch (e: Exception) {
            android.util.Log.e("PikopApp", "Places init failed: ${e.message}")
        }

        // Background Init (Non-blocking) for remaining services
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.Default) {
            // Paystack removed: Native SDK is Card-only. Using Hosted Checkout WebView instead.

            // Initialize Firebase safely
            try {
                if (FirebaseApp.getApps(this@PikopApp).isEmpty()) {
                    FirebaseApp.initializeApp(this@PikopApp)
                }
            } catch (e: Throwable) {
                android.util.Log.e("PikopApp", "Firebase init failed: ${e.message}")
            }
        }
    }
}
