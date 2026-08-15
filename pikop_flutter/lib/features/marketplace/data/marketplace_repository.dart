import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class MarketplaceRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> getProducts({String? category, String? city}) async {
    return await _apiClient.instance.get('/marketplace', queryParameters: {
      if (category != null) 'category': category,
      if (city != null) 'city': city,
    });
  }

  Future<Response> getVendorDetails(String vendorId) async {
    return await _apiClient.instance.get('/marketplace/vendors/$vendorId');
  }

  Future<Response> registerVendor({
    required String businessName,
    required String cacNumber,
    required String email,
    required String city,
    required int pickupAddressId,
    String? description,
    required String bankAccountName,
    required String bankAccountNumber,
    required String bankCode,
  }) async {
    return await _apiClient.instance.post('/marketplace/vendors/register', data: {
      'business_name': businessName,
      'cac_number': cacNumber,
      'contact_email': email,
      'city': city,
      'pickup_address_id': pickupAddressId,
      'description': description,
      'bank_account_name': bankAccountName,
      'bank_account_number': bankAccountNumber,
      'bank_code': bankCode,
    });
  }

  Future<Response> addProduct({
    required String vendorId,
    required String name,
    required double price,
    required int stockQuantity,
    String? description,
    String? category,
    String? unit,
    String? nafdacNumber,
    String? photoUrl,
  }) async {
    return await _apiClient.instance.post('/marketplace/products', data: {
      'vendor_id': vendorId,
      'name': name,
      'price': price,
      'stock_quantity': stockQuantity,
      'description': description,
      'category': category,
      'unit': unit,
      'nafdac_number': nafdacNumber,
      'photo_url': photoUrl,
    });
  }
}
