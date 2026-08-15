part of 'merchant_bloc.dart';

abstract class MerchantState extends Equatable {
  const MerchantState();
  @override
  List<Object?> get props => [];
}

class MerchantInitial extends MerchantState {}

class MerchantLoading extends MerchantState {}

class MerchantRegistrationSuccess extends MerchantState {
  final String apiKey;
  const MerchantRegistrationSuccess(this.apiKey);
  @override
  List<Object?> get props => [apiKey];
}

class BulkUploadSuccess extends MerchantState {
  final String batchId;
  const BulkUploadSuccess(this.batchId);
  @override
  List<Object?> get props => [batchId];
}

class MerchantFailure extends MerchantState {
  final String message;
  const MerchantFailure(this.message);
  @override
  List<Object?> get props => [message];
}
