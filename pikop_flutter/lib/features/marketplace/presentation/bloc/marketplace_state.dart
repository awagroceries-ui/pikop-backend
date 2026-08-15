part of 'marketplace_bloc.dart';

abstract class MarketplaceState extends Equatable {
  const MarketplaceState();
  @override
  List<Object?> get props => [];
}

class MarketplaceInitial extends MarketplaceState {}

class MarketplaceLoading extends MarketplaceState {}

class MarketplaceLoaded extends MarketplaceState {
  final List<dynamic> products;
  const MarketplaceLoaded(this.products);
  @override
  List<Object?> get props => [products];
}

class VendorRegistrationSuccess extends MarketplaceState {
  final String message;
  const VendorRegistrationSuccess(this.message);
  @override
  List<Object?> get props => [message];
}

class MarketplaceFailure extends MarketplaceState {
  final String message;
  const MarketplaceFailure(this.message);
  @override
  List<Object?> get props => [message];
}
