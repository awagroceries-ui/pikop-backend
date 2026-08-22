package com.ng.pikop.feature.order

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                    // Manual Escape Hatch if Paystack doesn't auto-redirect
                    if (isPaymentConfirmed) {
                        Button(
                            onClick = { onSuccess("manual_confirm") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("CONTINUE MISSION", fontWeight = FontWeight.ExtraBold)
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                                
                                android.util.Log.d("PikopPayment", "Finished: $currentUrl | Title: $title")

                                // AGGRESSIVE DETECTION: Domain keywords, Title keywords, or Short-links
                                if (currentUrl.contains("success") || 
                                    currentUrl.contains("callback") || 
                                    currentUrl.contains("pstk.co") ||
                                    title.contains("Successful", ignoreCase = true) ||
                                    title.contains("Approved", ignoreCase = true) ||
                                    title.contains("Verified", ignoreCase = true) ||
                                    title.contains("Confirmed", ignoreCase = true) ||
                                    title.contains("Thank you", ignoreCase = true)) {
                                    
                                    if (!isPaymentConfirmed) {
                                        isPaymentConfirmed = true
                                        android.util.Log.d("PikopPayment", "Success detected! Returning in 1.5s")
                                        postDelayed({ onSuccess("auto_detected") }, 1500)
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val currentUrl = request?.url?.toString() ?: ""
                                android.util.Log.d("PikopPayment", "Intercepting URL: $currentUrl")
                                
                                if (currentUrl.contains("callback") || 
                                    currentUrl.contains("success") || 
                                    currentUrl.contains("checkout.paystack.com")) {
                                    
                                    val reference = request?.url?.getQueryParameter("reference")
                                    if (reference != null) {
                                        isPaymentConfirmed = true
                                        onSuccess(reference)
                                        return true
                                    }
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
