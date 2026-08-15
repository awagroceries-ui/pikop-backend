import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/fulfiller_repository.dart';

part 'fulfiller_event.dart';
part 'fulfiller_state.dart';

class FulfillerBloc extends Bloc<FulfillerEvent, FulfillerState> {
  final FulfillerRepository fulfillerRepository;

  FulfillerBloc({required this.fulfillerRepository}) : super(FulfillerInitial()) {
    on<KycSessionStarted>(_onKycSessionStarted);
    on<DocumentUploadRequested>(_onDocumentUploadRequested);
  }

  Future<void> _onKycSessionStarted(KycSessionStarted event, Emitter<FulfillerState> emit) async {
    emit(KycLoading());
    try {
      final response = await fulfillerRepository.startKYC();
      final data = response.data;
      if (data['success'] == true) {
        emit(KycSessionReady(data['data']));
      } else {
        emit(KycFailure(data['message'] ?? 'Failed to start KYC'));
      }
    } catch (e) {
      emit(KycFailure(e.toString()));
    }
  }

  Future<void> _onDocumentUploadRequested(DocumentUploadRequested event, Emitter<FulfillerState> emit) async {
    emit(KycLoading());
    try {
      final response = await fulfillerRepository.uploadDocument(
        type: event.type,
        filePath: event.filePath,
        expiryDate: event.expiryDate,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(DocumentUploadSuccess(event.type));
      } else {
        emit(KycFailure(data['message'] ?? 'Upload failed'));
      }
    } catch (e) {
      emit(KycFailure(e.toString()));
    }
  }
}
