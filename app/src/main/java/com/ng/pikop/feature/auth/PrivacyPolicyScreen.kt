package com.ng.pikop.feature.auth

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    var privacyHtml by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        try {
            isLoading = true
            val config = apiService.getLegalConfig()
            privacyHtml = config["privacy_html"]
        } catch (_: Exception) {
            privacyHtml = "<h2>Privacy Policy</h2><p>Could not load latest policy from server. Please check your internet connection.</p>"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = false
                                    loadDataWithBaseURL(null, privacyHtml ?: "", "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, privacyHtml ?: "", "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (privacyHtml?.contains("Could not load") == true) {
                            Button(
                                onClick = { refreshKey++ },
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Text("Retry Loading")
                            }
                        }
                    }
                }
            }
        }
    }
}
