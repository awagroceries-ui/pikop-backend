import 'package:dio/dio.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

class DirectionsRepository {
  final Dio _dio = Dio();
  final String apiKey = "AIzaSyDEsNglOB5t0J-D_yfMciy3Yrzj4B5ZzoQ";

  Future<Map<String, dynamic>?> getDirections({
    required LatLng origin,
    required LatLng destination,
  }) async {
    try {
      final response = await _dio.get(
        'https://maps.googleapis.com/maps/api/directions/json',
        queryParameters: {
          'origin': '${origin.latitude},${origin.longitude}',
          'destination': '${destination.latitude},${destination.longitude}',
          'key': apiKey,
        },
      );

      if (response.data['status'] == 'OK') {
        final route = response.data['routes'][0];
        return {
          'polyline_points': route['overview_polyline']['points'],
          'duration': route['legs'][0]['duration']['text'],
          'distance': route['legs'][0]['distance']['text'],
        };
      }
    } catch (e) {
      print('[Directions] Error: $e');
    }
    return null;
  }
}
