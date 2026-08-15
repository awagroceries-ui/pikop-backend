part of 'foods_bloc.dart';

abstract class FoodsState extends Equatable {
  const FoodsState();
  @override
  List<Object?> get props => [];
}

class FoodsInitial extends FoodsState {}

class FoodsLoading extends FoodsState {}

class KitchensLoaded extends FoodsState {
  final List<dynamic> kitchens;
  const KitchensLoaded(this.kitchens);
  @override
  List<Object?> get props => [kitchens];
}

class KitchenRegistrationSuccess extends FoodsState {
  final String message;
  const KitchenRegistrationSuccess(this.message);
  @override
  List<Object?> get props => [message];
}

class FoodsFailure extends FoodsState {
  final String message;
  const FoodsFailure(this.message);
  @override
  List<Object?> get props => [message];
}
