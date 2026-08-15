part of 'delivery_bloc.dart';

abstract class DeliveryEvent extends Equatable {
  const DeliveryEvent();
  @override
  List<Object?> get props => [];
}

class QuoteRequested extends DeliveryEvent {
  final String pickupAddress;
  final String deliveryAddress;
  final String itemDescription;
  final double pickupLat;
  final double pickupLng;
  final double deliveryLat;
  final double deliveryLng;

  const QuoteRequested({
    required this.pickupAddress,
    required this.deliveryAddress,
    required this.itemDescription,
    required this.pickupLat,
    required this.pickupLng,
    required this.deliveryLat,
    required this.deliveryLng,
  });

  @override
  List<Object?> get props => [
        pickupAddress,
        deliveryAddress,
        itemDescription,
        pickupLat,
        pickupLng,
        deliveryLat,
        deliveryLng,
      ];
}

class PaymentInitialized extends DeliveryEvent {
  final String quoteId;
  final double amount;
  final String email;

  const PaymentInitialized({
    required this.quoteId,
    required this.amount,
    required this.email,
  });

  @override
  List<Object?> get props => [quoteId, amount, email];
}

