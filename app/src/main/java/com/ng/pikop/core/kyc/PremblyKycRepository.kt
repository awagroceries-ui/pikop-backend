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
 * Uses the official Inline JS Widget for maximum reliability.
 */
class PremblyKycRepository @Inject constructor(
    @Suppress("unused") private val apiService: ApiService,
    @Named("premblyPublicKey") private val publicKey: String,
    @Named("premblyWidgetId") private val widgetId: String
) : KycManager {

    override fun startVerification(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val activity = context.findActivity() ?: return
        activity.runOnUiThread {
            showInlineWidget(activity, firstName, lastName, email, referenceId, onSuccess, onError, onClose)
        }
    }

    override fun launchVerification(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        firstName: String,
        lastName: String,
        email: String,
        referenceId: String
    ) {
        // ActivityResultLauncher doesn't directly provide context, 
        // we'll rely on startVerification being called for Inline Widget UI.
        // For compatibility with the interface:
        android.util.Log.e("PremblyKYC", "launchVerification called - use startVerification for Inline Widget")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showInlineWidget(
        activity: Activity,
        firstName: String,
        lastName: String,
        email: String,
        referenceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        // Diagnostic Logging
        android.util.Log.d("PremblyKYC", "Injected widget_id: $widgetId, widget_key: $publicKey")

        // Enable Remote Debugging for troubleshooting
        WebView.setWebContentsDebuggingEnabled(true)
        
        val dialog = android.app.Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        val webView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            @Suppress("DEPRECATION")
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false

            // Capture JS console output for diagnostics
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    android.util.Log.d("PremblyWidgetJS", "${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                    return true
                }
                override fun onPermissionRequest(request: PermissionRequest) {
                    // Critical for liveness/selfie step
                    request.grant(request.resources)
                }
            }
            
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onResult(response: String) {
                    android.util.Log.d("PremblyKYC", "Result received: $response")
                    try {
                        val json = org.json.JSONObject(response)
                        val status = json.optString("status", "failed")
                        activity.runOnUiThread {
                            when (status) {
                                "success" -> onSuccess(response)
                                "cancelled" -> onClose()
                                else -> onError(json.optString("message", "Verification failed"))
                            }
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PremblyKYC", "Error parsing result", e)
                        activity.runOnUiThread { 
                            onError("Data error") 
                            dialog.dismiss()
                        }
                    }
                }
            }, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: ""
                    android.util.Log.d("PremblyKYC", "Navigating to: $url")
                    
                    // Handle Redirect URL (Success Fallback)
                    if (url.contains("webhooks/redirect")) {
                        android.util.Log.d("PremblyKYC", "Redirect detected. Triggering success callback.")
                        activity.runOnUiThread {
                            onSuccess("{\"status\":\"success\",\"message\":\"Redirect captured\"}")
                            dialog.dismiss()
                        }
                        return true
                    }
                    return false
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    android.util.Log.e("PremblyKYC", "WebView Error: $description")
                }
            }
        }

        // Inline Widget HTML Shell using V3 API
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Verification | Pikop</title>
                <style>
                    body { margin: 0; padding: 0; background-color: #ffffff; height: 100vh; font-family: -apple-system, sans-serif; }
                </style>
            </head>
            <body>
                <script src="https://js.prembly.com/v1/inline/widget-v3.js"></script>
                <script>
                    // Timeout safety
                    var loadTimeout = setTimeout(function() {
                        if (typeof IdentityKYC === 'undefined') {
                            AndroidBridge.onResult(JSON.stringify({ status: "failed", message: "Library load timeout" }));
                        }
                    }, 10000);

                    window.onload = function() {
                        clearTimeout(loadTimeout);
                        try {
                            if (typeof IdentityKYC === 'undefined') {
                                AndroidBridge.onResult(JSON.stringify({ status: "failed", message: "IdentityKYC not found" }));
                                return;
                            }
                            IdentityKYC.verify({
                                widget_id: "$widgetId",
                                widget_key: "$publicKey",
                                first_name: "$firstName",
                                last_name: "$lastName",
                                email: "$email",
                                user_ref: "$referenceId",
                                callback: function(response, data) {
                                    AndroidBridge.onResult(JSON.stringify(response));
                                }
                            });
                        } catch (e) {
                            AndroidBridge.onResult(JSON.stringify({ status: "failed", message: e.message }));
                        }
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        dialog.setContentView(webView)
        dialog.show()
        
        webView.loadDataWithBaseURL("https://js.prembly.com", html, "text/html", "UTF-8", null)
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
