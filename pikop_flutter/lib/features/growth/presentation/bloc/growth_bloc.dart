import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/growth_repository.dart';

part 'growth_event.dart';
part 'growth_state.dart';

class GrowthBloc extends Bloc<GrowthEvent, GrowthState> {
  final GrowthRepository growthRepository;

  GrowthBloc({required this.growthRepository}) : super(GrowthInitial()) {
    on<GrowthStatsRequested>(_onStatsRequested);
  }

  Future<void> _onStatsRequested(GrowthStatsRequested event, Emitter<GrowthState> emit) async {
    emit(GrowthLoading());
    try {
      final response = await growthRepository.getStats();
      final data = response.data;
      if (data['success'] == true) {
        emit(GrowthStatsLoaded(data['data']));
      } else {
        emit(GrowthFailure(data['message'] ?? 'Failed to load stats'));
      }
    } catch (e) {
      emit(GrowthFailure(e.toString()));
    }
  }
}
