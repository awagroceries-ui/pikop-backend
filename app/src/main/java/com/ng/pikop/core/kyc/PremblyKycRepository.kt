package com.ng.pikop.core.kyc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import com.ng.pikop.core.network.ApiService
import javax.inject.Inject
import javax.inject.Named

/**
 * Prembly (Identitypass) KYC implementation.
 * Uses a Sequential Loader to ensure production stability across all account tiers.
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
        
        // STABLE WIDGET ENDPOINT (The Definitive Production URL)
        // Using config_id and public_key as query parameters bypasses path segment issues.
        val premblyUrl = "https://widget.identitypass.com/launch" +
                "?public_key=$publicKey" +
                "&config_id=$configId" +
                "&user_ref=$referenceId" +
                "&email=$email" +
                "&is_widget=true"

        android.util.Log.d("PremblyKYC", "Launching Direct Widget: $premblyUrl")
        
        activity.runOnUiThread {
            showResilientWebView(activity, listOf(premblyUrl), onSuccess, onError, onClose)
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
            
            // Professional Mobile UA
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        if (currentUrlIndex < urls.size - 1) {
                            currentUrlIndex++
                            android.util.Log.w("PremblyKYC", "URL variant $currentUrlIndex-1 failed, trying variant $currentUrlIndex...")
                            view?.loadUrl(urls[currentUrlIndex])
                        } else {
                            android.util.Log.e("PremblyKYC", "All URL variants failed.")
                            onError(error?.description?.toString() ?: "Connection failed")
                            dialog.dismiss()
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url.toString()
                    android.util.Log.d("PremblyKYC", "Navigation Intercept: $uri")
                    
                    if (uri.contains("success") || uri.contains("approved") || uri.contains("verified") || uri.contains("redirect")) {
                        onSuccess("verified")
                        dialog.dismiss()
                        return true
                    }
                    if (uri.contains("cancel") || uri.contains("close") || uri.contains("exit") || uri.contains("failure")) {
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
