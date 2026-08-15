import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/support_bloc.dart';
import 'faq_list_screen.dart';
import 'support_chat_screen.dart';

class SupportHubScreen extends StatefulWidget {
  const SupportHubScreen({super.key});

  @override
  State<SupportHubScreen> createState() => _SupportHubScreenState();
}

class _SupportHubScreenState extends State<SupportHubScreen> {
  @override
  void initState() {
    super.initState();
    context.read<SupportBloc>().add(KnowledgeBaseRequested());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Help Center')),
      body: BlocConsumer<SupportBloc, SupportState>(
        listener: (context, state) {
          if (state is SupportSessionReady) {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => SupportChatScreen(conversationId: state.conversationId),
              ),
            );
          } else if (state is SupportFailure) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          }
        },
        builder: (context, state) {
          if (state is SupportLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is KnowledgeBaseLoaded) {
            final categories = state.articles.map((e) => e['category'] as String).toSet().toList();

            return Column(
              children: [
                _buildLiveChatCard(context),
                const Padding(
                  padding: EdgeInsets.fromLTRB(24, 32, 24, 16),
                  child: Text(
                    'Frequently Asked Questions',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    itemCount: categories.length,
                    itemBuilder: (context, index) {
                      final category = categories[index];
                      return _buildCategoryItem(context, category, state.articles);
                    },
                  ),
                ),
              ],
            );
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }

  Widget _buildLiveChatCard(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Card(
        color: PikopTheme.gold,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: InkWell(
          onTap: () => context.read<SupportBloc>().add(SupportSessionRequested()),
          borderRadius: BorderRadius.circular(16),
          child: const Padding(
            padding: EdgeInsets.all(20.0),
            child: Row(
              children: [
                Icon(Icons.chat_bubble_outline, color: PikopTheme.black, size: 30),
                SizedBox(width: 20),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Live Support Chat',
                        style: TextStyle(color: PikopTheme.black, fontWeight: FontWeight.bold, fontSize: 16),
                      ),
                      Text(
                        'Chat with our team in real-time.',
                        style: TextStyle(color: PikopTheme.black, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                Icon(Icons.chevron_right, color: PikopTheme.black),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCategoryItem(BuildContext context, String category, List<Map<String, dynamic>> articles) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: Colors.white.withOpacity(0.05),
      child: ListTile(
        title: Text(category, style: const TextStyle(fontWeight: FontWeight.w600)),
        trailing: const Icon(Icons.chevron_right, size: 20),
        onTap: () {
          final categoryArticles = articles.where((a) => a['category'] == category).toList();
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => FaqListScreen(category: category, articles: categoryArticles),
            ),
          );
        },
      ),
    );
  }
}
