package com.ng.pikop.feature.order

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.ng.pikop.feature.fulfiller.FulfillerProfileDialog
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class OrderStatusStep(
    val status: String,
    val description: String,
    val time: String,
    val isCompleted: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(
    orderId: String, 
    pickup: LatLng? = null, 
    delivery: LatLng? = null,
    onCelebration: () -> Unit = {},
    onLeave: () -> Unit = {}
) {
    var pickupLoc by remember { mutableStateOf(pickup) }
    var deliveryLoc by remember { mutableStateOf(delivery) }
    var fulfillerLocation by remember { mutableStateOf<LatLng?>(null) }
    var etaMinutes by remember { mutableStateOf<Int?>(null) }
    var history by remember { mutableStateOf<List<OrderStatusStep>>(emptyList()) }
    var fulfillerProfile by remember { mutableStateOf<FulfillerPublicProfile?>(null) }
    var trackingUrl by remember { mutableStateOf<String?>(null) }
    // Last time the live connection delivered something; the 3-second backup refresh stays quiet while live events flow.
    var lastLiveEventAt by remember { mutableStateOf(0L) }
    // Bumped on every live status change so the bottom sheet reloads immediately.
    var liveTick by remember { mutableIntStateOf(0) }
    
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var realRoadDestinationRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var realRoadActiveRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    // Route progress: the part behind the agent turns grey as the marker moves along the line
    var routeTravelled by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeRemaining by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeSegmentIndex by remember { mutableStateOf(0) }
    var agentOffRoute by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val userId by tokenManager.userId.collectAsState(initial = null)
    
    // Animation State
    val interpolatedLat = remember { Animatable(0f) }
    val interpolatedLng = remember { Animatable(0f) }
    val animatedFulfillerLoc by remember {
        derivedStateOf {
            if (interpolatedLat.value != 0f) LatLng(interpolatedLat.value.toDouble(), interpolatedLng.value.toDouble())
            else fulfillerLocation
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickupLoc ?: LatLng(6.5244, 3.3792), 14f)
    }

    val fetchHistory: () -> Unit = {
        coroutineScope.launch {
            try {
                isLoading = true
                error = null
                android.util.Log.d("TrackOrder", "Fetching details for mission: $orderId")
                val response = apiService.getOrderDetails(orderId)
                
                // DUAL-COMPATIBILITY: Support both {id: ...} and {data: {id: ...}}
                val details = response.data ?: response
                
                android.util.Log.d("TrackOrder", "Effective Data: $details")
                
                fulfillerProfile = details.fulfiller_profile
                trackingUrl = details.tracking_url
                
                if (details.pickup_lat != null && details.pickup_lng != null) {
                    pickupLoc = LatLng(details.pickup_lat, details.pickup_lng)
                }
                if (details.delivery_lat != null && details.delivery_lng != null) {
                    deliveryLoc = LatLng(details.delivery_lat, details.delivery_lng)
                }
                
                if (details.fulfiller_lat != null && details.fulfiller_lng != null) {
                    fulfillerLocation = LatLng(details.fulfiller_lat, details.fulfiller_lng)
                }

                history = details.history?.map { item ->
                    OrderStatusStep(
                        status = item.status ?: "UNKNOWN",
                        description = item.description ?: "",
                        time = formatTime(item.time ?: ""),
                        isCompleted = true
                    )
                } ?: emptyList()

                if (pickupLoc == null || deliveryLoc == null) {
                    android.util.Log.e("TrackOrder", "Coordinates missing in object: $details")
                    error = "Mission coordinates are missing from server response."
                }
            } catch (e: Exception) {
                android.util.Log.e("TrackOrder", "Fetch failed", e)
                error = "Failed to load mission details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) {
        fetchHistory()
    }

    // Safety net: if live events stop arriving, check every 3 seconds so the agent's
    // position and the mission status never go stale. The screen no longer needs to
    // be closed and reopened to see changes.
    LaunchedEffect(orderId) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            if (System.currentTimeMillis() - lastLiveEventAt < 8000L) continue
            try {
                val response = apiService.getOrderDetails(orderId)
                val d = response.data ?: response
                val fLat = d.fulfiller_lat
                val fLng = d.fulfiller_lng
                if (fLat != null && fLng != null && fLat != 0.0 && fLng != 0.0) {
                    val current = fulfillerLocation
                    if (current == null || current.latitude != fLat || current.longitude != fLng) {
                        if (interpolatedLat.value == 0f) {
                            interpolatedLat.snapTo(fLat.toFloat())
                            interpolatedLng.snapTo(fLng.toFloat())
                        } else {
                            launch { interpolatedLat.animateTo(fLat.toFloat(), tween(2000)) }
                            launch { interpolatedLng.animateTo(fLng.toFloat(), tween(2000)) }
                        }
                        fulfillerLocation = LatLng(fLat, fLng)
                    }
                }
                d.fulfiller_profile?.let { fulfillerProfile = it }
                d.history?.let { list ->
                    history = list.map { item ->
                        OrderStatusStep(
                            status = item.status ?: "UNKNOWN",
                            description = item.description ?: "",
                            time = formatTime(item.time ?: ""),
                            isCompleted = true
                        )
                    }
                }
                val st = d.status?.uppercase()
                if (st == "DELIVERED" || st == "CANCELLED" || st == "RELEASED") break
            } catch (e: Exception) {
                // offline for a moment: try again next round
            }
        }
    }

    DisposableEffect(orderId, userId) {
        if (userId != null) {
            SocketManager.connect(userId)
            SocketManager.emit("join_order", orderId)
        }
        
        val handleLocationUpdate: (JSONObject) -> Unit = { data ->
            lastLiveEventAt = System.currentTimeMillis()
            val lat = data.optDouble("lat", 0.0)
            val lng = data.optDouble("lng", 0.0)
            if (lat != 0.0 && lng != 0.0) {
                val newLoc = LatLng(lat, lng)
                
                coroutineScope.launch {
                    if (interpolatedLat.value == 0f) {
                        interpolatedLat.snapTo(lat.toFloat())
                        interpolatedLng.snapTo(lng.toFloat())
                    } else {
                        launch { interpolatedLat.animateTo(lat.toFloat(), tween(2000)) }
                        launch { interpolatedLng.animateTo(lng.toFloat(), tween(2000)) }
                    }
                }

                fulfillerLocation = newLoc
                deliveryLoc?.let { dest ->
                    val distanceKm = calculateDistance(newLoc, dest)
                    etaMinutes = ((distanceKm / 30.0) * 60).roundToInt().coerceAtLeast(1)
                }
            }
        }

        SocketManager.on("location_updated", handleLocationUpdate)
        SocketManager.on("location_changed", handleLocationUpdate)
        SocketManager.on("status_updated") { _ ->
            lastLiveEventAt = System.currentTimeMillis()
            liveTick++
            fetchHistory()
        }

        onDispose {
            SocketManager.off("location_updated")
            SocketManager.off("location_changed")
            SocketManager.off("status_updated")
            // v4.5 fix (#6): SocketManager is a single app-wide socket, already
            // owned and connected for the whole session by MainActivity. Tearing
            // it down here killed that shared connection for every other screen
            // (dashboard, active mission, chat) the moment this screen closed,
            // which is a likely cause of the live agent marker silently going
            // stale after navigating away from and back to tracking. Screens
            // should only register/unregister their own listeners, never the
            // shared connection itself.
        }
    }

    LaunchedEffect(pickupLoc, deliveryLoc, fulfillerLocation) {
        val p = pickupLoc
        val d = deliveryLoc
        val f = fulfillerLocation
        val defaultLagos = LatLng(6.5244, 3.3792)

        val validP = if (p != null && p.latitude != 0.0) p else null
        val validD = if (d != null && d.latitude != 0.0) d else null
        val validF = if (f != null && f.latitude != 0.0) f else null

        try {
            val builder = LatLngBounds.builder()
            var count = 0
            validP?.let { builder.include(it); count++ }
            validD?.let { builder.include(it); count++ }
            validF?.let { builder.include(it); count++ }

            if (count >= 2) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
            } else if (count == 1) {
                val singlePoint = validP ?: validD ?: validF ?: defaultLagos
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(singlePoint, 15f))
            } else {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLagos, 12f))
            }
        } catch (e: Exception) {
            val fallbackPoint = validP ?: validD ?: validF ?: defaultLagos
            cameraPositionState.position = CameraPosition.fromLatLngZoom(fallbackPoint, 14f)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Mission Tracking", color = MaterialTheme.colorScheme.primary) },
                actions = {
                    trackingUrl?.let { url ->
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Track my Pikop delivery live at: $url")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share tracking link"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        sheetPeekHeight = 160.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.primary,
        sheetContent = {
            TrackingBottomSheetContent(orderId, etaMinutes, history, fulfillerProfile, tokenManager, onCelebration, fetchHistory, liveTick, onLeave)
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error!!, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = fetchHistory) { Text("Retry") }
                }
            } else {
                Box {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        pickupLoc?.let { Marker(state = MarkerState(position = it), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)) }
                        deliveryLoc?.let { Marker(state = MarkerState(position = it), title = "Delivery", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) }
                        animatedFulfillerLoc?.let { 
                            val iconRes = when(fulfillerProfile?.mobility_type?.lowercase()) {
                                "agent", "walking" -> com.ng.pikop.R.drawable.marker_walking
                                "bicycle" -> com.ng.pikop.R.drawable.marker_bicycle
                                "bike", "motorcycle" -> com.ng.pikop.R.drawable.marker_bike
                                "car", "van", "truck" -> com.ng.pikop.R.drawable.marker_car
                                else -> com.ng.pikop.R.drawable.marker_bike // Default
                            }
                            
                            val scaledIcon: BitmapDescriptor = remember(iconRes) {
                                getScaledMarkerIcon(context, iconRes, sizeDp = 56)
                            }
                            
                            Marker(
                                state = MarkerState(position = it), 
                                title = "Agent", 
                                icon = scaledIcon,
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                zIndex = 10f
                            ) 
                        }
                        
                        LaunchedEffect(pickupLoc, deliveryLoc) {
                            val p = pickupLoc
                            val d = deliveryLoc
                            if (p != null && d != null) {
                                realRoadDestinationRoute = RoadRouteService.fetchRoadRoute(p, d, com.ng.pikop.BuildConfig.GOOGLE_MAPS_API_KEY)
                            }
                        }

                        val pickedUp = history.any { it.status == "PICKED_UP" || it.status == "IN_TRANSIT" || it.status == "ARRIVED_AT_DELIVERY" }

                        // Snap the agent onto the pickup -> delivery route and split it there
                        LaunchedEffect(animatedFulfillerLoc, realRoadDestinationRoute, pickedUp) {
                            val route = realRoadDestinationRoute
                            val ful = animatedFulfillerLoc
                            if (pickedUp && route.size >= 2 && ful != null) {
                                val split = RoadRouteService.splitRouteAtPosition(route, ful, routeSegmentIndex)
                                if (split.distanceToRouteMeters <= 250.0) {
                                    routeSegmentIndex = split.segmentIndex
                                    routeTravelled = split.travelled
                                    routeRemaining = split.remaining
                                    agentOffRoute = false
                                } else {
                                    routeTravelled = emptyList()
                                    routeRemaining = route
                                    agentOffRoute = true
                                }
                            } else {
                                routeSegmentIndex = 0
                                routeTravelled = emptyList()
                                routeRemaining = route
                                agentOffRoute = false
                            }
                        }

                        // Live leg from the agent to the next stop: needed before pickup, or if the agent leaves the route
                        LaunchedEffect(animatedFulfillerLoc, pickupLoc, deliveryLoc, pickedUp, agentOffRoute) {
                            val ful = animatedFulfillerLoc ?: pickupLoc
                            val target = if (pickedUp) deliveryLoc else pickupLoc
                            if (!pickedUp || agentOffRoute) {
                                if (ful != null && target != null) {
                                    realRoadActiveRoute = RoadRouteService.fetchRoadRoute(ful, target, com.ng.pikop.BuildConfig.GOOGLE_MAPS_API_KEY)
                                }
                            } else {
                                realRoadActiveRoute = emptyList()
                            }
                        }

                        val brandGreen = Color(0xFF00E676)
                        val travelledGrey = Color(0xFF9E9E9E).copy(alpha = 0.55f)
                        val upcomingGreen = brandGreen.copy(alpha = 0.35f)
                        val showProgress = pickedUp && !agentOffRoute && routeTravelled.isNotEmpty()

                        // 1) Whole trip line: faded preview, or the bright "remaining" part once the agent is moving along it
                        if (routeRemaining.isNotEmpty()) {
                            Polyline(
                                points = routeRemaining,
                                color = if (showProgress) brandGreen else upcomingGreen,
                                width = if (showProgress) 14f else 10f,
                                jointType = JointType.ROUND,
                                startCap = RoundCap(),
                                endCap = RoundCap()
                            )
                        } else if (pickupLoc != null && deliveryLoc != null) {
                            Polyline(
                                points = listOf(pickupLoc!!, deliveryLoc!!),
                                color = upcomingGreen,
                                width = 10f,
                                jointType = JointType.ROUND,
                                startCap = RoundCap(),
                                endCap = RoundCap()
                            )
                        }

                        // 2) Distance already covered: grey, behind the marker
                        if (showProgress) {
                            Polyline(
                                points = routeTravelled,
                                color = travelledGrey,
                                width = 10f,
                                jointType = JointType.ROUND,
                                startCap = RoundCap(),
                                endCap = RoundCap()
                            )
                        }

                        // 3) Agent -> next stop (to pickup, or back on course): bright
                        if (realRoadActiveRoute.isNotEmpty()) {
                            Polyline(
                                points = realRoadActiveRoute,
                                color = brandGreen,
                                width = 14f,
                                jointType = JointType.ROUND,
                                startCap = RoundCap(),
                                endCap = RoundCap()
                            )
                        }
                    }
                }
            }
        }
    }
}

