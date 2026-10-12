package com.ng.pikop.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    var pushEnabled by remember { mutableStateOf(true) }
    var emailEnabled by remember { mutableStateOf(true) }
    var smsEnabled by remember { mutableStateOf(false) }
    var promoEnabled by remember { mutableStateOf(true) }
    var promoLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            promoEnabled = apiService.getMarketingConsent().marketing_consent
        } catch (ex: Exception) {}
        promoLoaded = true
    }
    
    val updatePrefs = { p: Boolean, e: Boolean, s: Boolean ->
        scope.launch {
            try {
                apiService.updateNotificationPrefs(mapOf("push" to p, "email" to e, "sms" to s))
            } catch (ex: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp)) {
            NotificationToggle("Push Notifications", pushEnabled) { 
                pushEnabled = it
                updatePrefs(it, emailEnabled, smsEnabled)
            }
            NotificationToggle("Email Updates", emailEnabled) { 
                emailEnabled = it
                updatePrefs(pushEnabled, it, smsEnabled)
            }
            NotificationToggle("SMS Alerts", smsEnabled) { 
                smsEnabled = it
                updatePrefs(pushEnabled, emailEnabled, it)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NotificationToggle("Offers & promotions (email/SMS)", promoEnabled) { wanted ->
                if (!promoLoaded) return@NotificationToggle
                promoEnabled = wanted
                scope.launch {
                    try {
                        val res = apiService.setMarketingConsent(mapOf("marketing_consent" to wanted))
                        promoEnabled = res.marketing_consent
                        android.widget.Toast.makeText(context, res.message ?: "Saved", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        promoEnabled = !wanted
                        android.widget.Toast.makeText(context, "Could not save. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            Text(
                "You receive Pikop offers and promotions by default. Turn this off anytime and we will stop. You will still get important account and order messages.",
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}

@Composable
fun NotificationToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
