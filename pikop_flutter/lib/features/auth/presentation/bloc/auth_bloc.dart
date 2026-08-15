import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../data/auth_repository.dart';

part 'auth_event.dart';
part 'auth_state.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final AuthRepository authRepository;
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  AuthBloc({required this.authRepository}) : super(AuthInitial()) {
    on<LoginRequested>(_onLoginRequested);
    on<SignupRequested>(_onSignupRequested);
    on<VerifyEmailRequested>(_onVerifyEmailRequested);
    on<LogoutRequested>(_onLogoutRequested);
  }

  Future<void> _onLoginRequested(LoginRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final response = await authRepository.login(event.email, event.password);
      final data = response.data;

      if (data['success'] == true) {
        final tokens = data['data'];
        await _storage.write(key: 'accessToken', value: tokens['accessToken']);
        await _storage.write(key: 'refreshToken', value: tokens['refreshToken']);
        emit(AuthSuccess(data['data']));
      } else {
        if (data['message'] == 'ACCOUNT_UNVERIFIED') {
          emit(AuthUnverified(event.email, 'CUSTOMER')); // Fallback role
        } else {
          emit(AuthFailure(data['message'] ?? 'Login failed'));
        }
      }
    } catch (e) {
      emit(AuthFailure(e.toString()));
    }
  }

  Future<void> _onSignupRequested(SignupRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final response = await authRepository.signup(
        fullName: event.fullName,
        email: event.email,
        phone: event.phone,
        password: event.password,
        role: event.role,
      );
      final data = response.data;
      if (data['success'] == true) {
        emit(AuthUnverified(event.email, event.role));
      } else {
        emit(AuthFailure(data['message'] ?? 'Registration failed'));
      }
    } catch (e) {
      emit(AuthFailure(e.toString()));
    }
  }

  Future<void> _onVerifyEmailRequested(VerifyEmailRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final response = await authRepository.verifyEmail(event.email, event.otp);
      final data = response.data;
      if (data['success'] == true) {
        final tokens = data['data'];
        await _storage.write(key: 'accessToken', value: tokens['accessToken']);
        await _storage.write(key: 'refreshToken', value: tokens['refreshToken']);
        emit(AuthSuccess(data['data']));
      } else {
        emit(AuthFailure(data['message'] ?? 'Verification failed'));
      }
    } catch (e) {
      emit(AuthFailure(e.toString()));
    }
  }

  Future<void> _onLogoutRequested(LogoutRequested event, Emitter<AuthState> emit) async {
    await _storage.delete(key: 'accessToken');
    await _storage.delete(key: 'refreshToken');
    emit(Unauthenticated());
  }
}
