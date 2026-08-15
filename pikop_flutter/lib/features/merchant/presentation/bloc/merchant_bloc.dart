import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/merchant_repository.dart';

part 'merchant_event.dart';
part 'merchant_state.dart';

class MerchantBloc extends Bloc<MerchantEvent, MerchantState> {
  final MerchantRepository merchantRepository;

  MerchantBloc({required this.merchantRepository}) : super(MerchantInitial()) {
    on<MerchantRegistrationRequested>(_onRegistrationRequested);
    on<BulkUploadRequested>(_onBulkUploadRequested);
  }

  Future<void> _onRegistrationRequested(MerchantRegistrationRequested event, Emitter<MerchantState> emit) async {
    emit(MerchantLoading());
    try {
      final response = await merchantRepository.registerMerchant(
        businessName: event.businessName,
        email: event.email,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(MerchantRegistrationSuccess(data['data']['api_key']));
      } else {
        emit(MerchantFailure(data['message'] ?? 'Registration failed'));
      }
    } catch (e) {
      emit(MerchantFailure(e.toString()));
    }
  }

  Future<void> _onBulkUploadRequested(BulkUploadRequested event, Emitter<MerchantState> emit) async {
    emit(MerchantLoading());
    try {
      final response = await merchantRepository.bulkUpload(event.apiKey, event.orders);
      final data = response.data;
      if (data['success'] == true) {
        emit(BulkUploadSuccess(data['data']['batch_id']));
      } else {
        emit(MerchantFailure(data['message'] ?? 'Upload failed'));
      }
    } catch (e) {
      emit(MerchantFailure(e.toString()));
    }
  }
}
