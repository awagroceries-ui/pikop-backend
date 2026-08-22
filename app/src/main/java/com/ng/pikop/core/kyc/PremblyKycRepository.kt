package com.ng.pikop.core.kyc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.ng.pikop.core.network.ApiService
import javax.inject.Inject
import javax.inject.Named

/**
 * Prembly (Identitypass) KYC implementation.
 * Uses a High-Resilience Sequential Loader to bypass URL errors.
 */
class PremblyKycRepository @Inject constructor(
    private val apiService: ApiService,
    @Named("premblyPublicKey") private val publicKey: String
) : KycManager {

    override fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val activity = context.findActivity() ?: return
        
        // RESILIENT URL LIST with Multi-Param alignment
        val urls = listOf(
            "https://widget.identitypass.com/launch?public_key=$publicKey&app_id=$publicKey&merchant_key=$publicKey&user_ref=$referenceId&email=$email",
            "https://widget.prembly.com/launch?public_key=$publicKey&app_id=$publicKey&merchant_key=$publicKey&user_ref=$referenceId&email=$email",
            "https://app.prembly.com/launch?merchant_key=$publicKey&app_id=$publicKey&user_ref=$referenceId&email=$email"
        )

        android.util.Log.d("PremblyKYC", "Launching Resilient Loader...")
        
        activity.runOnUiThread {
            showResilientWebView(activity, urls, onSuccess, onError, onClose)
        }
    }

    override fun launchVerification(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        email: String,
        referenceId: String
    ) {
        startVerification(context, email, referenceId, {}, {}, {})
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showResilientWebView(
        activity: Activity,
        urls: List<String>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val dialog = android.app.Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        var currentUrlIndex = 0
        
        val webView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            
            // Standard User Agent
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    val failingUrl = request?.url.toString()
                    if (failingUrl == urls[currentUrlIndex] && currentUrlIndex < urls.size - 1) {
                        currentUrlIndex++
                        android.util.Log.w("PremblyKYC", "URL failed. Retrying with variant $currentUrlIndex...")
                        view?.loadUrl(urls[currentUrlIndex])
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url.toString()
                    if (uri.contains("success") || uri.contains("approved") || uri.contains("verified")) {
                        onSuccess("verified")
                        dialog.dismiss()
                        return true
                    }
                    if (uri.contains("cancel") || uri.contains("close") || uri.contains("exit")) {
                        onClose()
                        dialog.dismiss()
                        return true
                    }
                    return false
                }
            }
        }

        dialog.setContentView(webView)
        dialog.show()
        webView.loadUrl(urls[0])
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
