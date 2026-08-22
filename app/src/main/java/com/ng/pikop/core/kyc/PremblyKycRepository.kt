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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ng.pikop.core.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * Prembly (Identitypass) KYC implementation.
 * Uses a Compose-friendly Dialog + WebView flow for maximum reliability.
 */
class PremblyKycRepository @Inject constructor(
    private val apiService: ApiService,
    @Named("premblyPublicKey") private val publicKey: String
) : KycManager {

    // Internal state to hold the Composable UI if needed, but KycManager is usually called from ViewModel
    // We will use a dedicated screen or a global dialog trigger.
    
    override fun startVerification(
        context: Context,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val activity = context.findActivity() ?: return
        
        // RESILIENT LOADER: Try stable Identitypass endpoint with Triple-Key parameters
        val primaryUrl = "https://widget.identitypass.com/launch" +
                "?public_key=$publicKey" +
                "&merchant_key=$publicKey" +
                "&app_id=$publicKey" +
                "&user_ref=$referenceId" +
                "&email=$email"

        val fallbackUrl = "https://widget.prembly.com/launch" +
                "?public_key=$publicKey" +
                "&user_ref=$referenceId" +
                "&email=$email"

        android.util.Log.d("PremblyKYC", "Launching Resilient Widget Loader")
        
        activity.runOnUiThread {
            showWebViewDialog(activity, primaryUrl, fallbackUrl, onSuccess, onError, onClose)
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
    private fun showWebViewDialog(
        activity: Activity,
        url: String,
        fallbackUrl: String,
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
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            
            // Pro-Tier User Agent to prevent bot-detection blocking
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    android.util.Log.d("PremblyKYC", "Loaded: $url")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    val errorUrl = request?.url?.toString() ?: ""
                    if (errorUrl == url && !errorUrl.contains(fallbackUrl)) {
                        android.util.Log.w("PremblyKYC", "Primary URL failed. Trying fallback...")
                        view?.loadUrl(fallbackUrl)
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url.toString()
                    android.util.Log.d("PremblyKYC", "URL Trigger: $uri")
                    
                    if (uri.contains("success") || uri.contains("approved") || uri.contains("/webhooks/redirect")) {
                        onSuccess("verified")
                        dialog.dismiss()
                        return true
                    }
                    if (uri.contains("cancel") || uri.contains("close") || uri.contains("failure")) {
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
