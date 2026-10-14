package com.ng.pikop.core.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ng.pikop.R
import com.ng.pikop.core.network.SocketManager
import org.json.JSONObject

/**
 * Keeps an agent's live location flowing to the customer while a delivery is
 * active, even if the agent switches apps or locks the phone. Android requires
 * the visible "sharing location" notification while this runs.
 */
class MissionLocationService : Service() {

    private var callback: LocationCallback? = null
    private var orderId: String? = null
    private var lastSentAt = 0L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        val id = intent?.getStringExtra(EXTRA_ORDER_ID)
        if (id.isNullOrBlank() ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        orderId = id
        startAsForeground()
        startUpdates()
        // Safety net: never keep sharing location for more than 4 hours.
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelfSafely() }, 4 * 60 * 60 * 1000L)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Active delivery", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        val pi = android.app.PendingIntent.getActivity(
            this, 0, launch, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.pikop_notification_icon)
            .setContentTitle("Pikop delivery in progress")
            .setContentText("Sharing your live location with the customer")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startUpdates() {
        if (callback != null) return
        val client = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .setMinUpdateDistanceMeters(3f)
            .setWaitForAccurateLocation(false)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val id = orderId ?: return
                // Precision: skip weak fixes (more than 40 m of uncertainty) so the marker
                // does not jump around; but if nothing better has come for 30 s, send it anyway.
                val now = System.currentTimeMillis()
                if (loc.hasAccuracy() && loc.accuracy > 40f && now - lastSentAt < 30_000L) return
                lastSentAt = now
                val data = JSONObject().apply {
                    put("orderId", id)
                    put("lat", loc.latitude)
                    put("lng", loc.longitude)
                    if (loc.hasAccuracy()) put("accuracy", loc.accuracy.toDouble())
                    if (loc.hasBearing()) put("heading", loc.bearing.toDouble())
                }
                SocketManager.emit("update_mission_location", data)
            }
        }
        try {
            client.requestLocationUpdates(request, callback!!, mainLooper)
        } catch (e: SecurityException) {
            stopSelfSafely()
        }
    }

    private fun stopSelfSafely() {
        callback?.let { LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it) }
        callback = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        callback?.let { LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it) }
        callback = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "pikop_active_delivery"
        private const val NOTIF_ID = 4107
        private const val EXTRA_ORDER_ID = "order_id"
        private const val ACTION_STOP = "com.ng.pikop.STOP_MISSION_LOCATION"

        /** Returns true if the service was started. */
        fun start(context: Context, orderId: String): Boolean = try {
            val i = Intent(context, MissionLocationService::class.java).putExtra(EXTRA_ORDER_ID, orderId)
            ContextCompat.startForegroundService(context, i)
            true
        } catch (e: Exception) {
            android.util.Log.w("MissionLocation", "Could not start service: ${e.message}")
            false
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, MissionLocationService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) { /* not running */ }
        }
    }
}
