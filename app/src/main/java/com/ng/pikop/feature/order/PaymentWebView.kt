package com.ng.pikop.feature.order

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentWebView(
    url: String,
    onSuccess: (String, onResult: (Boolean) -> Unit) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isPaymentConfirmed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isPaymentConfirmed) "Payment Success" else "Secure Payment", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isPaymentConfirmed) Color(0xFF008751) else MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val currentUrl = request?.url?.toString() ?: ""
                                    android.util.Log.d("PikopPayment", "Navigating to: $currentUrl")
                                    
                                    // Handle Intent/Pikop schemes for return-to-app
                                    if (currentUrl.startsWith("intent://") || currentUrl.startsWith("pikop://")) {
                                        android.util.Log.d("PikopPayment", "SUCCESS: Intercepted return signal.")
                                        
                                        // Handle manual intent resolution for intent:// schemes
                                        if (currentUrl.startsWith("intent://")) {
                                            try {
                                                val intent = android.content.Intent.parseUri(currentUrl, android.content.Intent.URI_INTENT_SCHEME)
                                                context.startActivity(intent)
                                                isPaymentConfirmed = true
                                                onSuccess("intent_scheme") { }
                                                return true
                                            } catch (e: Exception) {
                                                android.util.Log.e("PikopPayment", "Failed to parse intent: ${e.message}")
                                            }
                                        }

                                        isPaymentConfirmed = true
                                        onSuccess("direct_link") { /* Auto-close handled in main */ }
                                        return true
                                    }

                                    // Intercept Webhook URL or Success keywords
                                    if (currentUrl.contains("callback") || currentUrl.contains("success") || currentUrl.contains("payments/webhook")) {
                                        if (!isPaymentConfirmed) {
                                            android.util.Log.d("PikopPayment", "SUCCESS: Callback/Webhook URL detected.")
                                            isPaymentConfirmed = true
                                            onSuccess("url_match") { /* Auto-close handled in main */ }
                                        }
                                        return true
                                    }

                                    if (currentUrl.contains("cancel")) {
                                        onCancel()
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
