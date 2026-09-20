package com.ng.pikop.core.cart

import androidx.compose.runtime.mutableStateListOf
import com.ng.pikop.core.network.DiscoveryItem

data class CartItem(
    val item: DiscoveryItem,
    var quantity: Int
)

object CartManager {
    val items = mutableStateListOf<CartItem>()
    
    val merchantId: String? get() = items.firstOrNull()?.item?.vendor_id?.ifBlank { items.firstOrNull()?.item?.id } 
    
    val totalAmount: Double get() = items.sumOf { it.item.price * it.quantity }
    
    fun addItem(newItem: DiscoveryItem): Boolean {
        val currentMerchant = items.firstOrNull()?.item?.vendor_id
        if (!currentMerchant.isNullOrBlank() && currentMerchant != newItem.vendor_id) {
            return false
        }
        
        val existing = items.find { it.item.id == newItem.id }
        if (existing != null) {
            existing.quantity += 1
        } else {
            items.add(CartItem(newItem, 1))
        }
        return true
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
        items.find { it.item.id == itemId }?.let { 
            it.quantity = newQuantity
        }
    }
}
