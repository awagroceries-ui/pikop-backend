package com.ng.pikop

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
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

        // Immediate Init (Main Thread required for native SDKs, Places, and Firebase)
        try {
            val key = BuildConfig.GOOGLE_MAPS_API_KEY
            android.util.Log.e("PikopApp", "Initializing Places with Key Prefix: ${key.take(8)}...")
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, key)
            }
        } catch (e: Exception) {
            android.util.Log.e("PikopApp", "Places init failed: ${e.message}")
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                android.util.Log.e("PikopApp", "Firebase initialized on Main Thread")
            }
        } catch (e: Throwable) {
            android.util.Log.e("PikopApp", "Firebase init failed: ${e.message}")
        }

        // Background Init (Non-blocking) for remaining services
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.Default) {
            // Paystack removed: Native SDK is Card-only. Using Hosted Checkout WebView instead.
        }
    }
}
