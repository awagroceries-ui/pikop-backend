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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Prembly (Identitypass) KYC implementation.
 * Uses a Sequential Loader to ensure production stability across all account tiers.
 */
class PremblyKycRepository @Inject constructor(
    private val apiService: ApiService
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
        
        android.util.Log.d("PremblyKYC", "Initiating Session via Backend...")
        
        MainScope().launch {
            try {
                val response = apiService.startKycSession(mapOf("provider" to "prembly"))
                val verificationUrl = response.data?.url ?: response.url
                
                if (!verificationUrl.isNullOrBlank()) {
                    android.util.Log.d("PremblyKYC", "Session received. Launching URL: $verificationUrl")
                    activity.runOnUiThread {
                        showResilientWebView(activity, listOf(verificationUrl), onSuccess, onError, onClose)
                    }
                } else {
                    android.util.Log.e("PremblyKYC", "Backend returned empty verification URL")
                    onError("Failed to initiate session")
                }
            } catch (e: Exception) {
                android.util.Log.e("PremblyKYC", "Initiation failure: ${e.message}")
                onError("Connection error: ${e.message}")
            }
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
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    
                    // Visual Error Detection: Scan for "Broken" state in the DOM
                    view?.evaluateJavascript(
                        "(function() { " +
                        "  var text = document.body.innerText || ''; " +
                        "  return text.includes('Something is broken') || text.includes('Error 404'); " +
                        "})();"
                    ) { isError ->
                        if (isError == "true") {
                            android.util.Log.e("PremblyKYC", "Visual Error Detected. Switching variants...")
                            handleVariantFailure(view)
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        handleVariantFailure(view)
                    }
                }

                private fun handleVariantFailure(view: WebView?) {
                    if (currentUrlIndex < urls.size - 1) {
                        currentUrlIndex++
                        android.util.Log.w("PremblyKYC", "Switching to variant $currentUrlIndex...")
                        view?.loadUrl(urls[currentUrlIndex])
                    } else {
                        android.util.Log.e("PremblyKYC", "All Prembly variants failed. Resorting to fallback.")
                        onError("Prembly failed all attempts")
                        dialog.dismiss()
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
