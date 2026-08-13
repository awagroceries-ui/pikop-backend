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
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
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
            cameraLauncher.launch(captureUri)
        }
    }

    LaunchedEffect(refreshKey) {
        try {
            val res = apiService.getFulfillerProfile()
            profile = res
            if (res.profile_photo_url != null && currentStep < 2) currentStep = 2
            if (res.didit_verification_status == "approved" && currentStep < 3) currentStep = 3
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
                3 -> SubmissionStep(
                    isLoading = isLoading,
                    status = profile?.kyc_status ?: "NOT_SUBMITTED",
                    onComplete = {
                        scope.launch {
                            isLoading = true
                            try {
                                apiService.submitApplication()
                                refreshKey++ // Trigger profile refresh to show PENDING_REVIEW
                                Toast.makeText(context, "Application submitted!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    }
                )
            }

            if (currentStep < 3) {
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
                                    2 -> if (profile?.didit_verification_status == "approved") currentStep = 3
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
                    else Text("Continue")
                }
            }
        }
    }
}

@Composable
fun StepIndicator(current: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        (1..3).forEach { i ->
            Box(modifier = Modifier.size(12.dp).padding(2.dp)) {
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
    api: ApiService, 
    sessionToken: String?,
    onSessionCreated: (String, String) -> Unit,
    onPermissionRequest: () -> Unit, 
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLaunching by remember { mutableStateOf(false) }

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
            
            val activity = context.findActivity() ?: return@Card
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
                        } else null
                    }

                    if (!tokenToUse.isNullOrBlank()) {
                        DiditSdk.startVerification(token = tokenToUse) { 
                            onRefresh() 
                        }
                        DiditSdk.launchVerificationUI(activity)
                    } else {
                        Toast.makeText(context, "Verification server returned no token", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PikopKyc", "Launch failed", e)
                    Toast.makeText(context, "Launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
