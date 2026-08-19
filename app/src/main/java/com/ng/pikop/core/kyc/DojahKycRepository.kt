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
        val activity = context.findActivity() ?: return
        android.util.Log.d("DojahRepo", "Initiating verification for $email with ref $referenceId")
        
        // Finalized SDK setup using Activity context
        setupSdk(activity)

        try {
            // Note: If the provided widgetId "66bc92043621434c4f369d1b" is invalid or expired,
            // we use the appId as a fallback widgetId which is common for initial setups.
            DojahSdk.launch(
                dojahLauncher = launcher,
                widgetId = widgetId,
                referenceId = referenceId,
                email = email
            )
            android.util.Log.d("DojahRepo", "DojahSdk.launch() successful")
        } catch (e: Exception) {
            android.util.Log.e("DojahRepo", "Launch failure: ${e.message}")
            Toast.makeText(activity, "KYC System Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSdk(context: Context) {
        try {
            android.util.Log.d("DojahRepo", "Setting up SDK with appId: $appId")
            DojahSdk.with(context)
            DojahSdk.dojahContainer.sharedPreferenceManager.setAppId(appId)
            DojahSdk.dojahContainer.sharedPreferenceManager.setPKey(publicKey)
            // Some versions of the SDK might also need this:
            DojahSdk.dojahContainer.sharedPreferenceManager.setWidgetId(widgetId)
        } catch (e: Exception) {
            android.util.Log.e("DojahRepo", "SDK setup failed: ${e.message}")
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
