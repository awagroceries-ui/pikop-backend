part of 'chat_bloc.dart';

abstract class ChatEvent extends Equatable {
  const ChatEvent();
  @override
  List<Object?> get props => [];
}

class ChatHistoryRequested extends ChatEvent {
  final String conversationId;
  const ChatHistoryRequested(this.conversationId);
  @override
  List<Object?> get props => [conversationId];
}

class MessageSent extends ChatEvent {
  final String conversationId;
  final int senderId;
  final String senderType;
  final String content;

  const MessageSent({
    required this.conversationId,
    required this.senderId,
    required this.senderType,
    required this.content,
  });

  @override
  List<Object?> get props => [conversationId, senderId, senderType, content];
}

class MessageReceived extends ChatEvent {
  final Map<String, dynamic> message;
  const MessageReceived(this.message);
  @override
  List<Object?> get props => [message];
}
