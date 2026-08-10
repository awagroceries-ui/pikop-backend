package com.ng.pikop.feature.fulfiller

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import me.didit.sdk.DiditSdk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycUploadScreen(onBack: () -> Unit) {
    var profile by remember { mutableStateOf<FulfillerProfileResponse?>(null) }
    var currentStep by remember { mutableStateOf(1) }
    
    // Step 2: Profile Photo
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Step 3: Branching Data
    var mobilityType by remember { mutableStateOf("on_foot") }
    var regNumber by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    // Camera Setup for Profile Photo
    val profilePhotoFile = remember { File(context.cacheDir, "profile_live.jpg") }
    val profilePhotoProviderUri = remember { 
        FileProvider.getUriForFile(context, "${context.packageName}.provider", profilePhotoFile) 
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) profilePhotoUri = profilePhotoProviderUri
    }

    // Manual Doc Setup
    var docUri by remember { mutableStateOf<Uri?>(null) }
    var docType by remember { mutableStateOf("DRIVING_LICENSE") }
    val docLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { docUri = it }

    LaunchedEffect(Unit) {
        try {
            profile = apiService.getFulfillerProfile()
            if (profile?.profile_photo_url != null) currentStep = 2 // Skip to identity if photo exists
            if (profile?.didit_verification_status == "approved") currentStep = 3
        } catch (e: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fulfiller Onboarding") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepIndicator(currentStep)
            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                1 -> ProfilePhotoStep(profilePhotoUri) { cameraLauncher.launch(profilePhotoProviderUri) }
                2 -> IdentityStep(profile?.didit_verification_status ?: "not_started", apiService) { profile = it }
                3 -> BranchingStep(
                        userClass = profile?.primary_class ?: "agent",
                        mobilityType = mobilityType,
                        onMobilityChange = { mobilityType = it },
                        regNumber = regNumber,
                        onRegChange = { regNumber = it },
                        make = make,
                        onMakeChange = { make = it },
                        model = model,
                        onModelChange = { model = it }
                    )
                4 -> DocumentStep(docType, { docType = it }, docUri) { docLauncher.launch("image/*") }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            when (currentStep) {
                                1 -> {
                                    val requestFile = profilePhotoFile.asRequestBody("image/*".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("photo", profilePhotoFile.name, requestFile)
                                    apiService.uploadProfilePhoto(body)
                                    currentStep = 2
                                }
                                2 -> if (profile?.didit_verification_status == "approved") currentStep = 3
                                3 -> {
                                    apiService.updateFulfillerProfile(ProfileUpdateRequest(
                                        mobility_type = if (profile?.primary_class == "agent") mobilityType else null,
                                        vehicle_details = if (profile?.primary_class != "agent") VehicleDetails(regNumber, make, model, "Black") else null
                                    ))
                                    currentStep = if (profile?.primary_class == "agent") 5 else 4
                                }
                                4 -> {
                                    val file = getFileFromUri(context, docUri!!)
                                    val body = MultipartBody.Part.createFormData("document", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                                    apiService.uploadKYC(docType.toRequestBody("text/plain".toMediaTypeOrNull()), body)
                                    currentStep = 5
                                }
                                5 -> {
                                    apiService.submitApplication()
                                    Toast.makeText(context, "Application Submitted!", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isStepValid(currentStep, profilePhotoUri, profile, mobilityType, regNumber, docUri)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(if (currentStep == 5) "Finalize & Submit" else "Continue")
            }
        }
    }
}

@Composable
fun StepIndicator(current: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        (1..5).forEach { i ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(2.dp)
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .let {
                        if (i == current) it.padding(0.dp) else it
                    }
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (i <= current) MaterialTheme.colorScheme.primary else Color.LightGray,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
            if (i < 5) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun ProfilePhotoStep(uri: Uri?, onCapture: () -> Unit) {
    Text("Face Capture", style = MaterialTheme.typography.titleLarge)
    Text("Look straight into the camera. Live capture required.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(modifier = Modifier.height(24.dp))
    
    Card(onClick = onCapture, modifier = Modifier.size(200.dp), shape = androidx.compose.foundation.shape.CircleShape) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) {
                // In real app, use Coil here
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun IdentityStep(status: String, api: ApiService, onUpdate: (FulfillerProfileResponse) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Text("Identity Verification", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(24.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (status == "not_started" || status == "declined") {
                scope.launch {
                    try {
                        val session = api.startDiditVerification()
                        DiditSdk.startVerification(token = session.session_token) { _ -> }
                    } catch (e: Exception) {}
                }
            }
        }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Hosted Identity Flow", style = MaterialTheme.typography.titleSmall)
                Text(status.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun BranchingStep(
    userClass: String,
    mobilityType: String, onMobilityChange: (String) -> Unit,
    regNumber: String, onRegChange: (String) -> Unit,
    make: String, onMakeChange: (String) -> Unit,
    model: String, onModelChange: (String) -> Unit
) {
    if (userClass == "agent") {
        Text("Mobility Selection", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("on_foot", "public_transit", "bicycle").forEach { type ->
                FilterChip(
                    selected = mobilityType == type,
                    onClick = { onMobilityChange(type) },
                    label = { Text(type.replace('_', ' ').uppercase()) }
                )
            }
        }
    } else {
        Text("Vehicle Details", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = regNumber, onValueChange = onRegChange, label = { Text("Plate Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = make, onValueChange = onMakeChange, label = { Text("Vehicle Make") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = model, onValueChange = onModelChange, label = { Text("Vehicle Model") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DocumentStep(type: String, onTypeChange: (String) -> Unit, uri: Uri?, onPick: () -> Unit) {
    Text("Operational Documents", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = type == "DRIVING_LICENSE", onClick = { onTypeChange("DRIVING_LICENSE") }, label = { Text("License") })
        FilterChip(selected = type == "VEHICLE_INSURANCE", onClick = { onTypeChange("VEHICLE_INSURANCE") }, label = { Text("Insurance") })
    }
    Spacer(modifier = Modifier.height(16.dp))
    Card(onClick = onPick, modifier = Modifier.fillMaxWidth().height(120.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) Text("Doc Selected ✅") else Icon(Icons.Default.UploadFile, null)
        }
    }
}

private fun isStepValid(step: Int, photo: Uri?, profile: FulfillerProfileResponse?, mobility: String, reg: String, doc: Uri?): Boolean {
    return when (step) {
        1 -> photo != null
        2 -> profile?.didit_verification_status == "approved"
        3 -> if (profile?.primary_class == "agent") true else reg.isNotBlank()
        4 -> doc != null
        else -> true
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "kyc_upload_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}
