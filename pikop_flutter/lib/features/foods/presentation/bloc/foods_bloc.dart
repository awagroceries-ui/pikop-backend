import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/kitchen_repository.dart';

part 'foods_event.dart';
part 'foods_state.dart';

class FoodsBloc extends Bloc<FoodsEvent, FoodsState> {
  final KitchenRepository kitchenRepository;

  FoodsBloc({required this.kitchenRepository}) : super(FoodsInitial()) {
    on<KitchensRequested>(_onKitchensRequested);
    on<KitchenRegistrationRequested>(_onKitchenRegistrationRequested);
  }

  Future<void> _onKitchensRequested(
    KitchensRequested event,
    Emitter<FoodsState> emit,
  ) async {
    emit(FoodsLoading());
    try {
      final response = await kitchenRepository.getKitchens(
        cuisineType: event.cuisineType,
        city: event.city,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(KitchensLoaded(data['data']));
      } else {
        emit(FoodsFailure(data['message'] ?? 'Failed to load kitchens'));
      }
    } catch (e) {
      emit(FoodsFailure(e.toString()));
    }
  }

  Future<void> _onKitchenRegistrationRequested(
    KitchenRegistrationRequested event,
    Emitter<FoodsState> emit,
  ) async {
    emit(FoodsLoading());
    try {
      final response = await kitchenRepository.registerKitchen(
        businessName: event.businessName,
        cacNumber: event.cacNumber,
        email: event.email,
        city: event.city,
        cuisineType: event.cuisineType,
        pickupAddressId: event.pickupAddressId,
        description: event.description,
        safetyDocs: event.safetyDocs,
        bankAccountName: event.bankAccountName,
        bankAccountNumber: event.bankAccountNumber,
        bankCode: event.bankCode,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(KitchenRegistrationSuccess(data['message']));
      } else {
        emit(FoodsFailure(data['message'] ?? 'Registration failed'));
      }
    } catch (e) {
      emit(FoodsFailure(e.toString()));
    }
  }
}
