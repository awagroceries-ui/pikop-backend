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
import javax.inject.Named

/**
 * Prembly (Identitypass) KYC implementation.
 * Uses the official Inline JS Widget for maximum reliability.
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
        activity.runOnUiThread {
            showInlineWidget(activity, email, referenceId, onSuccess, onError, onClose)
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
    private fun showInlineWidget(
        activity: Activity,
        email: String,
        referenceId: String,
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
            settings.databaseEnabled = true
            
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onSuccess(response: String) {
                    android.util.Log.d("PremblyKYC", "Success: ${'$'}response")
                    activity.runOnUiThread {
                        onSuccess(response)
                        dialog.dismiss()
                    }
                }
                
                @JavascriptInterface
                fun onClose() {
                    android.util.Log.d("PremblyKYC", "Closed")
                    activity.runOnUiThread {
                        onClose()
                        dialog.dismiss()
                    }
                }

                @JavascriptInterface
                fun onError(error: String) {
                    android.util.Log.e("PremblyKYC", "Error: ${'$'}error")
                    activity.runOnUiThread {
                        onError(error)
                        dialog.dismiss()
                    }
                }
            }, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        android.util.Log.e("PremblyKYC", "WebView Error: ${'$'}{error?.description}")
                    }
                }
            }
        }

        // Inline Widget HTML Shell
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Verification | Pikop</title>
                <style>
                    body { margin: 0; padding: 0; background-color: #ffffff; display: flex; flex-direction: column; height: 100vh; font-family: -apple-system, sans-serif; }
                    #prembly-widget-container { flex: 1; width: 100%; height: 100%; }
                </style>
            </head>
            <body>
                <div id="prembly-widget-container"></div>
                <script src="https://js.prembly.com/v1/inline/widget.js"></script>
                <script>
                    try {
                        var widget = new PremblyWidget({
                            publicKey: "$publicKey",
                            configId: "$configId",
                            userRef: "$referenceId",
                            email: "$email",
                            onSuccess: function(response) { 
                                AndroidBridge.onSuccess(JSON.stringify(response)); 
                            },
                            onClose: function() { 
                                AndroidBridge.onClose(); 
                            },
                            onError: function(err) {
                                AndroidBridge.onError(err ? err.message || JSON.stringify(err) : "Unknown Error");
                            }
                        });
                        widget.launch();
                    } catch (e) {
                        AndroidBridge.onError(e.message);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        dialog.setContentView(webView)
        dialog.show()
        
        // Load the local HTML shell with the remote script base
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
