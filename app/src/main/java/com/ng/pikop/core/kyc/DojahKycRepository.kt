package com.ng.pikop.core.kyc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.dojah.kyc_sdk_kotlin.DojahSdk
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
        if (activity == null) {
            android.util.Log.e("DojahRepo", "CRITICAL: Cannot launch KYC without an Activity context.")
            return
        }

        android.util.Log.d("DojahRepo", "INIT: $email | ref: $referenceId")
        
        // Step 1: SDK Setup (Ensuring Container is initialized with Activity context)
        try {
            DojahSdk.with(activity)
            setupSdk(activity)
        } catch (e: Exception) {
            android.util.Log.w("DojahRepo", "Step 1 Warmup warning: ${e.message}")
        }

        try {
            android.util.Log.d("DojahRepo", "STEP 2: Issuing DojahSdk.launch...")
            DojahSdk.launch(
                dojahLauncher = launcher,
                widgetId = widgetId,
                referenceId = referenceId,
                email = email
            )
            android.util.Log.d("DojahRepo", "SUCCESS: Widget command sent to system.")
        } catch (e: Throwable) {
            android.util.Log.e("DojahRepo", "FAILURE: Primary launch failed: ${e.message}", e)
            try {
                android.util.Log.d("DojahRepo", "RETRY: Attempting fallback with appId as widgetId...")
                DojahSdk.launch(
                    dojahLauncher = launcher,
                    widgetId = appId,
                    referenceId = referenceId,
                    email = email
                )
                android.util.Log.d("DojahRepo", "SUCCESS: Fallback launch issued.")
            } catch (e2: Throwable) {
                android.util.Log.e("DojahRepo", "CRITICAL: Fallback failed: ${e2.message}", e2)
                Toast.makeText(activity, "KYC System Error. Please try again later.", Toast.LENGTH_LONG).show()
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
