package com.ng.pikop.feature.fulfiller

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.network.ApiService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycUploadScreen(onBack: () -> Unit) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var docType by remember { mutableStateOf("ID_CARD") }
    var isUploading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fulfiller Verification") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_badge),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Upload Documents",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "To start delivering, we need to verify your identity.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Doc Type Selection
            Text("Document Type", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = docType == "ID_CARD",
                    onClick = { docType = "ID_CARD" },
                    label = { Text("ID Card") }
                )
                FilterChip(
                    selected = docType == "DRIVING_LICENSE",
                    onClick = { docType = "DRIVING_LICENSE" },
                    label = { Text("License") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Upload Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = { launcher.launch("image/*") }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selectedUri != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Image Selected", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text("Tap to select image", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    isUploading = true
                    scope.launch {
                        try {
                            val file = getFileFromUri(context, uri)
                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
                            val type = docType.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            apiService.uploadKYC(type, body)
                            Toast.makeText(context, "Upload Successful!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedUri != null && !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Submit for Verification")
                }
            }
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "kyc_temp_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}
