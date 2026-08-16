import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class DeliveryRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> getQuote({
    required String pickupAddress,
    required String deliveryAddress,
    required String itemDescription,
    required double pickupLat,
    required double pickupLng,
    required double deliveryLat,
    required double deliveryLng,
  }) async {
    return await _apiClient.instance.post('/orders/quote', data: {
      'pickup_address': pickupAddress,
      'delivery_address': deliveryAddress,
      'item_description': itemDescription,
      'pickup_lat': pickupLat,
      'pickup_lng': pickupLng,
      'delivery_lat': deliveryLat,
      'delivery_lng': deliveryLng,
    });
  }

  Future<Response> updateStatus(int missionId, String status) async {
    return await _apiClient.instance.patch('/orders/$missionId/status', data: {
      'status': status,
    });
  }

  Future<Response> getOrderDetails(int missionId) async {
    return await _apiClient.instance.get('/orders/$missionId');
  }

  Future<Response> initiateReturn(int missionId) async {
    return await _apiClient.instance.post('/orders/$missionId/return');
  }
}
