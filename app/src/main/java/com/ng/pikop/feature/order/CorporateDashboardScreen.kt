package com.ng.pikop.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
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
import com.ng.pikop.core.network.CorporateAccount
import com.ng.pikop.core.network.CorporateStaff
import com.ng.pikop.core.network.CreateCorporateRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorporateDashboardScreen(onBack: () -> Unit) {
    var accounts by remember { mutableStateOf<List<CorporateAccount>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<CorporateAccount?>(null) }
    var staffList by remember { mutableStateOf<List<CorporateStaff>>(emptyList()) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    // Fetch Accounts
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            accounts = apiService.getMyCorporateAccounts()
        } catch (_: Exception) {}
        isLoading = false
    }

    // Fetch Staff when account selected
    LaunchedEffect(selectedAccount) {
        selectedAccount?.id?.let { id ->
            try {
                staffList = apiService.getCorporateStaff(id)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corporate Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Account")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }

            if (accounts.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No corporate accounts found", color = Color.Gray)
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Setup Business Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { acc ->
                        CorporateAccountItem(
                            account = acc,
                            isSelected = selectedAccount?.id == acc.id,
                            onClick = { selectedAccount = if (selectedAccount?.id == acc.id) null else acc },
                            onAddStaff = { showAddStaffDialog = true }
                        )
                        
                        if (selectedAccount?.id == acc.id) {
                            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                                Text(
                                    "Staff Members", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                staffList.forEach { staff ->
                                    Text(
                                        "${staff.full_name} (${staff.role})", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                if (staffList.isEmpty()) {
                                    Text("No other staff added", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCorporateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, email, type ->
                scope.launch {
                    try {
                        apiService.createCorporateAccount(CreateCorporateRequest(name, email, type))
                        accounts = apiService.getMyCorporateAccounts()
                    } catch (_: Exception) {}
                    showCreateDialog = false
                }
            }
        )
    }

    if (showAddStaffDialog && selectedAccount != null) {
        AddStaffDialog(
            onDismiss = { showAddStaffDialog = false },
            onConfirm = { email, role ->
                scope.launch {
                    try {
                        apiService.addStaffToCorporate(selectedAccount!!.id!!, mapOf("email" to email, "role" to role))
                        staffList = apiService.getCorporateStaff(selectedAccount!!.id!!)
                    } catch (_: Exception) {}
                    showAddStaffDialog = false
                }
            }
        )
    }
}

@Composable
fun CorporateAccountItem(
    account: CorporateAccount,
    isSelected: Boolean,
    onClick: () -> Unit,
    onAddStaff: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.company_name ?: "Unknown", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Billing: ${account.billing_type}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (isSelected) {
                IconButton(onClick = onAddStaff) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                account.status ?: "PENDING",
                style = MaterialTheme.typography.labelSmall,
                color = if (account.status == "active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CreateCorporateDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("prepaid_wallet") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Business Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Billing Email") })
                Text("Billing Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "prepaid_wallet", onClick = { type = "prepaid_wallet" }, label = { Text("Prepaid") })
                    FilterChip(selected = type == "direct_debit", onClick = { type = "direct_debit" }, label = { Text("Direct Debit") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, type) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STAFF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Staff Email") })
                Text("Role", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = role == "STAFF", onClick = { role = "STAFF" }, label = { Text("Staff") })
                    FilterChip(selected = role == "ADMIN", onClick = { role = "ADMIN" }, label = { Text("Admin") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, role) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Invite", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
