package com.ng.pikop.feature.fulfiller

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import me.didit.sdk.DiditSdk
import android.content.Intent
import android.provider.MediaStore
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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
fun KycUploadScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    
    var profile by remember { mutableStateOf<FulfillerProfileResponse?>(null) }
    var currentStep by rememberSaveable { mutableIntStateOf(0) } // Start at 0 for Class Selection
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // Persist verification session across process death
    var activeSessionToken by rememberSaveable { mutableStateOf<String?>(null) }
    var activeSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    // Photo State - Survives recreation
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

    val cameraLauncher = rememberLauncherForActivityResult(TakeSelfieContract()) { success ->
        if (success) {
            try {
                captureFile.inputStream().use { input ->
                    internalPhotoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                profilePhotoUriString = Uri.fromFile(internalPhotoFile).toString()
                android.util.Log.d("PikopCamera", "Photo verified and saved internally")
            } catch (e: Exception) {
                android.util.Log.e("PikopCamera", "Copy failed", e)
                Toast.makeText(context, "Error saving photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            if (currentStep == 1) {
                cameraLauncher.launch(captureUri)
            } else if (currentStep == 2) {
                // Permissions granted for Step 2, re-trigger the launch logic 
                // but handled by the IdentityStep click or auto-launch
                refreshKey++
            }
        }
    }

    LaunchedEffect(refreshKey) {
        try {
            val res = apiService.getFulfillerProfile()
            profile = res
            
            // Auto-advance logic: Sync step with backend state
            if (res.kyc_status == "PENDING_REVIEW" || res.kyc_status == "VERIFIED") {
                currentStep = 5
            } else if (res.profile_photo_url == null) {
                currentStep = if (res.primary_class == null) 0 else 1
            } else if (res.didit_verification_status != "approved") {
                currentStep = 2
            } else if (res.primary_class != "agent" && (res.registration_number == null)) {
                // If not an agent and no vehicle info, go to License/Vehicle steps
                // We'll start at Step 3 (License)
                currentStep = 3
            } else {
                currentStep = 5
            }
        } catch (e: Exception) {
            android.util.Log.e("PikopKyc", "Profile refresh failed", e)
        }
    }

    // Auto-poll status when in identity step and pending
    LaunchedEffect(currentStep, profile?.didit_verification_status) {
        if (currentStep == 2 && profile?.didit_verification_status != "approved") {
            while(true) {
                kotlinx.coroutines.delay(10000)
                refreshKey++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fulfiller Onboarding") },
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
                0 -> FulfillerTypeSelectionScreen(
                    onClassSelected = { refreshKey++ }
                )
                1 -> ProfilePhotoStep(profilePhotoUri) { 
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        if (captureFile.exists()) captureFile.delete()
                        cameraLauncher.launch(captureUri)
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                }
                2 -> IdentityStep(
                    status = profile?.didit_verification_status ?: "not_started", 
                    role = profile?.primary_class ?: "agent",
                    api = apiService,
                    sessionToken = activeSessionToken,
                    onSessionCreated = { token, id -> 
                        activeSessionToken = token
                        activeSessionId = id
                    },
                    onPermissionRequest = { 
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.CAMERA, 
                            Manifest.permission.RECORD_AUDIO
                        )) 
                    },
                    onRefresh = { refreshKey++ }
                )
                3 -> LicenseStep(
                    role = profile?.primary_class ?: "rider",
                    api = apiService,
                    onComplete = { currentStep = 4 }
                )
                4 -> VehicleStep(api = apiService, onComplete = { currentStep = 5 })
                5 -> SubmissionStep(
                    isLoading = isLoading,
                    status = profile?.kyc_status ?: "NOT_SUBMITTED",
                    onComplete = {
                        scope.launch {
                            isLoading = true
                            try {
                                apiService.submitApplication()
                                refreshKey++ 
                                Toast.makeText(context, "Application submitted!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    }
                )
            }

            if (currentStep < 5) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                when (currentStep) {
                                    1 -> {
                                        if (internalPhotoFile.exists()) {
                                            val compressed = ImageUtils.compressFile(context, internalPhotoFile)
                                            val body = MultipartBody.Part.createFormData("photo", "profile.jpg", compressed.asRequestBody("image/*".toMediaTypeOrNull()))
                                            apiService.uploadProfilePhoto(body)
                                            currentStep = 2
                                        }
                                    }
                                    2 -> {
                                        if (profile?.didit_verification_status == "approved") {
                                            if (profile?.primary_class == "agent") currentStep = 5
                                            else currentStep = 3
                                        }
                                    }
                                    // 3 (License) and 4 (Vehicle) have internal buttons
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && (currentStep != 1 || profilePhotoUri != null)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else if (currentStep == 2) Text("Refresh Status")
                    else Text("Continue")
                }
            }
        }
    }
}

@Composable
fun VehicleStep(api: ApiService, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var regNum by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Vehicle Details", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = regNum, onValueChange = { regNum = it }, label = { Text("Registration Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = make, onValueChange = { make = it }, label = { Text("Vehicle Make (e.g. Honda)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Vehicle Model") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
        
        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    try {
                        api.updateFulfillerProfile(ProfileUpdateRequest(
                            vehicle_details = VehicleDetails(regNum, make, model, color)
                        ))
                        onComplete()
                    } catch (_: Exception) {}
                    isSaving = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && regNum.isNotBlank() && make.isNotBlank()
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Save Vehicle Info")
        }
    }
}

@Composable
fun LicenseStep(role: String, api: ApiService, onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    val docType = if (role == "rider") "RIDERS_LICENSE" else "DRIVERS_LICENSE"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Legal Authorization", style = MaterialTheme.typography.titleLarge)
        Text("Please upload a clear photo of your valid ${docType.replace("_", " ")}.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Card(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = if (selectedUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (selectedUri != null) Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                else Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            }
        }

        Button(
            onClick = {
                scope.launch {
                    isUploading = true
                    try {
                        val file = getFileFromUri(context, selectedUri!!)
                        val compressed = ImageUtils.compressFile(context, file)
                        val body = MultipartBody.Part.createFormData("document", "license.jpg", compressed.asRequestBody("image/*".toMediaTypeOrNull()))
                        val typePart = docType.toRequestBody("text/plain".toMediaTypeOrNull())
                        api.uploadKYC(typePart, body)
                        onComplete()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                    } finally { isUploading = false }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading && selectedUri != null
        ) {
            if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Upload License")
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "kyc_temp_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }
    return tempFile
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

@Composable
fun ProfilePhotoStep(uri: Uri?, onCapture: () -> Unit) {
    Text("Face Capture", style = MaterialTheme.typography.titleLarge)
    Text("Live capture required for security.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(modifier = Modifier.height(24.dp))
    Card(onClick = onCapture, modifier = Modifier.size(200.dp), shape = androidx.compose.foundation.shape.CircleShape) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) Icon(Icons.Default.Check, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            else Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        }
    }
}

@Composable
fun IdentityStep(
    status: String, 
    role: String,
    api: ApiService, 
    sessionToken: String?,
    onSessionCreated: (String, String) -> Unit,
    onPermissionRequest: () -> Unit, 
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLaunching by remember { mutableStateOf(false) }
    
    var mobilityType by rememberSaveable { mutableStateOf("on_foot") }

    val statusColor = when (status.lowercase()) {
        "approved" -> Color(0xFF4CAF50)
        "declined" -> MaterialTheme.colorScheme.error
        "pending", "needs_review" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (status.lowercase()) {
        "approved" -> "Identity Verified"
        "declined" -> "Verification Declined"
        "pending", "needs_review" -> "Pending Approval"
        else -> "Verify Identity"
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), onClick = {
            if (isLaunching || status == "approved") return@Card
            
            val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            
            if (!hasCamera || !hasAudio) {
                onPermissionRequest()
                return@Card
            }
            
            val activity = context.findActivity() 
            if (activity == null) {
                Toast.makeText(context, "Error: App environment not ready.", Toast.LENGTH_SHORT).show()
                return@Card
            }

            scope.launch {
                isLaunching = true
                try {
                    val tokenToUse = if (!sessionToken.isNullOrBlank()) {
                        android.util.Log.d("PikopKyc", "Resuming existing session: $sessionToken")
                        sessionToken
                    } else {
                        android.util.Log.d("PikopKyc", "Requesting new verification session...")
                        val session = api.startDiditVerification()
                        if (!session.session_token.isNullOrBlank()) {
                            onSessionCreated(session.session_token, session.session_id ?: "")
                            session.session_token
                        } else {
                            throw Exception("Server returned empty session token.")
                        }
                    }

                    if (!tokenToUse.isNullOrBlank()) {
                        DiditSdk.startVerification(token = tokenToUse) { 
                            onRefresh() 
                        }
                        DiditSdk.launchVerificationUI(activity)
                    } else {
                        Toast.makeText(context, "Verification token missing.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PikopKyc", "Launch failed", e)
                    val errorMsg = ErrorUtils.parseError(e)
                    Toast.makeText(context, "Identity Launch Failed: $errorMsg", Toast.LENGTH_LONG).show()
                } finally { isLaunching = false }
            }
        }) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isLaunching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Icon(
                    imageVector = if (status == "approved") Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = if (status == "approved") Color(0xFF4CAF50) else Color.Unspecified
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(statusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(status.uppercase(), style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
            }
        }

        if (status.lowercase() == "approved" && role.lowercase() == "agent") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Your Mobility", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val options = listOf("on_foot" to "Walking", "public_transit" to "Public Transit", "bicycle" to "Bicycle")
                    options.forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mobilityType == key, onClick = { 
                                mobilityType = key
                                scope.launch {
                                    try {
                                        api.updateFulfillerProfile(ProfileUpdateRequest(mobility_type = key))
                                    } catch (e: Exception) {}
                                }
                            })
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        if (status.lowercase() == "pending" || status.lowercase() == "needs_review") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Our security partner is reviewing your documents. This usually takes 2-5 minutes. Tap refresh above to check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

@Composable
fun SubmissionStep(
    isLoading: Boolean,
    status: String,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.LibraryAddCheck, 
            null, 
            modifier = Modifier.size(64.dp), 
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Final Review", style = MaterialTheme.typography.titleLarge)
        Text(
            "Your identity has been verified. Tap the button below to submit your application for final manual review by our operations team.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (status == "PENDING_REVIEW") {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Text(
                    "Application Submitted! We will notify you once you're activated.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF2E7D32)
                )
            }
        } else {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Complete Onboarding")
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
