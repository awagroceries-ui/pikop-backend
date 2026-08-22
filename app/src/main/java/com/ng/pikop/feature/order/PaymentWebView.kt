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
    var timerSeconds by remember { mutableIntStateOf(10) }

    // Manual Recovery Timer: Show button after 10 seconds
    LaunchedEffect(Unit) {
        while (timerSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            timerSeconds--
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
                    // Manual Escape Hatch: Always show if confirmed or after 10s
                    if (isPaymentConfirmed || timerSeconds == 0) {
                        Button(
                            onClick = { onSuccess("manual_confirmed") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaymentConfirmed) Color(0xFF008751) else MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("CONTINUE TO MISSION", fontWeight = FontWeight.ExtraBold)
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
            if (timerSeconds > 0 && !isPaymentConfirmed) {
                Surface(color = Color.LightGray.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "The 'CONTINUE' button will appear in $timerSeconds seconds.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                    
                                    // Aggressive detection
                                    if (currentUrl.contains("success") || 
                                        currentUrl.contains("callback") || 
                                        title.contains("Successful", ignoreCase = true) ||
                                        title.contains("Approved", ignoreCase = true)) {
                                        
                                        isPaymentConfirmed = true
                                        android.util.Log.d("PikopPayment", "Success detected! Auto-redirecting in 2s")
                                        postDelayed({ onSuccess("auto_detected") }, 2000)
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val currentUrl = request?.url?.toString() ?: ""
                                    if (currentUrl.contains("callback") || currentUrl.contains("success")) {
                                        val reference = request?.url?.getQueryParameter("reference") ?: "manual"
                                        onSuccess(reference)
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
