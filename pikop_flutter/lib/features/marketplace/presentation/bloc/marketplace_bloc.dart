import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/marketplace_repository.dart';

part 'marketplace_event.dart';
part 'marketplace_state.dart';

class MarketplaceBloc extends Bloc<MarketplaceEvent, MarketplaceState> {
  final MarketplaceRepository marketplaceRepository;

  MarketplaceBloc({required this.marketplaceRepository}) : super(MarketplaceInitial()) {
    on<MarketplaceProductsRequested>(_onMarketplaceProductsRequested);
    on<VendorRegistrationRequested>(_onVendorRegistrationRequested);
  }

  Future<void> _onMarketplaceProductsRequested(
    MarketplaceProductsRequested event,
    Emitter<MarketplaceState> emit,
  ) async {
    emit(MarketplaceLoading());
    try {
      final response = await marketplaceRepository.getProducts(
        category: event.category,
        city: event.city,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(MarketplaceLoaded(data['data']));
      } else {
        emit(MarketplaceFailure(data['message'] ?? 'Failed to load products'));
      }
    } catch (e) {
      emit(MarketplaceFailure(e.toString()));
    }
  }

  Future<void> _onVendorRegistrationRequested(
    VendorRegistrationRequested event,
    Emitter<MarketplaceState> emit,
  ) async {
    emit(MarketplaceLoading());
    try {
      final response = await marketplaceRepository.registerVendor(
        businessName: event.businessName,
        cacNumber: event.cacNumber,
        email: event.email,
        city: event.city,
        pickupAddressId: event.pickupAddressId,
        description: event.description,
        bankAccountName: event.bankAccountName,
        bankAccountNumber: event.bankAccountNumber,
        bankCode: event.bankCode,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(VendorRegistrationSuccess(data['message']));
      } else {
        emit(MarketplaceFailure(data['message'] ?? 'Registration failed'));
      }
    } catch (e) {
      emit(MarketplaceFailure(e.toString()));
    }
  }
}
