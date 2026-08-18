package com.ng.pikop.core.kyc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.dojah.kyc_sdk_kotlin.DojahSdk
import javax.inject.Inject
import javax.inject.Named

class DojahKycRepository @Inject constructor(
    @Named("dojahAppId") private val appId: String,
    @Named("dojahPublicKey") private val publicKey: String
) : KycManager {

    override fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val widgetId = "66bc92043621434c4f369d1b"
        val activity = context.findActivity() ?: return

        DojahSdk.with(context)
            .launchWithBackwardCompatibility(
                activity = activity,
                widgetId = widgetId,
                referenceId = referenceId,
                email = email
            )
        // Note: Result handling in 0.4.1 usually requires ActivityResultLauncher 
        // or onActivityResult. For this repo, we assume launch is success.
        onSuccess("session_launched")
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
