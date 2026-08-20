package com.ng.pikop.core.kyc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.dojah.kyc_sdk_kotlin.DojahSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class DojahKycRepository @Inject constructor(
    @Named("dojahAppId") private val appId: String,
    @Named("dojahPublicKey") private val publicKey: String
) : KycManager {

    private val widgetId = "66bc92043621434c4f369d1b"

    override fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val activity = context.findActivity() ?: return

        // Ensure SDK is initialized with credentials
        setupSdk(context)

        DojahSdk.launchWithBackwardCompatibility(
            activity = activity,
            widgetId = widgetId,
            referenceId = referenceId,
            email = email
        )
        onSuccess("session_launched")
    }

    override fun launchVerification(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        email: String,
        referenceId: String
    ) {
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            android.util.Log.e("DojahRepo", "CRITICAL: Activity is null or finishing. Cannot launch KYC.")
            return
        }

        android.util.Log.d("DojahRepo", "INIT: $email | ref: $referenceId")
        
        // Step 1: SDK Setup (Ensuring Container is initialized with Activity context on UI thread)
        activity.runOnUiThread {
            try {
                DojahSdk.with(activity.applicationContext)
                setupSdk(activity)
            } catch (e: Exception) {
                android.util.Log.w("DojahRepo", "Step 1 warmup warning: ${e.message}")
            }
        }

        // Give UI thread a moment to stabilize
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(500)
            try {
                android.util.Log.d("DojahRepo", "STEP 2: Issuing DojahSdk.launch...")
                DojahSdk.launch(
                    dojahLauncher = launcher,
                    widgetId = widgetId,
                    referenceId = referenceId,
                    email = email
                )
                android.util.Log.d("DojahRepo", "SUCCESS: Widget command sent.")
            } catch (e: Throwable) {
                android.util.Log.e("DojahRepo", "FAILURE: ${e.message}", e)
                Toast.makeText(activity, "KYC Error: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Final fallback
                try {
                    DojahSdk.launch(dojahLauncher = launcher, widgetId = appId, referenceId = referenceId, email = email)
                } catch (_: Exception) {}
            }
        }
    }

    private fun setupSdk(context: Context) {
        try {
            android.util.Log.d("DojahRepo", "Setting up SDK | appId: $appId | pKey: ${publicKey.take(8)}... | widgetId: $widgetId")
            DojahSdk.with(context)
            DojahSdk.dojahContainer.sharedPreferenceManager.setAppId(appId)
            DojahSdk.dojahContainer.sharedPreferenceManager.setPKey(publicKey)
            DojahSdk.dojahContainer.sharedPreferenceManager.setWidgetId(widgetId)
            android.util.Log.d("DojahRepo", "SDK Configured Successfully")
        } catch (e: Exception) {
            android.util.Log.e("DojahRepo", "SDK setup critical failure: ${e.message}")
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}
