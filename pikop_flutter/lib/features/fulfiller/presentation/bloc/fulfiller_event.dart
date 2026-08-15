part of 'fulfiller_bloc.dart';

abstract class FulfillerEvent extends Equatable {
  const FulfillerEvent();
  @override
  List<Object?> get props => [];
}

class KycSessionStarted extends FulfillerEvent {}

class DocumentUploadRequested extends FulfillerEvent {
  final String type;
  final String filePath;
  final String? expiryDate;

  const DocumentUploadRequested({
    required this.type,
    required this.filePath,
    this.expiryDate,
  });

  @override
  List<Object?> get props => [type, filePath, expiryDate];
}
