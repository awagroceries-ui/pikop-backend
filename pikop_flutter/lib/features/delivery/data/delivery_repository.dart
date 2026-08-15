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

  Future<Response> initializePayment({
    required String quoteId,
    required double amount,
    required String email,
  }) async {
    return await _apiClient.instance.post('/payments/initialize', data: {
      'quote_id': quoteId,
      'amount': amount,
      'email': email,
    });
  }
}
