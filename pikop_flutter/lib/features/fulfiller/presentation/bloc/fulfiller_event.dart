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

class MissionOfferReceived extends FulfillerEvent {
  final Map<String, dynamic> offer;
  const MissionOfferReceived(this.offer);
  @override
  List<Object?> get props => [offer];
}

class MissionAccepted extends FulfillerEvent {
  final int missionId;
  const MissionAccepted(this.missionId);
  @override
  List<Object?> get props => [missionId];
}

class FulfillerStatusUpdated extends FulfillerEvent {
  final String status;
  final double? lat;
  final double? lng;
  const FulfillerStatusUpdated({required this.status, this.lat, this.lng});
  @override
  List<Object?> get props => [status, lat, lng];
}


