part of 'support_bloc.dart';

abstract class SupportState extends Equatable {
  const SupportState();
  @override
  List<Object?> get props => [];
}

class SupportInitial extends SupportState {}

class SupportLoading extends SupportState {}

class KnowledgeBaseLoaded extends SupportState {
  final List<Map<String, dynamic>> articles;
  const KnowledgeBaseLoaded(this.articles);
  @override
  List<Object?> get props => [articles];
}

class SupportSessionReady extends SupportState {
  final String conversationId;
  const SupportSessionReady(this.conversationId);
  @override
  List<Object?> get props => [conversationId];
}

class SupportFailure extends SupportState {
  final String message;
  const SupportFailure(this.message);
  @override
  List<Object?> get props => [message];
}
