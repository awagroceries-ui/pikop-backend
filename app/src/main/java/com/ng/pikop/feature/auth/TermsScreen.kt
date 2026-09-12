package com.ng.pikop.feature.auth

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import kotlinx.coroutines.launch

@Composable
fun TermsScreen(
    onAccept: () -> Unit, 
    isViewer: Boolean = false,
    showFulfillerTerms: Boolean = false
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    
    var isChecked by remember { mutableStateOf(isViewer) }
    var termsHtml by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val config = apiService.getLegalConfig()
            termsHtml = config["terms_html"]
        } catch (e: Exception) {
            termsHtml = "<h2>Terms & Conditions</h2><p>Could not load latest terms from server. Please check your internet connection.</p>"
        } finally {
            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = false
                                loadDataWithBaseURL(null, termsHtml ?: "", "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, termsHtml ?: "", "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                if (!isViewer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = "I have read and agree to the Terms & Conditions of Awa Foods & Groceries",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isChecked
                ) {
                    Text(if (isViewer) "Close" else "Accept and Continue")
                }
            }
        }
    }
}
