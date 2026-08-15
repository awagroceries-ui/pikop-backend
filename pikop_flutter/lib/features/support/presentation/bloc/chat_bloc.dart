import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../../../core/services/socket_service.dart';
import '../../data/support_repository.dart';

part 'chat_event.dart';
part 'chat_state.dart';

class ChatBloc extends Bloc<ChatEvent, ChatState> {
  final SupportRepository supportRepository;
  final SocketService socketService;

  ChatBloc({
    required this.supportRepository,
    required this.socketService,
  }) : super(ChatInitial()) {
    on<ChatHistoryRequested>(_onChatHistoryRequested);
    on<MessageSent>(_onMessageSent);
    on<MessageReceived>(_onMessageReceived);

    // Socket Listener
    socketService.on('receive_message', (data) {
      add(MessageReceived(Map<String, dynamic>.from(data)));
    });
  }

  Future<void> _onChatHistoryRequested(ChatHistoryRequested event, Emitter<ChatState> emit) async {
    emit(ChatLoading());
    try {
      socketService.emit('join_support', event.conversationId);
      final response = await supportRepository.getMessages(event.conversationId);
      final messages = List<Map<String, dynamic>>.from(response.data);
      emit(ChatLoaded(messages));
    } catch (e) {
      emit(ChatFailure(e.toString()));
    }
  }

  void _onMessageSent(MessageSent event, Emitter<ChatState> emit) {
    socketService.emit('send_message', {
      'conversation_id': event.conversationId,
      'sender_id': event.senderId,
      'sender_type': event.senderType,
      'content': event.content,
    });
  }

  void _onMessageReceived(MessageReceived event, Emitter<ChatState> emit) {
    if (state is ChatLoaded) {
      final currentMessages = (state as ChatLoaded).messages;
      emit(ChatLoaded([...currentMessages, event.message]));
    }
  }
}
