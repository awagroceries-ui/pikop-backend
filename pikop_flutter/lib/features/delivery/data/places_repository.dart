import 'package:dio/dio.dart';

class PlacesRepository {
  final Dio _dio = Dio();
  final String apiKey = "AIzaSyDEsNglOB5t0J-D_yfMciy3Yrzj4B5ZzoQ";

  Future<List<dynamic>> getAutocomplete(String input) async {
    if (input.isEmpty) return [];

    try {
      final response = await _dio.get(
        'https://maps.googleapis.com/maps/api/place/autocomplete/json',
        queryParameters: {
          'input': input,
          'key': apiKey,
          'components': 'country:ng', // Restrict to Nigeria
        },
      );

      if (response.data['status'] == 'OK') {
        return response.data['predictions'];
      }
    } catch (e) {
      print('[Places] Autocomplete Error: $e');
    }
    return [];
  }

  Future<Map<String, double>?> getPlaceCoordinates(String placeId) async {
    try {
      final response = await _dio.get(
        'https://maps.googleapis.com/maps/api/place/details/json',
        queryParameters: {
          'place_id': placeId,
          'fields': 'geometry',
          'key': apiKey,
        },
      );

      if (response.data['status'] == 'OK') {
        final location = response.data['result']['geometry']['location'];
        return {
          'lat': location['lat'] as double,
          'lng': location['lng'] as double,
        };
      }
    } catch (e) {
      print('[Places] Details Error: $e');
    }
    return null;
  }
}
