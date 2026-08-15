import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/support_repository.dart';

part 'support_event.dart';
part 'support_state.dart';

class SupportBloc extends Bloc<SupportEvent, SupportState> {
  final SupportRepository supportRepository;

  SupportBloc({required this.supportRepository}) : super(SupportInitial()) {
    on<KnowledgeBaseRequested>(_onKnowledgeBaseRequested);
    on<SupportSessionRequested>(_onSupportSessionRequested);
  }

  Future<void> _onKnowledgeBaseRequested(KnowledgeBaseRequested event, Emitter<SupportState> emit) async {
    emit(SupportLoading());
    try {
      final response = await supportRepository.getKnowledgeBase();
      final data = response.data;
      if (data['success'] == true) {
        emit(KnowledgeBaseLoaded(List<Map<String, dynamic>>.from(data['data'])));
      } else {
        emit(SupportFailure(data['message'] ?? 'Failed to load help center'));
      }
    } catch (e) {
      emit(SupportFailure(e.toString()));
    }
  }

  Future<void> _onSupportSessionRequested(SupportSessionRequested event, Emitter<SupportState> emit) async {
    emit(SupportLoading());
    try {
      final response = await supportRepository.getOrCreateConversation();
      final data = response.data;
      if (data['success'] == true) {
        emit(SupportSessionReady(data['data']['id']));
      } else {
        emit(SupportFailure(data['message'] ?? 'Failed to start chat'));
      }
    } catch (e) {
      emit(SupportFailure(e.toString()));
    }
  }
}
