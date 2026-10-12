package com.ng.pikop.core.location

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * v4.10 (speed): store screens used to WAIT for a fresh GPS fix before asking the server for items,
 * which can take several seconds. This returns the phone's last known position instantly, and only
 * falls back to a fresh fix (capped at 2.5 seconds) if there isn't one. Never throws.
 */
suspend fun FusedLocationProviderClient.quickLocation(): Location? {
    return try {
        lastLocation.await()
            ?: withTimeoutOrNull(2500) {
                getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            }
    } catch (e: Exception) {
        null
    }
}
