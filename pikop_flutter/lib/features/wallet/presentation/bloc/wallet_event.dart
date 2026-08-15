part of 'wallet_bloc.dart';

abstract class WalletEvent extends Equatable {
  const WalletEvent();
  @override
  List<Object?> get props => [];
}

class WalletInfoRequested extends WalletEvent {}

class WithdrawalRequested extends WalletEvent {
  final double amount;
  const WithdrawalRequested(this.amount);
  @override
  List<Object?> get props => [amount];
}
