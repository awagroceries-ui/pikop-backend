package com.ng.pikop.feature.merchant

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    merchantType: String, // vendor, kitchen
    merchantId: String,
    productId: String? = null,
    onSuccess: () -> Unit,
    onCelebration: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // v4.5 fix (#7): previously every non-kitchen merchant (Groceries, Shop,
    // Pharmacy, etc.) got the exact same mashed-together list of categories,
    // which made no sense for a Shop merchant trying to list groceries-style
    // items or vice versa. This now also checks the merchant's own
    // registered store type (storeCategory, fetched below) so Groceries and
    // Shop merchants each see categories appropriate to their store.
    var storeCategory by remember { mutableStateOf<String?>(null) }

    fun categoriesFor(storeCat: String?): List<String> = when {
        merchantType == "kitchen" ->
            listOf("Local", "Fast Food", "Continental", "Healthy", "Pastries", "Drinks", "Soups & Stews", "Grills & BBQ", "Custom / Other...")
        storeCat.equals("Groceries", ignoreCase = true) ->
            listOf("Fresh Produce", "Dairy & Eggs", "Beverages", "Snacks", "Frozen Foods", "Bakery", "Grains & Staples", "Household", "Custom / Other...")
        storeCat.equals("Shop", ignoreCase = true) ->
            listOf("Electronics", "Phones & Accessories", "Pharmacy", "Fashion", "Beauty", "Household", "Custom / Other...")
        else ->
            // Fallback while the store's category is still loading, or
            // for a store type with no dedicated list yet.
            listOf("Fresh Produce", "Dairy & Eggs", "Beverages", "Snacks", "Electronics", "Pharmacy", "Fashion", "Beauty", "Household", "Custom / Other...")
    }

    val presetCategories = remember(merchantType, storeCategory) { categoriesFor(storeCategory) }

    var expandedCategoryDropdown by remember { mutableStateOf(false) }
    var isCustomCategory by remember { mutableStateOf(false) }
    var customCategoryInput by remember { mutableStateOf("") }
    
    // Vendor Specific
    var stockQuantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("item") }
    var nafdacNumber by remember { mutableStateOf("") }
    
    // Kitchen Specific
    var prepTime by remember { mutableStateOf("30") }
    var available by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(productId != null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(merchantId, productId) {
        // v4.5 fix (#7): fetch the merchant's own store details (for its
        // registered category) every time -- not just when editing an
        // existing item -- so the category dropdown is correct from the
        // very first "Add Item" too.
        try {
            if (merchantType == "vendor") {
                val res = apiService.getVendorDetails(merchantId)
                storeCategory = res.data?.category
                if (productId != null) {
                    val p = res.data?.products?.find { it.id == productId }
                    p?.let { prod ->
                        name = prod.name
                        price = prod.price.toString()
                        description = prod.description ?: ""
                        category = prod.category ?: ""
                        photoUrl = prod.photo_url
                        stockQuantity = prod.stock_quantity.toString()
                        unit = prod.unit ?: "item"
                        nafdacNumber = prod.nafdac_number ?: ""
                    }
                }
            } else {
                val res = apiService.getKitchenDetails(merchantId)
                storeCategory = res.data?.category
                if (productId != null) {
                    val m = res.data?.menu?.find { it.id == productId }
                    m?.let { item ->
                        name = item.name
                        price = item.price.toString()
                        description = item.description ?: ""
                        category = item.category ?: ""
                        photoUrl = item.photo_url
                        prepTime = item.prep_time_minutes.toString()
                        available = item.available
                    }
                }
            }
            // Decide custom-category state using the store-type-aware list
            // computed directly (not the `presetCategories` remember value,
            // which may not have recomposed with the freshly-set
            // storeCategory yet at this point in the coroutine).
            if (category.isNotBlank() && !categoriesFor(storeCategory).contains(category)) {
                isCustomCategory = true
                customCategoryInput = category
            }
        } catch (e: Exception) {
            if (productId != null) {
                Toast.makeText(context, "Error loading item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isFetching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Add Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isFetching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = "https://api.pikop.com.ng$photoUrl",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text("Upload Photo", color = Color.Gray)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₦)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown Selection
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryDropdown,
                    onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (isCustomCategory) "Custom / Other..." else category.ifBlank { "Select Category" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryDropdown,
                        onDismissRequest = { expandedCategoryDropdown = false }
                    ) {
                        presetCategories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    if (option == "Custom / Other...") {
                                        isCustomCategory = true
                                        category = customCategoryInput
                                    } else {
                                        isCustomCategory = false
                                        category = option
                                    }
                                    expandedCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                if (isCustomCategory) {
                    OutlinedTextField(
                        value = customCategoryInput,
                        onValueChange = { 
                            customCategoryInput = it
                            category = it
                        },
                        label = { Text("Specify Custom Category") },
                        placeholder = { Text("e.g. Seafood, Organic, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                if (merchantType == "vendor") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it },
                            label = { Text("Stock Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (e.g. kg)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = nafdacNumber,
                        onValueChange = { nafdacNumber = it },
                        label = { Text("NAFDAC Reg No. (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = prepTime,
                        onValueChange = { prepTime = it },
                        label = { Text("Prep Time (Mins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = available, onCheckedChange = { available = it })
                        Text("Available for Order")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                var finalPhotoUrl = photoUrl
                                
                                // Upload image if selected
                                if (selectedImageUri != null) {
                                    val file = ImageUtils.compressImage(context, selectedImageUri!!)
                                    if (file != null) {
                                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                                        val uploadRes = apiService.uploadOrderPhoto(body)
                                        finalPhotoUrl = uploadRes["url"]
                                    }
                                }

                                if (merchantType == "vendor") {
                                    if (productId == null) {
                                        apiService.addProduct(CreateProductRequest(
                                            vendor_id = merchantId,
                                            name = name,
                                            price = price.toDoubleOrNull() ?: 0.0,
                                            description = description.ifBlank { null },
                                            category = category.ifBlank { null },
                                            stock_quantity = stockQuantity.toIntOrNull() ?: 0,
                                            unit = unit.ifBlank { "item" },
                                            nafdac_number = nafdacNumber.ifBlank { null },
                                            photo_url = finalPhotoUrl
                                        ))
                                    } else {
                                        apiService.updateProduct(productId, ProductUpdateRequest(
                                            name = name,
                                            price = price.toDoubleOrNull(),
                                            stock_quantity = stockQuantity.toIntOrNull(),
                                            description = description,
                                            category = category,
                                            unit = unit,
                                            nafdac_number = nafdacNumber,
                                            photo_url = finalPhotoUrl
                                        ))
                                    }
                                } else {
                                    if (productId == null) {
                                        apiService.addMenuItem(CreateMenuItemRequest(
                                            kitchen_id = merchantId,
                                            name = name,
                                            price = price.toDoubleOrNull() ?: 0.0,
                                            description = description.ifBlank { null },
                                            category = category.ifBlank { null },
                                            prep_time_minutes = prepTime.toIntOrNull() ?: 30,
                                            photo_url = finalPhotoUrl,
                                            available = available
                                        ))
                                    } else {
                                        apiService.updateMenuItem(productId, MenuItemUpdateRequest(
                                            name = name,
                                            price = price.toDoubleOrNull(),
                                            description = description,
                                            category = category,
                                            photo_url = finalPhotoUrl,
                                            prep_time_minutes = prepTime.toIntOrNull(),
                                            available = available
                                        ))
                                    }
                                }

                                Toast.makeText(context, "Item Saved Successfully!", Toast.LENGTH_SHORT).show()
                                if (productId == null) onCelebration()
                                onSuccess()
                            } catch (e: Exception) {
                                val errorMsg = ErrorUtils.parseError(e)
                                Toast.makeText(context, "Failed to save: $errorMsg", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && name.isNotBlank() && price.isNotBlank()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Save Listing")
                }
            }
        }
    }
}
