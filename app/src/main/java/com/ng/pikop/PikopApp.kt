package com.ng.pikop

import android.app.Application
import co.paystack.android.PaystackSdk

class PikopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PaystackSdk.initialize(applicationContext)
        // Configured with your live Paystack Public Key
        PaystackSdk.setPublicKey("pk_live_e458eda08e2d7d24cc96a3f0b886023f9bfd9c15")
    }
}
