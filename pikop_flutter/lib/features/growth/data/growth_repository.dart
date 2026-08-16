import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class GrowthRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> getStats() async {
    return await _apiClient.instance.get('/growth/stats');
  }

  Future<Response> validateCoupon(String code, double amount) async {
    return await _apiClient.instance.post('/growth/coupons/validate', data: {
      'code': code,
      'amount': amount,
    });
  }
}
