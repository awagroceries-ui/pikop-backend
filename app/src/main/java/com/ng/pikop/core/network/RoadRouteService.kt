package com.ng.pikop.core.network

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

object RoadRouteService {

    private val routeCache = mutableMapOf<String, List<LatLng>>()

    /**
     * Fetches real road street routes using Google Directions API or OSRM routing engine.
     */
    suspend fun fetchRoadRoute(start: LatLng, end: LatLng, apiKey: String = ""): List<LatLng> {
        val cacheKey = "${"%.4f".format(start.latitude)},${"%.4f".format(start.longitude)}_${"%.4f".format(end.latitude)},${"%.4f".format(end.longitude)}"
        routeCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                // 1. Try OSRM Public Driving Routing Engine (Free, high-precision real road network)
                val osrmUrl = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=polyline"
                val conn = URL(osrmUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val stream = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(stream)
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).optString("geometry")
                        if (!geometry.isNullOrEmpty()) {
                            val decoded = decodePolyline(geometry)
                            if (decoded.size >= 2) {
                                routeCache[cacheKey] = decoded
                                return@withContext decoded
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("RoadRouteService", "OSRM fetch failed: ${e.message}")
            }

            // 2. Try Google Maps Directions API if API key provided
            if (apiKey.isNotBlank()) {
                try {
                    val googleUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}&key=$apiKey"
                    val conn = URL(googleUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    if (conn.responseCode == 200) {
                        val stream = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(stream)
                        val routes = json.optJSONArray("routes")
                        if (routes != null && routes.length() > 0) {
                            val points = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline")
                                .getString("points")
                            val decoded = decodePolyline(points)
                            if (decoded.size >= 2) {
                                routeCache[cacheKey] = decoded
                                return@withContext decoded
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RoadRouteService", "Google Directions fetch failed: ${e.message}")
                }
            }

            // 3. Fallback: Interpolated multi-segment road curve
            val fallback = createFallbackRoadPoints(start, end)
            routeCache[cacheKey] = fallback
            fallback
        }
    }

    /**
     * Decodes encoded Google Polyline strings into a list of LatLng coordinates.
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    /**
     * Interpolates exact coordinates at a progress fraction (0.0 to 1.0) along a multi-point polyline.
     */
    fun interpolatePointAlongPolyline(points: List<LatLng>, fraction: Float): LatLng {
        if (points.isEmpty()) return LatLng(0.0, 0.0)
        if (points.size == 1 || fraction <= 0.0f) return points.first()
        if (fraction >= 1.0f) return points.last()

        // Calculate total length of polyline
        var totalDistance = 0.0
        val segmentDistances = DoubleArray(points.size - 1)
        for (i in 0 until points.size - 1) {
            val dist = calculateDistanceMeters(points[i], points[i + 1])
            segmentDistances[i] = dist
            totalDistance += dist
        }

        if (totalDistance == 0.0) return points.first()

        val targetDistance = totalDistance * fraction.coerceIn(0.0f, 1.0f)
        var accumulated = 0.0

        for (i in 0 until points.size - 1) {
            val segDist = segmentDistances[i]
            if (accumulated + segDist >= targetDistance) {
                val segFraction = if (segDist == 0.0) 0.0 else (targetDistance - accumulated) / segDist
                val p1 = points[i]
                val p2 = points[i + 1]
                val lat = p1.latitude + (p2.latitude - p1.latitude) * segFraction
                val lng = p1.longitude + (p2.longitude - p1.longitude) * segFraction
                return LatLng(lat, lng)
            }
            accumulated += segDist
        }

        return points.last()
    }

    private fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Double {
        val r = 6371000.0
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLng = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun createFallbackRoadPoints(start: LatLng, end: LatLng): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(start)
        val numSegments = 8
        for (i in 1 until numSegments) {
            val f = i.toDouble() / numSegments
            val lat = start.latitude + (end.latitude - start.latitude) * f
            val lng = start.longitude + (end.longitude - start.longitude) * f
            val offsetLat = sin(f * Math.PI) * (end.longitude - start.longitude) * 0.08
            val offsetLng = cos(f * Math.PI) * (end.latitude - start.latitude) * -0.08
            points.add(LatLng(lat + offsetLat, lng + offsetLng))
        }
        points.add(end)
        return points
    }
}
