part of 'auth_bloc.dart';

abstract class AuthEvent extends Equatable {
  const AuthEvent();
  @override
  List<Object?> get props => [];
}

class LoginRequested extends AuthEvent {
  final String email;
  final String password;
  const LoginRequested(this.email, this.password);
  @override
  List<Object?> get props => [email, password];
}

class SignupRequested extends AuthEvent {
  final String fullName;
  final String email;
  final String phone;
  final String password;
  final String role;
  const SignupRequested({
    required this.fullName,
    required this.email,
    required this.phone,
    required this.password,
    required this.role,
  });
  @override
  List<Object?> get props => [fullName, email, phone, password, role];
}

class VerifyEmailRequested extends AuthEvent {
  final String email;
  final String otp;
  const VerifyEmailRequested(this.email, this.otp);
  @override
  List<Object?> get props => [email, otp];
}

class LogoutRequested extends AuthEvent {}
