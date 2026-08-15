import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/wallet_repository.dart';

part 'wallet_event.dart';
part 'wallet_state.dart';

class WalletBloc extends Bloc<WalletEvent, WalletState> {
  final WalletRepository walletRepository;

  WalletBloc({required this.walletRepository}) : super(WalletInitial()) {
    on<WalletInfoRequested>(_onWalletInfoRequested);
    on<WithdrawalRequested>(_onWithdrawalRequested);
  }

  Future<void> _onWalletInfoRequested(WalletInfoRequested event, Emitter<WalletState> emit) async {
    emit(WalletLoading());
    try {
      final response = await walletRepository.getWalletInfo();
      final data = response.data;
      if (data['success'] == true) {
        final walletData = data['data'];
        emit(WalletLoaded(
          balance: (walletData['balance'] as num).toDouble(),
          currency: walletData['currency'],
          transactions: walletData['transactions'],
        ));
      } else {
        emit(WalletFailure(data['message'] ?? 'Failed to load wallet'));
      }
    } catch (e) {
      emit(WalletFailure(e.toString()));
    }
  }

  Future<void> _onWithdrawalRequested(WithdrawalRequested event, Emitter<WalletState> emit) async {
    emit(WalletLoading());
    try {
      final response = await walletRepository.requestWithdrawal(event.amount);
      final data = response.data;
      if (data['success'] == true) {
        emit(WithdrawalSuccess());
        add(WalletInfoRequested()); // Refresh
      } else {
        emit(WalletFailure(data['message'] ?? 'Withdrawal failed'));
      }
    } catch (e) {
      emit(WalletFailure(e.toString()));
    }
  }
}
