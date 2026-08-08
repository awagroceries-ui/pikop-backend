package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPikopScreen(
    onBack: () -> Unit,
    onViewTerms: (Boolean) -> Unit, // Passes isFulfiller
    onViewPrivacy: () -> Unit,
    isFulfiller: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Pikop") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(150.dp)
            )
            
            Text(
                text = "Pikop",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Version 1.0.0 (Alpha)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "A product of Awa Foods & Groceries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pikop is a logistics platform built to simplify deliveries across Nigeria. We connect users with professional fulfillers to move items safely and efficiently.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legal Links
            OutlinedButton(
                onClick = { onViewTerms(isFulfiller) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terms & Conditions")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onViewPrivacy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Policy")
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "© 2026 Awa Foods & Groceries",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = "Made with ❤️ in Nigeria",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
