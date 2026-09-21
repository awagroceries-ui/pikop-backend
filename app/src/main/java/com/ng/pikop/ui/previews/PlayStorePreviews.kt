package com.ng.pikop.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.ui.theme.PikopGreen
import com.ng.pikop.ui.theme.PikopGold
import com.ng.pikop.ui.theme.PikopOrange
import com.ng.pikop.ui.theme.PikopTheme

// -------------------------------------------------------------
// Screenshot 1: Four Services, One Super App
// -------------------------------------------------------------
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun Screenshot1_HomeScreen() {
    PikopTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Banner
            Surface(
                color = PikopGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Deliver to", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("Victoria Island, Lagos 📍", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Search food, groceries, packages...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 Services Section
            Text(
                "Our Services",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ServiceItem("Dispatch", Icons.Default.LocalShipping, PikopGreen)
                ServiceItem("Food", Icons.Default.Restaurant, PikopOrange)
                ServiceItem("Groceries", Icons.Default.LocalGroceryStore, PikopGold)
                ServiceItem("Shop", Icons.Default.ShoppingBag, Color(0xFF2196F3))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Escrow Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = PikopGold.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Celebration, null, tint = PikopGold, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100% Escrow Protected", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Pay on Delivery with complete peace of mind.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active Mission Card Mock
            Text(
                "Active Orders",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = PikopGreen)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AssignmentTurnedIn, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Package in Transit 🚚", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Rider is 1.2km away • ETA 8 mins", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = PikopGold, contentColor = Color.Black)
                    ) {
                        Text("TRACK", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// -------------------------------------------------------------
// Screenshot 2: Real-Time Mission Tracking
// -------------------------------------------------------------
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun Screenshot2_LiveTracking() {
    PikopTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Simulated Map Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFE5E3DF))
            ) {
                // Route Line Illustration
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = PikopGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.TwoWheeler, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text("Rider: Ibrahim M.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    Text("Speed: 32 km/h", fontSize = 10.sp, color = Color.Gray)
                }

                // Header Overlay
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Navigation, null, tint = PikopGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live GPS Tracking Active", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Bottom Tracking Details Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Order #PK-98214", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Estimated Arrival: 8 Mins", color = PikopGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Phone, null, tint = PikopGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // OTP Codes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PICKUP CODE", fontSize = 10.sp, color = Color.Gray)
                                Text("4892", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PikopGreen)
                            }
                            Divider(modifier = Modifier.height(36.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DELIVERY CODE", fontSize = 10.sp, color = Color.Gray)
                                Text("7319", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PikopOrange)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Points
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RadioButtonChecked, null, tint = PikopGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pickup: Mama Jay Kitchen, Ikeja", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = PikopOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delivery: 14 Allen Avenue, Ikeja", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Screenshot 3: Multi-Item Shopping Cart
// -------------------------------------------------------------
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun Screenshot3_ShoppingCart() {
    PikopTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Your Shopping Cart 🛒", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Store Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, null, tint = PikopGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Store: SuperMart Groceries (VI)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Divider()

                // Cart Item 1
                CartItemMock("Fresh Whole Chicken (2kg)", "₦4,500", "2")
                // Cart Item 2
                CartItemMock("Basmati Rice 5kg Sack", "₦12,000", "1")
                // Cart Item 3
                CartItemMock("Pure Vegetable Oil 2L", "₦3,800", "1")

                Spacer(modifier = Modifier.weight(1f))

                // Promo Coupon Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PikopGold.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, null, tint = PikopGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUPERMART-500 Applied", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("-₦500", color = PikopGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Bottom Summary & Checkout
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = Color.Gray, fontSize = 13.sp)
                        Text("₦24,800", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text("₦24,300", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PikopGreen)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PikopGreen)
                    ) {
                        Icon(Icons.Default.ShoppingCartCheckout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROCEED TO CHECKOUT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemMock(name: String, price: String, qty: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(price, color = PikopGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("−", fontWeight = FontWeight.Bold) }
                }
                Text(qty, modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold, color = PikopGreen) }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Screenshot 4: 100% Escrow Protected Payments
// -------------------------------------------------------------
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun Screenshot4_SecureCheckout() {
    PikopTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Secure Checkout", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Escrow Banner Highlight
                Card(
                    colors = CardDefaults.cardColors(containerColor = PikopGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, tint = PikopGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("100% Escrow Protection", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("Funds held safely until you inspect & confirm receipt.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }

                Text("Select Payment Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                PaymentOptionItem("Cash on Delivery (Escrow)", "Pay in-app; seller paid after confirmation", Icons.Default.Payments, true)
                PaymentOptionItem("Debit / Credit Card", "Fast & instant via Paystack", Icons.Default.CreditCard, false)
                PaymentOptionItem("Pikop Wallet", "Balance: ₦45,000", Icons.Default.AccountBalanceWallet, false)
                PaymentOptionItem("Bank Transfer / USSD", "Instant virtual bank account transfer", Icons.Default.AccountBalance, false)

                Spacer(modifier = Modifier.weight(1f))

                // Price Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Item Price", fontSize = 12.sp, color = Color.Gray)
                            Text("₦24,300", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Fee", fontSize = 12.sp, color = Color.Gray)
                            Text("₦1,200", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("10% Escrow Platform Fee", fontSize = 12.sp, color = PikopOrange)
                            Text("₦2,430", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PikopOrange)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Payable", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("₦27,930", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PikopGreen)
                        }
                    }
                }

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PikopGreen)
                ) {
                    Text("ACTIVATE MISSION (₦27,930)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PaymentOptionItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PikopGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, PikopGreen) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = {})
            Spacer(modifier = Modifier.width(8.dp))
            Icon(icon, null, tint = if (selected) PikopGreen else Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

// -------------------------------------------------------------
// Screenshot 5: 24/7 AI Support Assistant
// -------------------------------------------------------------
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun Screenshot5_AiSupport() {
    PikopTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Bar
            Surface(
                color = PikopGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Pikop AI Agent 🤖", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("24/7 Instant Knowledge Support", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chat Message User
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = PikopGreen,
                        shape = RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp),
                        modifier = Modifier.widthIn(max = 260.dp)
                    ) {
                        Text("How does the Cash on Delivery escrow system work?", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }

                // Chat Message AI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = PikopOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pikop AI Agent", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PikopOrange)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "When you select COD, you pay in-app at checkout, but Pikop holds the funds safely in escrow. Money is only released to the seller after you receive the item and tap 'Confirm Receipt'!",
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Input Box
                OutlinedTextField(
                    value = "Can I request a refund if item is damaged?",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = PikopOrange,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Play Store Feature Graphic (1024 x 500 px aspect ratio)
// -------------------------------------------------------------
@Preview(showBackground = true, widthDp = 512, heightDp = 250)
@Composable
fun FeatureGraphicPreview() {
    PikopTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PikopGreen)
                .padding(24.dp)
        ) {
            // Background Decorative Accents
            Surface(
                color = PikopGold.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-60).dp)
            ) {}

            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = CircleShape,
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 40.dp)
            ) {}

            // Main Content Layout
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Branding & Taglines
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = PikopGold,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "NIGERIA'S LOGISTICS & MARKETPLACE SUPER APP",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Pikop",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        maxLines = 1
                    )

                    Text(
                        "Four Services. One Seamless App.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Service Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FeatureBadge("Dispatch 🚚", PikopGreen, Color.White)
                        FeatureBadge("Food 🍔", PikopOrange, Color.White)
                        FeatureBadge("Groceries 🛒", PikopGold, Color.Black)
                        FeatureBadge("Shop 🛍️", Color(0xFF2196F3), Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Column: Escrow Trust Badge Card
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = PikopGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "100% Escrow\nProtected",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pay on Delivery",
                            color = PikopGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBadge(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

