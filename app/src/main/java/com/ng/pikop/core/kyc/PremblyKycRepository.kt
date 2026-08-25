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
 * Uses the High-Stability Identitypass Hosted Pattern.
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
        
        // STABLE HOSTED ENDPOINT
        // This is the most reliable production endpoint for Prembly integrations.
        val premblyUrl = "https://widget.identitypass.com/launch" +
                "?public_key=$publicKey" +
                "&merchant_key=$publicKey" + 
                "&config_id=$configId" +
                "&user_ref=$referenceId" +
                "&email=$email" +
                "&is_widget=true"

        android.util.Log.d("PremblyKYC", "Launching Resilient Hosted Loader: $premblyUrl")
        
        activity.runOnUiThread {
            showHostedWebView(activity, premblyUrl, onSuccess, onError, onClose)
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
    private fun showHostedWebView(
        activity: Activity,
        url: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val dialog = android.app.Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        val webView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Modern Webview settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            
            // Professional Mobile User-Agent
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        android.util.Log.e("PremblyKYC", "Load Error: ${error?.description}")
                        onError(error?.description?.toString() ?: "Connection failed")
                        dialog.dismiss()
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url.toString()
                    android.util.Log.d("PremblyKYC", "Intercept: $uri")
                    
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
        webView.loadUrl(url)
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
