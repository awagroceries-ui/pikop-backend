import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class AuthRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> login(String email, String password) async {
    return await _apiClient.instance.post('/auth/login', data: {
      'email': email,
      'password': password,
    });
  }

  Future<Response> signup({
    required String fullName,
    required String email,
    required String phone,
    required String password,
    String role = 'CUSTOMER',
  }) async {
    return await _apiClient.instance.post('/auth/signup', data: {
      'full_name': fullName,
      'email': email,
      'phone': phone,
      'password': password,
      'role': role,
    });
  }

  Future<Response> verifyEmail(String email, String otp) async {
    return await _apiClient.instance.post('/auth/verify-email', data: {
      'email': email,
      'otp': otp,
    });
  }
}
