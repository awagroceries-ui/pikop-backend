import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class KitchenRepository {
  final ApiClient _apiClient = ApiClient();

  Future<Response> getKitchens({String? cuisineType, String? city}) async {
    return await _apiClient.instance.get('/kitchens', queryParameters: {
      if (cuisineType != null) 'cuisine_type': cuisineType,
      if (city != null) 'city': city,
    });
  }

  Future<Response> getKitchenDetails(String kitchenId) async {
    return await _apiClient.instance.get('/kitchens/$kitchenId');
  }

  Future<Response> registerKitchen({
    required String businessName,
    required String cacNumber,
    required String email,
    required String city,
    required String cuisineType,
    required int pickupAddressId,
    String? description,
    required Map<String, String> safetyDocs,
    required String bankAccountName,
    required String bankAccountNumber,
    required String bankCode,
  }) async {
    return await _apiClient.instance.post('/kitchens/register', data: {
      'business_name': businessName,
      'cac_number': cacNumber,
      'contact_email': email,
      'city': city,
      'cuisine_type': cuisineType,
      'pickup_address_id': pickupAddressId,
      'description': description,
      'state_food_safety_docs': safetyDocs,
      'bank_account_name': bankAccountName,
      'bank_account_number': bankAccountNumber,
      'bank_code': bankCode,
    });
  }

  Future<Response> addMenuItem({
    required String kitchenId,
    required String name,
    required double price,
    String? description,
    String? category,
    String? photoUrl,
    int? prepTime,
    List<Map<String, dynamic>>? modifiers,
  }) async {
    return await _apiClient.instance.post('/kitchens/menu-items', data: {
      'kitchen_id': kitchenId,
      'name': name,
      'price': price,
      'description': description,
      'category': category,
      'photo_url': photoUrl,
      'prep_time_minutes': prepTime,
      'modifiers': modifiers,
    });
  }
}
