package com.ng.pikop.feature.order

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

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
    var statusMessage by remember { mutableStateOf("Secure Payment") }
    
    // Timeout Handling
    var timeoutTicks by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (!isPaymentConfirmed) {
            delay(1000)
            timeoutTicks++
            if (timeoutTicks >= 120) { // 2 minute hard limit
                statusMessage = "Verification taking too long..."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isPaymentConfirmed) "Payment Confirmed" else statusMessage, 
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
            if (isPaymentConfirmed) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF008751))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Finalizing mission...", fontWeight = FontWeight.Bold)
                        Text("Please do not close this screen.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    @SuppressLint("SetJavaScriptEnabled")
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // Auto-scroll or focus might be needed for some bank pages
                                    }

                                    // Legacy Overload
                                    override fun shouldOverrideUrlLoading(view: WebView?, urlString: String?): Boolean {
                                        return handleRedirection(urlString ?: "")
                                    }

                                    // Modern Overload
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        return handleRedirection(request?.url?.toString() ?: "")
                                    }

                                    private fun handleRedirection(currentUrl: String): Boolean {
                                        android.util.Log.d("PikopPayment", "Navigating to: $currentUrl")
                                        
                                        // 1. Strict Scheme Matching (pikop://payment/success)
                                        if (currentUrl.startsWith("pikop://payment/success") || currentUrl.startsWith("intent://payment/success")) {
                                            android.util.Log.d("PikopPayment", "SUCCESS: Scheme detected.")
                                            
                                            // Extract reference from query params
                                            val uri = Uri.parse(currentUrl.replace("intent://", "pikop://"))
                                            val reference = uri.getQueryParameter("reference") ?: 
                                                           uri.getQueryParameter("trxref") ?: "detected_callback"
                                            
                                            isPaymentConfirmed = true
                                            onSuccess(reference) { /* Done */ }
                                            return true
                                        }

                                        // 2. Keyword fallback for resilient interception
                                        if (currentUrl.contains("payments/webhook") || (currentUrl.contains("success") && currentUrl.contains("reference="))) {
                                            if (!isPaymentConfirmed) {
                                                android.util.Log.d("PikopPayment", "SUCCESS: Success URL pattern matched.")
                                                val uri = Uri.parse(currentUrl)
                                                val reference = uri.getQueryParameter("reference") ?: "detected_url"
                                                
                                                isPaymentConfirmed = true
                                                onSuccess(reference) { }
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
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
