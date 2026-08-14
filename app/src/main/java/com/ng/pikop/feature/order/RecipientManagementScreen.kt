package com.ng.pikop.feature.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.RecipientRequest
import com.ng.pikop.core.network.SavedRecipient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    var recipients by remember { mutableStateOf<List<SavedRecipient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val fetchRecipients = {
        scope.launch {
            isLoading = true
            try {
                recipients = apiService.getSavedRecipients()
            } catch (e: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { fetchRecipients() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Recipients") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipient")
            }
        }
    ) { padding ->
        if (isLoading && recipients.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(recipients) { recipient ->
                    RecipientItem(recipient, onDelete = {
                        scope.launch {
                            try {
                                apiService.deleteRecipient(recipient.id!!)
                                fetchRecipients()
                            } catch (e: Exception) {}
                        }
                    })
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecipientDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, label ->
                scope.launch {
                    try {
                        apiService.addRecipient(RecipientRequest(name, phone, label))
                        showAddDialog = false
                        fetchRecipients()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun RecipientItem(recipient: SavedRecipient, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recipient.name ?: "", fontWeight = FontWeight.Bold)
                Text(recipient.phone ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                recipient.label?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}

@Composable
fun AddRecipientDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recipient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") })
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. Office)") })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, phone, label) }, enabled = name.isNotBlank() && phone.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
