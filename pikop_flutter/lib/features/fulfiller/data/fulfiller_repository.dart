import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class FulfillerRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> startKYC() async {
    return await _apiClient.instance.post('/fulfillers/kyc/start');
  }

  Future<Response> uploadDocument({
    required String type,
    required String filePath,
    String? expiryDate,
  }) async {
    final formData = FormData.fromMap({
      'doc_type': type,
      'expiry_date': expiryDate,
      'file': await MultipartFile.fromFile(filePath),
    });

    return await _apiClient.instance.post(
      '/fulfillers/kyc/document',
      data: formData,
    );
  }
}
