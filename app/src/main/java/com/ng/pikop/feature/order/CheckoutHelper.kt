package com.ng.pikop.feature.order

import android.app.Activity
import co.paystack.android.PaystackSdk
import co.paystack.android.Transaction
import co.paystack.android.model.Charge

object CheckoutHelper {
    fun startCheckout(
        activity: Activity,
        email: String,
        amountInKobo: Long,
        accessCode: String,
        onSuccess: (Transaction) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val charge = Charge().apply {
            amount = amountInKobo.toInt()
            this.email = email
            this.accessCode = accessCode
            currency = "NGN"
        }

        PaystackSdk.chargeCard(activity, charge, object : co.paystack.android.Paystack.TransactionCallback {
            override fun onSuccess(transaction: Transaction?) {
                if (transaction != null) {
                    onSuccess(transaction)
                }
            }

            override fun beforeValidate(transaction: Transaction?) {
            }

            override fun onError(error: Throwable?, transaction: Transaction?) {
                if (error != null) {
                    onError(error)
                }
            }
        })
    }
}
