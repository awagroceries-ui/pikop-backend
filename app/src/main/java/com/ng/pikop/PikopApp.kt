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
            PaystackSdk.setPublicKey("pk_live_e458eda08e2d7d24cc96a3f0b886023f9bfd9c15")
        } catch (e: Exception) {}
        
        // Initialize Google Places
        try {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, "AIzaSyDEsNglOB5t0J-D_yfMciy3Yrzj4B5ZzoQ")
            }
        } catch (e: Exception) {}

        // Initialize Firebase safely
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {}
    }
}
