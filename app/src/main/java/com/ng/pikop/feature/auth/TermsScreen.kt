package com.ng.pikop.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TermsScreen(onAccept: () -> Unit) {
    var isChecked by remember { mutableStateOf(false) }

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = """
                        Welcome to Pikop. By using our service, you agree to the following terms:
                        
                        1. Service Usage: You agree to use Pikop only for lawful purposes.
                        2. Account Security: You are responsible for maintaining the confidentiality of your account.
                        3. Privacy: Your data is handled according to our privacy policy.
                        4. Delivery Policy: We aim for timely deliveries but are not liable for external delays.
                        5. Payment: All transactions are processed securely via Paystack.
                        
                        (This is a simplified version of the Terms and Conditions for development purposes.)
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it }
                    )
                    Text(
                        text = "I have read and agree to the Terms & Conditions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isChecked
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
