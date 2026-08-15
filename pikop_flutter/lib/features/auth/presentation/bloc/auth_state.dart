part of 'auth_bloc.dart';

abstract class AuthState extends Equatable {
  const AuthState();
  @override
  List<Object?> get props => [];
}

class AuthInitial extends AuthState {}

class AuthLoading extends AuthState {}

class AuthSuccess extends AuthState {
  final Map<String, dynamic> userData;
  const AuthSuccess(this.userData);
  @override
  List<Object?> get props => [userData];
}

class AuthUnverified extends AuthState {
  final String email;
  final String role;
  const AuthUnverified(this.email, this.role);
  @override
  List<Object?> get props => [email, role];
}

class AuthFailure extends AuthState {
  final String message;
  const AuthFailure(this.message);
  @override
  List<Object?> get props => [message];
}

class Unauthenticated extends AuthState {}
