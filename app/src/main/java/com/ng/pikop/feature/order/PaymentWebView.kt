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
    var countdown by remember { mutableIntStateOf(10) } 
    var isProcessing by remember { mutableStateOf(false) }

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
                        if (isPaymentConfirmed) "Success!" else "Secure Payment", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Manual Escape Hatch: Active after countdown or success detection
                    if (isPaymentConfirmed || countdown == 0) {
                        Button(
                            onClick = { 
                                isProcessing = true
                                onSuccess("manual_confirm") { success ->
                                    if (!success) isProcessing = false
                                } 
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaymentConfirmed) Color(0xFF008751) else MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("CONTINUE MISSION", fontWeight = FontWeight.ExtraBold)
                            }
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
            // Guide Banner
            Surface(
                color = if (isPaymentConfirmed) Color(0xFFE8F5E9) else Color.LightGray.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPaymentConfirmed) "Success! Tap CONTINUE MISSION above to proceed." 
                           else if (countdown > 0) "Complete payment on the page below. Continue button ready in ${countdown}s." 
                           else "If payment is finished, tap CONTINUE MISSION above.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (isPaymentConfirmed) Color(0xFF2E7D32) else Color.Unspecified
                )
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
                                    
                                    // Aggressive detection
                                    if (currentUrl.contains("success") || 
                                        currentUrl.contains("callback") || 
                                        title.contains("Successful", ignoreCase = true) ||
                                        title.contains("Approved", ignoreCase = true)) {
                                        
                                        isPaymentConfirmed = true
                                        android.util.Log.d("PikopPayment", "Success detected! Page ready for return.")
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val currentUrl = request?.url?.toString() ?: ""
                                    if (currentUrl.contains("callback") || currentUrl.contains("success")) {
                                        isPaymentConfirmed = true
                                        onSuccess("url_detected") { /* Auto return */ }
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
