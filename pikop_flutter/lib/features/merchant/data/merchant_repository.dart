import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class MerchantRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> registerMerchant({
    required String businessName,
    required String email,
  }) async {
    return await _apiClient.instance.post('/merchants/register', data: {
      'business_name': businessName,
      'contact_email': email,
    });
  }

  Future<Response> bulkUpload(String apiKey, List<Map<String, dynamic>> orders) async {
    return await _apiClient.instance.post(
      '/merchants/orders/bulk',
      data: {'orders': orders},
      options: Options(headers: {'x-pikop-api-key': apiKey}),
    );
  }

  Future<Response> getBatches(String apiKey) async {
    return await _apiClient.instance.get(
      '/merchants/batches',
      options: Options(headers: {'x-pikop-api-key': apiKey}),
    );
  }
}
