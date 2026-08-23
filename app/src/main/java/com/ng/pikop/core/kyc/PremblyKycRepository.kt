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
 * Uses a High-Resilience Sequential Loader with explicit Config ID and Guest Mode support.
 */
class PremblyKycRepository @Inject constructor(
    private val apiService: ApiService,
    @Named("premblyPublicKey") private val publicKey: String,
    @Named("premblyConfigId") private val configId: String
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
        
        // RESILIENT URL LIST - Direct Widget Launch with Guest Mode (is_widget=true)
        val urls = listOf(
            "https://widget.identitypass.com/launch/$configId?public_key=$publicKey&user_ref=$referenceId&email=$email&is_widget=true",
            "https://widget.prembly.com/launch/$configId?public_key=$publicKey&user_ref=$referenceId&email=$email&is_widget=true",
            "https://widget.identitypass.com/launch?public_key=$publicKey&config_id=$configId&user_ref=$referenceId&email=$email&is_widget=true"
        )

        android.util.Log.d("PremblyKYC", "Launching Guest-Mode Widget: ${urls[0]}")
        
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
            
            clearCache(true)
            clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
            
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            
            // Modern Mobile UA to satisfy widget security
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    val failingUrl = request?.url.toString()
                    // Only retry if the failing URL is the one we tried to load (ignores sub-resources)
                    if (request?.isForMainFrame == true && currentUrlIndex < urls.size - 1) {
                        currentUrlIndex++
                        android.util.Log.w("PremblyKYC", "Variant $currentUrlIndex loading after error: ${error?.description}")
                        view?.loadUrl(urls[currentUrlIndex])
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url.toString()
                    android.util.Log.d("PremblyKYC", "Nav: $uri")
                    
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
