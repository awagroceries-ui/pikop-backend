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
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isPaymentConfirmed by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(10) }
    var isAutoDetectActive by remember { mutableStateOf(false) }

    // Manual Recovery Timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isPaymentConfirmed) "Payment Received" else "Secure Payment", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isActive = isPaymentConfirmed || countdown == 0
                    Button(
                        onClick = { onSuccess("manual_confirm") },
                        enabled = isActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaymentConfirmed) Color(0xFF008751) else MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    ) {
                        if (countdown > 0 && !isPaymentConfirmed) {
                            Text("WAIT ${countdown}s", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("I HAVE PAID", fontWeight = FontWeight.ExtraBold)
                        }
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
            // Success detection banner
            if (isPaymentConfirmed) {
                Surface(color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Payment Detected! Tap 'I HAVE PAID' if you are not redirected.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

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
                                    val currentUrl = url ?: ""
                                    val title = view?.title ?: ""
                                    
                                    if (currentUrl.contains("success") || 
                                        currentUrl.contains("callback") || 
                                        title.contains("Successful", ignoreCase = true) ||
                                        title.contains("Approved", ignoreCase = true)) {
                                        
                                        isPaymentConfirmed = true
                                        // Still try auto-detect success, but rely more on the manual button
                                        if (!isAutoDetectActive) {
                                            isAutoDetectActive = true
                                            postDelayed({ onSuccess("auto_detected") }, 3000)
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val currentUrl = request?.url?.toString() ?: ""
                                    if (currentUrl.contains("callback") || currentUrl.contains("success")) {
                                        isPaymentConfirmed = true
                                        onSuccess("url_detected")
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
