package com.ng.pikop.core.kyc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.dojah.kyc_sdk_kotlin.DojahSdk
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * Dojah KYC implementation.
 * Used as high-reliability fallback for Prembly.
 */
class DojahKycRepository @Inject constructor(
    @Named("dojahAppId") private val appId: String,
    @Named("dojahPublicKey") private val publicKey: String
) : KycManager {

    // Dojah Widget ID (Restored from history)
    private val widgetId = "66bc92043621434c4f369d1b"

    override fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        // startVerification is intended for SDK-less or pure callback flows
        // For Dojah, we primarily use the ActivityResultLauncher flow (launchVerification)
        val activity = context.findActivity() ?: return
        activity.runOnUiThread {
            try {
                setupSdk(activity)
                DojahSdk.launchWithBackwardCompatibility(
                    activity = activity,
                    widgetId = widgetId,
                    referenceId = referenceId,
                    email = email
                )
                onSuccess("session_launched")
            } catch (e: Exception) {
                android.util.Log.e("DojahRepo", "Direct launch failed: ${e.message}")
                onError(e.message ?: "Launch failed")
            }
        }
    }

    override fun launchVerification(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        email: String,
        referenceId: String
    ) {
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            android.util.Log.e("DojahRepo", "CRITICAL: Activity not ready for KYC.")
            return
        }

        android.util.Log.d("DojahRepo", "Initializing Dojah for: $email")
        
        activity.runOnUiThread {
            try {
                setupSdk(activity)
            } catch (e: Exception) {
                android.util.Log.w("DojahRepo", "Warmup warning: ${e.message}")
            }
        }

        MainScope().launch {
            delay(500)
            try {
                DojahSdk.launch(
                    dojahLauncher = launcher,
                    widgetId = widgetId,
                    referenceId = referenceId,
                    email = email
                )
                android.util.Log.d("DojahRepo", "Dojah SDK command sent.")
            } catch (e: Throwable) {
                android.util.Log.e("DojahRepo", "SDK launch failed: ${e.message}")
                Toast.makeText(activity, "KYC System Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSdk(context: Context) {
        try {
            DojahSdk.with(context)
            DojahSdk.dojahContainer.sharedPreferenceManager.setAppId(appId)
            DojahSdk.dojahContainer.sharedPreferenceManager.setPKey(publicKey)
            DojahSdk.dojahContainer.sharedPreferenceManager.setWidgetId(widgetId)
            android.util.Log.d("DojahRepo", "SDK Parameters Verified")
        } catch (e: Exception) {
            android.util.Log.e("DojahRepo", "Setup critical error: ${e.message}")
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
