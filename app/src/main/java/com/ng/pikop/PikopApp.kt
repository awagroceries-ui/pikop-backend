package com.ng.pikop

import android.app.Application
import com.google.android.libraries.places.api.Places
import co.paystack.android.PaystackSdk
import com.google.firebase.FirebaseApp
import com.dojah.kyc_sdk_kotlin.DojahSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@HiltAndroidApp
class PikopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        android.util.Log.d("PikopApp", "Application onCreate - Parallel Init Start")

        // Background Init (Non-blocking) to prevent launch timeout
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.Default) {
            // Initialize Dojah SDK Container (Moved to background for speed)
            try {
                DojahSdk.with(this@PikopApp)
                android.util.Log.d("PikopApp", "Dojah init complete (background)")
            } catch (e: Exception) {
                android.util.Log.e("PikopApp", "Dojah init failed: ${e.message}")
            }

            // Initialize Paystack
            try {
                PaystackSdk.initialize(applicationContext)
                PaystackSdk.setPublicKey("pk_live_346dba41298095981968ef0c243c8c9fc022311a")
            } catch (e: Exception) {
                android.util.Log.e("PikopApp", "Paystack init failed: ${e.message}")
            }
            
            // Initialize Google Places
            try {
                if (!Places.isInitialized()) {
                    Places.initialize(applicationContext, "AIzaSyDEsNglOB5t0J-D_yfMciy3Yrzj4B5ZzoQ")
                }
            } catch (e: Exception) {
                android.util.Log.e("PikopApp", "Places init failed: ${e.message}")
            }

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
