package com.ng.pikop.core.kyc

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface KycManager {
    fun startVerification(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    )

    fun launchVerification(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        firstName: String,
        lastName: String,
        email: String,
        referenceId: String
    )
}