fun createRoadPolyline(start: LatLng, end: LatLng): List<LatLng> {
    val points = mutableListOf<LatLng>()
    points.add(start)
    val midLat = (start.latitude + end.latitude) / 2
    val midLng = (start.longitude + end.longitude) / 2
    val offsetLat = (end.latitude - start.latitude) * 0.15
    val offsetLng = (end.longitude - start.longitude) * -0.15
    points.add(LatLng(start.latitude + offsetLat, midLng))
    points.add(LatLng(midLat, end.longitude + offsetLng))
    points.add(end)
    return points
}

fun getScaledMarkerIcon(context: android.content.Context, resId: Int, sizeDp: Int = 56): BitmapDescriptor {
    return try {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val src = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)

        // The artwork has a lot of transparent padding, which made the agent look tiny/invisible.
        // Crop to the visible part, then draw it inside a white disc with a green ring and shadow.
        var minX = src.width; var minY = src.height; var maxX = -1; var maxY = -1
        val row = IntArray(src.width)
        for (y in 0 until src.height) {
            src.getPixels(row, 0, src.width, 0, y, src.width, 1)
            for (x in 0 until src.width) {
                if ((row[x] ushr 24) > 20) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
        }
        val cropped = if (maxX > minX && maxY > minY)
            android.graphics.Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1) else src

        val out = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val c = sizePx / 2f
        val ring = 3f * density
        val radius = c - 4f * density

        val shadow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(90, 0, 0, 0)
        }
        canvas.drawCircle(c, c + 1.5f * density, radius, shadow)
        val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawCircle(c, c, radius, fill)
        val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#008751")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = ring
        }
        canvas.drawCircle(c, c, radius - ring / 2f, stroke)

        val inner = (radius - ring) * 1.55f
        val scale = minOf(inner / cropped.width, inner / cropped.height)
        val w = (cropped.width * scale).toInt().coerceAtLeast(1)
        val h = (cropped.height * scale).toInt().coerceAtLeast(1)
        val scaled = android.graphics.Bitmap.createScaledBitmap(cropped, w, h, true)
        canvas.drawBitmap(scaled, c - w / 2f, c - h / 2f, null)
        BitmapDescriptorFactory.fromBitmap(out)
    } catch (_: Exception) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
    }
}

