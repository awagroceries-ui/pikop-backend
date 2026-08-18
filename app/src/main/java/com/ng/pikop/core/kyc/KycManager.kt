package com.ng.pikop.core.kyc

import android.content.Context

interface KycManager {
    fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    )
}
