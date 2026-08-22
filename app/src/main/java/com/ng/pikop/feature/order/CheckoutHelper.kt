package com.ng.pikop.feature.order

import com.google.android.gms.maps.model.LatLng

/**
 * Singleton to pass complex order data between screens without
 * breaking the Navigation Component with long, encoded URL strings.
 */
object CheckoutHelper {
    var activeQuote: CheckoutData? = null

    data class CheckoutData(
        val url: String,
        val quoteId: String,
        val pLat: Double,
        val pLng: Double,
        val dLat: Double,
        val dLng: Double,
        val itemPhotoUrl: String,
        val pickupSummary: String,
        val deliverySummary: String,
        val recipientName: String,
        val recipientPhone: String,
        val notes: String?,
        val promoId: String?
    )
}
