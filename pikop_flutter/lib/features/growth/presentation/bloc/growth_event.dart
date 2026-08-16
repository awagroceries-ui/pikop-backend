part of 'growth_bloc.dart';

abstract class GrowthEvent extends Equatable {
  const GrowthEvent();
  @override
  List<Object?> get props => [];
}

class GrowthStatsRequested extends GrowthEvent {}
