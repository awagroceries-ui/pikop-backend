part of 'fulfiller_bloc.dart';

abstract class FulfillerState extends Equatable {
  const FulfillerState();
  @override
  List<Object?> get props => [];
}

class FulfillerInitial extends FulfillerState {}

class KycLoading extends FulfillerState {}

class KycSessionReady extends FulfillerState {
  final Map<String, dynamic> sessionData;
  const KycSessionReady(this.sessionData);
  @override
  List<Object?> get props => [sessionData];
}

class DocumentUploadSuccess extends FulfillerState {
  final String type;
  const DocumentUploadSuccess(this.type);
  @override
  List<Object?> get props => [type];
}

class KycFailure extends FulfillerState {
  final String message;
  const KycFailure(this.message);
  @override
  List<Object?> get props => [message];
}
