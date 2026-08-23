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
    var countdown by remember { mutableIntStateOf(20) } // Increased to 20s to prevent premature clicks

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
                    // Manual Escape Hatch: Only visible after 20 seconds OR if auto-detected
                    if (isPaymentConfirmed || countdown == 0) {
                        Button(
                            onClick = { onSuccess("manual_confirmed") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaymentConfirmed) Color(0xFF008751) else MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text("CONTINUE TO MISSION", fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        // Show timer as inactive text
                        Text(
                            text = "Verify in ${countdown}s",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
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
            // Prominent Guide Banner
            Surface(
                color = if (countdown == 0) MaterialTheme.colorScheme.errorContainer else Color.LightGray.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (countdown > 0) 
                        "Complete payment on the page below. A manual confirm button will appear shortly." 
                        else "If payment is finished but page hasn't closed, tap CONTINUE TO MISSION above.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = if (countdown == 0) FontWeight.Bold else FontWeight.Normal
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
