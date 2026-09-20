package com.ng.pikop.core.cart

import androidx.compose.runtime.mutableStateListOf
import com.ng.pikop.core.network.DiscoveryItem

data class CartItem(
    val item: DiscoveryItem,
    var quantity: Int
)

object CartManager {
    val items = mutableStateListOf<CartItem>()
    
    val merchantId: String? get() = items.firstOrNull()?.item?.vendor_id?.ifBlank { items.firstOrNull()?.item?.id } // Simplified logic
    
    val totalAmount: Double get() = items.sumOf { it.item.price * it.quantity }
    
    fun addItem(newItem: DiscoveryItem) {
        val currentMerchant = items.firstOrNull()?.item?.vendor_id
        if (currentMerchant != null && currentMerchant != newItem.vendor_id) {
            // Policy: Only one merchant at a time. This should be handled by UI prompt.
        }
        
        val existing = items.find { it.item.id == newItem.id }
        if (existing != null) {
            existing.quantity += 1
        } else {
            items.add(CartItem(newItem, 1))
        }
    }
    
    fun removeItem(itemId: String) {
        items.removeIf { it.item.id == itemId }
    }
    
    fun clear() {
        items.clear()
    }
    
    fun updateQuantity(itemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItem(itemId)
            return
        }
        items.find { it.item.id == itemId }?.quantity = newQuantity
    }
}
