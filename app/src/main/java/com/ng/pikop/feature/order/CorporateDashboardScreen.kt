package com.ng.pikop.feature.order

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorporateDashboardScreen(onBack: () -> Unit) {
    var dashboardData by remember { mutableStateOf<CorporateDashboardData?>(null) }
    var staffList by remember { mutableStateOf<List<CorporateStaff>>(emptyList()) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    fun refreshData() {
        scope.launch {
            isLoading = true
            try {
                val dash = apiService.getCorporateDashboard()
                dashboardData = dash.data
                
                val staff = apiService.getCorporateStaff()
                staffList = staff.data
            } catch (e: Exception) {
                // If not setup, dashboardData remains null
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corporate Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }

            if (dashboardData == null && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Business Account Not Activated", fontWeight = FontWeight.Bold)
                        Text("Verify your company details to enable centralized billing for your team.", textAlign = TextAlign.Center, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Complete Setup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (dashboardData != null) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        // Stats Card
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Monthly Spend Overview", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        Text("₦${"%,.0f".format(dashboardData?.wallet?.balance)}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Active Staff", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        Text("${dashboardData?.stats?.active_staff}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Authorized Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showAddStaffDialog = true }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Member")
                            }
                        }
                    }

                    if (staffList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No staff members added yet.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(staffList) { staff ->
                            StaffMemberItem(staff)
                        }
                    }

                    item {
                        Text("Top Spenders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(dashboardData?.top_users ?: emptyList()) { user ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(user.full_name, fontWeight = FontWeight.SemiBold)
                                Text("₦${"%,.0f".format(user.spend)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
            onConfirm = { name, email, cac, addr ->
                scope.launch {
                    try {
                        apiService.setupCorporateProfile(CreateCorporateRequest(
                            company_name = name,
                            billing_email = email,
                            cac_number = cac,
                            business_address = addr
                        ))
                        refreshData()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Setup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    showCreateDialog = false
                }
            }
        )
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            onDismiss = { showAddStaffDialog = false },
            onConfirm = { email, role, daily, monthly ->
                scope.launch {
                    try {
                        apiService.addStaffMember(mapOf(
                            "email" to email,
                            "role" to role,
                            "daily_limit" to daily,
                            "monthly_limit" to monthly
                        ))
                        refreshData()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to add staff: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    showAddStaffDialog = false
                }
            }
        )
    }
}

@Composable
fun StaffMemberItem(staff: CorporateStaff) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(staff.full_name ?: "Unknown", fontWeight = FontWeight.Bold)
                    Text(staff.email ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Badge { Text(staff.role ?: "STAFF") }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Daily Limit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(if (staff.daily_spend_limit > 0) "₦${"%,.0f".format(staff.daily_spend_limit)}" else "Unlimited", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Monthly Limit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(if (staff.monthly_spend_limit > 0) "₦${"%,.0f".format(staff.monthly_spend_limit)}" else "Unlimited", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateCorporateDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var cac by remember { mutableStateOf("") }
    var addr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Business Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Billing Email") })
                OutlinedTextField(value = cac, onValueChange = { cac = it }, label = { Text("CAC Number") })
                OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Business Address") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, cac, addr) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit Application", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STAFF") }
    var dailyLimit by remember { mutableStateOf("0") }
    var monthlyLimit by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Staff Email") })
                
                Text("Role", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = role == "STAFF", onClick = { role = "STAFF" }, label = { Text("Staff") })
                    FilterChip(selected = role == "ADMIN", onClick = { role = "ADMIN" }, label = { Text("Admin") })
                }

                OutlinedTextField(
                    value = dailyLimit, 
                    onValueChange = { dailyLimit = it }, 
                    label = { Text("Daily Spend Limit (₦)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = monthlyLimit, 
                    onValueChange = { monthlyLimit = it }, 
                    label = { Text("Monthly Spend Limit (₦)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Text("Set to 0 for unlimited.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, role, dailyLimit, monthlyLimit) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Authorize Member", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
