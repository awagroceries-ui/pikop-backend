part of 'merchant_bloc.dart';

abstract class MerchantEvent extends Equatable {
  const MerchantEvent();
  @override
  List<Object?> get props => [];
}

class MerchantRegistrationRequested extends MerchantEvent {
  final String businessName;
  final String email;
  const MerchantRegistrationRequested({required this.businessName, required this.email});
  @override
  List<Object?> get props => [businessName, email];
}

class BulkUploadRequested extends MerchantEvent {
  final String apiKey;
  final List<Map<String, dynamic>> orders;
  const BulkUploadRequested({required this.apiKey, required this.orders});
  @override
  List<Object?> get props => [apiKey, orders];
}
