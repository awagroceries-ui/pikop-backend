part of 'wallet_bloc.dart';

abstract class WalletState extends Equatable {
  const WalletState();
  @override
  List<Object?> get props => [];
}

class WalletInitial extends WalletState {}

class WalletLoading extends WalletState {}

class WalletLoaded extends WalletState {
  final double balance;
  final String currency;
  final List<dynamic> transactions;
  const WalletLoaded({required this.balance, required this.currency, required this.transactions});
  @override
  List<Object?> get props => [balance, currency, transactions];
}

class WithdrawalSuccess extends WalletState {}

class WalletFailure extends WalletState {
  final String message;
  const WalletFailure(this.message);
  @override
  List<Object?> get props => [message];
}
