package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var selectedRating by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate the Customer") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("How was your experience with this delivery?")
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { selectedRating = star }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= selectedRating) Color(0xFFFFB300) else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedRating) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}
