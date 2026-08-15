part of 'delivery_bloc.dart';

abstract class DeliveryState extends Equatable {
  const DeliveryState();
  @override
  List<Object?> get props => [];
}

class DeliveryInitial extends DeliveryState {}

class DeliveryLoading extends DeliveryState {}

class QuoteSuccess extends DeliveryState {
  final Map<String, dynamic> quoteData;
  const QuoteSuccess(this.quoteData);
  @override
  List<Object?> get props => [quoteData];
}

class PaymentInitSuccess extends DeliveryState {
  final String checkoutUrl;
  const PaymentInitSuccess(this.checkoutUrl);
  @override
  List<Object?> get props => [checkoutUrl];
}

class DeliveryFailure extends DeliveryState {
  final String message;
  const DeliveryFailure(this.message);
  @override
  List<Object?> get props => [message];
}
