import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class WalletRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> getWalletInfo() async {
    return await _apiClient.instance.get('/wallets/me');
  }

  Future<Response> requestWithdrawal(double amount) async {
    return await _apiClient.instance.post('/wallets/withdraw', data: {
      'amount': amount,
    });
  }
}
