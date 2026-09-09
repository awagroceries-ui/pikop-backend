package com.ng.pikop.feature.fulfiller

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import android.provider.MediaStore
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class TakeSelfieContract : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return super.createIntent(context, input).apply {
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycUploadScreen(
    userEmail: String,
    onBack: () -> Unit,
    viewModel: KycViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLaunching by viewModel.isLaunching.collectAsStateWithLifecycle()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var selectedClass by rememberSaveable { mutableStateOf<String?>(null) }
    var isSavingStep by rememberSaveable { mutableStateOf(false) }

    // Personal Details State
    var dob by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var homeAddress by rememberSaveable { mutableStateOf("") }

    // Photo State
    var profilePhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val profilePhotoUri = remember(profilePhotoUriString) { 
        profilePhotoUriString?.let { Uri.parse(it) } 
    }
    
    val internalPhotoFile = remember { File(context.filesDir, "profile_verified.jpg") }
    val captureFile = remember { File(context.externalCacheDir ?: context.cacheDir, "profile_capture.jpg") }
    val captureUri = remember { 
        FileProvider.getUriForFile(context, "${context.packageName}.provider", captureFile) 
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dojahLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refreshProfile() }

    val cameraLauncher = rememberLauncherForActivityResult(TakeSelfieContract()) { success ->
        if (success) {
            try {
                captureFile.inputStream().use { input ->
                    internalPhotoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                profilePhotoUriString = Uri.fromFile(internalPhotoFile).toString()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            if (currentStep == 1) cameraLauncher.launch(captureUri)
            else if (currentStep == 2) refreshKey++
        }
    }

    LaunchedEffect(refreshKey) { viewModel.refreshProfile() }

    LaunchedEffect(profile) {
        if (isSavingStep) return@LaunchedEffect
        val res = profile ?: return@LaunchedEffect
        
        // SYNC LOGIC: Only update step if it's a major state change (Verified/Pending)
        // or if we are not currently manually advancing.
        if (res.kyc_status == "PENDING_REVIEW" || res.kyc_status == "VERIFIED") {
            currentStep = 6
        } else if (res.home_address == null) {
            currentStep = 0
        } else if (res.profile_photo_url == null) {
            currentStep = 2
        } else if (res.kyc_verification_status != "approved") {
            if (3 > currentStep) currentStep = 3
        } else if (res.primary_class?.lowercase() == "driver" && res.registration_number == null) {
            if (4 > currentStep) currentStep = 4
        } else if (res.account_number == null) {
            if (5 > currentStep) currentStep = 5
        } else {
            currentStep = 6
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Activation") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepIndicator(currentStep)
            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                0 -> PersonalDetailsStep(
                    dob = dob,
                    gender = gender,
                    address = homeAddress,
                    onDobChange = { dob = it },
                    onGenderChange = { gender = it },
                    onAddressChange = { homeAddress = it }
                )
                1 -> FulfillerTypeSelectionScreen(
                    selectedClass = selectedClass,
                    onClassSelected = { cls -> selectedClass = cls }
                )
                2 -> ProfilePhotoStep(profilePhotoUri) { 
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        if (captureFile.exists()) captureFile.delete()
                        cameraLauncher.launch(captureUri)
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                }
                3 -> IdentityStep(
                    status = profile?.kyc_verification_status ?: "not_started", 
                    role = profile?.primary_class ?: "agent",
                    isLaunching = isLaunching,
                    onVerifyClick = {
                        val email = if (userEmail.isNotBlank()) userEmail else "verify@pikop.ng"
                        // Switch to startVerification for official Inline JS Widget (WebView Dialog)
                        viewModel.startVerification(context, email) {
                            viewModel.refreshProfile()
                        }
                    },
                    onPermissionRequest = { 
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) 
                    },
                    onRefresh = { viewModel.refreshProfile() }
                )
                4 -> VehicleStep(tokenManager = remember { TokenManager(context) }, onComplete = { currentStep = 5 })
                5 -> BankStep(tokenManager = remember { TokenManager(context) }, onComplete = { currentStep = 6 })
                6 -> SubmissionStep(
                    isLoading = isLoading,
                    status = profile?.kyc_status ?: "NOT_SUBMITTED",
                    onComplete = {
                        viewModel.submitApplication(
                            onSuccess = { 
                                Toast.makeText(context, "Application submitted for review!", Toast.LENGTH_SHORT).show()
                                viewModel.refreshProfile()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    onReturnHome = onBack
                )
            }

            if (currentStep < 6) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch {
                            val tokenManager = TokenManager(context)
                            val api = ApiService.create(tokenManager)
                            isSavingStep = true
                            try {
                                when (currentStep) {
                                    0 -> {
                                        if (dob.isNotBlank() && gender.isNotBlank() && homeAddress.isNotBlank()) {
                                            currentStep = 1
                                            api.updateFulfillerProfile(ProfileUpdateRequest(
                                                date_of_birth = dob,
                                                gender = gender,
                                                home_address = homeAddress
                                            ))
                                            viewModel.refreshProfile()
                                        }
                                    }
                                    1 -> {
                                        if (selectedClass != null) {
                                            currentStep = 2 // Optimistic Advance
                                            api.updateFulfillerProfile(ProfileUpdateRequest(primary_class = selectedClass))
                                            viewModel.refreshProfile()
                                        }
                                    }
                                    2 -> {
                                        if (internalPhotoFile.exists()) {
                                            currentStep = 3 // Optimistic Advance
                                            val compressed = ImageUtils.compressFile(context, internalPhotoFile)
                                            val body = MultipartBody.Part.createFormData("file", "profile.jpg", compressed.asRequestBody("image/*".toMediaTypeOrNull()))
                                            api.uploadProfilePhoto(body)
                                            viewModel.refreshProfile()
                                        }
                                    }
                                    3 -> {
                                        if (profile?.kyc_verification_status == "approved") {
                                            if (profile?.primary_class?.lowercase() == "driver") currentStep = 4
                                            else currentStep = 5
                                        } else {
                                            viewModel.refreshProfile()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
                            } finally {
                                isSavingStep = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading && !isSavingStep && (
                        (currentStep == 0 && dob.isNotBlank() && gender.isNotBlank() && homeAddress.isNotBlank()) ||
                        (currentStep == 1 && selectedClass != null) ||
                        (currentStep == 2 && profilePhotoUri != null) ||
                        (currentStep == 3)
                    )
                ) {
                    if (isLoading || isSavingStep) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else if (currentStep == 3) Text("Verify & Refresh")
                    else Text("CONTINUE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StepIndicator(current: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        (0..5).forEach { i ->
            Box(modifier = Modifier.size(10.dp).padding(2.dp)) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (i <= current) MaterialTheme.colorScheme.primary else Color.LightGray,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsStep(
    dob: String,
    gender: String,
    address: String,
    onDobChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAddressChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Personal Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("We need a few more details to activate your account.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        OutlinedTextField(
            value = dob,
            onValueChange = onDobChange,
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Cake, null) },
            placeholder = { Text("1990-01-01") }
        )

        var genderExpanded by remember { mutableStateOf(false) }
        val genders = listOf("Male", "Female", "Other")
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = it }
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                leadingIcon = { Icon(Icons.Default.Wc, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genders.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g) },
                        onClick = {
                            onGenderChange(g)
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Home Address") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Home, null) },
            minLines = 2
        )
    }
}

@Composable
fun ProfilePhotoStep(uri: Uri?, onCapture: () -> Unit) {
    Text("Face Capture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Live capture required for security.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(modifier = Modifier.height(32.dp))
    Card(onClick = onCapture, modifier = Modifier.size(220.dp), shape = androidx.compose.foundation.shape.CircleShape, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) Icon(Icons.Default.Check, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            else Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
        }
    }
}

@Composable
fun IdentityStep(status: String, role: String, isLaunching: Boolean, onVerifyClick: () -> Unit, onPermissionRequest: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val statusColor = when (status.lowercase()) {
        "approved" -> Color(0xFF4CAF50)
        "declined" -> MaterialTheme.colorScheme.error
        "pending", "needs_review" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Identity Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), onClick = {
            if (isLaunching || status == "approved") return@Card
            val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!hasCamera || !hasAudio) onPermissionRequest() else onVerifyClick()
        }) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isLaunching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Icon(imageVector = if (status == "approved") Icons.Default.CheckCircle else Icons.Default.Fingerprint, contentDescription = null, tint = if (status == "approved") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (status == "approved") "Identity Verified" else "Start Verification", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(status.uppercase(), style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankStep(tokenManager: TokenManager, onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiService.create(tokenManager) }
    
    var banks by remember { mutableStateOf<List<Bank>>(emptyList()) }
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    
    var isVerifying by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isBankListLoading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = api.getBanks()
            banks = response.data
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load banks", Toast.LENGTH_SHORT).show()
        } finally {
            isBankListLoading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Payout Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Where should we send your earnings?", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        if (isBankListLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedBank?.name ?: "Select Bank",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bank Name") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    banks.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.name) },
                            onClick = {
                                selectedBank = bank
                                expanded = false
                                accountName = "" // Reset verified name
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = accountNumber,
            onValueChange = { 
                if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                    accountNumber = it
                    accountName = "" // Reset verified name
                }
            },
            label = { Text("Account Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        if (accountName.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Verified Name", style = MaterialTheme.typography.labelSmall)
                        Text(accountName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (accountNumber.length == 10 && selectedBank != null && accountName.isBlank()) {
            Button(
                onClick = {
                    scope.launch {
                        isVerifying = true
                        try {
                            val res = api.resolveAccount(mapOf(
                                "account_number" to accountNumber,
                                "bank_code" to selectedBank!!.code
                            ))
                            if (res.status && res.data != null) {
                                accountName = res.data.account_name
                            } else {
                                Toast.makeText(context, "Could not verify account", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally { isVerifying = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVerifying
            ) {
                if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Verify Account")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    try {
                        api.updateFulfillerProfile(ProfileUpdateRequest(
                            bank_name = selectedBank?.name,
                            account_number = accountNumber,
                            bank_code = selectedBank?.code,
                            account_name = accountName
                        ))
                        onComplete()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally { isSaving = false }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isSaving && accountName.isNotBlank()
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("SAVE & CONTINUE")
        }
    }
}

@Composable
fun VehicleStep(tokenManager: TokenManager, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiService.create(tokenManager) }
    var regNum by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Vehicle Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = regNum, onValueChange = { regNum = it }, label = { Text("Registration Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = make, onValueChange = { make = it }, label = { Text("Vehicle Make (e.g. Honda)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    try {
                        api.updateFulfillerProfile(ProfileUpdateRequest(vehicle_details = VehicleDetails(regNum, make, null, null)))
                        onComplete()
                    } catch (_: Exception) {}
                    isSaving = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isSaving && regNum.isNotBlank() && make.isNotBlank()
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Save & Finalize")
        }
    }
}

@Composable
fun SubmissionStep(isLoading: Boolean, status: String, onComplete: () -> Unit, onReturnHome: () -> Unit) {
    val isSubmitted = status == "PENDING_REVIEW" || status == "VERIFIED"
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSubmitted) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Verification Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Please Return to Home Screen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your identity verification and profile details have been successfully completed. Please return to the home screen and wait for final admin approval.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReturnHome,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RETURN TO HOME", fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(Icons.Default.LibraryAddCheck, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Final Review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Your profile is complete. Tap below to submit for activation.", textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("SUBMIT APPLICATION", fontWeight = FontWeight.Bold)
            }
        }
    }
}
