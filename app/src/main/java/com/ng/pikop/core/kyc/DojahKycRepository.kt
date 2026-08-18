package com.ng.pikop.core.kyc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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

        // Set credentials in the SDK container
        DojahSdk.with(context)
        DojahSdk.dojahContainer.sharedPreferenceManager.setAppId(appId)
        DojahSdk.dojahContainer.sharedPreferenceManager.setPKey(publicKey)

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
        // Initialize SDK and set credentials
        DojahSdk.with(context)
        DojahSdk.dojahContainer.sharedPreferenceManager.setAppId(appId)
        DojahSdk.dojahContainer.sharedPreferenceManager.setPKey(publicKey)

        DojahSdk.launch(
            dojahLauncher = launcher,
            widgetId = widgetId,
            referenceId = referenceId,
            email = email
        )
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
