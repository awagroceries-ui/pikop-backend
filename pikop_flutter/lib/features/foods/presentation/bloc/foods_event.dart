part of 'foods_bloc.dart';

abstract class FoodsEvent extends Equatable {
  const FoodsEvent();
  @override
  List<Object?> get props => [];
}

class KitchensRequested extends FoodsEvent {
  final String? cuisineType;
  final String? city;
  const KitchensRequested({this.cuisineType, this.city});
  @override
  List<Object?> get props => [cuisineType, city];
}

class KitchenRegistrationRequested extends FoodsEvent {
  final String businessName;
  final String cacNumber;
  final String email;
  final String city;
  final String cuisineType;
  final int pickupAddressId;
  final String? description;
  final Map<String, String> safetyDocs;
  final String bankAccountName;
  final String bankAccountNumber;
  final String bankCode;

  const KitchenRegistrationRequested({
    required this.businessName,
    required this.cacNumber,
    required this.email,
    required this.city,
    required this.cuisineType,
    required this.pickupAddressId,
    this.description,
    required this.safetyDocs,
    required this.bankAccountName,
    required this.bankAccountNumber,
    required this.bankCode,
  });

  @override
  List<Object?> get props => [businessName, cacNumber, email, city, cuisineType, pickupAddressId, description, safetyDocs, bankAccountName, bankAccountNumber, bankCode];
}
