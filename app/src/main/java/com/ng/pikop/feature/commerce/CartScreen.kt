package com.ng.pikop.feature.commerce

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.cart.CartItem
import com.ng.pikop.core.cart.CartManager
import com.ng.pikop.ui.theme.PikopGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onCheckout: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (CartManager.items.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text("₦${"%,.2f".format(CartManager.totalAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PikopGreen)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proceed to Checkout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (CartManager.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your cart is empty.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(CartManager.items) { cartItem ->
                    CartItemRow(
                        item = cartItem,
                        onIncrease = { CartManager.updateQuantity(cartItem.item.id, cartItem.quantity + 1) },
                        onDecrease = { CartManager.updateQuantity(cartItem.item.id, cartItem.quantity - 1) },
                        onRemove = { CartManager.removeItem(cartItem.item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.item.name, fontWeight = FontWeight.Bold)
                Text("₦${"%,.2f".format(item.item.price)}", style = MaterialTheme.typography.bodySmall, color = PikopGreen)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) { Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrease) { Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
