import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/delivery_repository.dart';

part 'delivery_event.dart';
part 'delivery_state.dart';

class DeliveryBloc extends Bloc<DeliveryEvent, DeliveryState> {
  final DeliveryRepository deliveryRepository;

  DeliveryBloc({required this.deliveryRepository}) : super(DeliveryInitial()) {
    on<QuoteRequested>(_onQuoteRequested);
    on<PaymentInitialized>(_onPaymentInitialized);
  }

  Future<void> _onQuoteRequested(QuoteRequested event, Emitter<DeliveryState> emit) async {
    emit(DeliveryLoading());
    try {
      final response = await deliveryRepository.getQuote(
        pickupAddress: event.pickupAddress,
        deliveryAddress: event.deliveryAddress,
        itemDescription: event.itemDescription,
        pickupLat: event.pickupLat,
        pickupLng: event.pickupLng,
        deliveryLat: event.deliveryLat,
        deliveryLng: event.deliveryLng,
      );

      final data = response.data;
      if (data['success'] == true) {
        emit(QuoteSuccess(data['data']));
      } else {
        emit(DeliveryFailure(data['message'] ?? 'Failed to get quote'));
      }
    } catch (e) {
      emit(DeliveryFailure(e.toString()));
    }
  }

  Future<void> _onPaymentInitialized(PaymentInitialized event, Emitter<DeliveryState> emit) async {
    emit(DeliveryLoading());
    try {
      final response = await deliveryRepository.initializePayment(
        quoteId: event.quoteId,
        amount: event.amount,
        email: event.email,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(PaymentInitSuccess(data['data']['authorization_url']));
      } else {
        emit(DeliveryFailure(data['message'] ?? 'Payment init failed'));
      }
    } catch (e) {
      emit(DeliveryFailure(e.toString()));
    }
  }
}
