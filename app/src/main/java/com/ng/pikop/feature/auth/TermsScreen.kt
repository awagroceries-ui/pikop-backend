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
fun TermsScreen(
    onAccept: () -> Unit, 
    isViewer: Boolean = false,
    showFulfillerTerms: Boolean = false
) {
    var isChecked by remember { mutableStateOf(isViewer) }

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
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last Updated: August 2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                val splitInfo = if (showFulfillerTerms) {
                    " Once a delivery is matched, a 75/25 split is applied between the Fulfiller and Awa Foods & Groceries."
                } else {
                    ""
                }
                
                Text(
                    text = """
                        Welcome to Pikop, a logistics and delivery platform owned and operated by Awa Foods & Groceries, a company incorporated under the laws of the Federal Republic of Nigeria.

                        1. ACCEPTANCE OF TERMS
                        By creating an account or using the Pikop mobile application ("the App"), you agree to be bound by these Terms and Conditions. If you do not agree, please do not use our services.

                        2. ELIGIBILITY
                        You must be at least 18 years of age to use Pikop. By using the App, you represent and warrant that you have the right, authority, and capacity to enter into this agreement.

                        3. SERVICES PROVIDED
                        Pikop acts as a marketplace connecting Users (Customers) with independent Fulfillers (Drivers/Couriers) for the transportation of goods. Awa Foods & Groceries does not provide transportation services itself and is not a common carrier.

                        4. PAYMENTS AND FEES
                        All payments are processed securely via Paystack. Prices are estimated based on item size (classified via AI) and distance.$splitInfo

                        5. CANCELLATION POLICY
                        Cancellations are free while the App is searching for a driver. Once a Fulfiller is matched, a standard cancellation fee of ₦200 may apply to compensate the Fulfiller for their time and transit.

                        6. LIMITATION OF LIABILITY
                        Awa Foods & Groceries is not liable for any direct, indirect, or consequential loss arising from the use of the App or the actions of independent Fulfillers, except where required by Nigerian Law.

                        7. PRIVACY
                        Your use of Pikop is also governed by our Privacy Policy, which outlines how we handle your personal data in compliance with the Nigeria Data Protection Regulation (NDPR).

                        8. GOVERNING LAW
                        These terms are governed by and construed in accordance with the laws of the Federal Republic of Nigeria.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
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
