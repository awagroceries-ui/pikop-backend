part of 'growth_bloc.dart';

abstract class GrowthState extends Equatable {
  const GrowthState();
  @override
  List<Object?> get props => [];
}

class GrowthInitial extends GrowthState {}

class GrowthLoading extends GrowthState {}

class GrowthStatsLoaded extends GrowthState {
  final Map<String, dynamic> stats;
  const GrowthStatsLoaded(this.stats);
  @override
  List<Object?> get props => [stats];
}

class GrowthFailure extends GrowthState {
  final String message;
  const GrowthFailure(this.message);
  @override
  List<Object?> get props => [message];
}