@Composable
fun TrackingBottomSheetContent(orderId: String, eta: Int?, history: List<OrderStatusStep>, profile: FulfillerPublicProfile?, tokenManager: TokenManager, onCelebration: () -> Unit = {}, onRefresh: () -> Unit, liveTick: Int = 0, onLeave: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    
    var orderDetails by remember { mutableStateOf<OrderDetailsResponse?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var isActionLoading by remember { mutableStateOf(false) }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showFulfillerProfile by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // v4.5 fix (#5b): this effect was only keyed on orderId/refreshKey, and
    // refreshKey is never incremented anywhere, so the DELIVERED/unrated
    // check below only ran once, on first composition. If the delivery
    // completed in real time while the customer was already sitting on this
    // screen (the normal case -- the parent TrackOrderScreen DOES get a live
    // "status_updated" socket event and refreshes its `history` state), this
    // effect never re-ran, so the review popup silently never appeared.
    // Keying on `history` (passed down from the parent, refreshed on every
    // socket status update) makes this re-check fire every time the mission
    // status changes, including on delivery completion.
    // Keeps the status bar, codes and buttons current without leaving the screen.
    LaunchedEffect(orderId) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            val st = orderDetails?.status?.uppercase()
            if (st == "DELIVERED" || st == "RELEASED" || st == "CANCELLED") break
            refreshKey++
        }
    }

    LaunchedEffect(orderId, refreshKey, history, liveTick) {
        try {
            val prevStatus = orderDetails?.status
            val res = apiService.getOrderDetails(orderId)
            val data = res.data ?: res
            orderDetails = data
            
            // Celebration on Completion (v4.6)
            if ((data.status == "DELIVERED" || data.status == "RELEASED") && 
                prevStatus != null && prevStatus != "DELIVERED" && prevStatus != "RELEASED") {
                onCelebration()
            }
            
            // Auto-show rating if delivered and not yet rated
            if ((data.status == "DELIVERED" || data.status == "RELEASED") && data.customer_rating == null) {
                showRatingDialog = true
            }
        } catch (e: Exception) {
            android.util.Log.e("TrackOrder", "Fetch details error: ${e.message}", e)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, CircleShape).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            
            val currentStatus = orderDetails?.status?.uppercase() ?: ""
            val isPendingConfirmation = currentStatus == "DELIVERED_PENDING_CONFIRMATION"

            if (currentStatus == "PENDING_ACKNOWLEDGMENT") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Receiver Not Responding", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("You can choose to proceed with dispatch anyway or abort this request.", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            apiService.resolveTimeout(orderId, mapOf("choice" to "proceed"))
                                            android.widget.Toast.makeText(context, "Proceeding with dispatch...", android.widget.Toast.LENGTH_SHORT).show()
                                            onRefresh()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, ErrorUtils.parseError(e), android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Proceed", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            apiService.resolveTimeout(orderId, mapOf("choice" to "cancel"))
                                            android.widget.Toast.makeText(context, "Mission Aborted", android.widget.Toast.LENGTH_SHORT).show()
                                            onRefresh()
                                            onLeave()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, ErrorUtils.parseError(e), android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Abort", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = if (eta != null) "Arriving in $eta mins" else "Status: ${currentStatus.replace('_', ' ')}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Order #$orderId", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Order ID", orderId))
                            android.widget.Toast.makeText(context, "Order ID copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            MissionProgressBar(currentStatus)

            if (isPendingConfirmation) {
                Spacer(modifier = Modifier.height(16.dp))
                // ... (existing code for pending confirmation)
            }

            // Pickup & Delivery Codes for Customer
            if (orderDetails?.pickup_code != null || orderDetails?.delivery_code != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Confirmation Codes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Share these with the agent at handoff.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val pCode = orderDetails?.pickup_code
                            if (pCode != null) {
                                CodeBox("Pickup", pCode, Modifier.weight(1f))
                            }
                            val dCode = orderDetails?.delivery_code
                            if (dCode != null) {
                                CodeBox("Delivery", dCode, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if ((currentStatus == "DELIVERED" || currentStatus == "RELEASED") && orderDetails?.customer_rating == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showRatingDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
                ) {
                    Icon(Icons.Default.Star, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rate Delivery Experience")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            profile?.let { 
                Box(modifier = Modifier.clickable { showFulfillerProfile = true }) {
                    FulfillerCard(it)
                }
                Spacer(modifier = Modifier.height(16.dp)) 
            }
            
            if (showFulfillerProfile && profile != null) {
                FulfillerProfileDialog(profile = profile!!, onDismiss = { showFulfillerProfile = false })
            }

            if (showDisputeDialog) {
                SecurePayDisputeDialog(
                    onDismiss = { showDisputeDialog = false },
                    onConfirm = { reason, notes, category, severity ->
                        coroutineScope.launch {
                            try {
                                apiService.reportProblem(orderId, mapOf(
                                    "reason" to reason, 
                                    "notes" to notes,
                                    "category" to category,
                                    "severity" to severity
                                ))
                                android.widget.Toast.makeText(context, "Issue Reported. Support will investigate.", android.widget.Toast.LENGTH_LONG).show()
                                showDisputeDialog = false
                                onRefresh()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Report failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            if (showRatingDialog) {
                FulfillerRatingDialog(
                    onDismiss = { showRatingDialog = false },
                    onConfirm = { rating, comment ->
                        coroutineScope.launch {
                            try {
                                apiService.rateFulfiller(orderId, RatingRequest(rating, comment))
                                android.widget.Toast.makeText(context, "Thank you for your rating!", android.widget.Toast.LENGTH_SHORT).show()
                                showRatingDialog = false
                                onRefresh()
                            } catch (e: Exception) {
                                val errorMsg = com.ng.pikop.core.network.ErrorUtils.parseError(e)
                                android.widget.Toast.makeText(context, "Rating failed: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            val isMatched = history.any { it.status == "MATCHED" || it.status == "ACCEPTED" }
            val hasPickedUp = history.any { it.status == "PICKED_UP" || it.status == "IN_TRANSIT" || it.status == "ARRIVED_AT_DELIVERY" }
            val isFinal = history.any { it.status == "DELIVERED" || it.status == "CANCELLED" || it.status == "RECIPIENT_ABSENT" || it.status == "RELEASED" }
            
            val canCancel = !hasPickedUp && !isFinal
            
            if (canCancel) {
                val scope = rememberCoroutineScope()
                val apiService = remember { ApiService.create(tokenManager) }
                var showCancelConfirm by remember { mutableStateOf(false) }
                
                OutlinedButton(
                    onClick = { showCancelConfirm = true }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) { 
                    Text("Cancel Delivery") 
                }
                
                if (showCancelConfirm) {
                    AlertDialog(
                        onDismissRequest = { showCancelConfirm = false }, 
                        title = { Text("Abort Mission?") }, 
                        text = { 
                            Text(
                                if (isMatched) "An agent is already matched. Cancelling now will incur a 25% penalty fee. Proceed?"
                                else "Are you sure you want to cancel this delivery request?"
                            ) 
                        }, 
                        confirmButton = { 
                            Button(
                                onClick = { 
                                    coroutineScope.launch { 
                                        isActionLoading = true
                                        try { 
                                            val response = apiService.cancelOrder(orderId, mapOf("reason" to "User requested cancellation"))
                                            android.widget.Toast.makeText(context, response.message ?: "Mission Aborted", android.widget.Toast.LENGTH_LONG).show()
                                            onRefresh() 
                                            showCancelConfirm = false 
                                            onLeave()
                                        } catch (e: Exception) {
                                            val errorMsg = com.ng.pikop.core.network.ErrorUtils.parseError(e)
                                            android.util.Log.e("TrackOrder", "Cancel failed: $errorMsg", e)
                                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                                        } finally { isActionLoading = false }
                                    }
                                }, 
                                enabled = !isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { 
                                if (isActionLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                else Text("Confirm Abort") 
                            } 
                        }, 
                        dismissButton = { 
                            TextButton(onClick = { showCancelConfirm = false }) { 
                                Text("Keep Order") 
                            } 
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(text = "Delivery Progress", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            if (history.isEmpty()) { Text("No updates yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) } 
            else { LazyColumn(modifier = Modifier.height(300.dp)) { items(history) { step -> TimelineItem(step) } } }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FulfillerRatingDialog(onDismiss: () -> Unit, onConfirm: (Int, String?) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your Experience") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("How was your delivery agent?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { i ->
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (i <= rating) Color(0xFFFF9F0A) else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Add a comment (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rating, comment.ifBlank { null }) }) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}

@Composable
fun SecurePayDisputeDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var reason by remember { mutableStateOf("Item Damaged") }
    var notes by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("MEDIUM") }
    val reasons = listOf("Item Damaged", "Wrong Item", "Not as Described", "Delivery Issue")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Item Issue", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Severity", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = severity == "LOW", onClick = { severity = "LOW" }, label = { Text("Low") })
                        FilterChip(selected = severity == "MEDIUM", onClick = { severity = "MEDIUM" }, label = { Text("Medium") })
                        FilterChip(selected = severity == "HIGH", onClick = { severity = "HIGH" }, label = { Text("High") })
                    }
                }

                Column {
                    Text("What is the issue?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    reasons.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = reason == r, onClick = { reason = r })
                            Text(r, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes, 
                    onValueChange = { notes = it }, 
                    label = { Text("Details / Evidence Description") }, 
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason, notes, reason.uppercase().replace(" ", "_"), severity) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Submit Dispute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FulfillerCard(profile: FulfillerPublicProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Profile Photo or Initial
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    if (!profile.profile_photo_url.isNullOrBlank()) {
                        // Use a fallback for the photo if needed, or just initial for now
                        Text((profile.full_name ?: "A").take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    } else {
                        Text((profile.full_name ?: "A").take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = profile.full_name ?: "Agent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (profile.kyc_status == "VERIFIED") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF008751).copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF008751).copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                // v4.5 fix: this was pikop_badge (the general
                                // app logomark, not a verification symbol) --
                                // using the real branded "VERIFIED" badge here instead.
                                Image(
                                    painter = painterResource(id = com.ng.pikop.R.drawable.pikop_verified_badge),
                                    contentDescription = "Verified Agent",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "VERIFIED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF008751)
                                )
                            }
                        }
                    }
                }
                
                // Tier Badge
                val tierInfo = when(profile.tier?.lowercase()) {
                    "gold" -> Pair("GOLD AGENT", Color(0xFFFFD700))
                    "silver" -> Pair("SILVER AGENT", Color(0xFFC0C0C0))
                    else -> Pair("BRONZE AGENT", Color(0xFFCD7F32))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp), tint = tierInfo.second)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tierInfo.first, style = MaterialTheme.typography.labelSmall, color = tierInfo.second, fontWeight = FontWeight.Bold)
                }

                // Vehicle Details (Conditional)
                if (profile.primary_class?.lowercase() != "agent") {
                    profile.vehicle_registration_number?.let { reg ->
                        Text(
                            text = "${profile.make ?: ""} • $reg", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF9F0A))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = String.format("%.1f", profile.rating_avg ?: 5.0), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) 
                }
                Text(text = "${profile.rating_count ?: 0} reviews", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

// v4.5 fix (#5): Visual progress bar for the customer tracking screen, driven by
// the mission status machine. ETA already existed as text; this adds the bar.
private val MISSION_PROGRESS_STAGES = listOf(
    listOf("SEARCHING", "PAYMENT_PENDING", "PAYMENT_CAPTURED", "PENDING", "PENDING_ACKNOWLEDGMENT", "PROCESSING", "SCHEDULED"),
    listOf("MATCHED", "QUEUED"),
    listOf("ARRIVED_AT_PICKUP"),
    listOf("PICKED_UP", "IN_TRANSIT"),
    listOf("ARRIVED_AT_DELIVERY"),
    listOf("DELIVERED", "DELIVERED_PENDING_CONFIRMATION", "RELEASED", "CONFIRMED")
)
private val MISSION_PROGRESS_LABELS = listOf(
    "Order Placed", "Agent Assigned", "At Pickup", "In Transit", "At Destination", "Delivered"
)

@Composable
fun MissionProgressBar(currentStatus: String) {
    val stageIndex = MISSION_PROGRESS_STAGES.indexOfFirst { currentStatus in it }
    if (stageIndex < 0) return // Unknown/terminal (CANCELLED etc.) status: nothing meaningful to show.

    val totalStages = MISSION_PROGRESS_STAGES.size

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MISSION_PROGRESS_LABELS.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            color = if (i <= stageIndex) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = MISSION_PROGRESS_LABELS.getOrElse(stageIndex) { "" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TimelineItem(step: OrderStatusStep) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(12.dp).background(if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
            Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color.Gray))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = step.status.replace('_', ' '), style = MaterialTheme.typography.bodyLarge, fontWeight = if (step.isCompleted) FontWeight.Bold else FontWeight.Normal, color = if (step.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Text(text = step.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            }
            Text(text = step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun CodeBox(label: String, code: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Pikop Code", code)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "$label code copied!", android.widget.Toast.LENGTH_SHORT).show()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        }
    }
}

fun calculateDistance(start: LatLng, end: LatLng): Double {
    val r = 6371; val dLat = Math.toRadians(end.latitude - start.latitude); val dLon = Math.toRadians(end.longitude - start.longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun formatTime(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoTimestamp!!)
        java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(date!!)
    } catch (e: Exception) { "" }
}

fun formatCountdown(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return "--:--"
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val target = parser.parse(isoTimestamp!!)?.time ?: return "--:--"
        val diff = target - System.currentTimeMillis()
        if (diff <= 0) return "Releasing now..."
        
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        "${hours}h ${minutes}m"
    } catch (e: Exception) { "--:--" }
}
