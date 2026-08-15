import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../../../../core/services/socket_service.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../bloc/chat_bloc.dart';
import '../../data/support_repository.dart';

class SupportChatScreen extends StatelessWidget {
  final String conversationId;
  const SupportChatScreen({super.key, required this.conversationId});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => ChatBloc(
        supportRepository: context.read<SupportRepository>(),
        socketService: context.read<SocketService>(),
      )..add(ChatHistoryRequested(conversationId)),
      child: SupportChatView(conversationId: conversationId),
    );
  }
}

class SupportChatView extends StatefulWidget {
  final String conversationId;
  const SupportChatView({super.key, required this.conversationId});

  @override
  State<SupportChatView> createState() => _SupportChatViewState();
}

class _SupportChatViewState extends State<SupportChatView> {
  final _messageController = TextEditingController();
  final _scrollController = ScrollController();

  @override
  Widget build(BuildContext context) {
    final authState = context.read<AuthBloc>().state as AuthSuccess;
    final userId = authState.userData['user']['id'];
    final userRole = authState.userData['user']['role'];

    return Scaffold(
      appBar: AppBar(title: const Text('Live Support')),
      body: Column(
        children: [
          Expanded(
            child: BlocConsumer<ChatBloc, ChatState>(
              listener: (context, state) {
                if (state is ChatLoaded) {
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (_scrollController.hasClients) {
                      _scrollController.jumpTo(_scrollController.position.maxScrollExtent);
                    }
                  });
                }
              },
              builder: (context, state) {
                if (state is ChatLoading) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (state is ChatLoaded) {
                  return ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(16),
                    itemCount: state.messages.length,
                    itemBuilder: (context, index) {
                      final msg = state.messages[index];
                      final isMe = msg['sender_type'] != 'ADMIN';
                      return _buildMessageBubble(msg, isMe);
                    },
                  );
                }
                return const SizedBox.shrink();
              },
            ),
          ),
          _buildMessageInput(context, userId, userRole),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(Map<String, dynamic> msg, bool isMe) {
    return Align(
      alignment: isMe ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isMe ? PikopTheme.gold : Colors.white.withOpacity(0.05),
          borderRadius: BorderRadius.circular(16).copyWith(
            bottomRight: isMe ? const Radius.circular(0) : const Radius.circular(16),
            bottomLeft: isMe ? const Radius.circular(16) : const Radius.circular(0),
          ),
        ),
        child: Text(
          msg['content'] ?? '',
          style: TextStyle(color: isMe ? PikopTheme.black : Colors.white),
        ),
      ),
    );
  }

  Widget _buildMessageInput(BuildContext context, int userId, String userRole) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: PikopTheme.black,
        border: Border(top: BorderSide(color: Colors.white.withOpacity(0.05))),
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _messageController,
              decoration: InputDecoration(
                hintText: 'Type your message...',
                filled: true,
                fillColor: Colors.white.withOpacity(0.05),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                  borderSide: BorderSide.none,
                ),
                contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              ),
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            onPressed: () {
              if (_messageController.text.isEmpty) return;
              context.read<ChatBloc>().add(MessageSent(
                    conversationId: widget.conversationId,
                    senderId: userId,
                    senderType: userRole,
                    content: _messageController.text,
                  ));
              _messageController.clear();
            },
            icon: const Icon(Icons.send, color: PikopTheme.gold),
          ),
        ],
      ),
    );
  }
}
