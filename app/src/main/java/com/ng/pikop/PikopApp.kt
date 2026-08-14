package com.ng.pikop

import android.app.Application
import com.google.android.libraries.places.api.Places
import co.paystack.android.PaystackSdk
import com.google.firebase.FirebaseApp

class PikopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Paystack
        try {
            PaystackSdk.initialize(applicationContext)
            PaystackSdk.setPublicKey("pk_live_346dba41298095981968ef0c243c8c9fc022311a")
        } catch (e: Exception) {}
        
        // Initialize Google Places
        try {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, "AIzaSyDEsNglOB5t0J-D_yfMciy3Yrzj4B5ZzoQ")
            }
        } catch (e: Exception) {}

        // Initialize Firebase safely
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            android.util.Log.e("PikopApp", "Firebase init failed: ${e.message}")
        }

        // Initialize Didit SDK
        try {
            me.didit.sdk.DiditSdk.initialize(this)
        } catch (e: Throwable) {
            android.util.Log.e("PikopApp", "Didit init failed: ${e.message}")
        }
    }
}
