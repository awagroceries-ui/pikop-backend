import 'package:flutter/material.dart';
import '../../../../core/theme/pikop_theme.dart';

class FaqListScreen extends StatelessWidget {
  final String category;
  final List<Map<String, dynamic>> articles;

  const FaqListScreen({
    super.key,
    required this.category,
    required this.articles,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(category)),
      body: ListView.builder(
        padding: const EdgeInsets.all(24),
        itemCount: articles.length,
        itemBuilder: (context, index) {
          final article = articles[index];
          return Container(
            margin: const EdgeInsets.only(bottom: 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  article['title'] ?? '',
                  style: const TextStyle(color: PikopTheme.gold, fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(height: 12),
                Text(
                  article['content'] ?? '',
                  style: const TextStyle(color: Colors.white70, height: 1.6),
                ),
                const SizedBox(height: 24),
                const Divider(color: Colors.white12),
              ],
            ),
          );
        },
      ),
    );
  }
}
