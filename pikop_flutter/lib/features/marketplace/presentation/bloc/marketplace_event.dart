part of 'marketplace_bloc.dart';

abstract class MarketplaceEvent extends Equatable {
  const MarketplaceEvent();
  @override
  List<Object?> get props => [];
}

class MarketplaceProductsRequested extends MarketplaceEvent {
  final String? category;
  final String? city;
  const MarketplaceProductsRequested({this.category, this.city});
  @override
  List<Object?> get props => [category, city];
}

class VendorRegistrationRequested extends MarketplaceEvent {
  final String businessName;
  final String cacNumber;
  final String email;
  final String city;
  final int pickupAddressId;
  final String? description;
  final String bankAccountName;
  final String bankAccountNumber;
  final String bankCode;

  const VendorRegistrationRequested({
    required this.businessName,
    required this.cacNumber,
    required this.email,
    required this.city,
    required this.pickupAddressId,
    this.description,
    required this.bankAccountName,
    required this.bankAccountNumber,
    required this.bankCode,
  });

  @override
  List<Object?> get props => [businessName, cacNumber, email, city, pickupAddressId, description, bankAccountName, bankAccountNumber, bankCode];
}
