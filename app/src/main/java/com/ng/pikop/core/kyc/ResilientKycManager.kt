package com.ng.pikop.core.kyc

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import javax.inject.Inject

/**
 * Composite KYC Manager that provides automatic fallback from Prembly to Dojah.
 * If the primary (Prembly) reports an error or fails to load, it silently
 * switches the user to the Dojah SDK.
 */
class ResilientKycManager @Inject constructor(
    private val premblyRepo: PremblyKycRepository,
    private val dojahRepo: DojahKycRepository
) : KycManager {

    override fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        android.util.Log.d("ResilientKYC", "Attempting primary provider (Prembly)...")
        premblyRepo.startVerification(
            context = context,
            email = email,
            referenceId = referenceId,
            onSuccess = onSuccess,
            onError = { error ->
                android.util.Log.w("ResilientKYC", "Primary failed ($error). Switching to Dojah.")
                dojahRepo.startVerification(context, email, referenceId, onSuccess, onError, onClose)
            },
            onClose = onClose
        )
    }

    override fun launchVerification(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        email: String,
        referenceId: String
    ) {
        android.util.Log.d("ResilientKYC", "Launching primary (Prembly) with SDK fallback...")
        premblyRepo.startVerification(
            context = context,
            email = email,
            referenceId = referenceId,
            onSuccess = { },
            onError = { error ->
                android.util.Log.w("ResilientKYC", "Prembly load failed ($error). Switching to Dojah SDK.")
                dojahRepo.launchVerification(context, launcher, email, referenceId)
            },
            onClose = { }
        )
    }
}
