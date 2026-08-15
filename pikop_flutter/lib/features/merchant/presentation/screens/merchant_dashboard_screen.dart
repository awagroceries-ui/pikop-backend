import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/merchant_bloc.dart';

class MerchantDashboardScreen extends StatelessWidget {
  const MerchantDashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Merchant Portal')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Bulk Operations',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Manage your business orders at scale.',
              style: TextStyle(color: PikopTheme.grey),
            ),
            const SizedBox(height: 32),
            _buildCard(
              context,
              'Bulk Upload (JSON)',
              'Deploy multiple missions simultaneously.',
              Icons.upload_file,
              () {
                // TODO: Show JSON Input Modal
              },
            ),
            const SizedBox(height: 16),
            _buildCard(
              context,
              'API Credentials',
              'View and manage your live API keys.',
              Icons.vpn_key_outlined,
              () {
                // TODO: Show API Key Management
              },
            ),
            const SizedBox(height: 32),
            const Text(
              'Batch History',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(32),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.05),
                borderRadius: BorderRadius.circular(16),
              ),
              child: const Column(
                children: [
                  Icon(Icons.history, size: 40, color: PikopTheme.grey),
                  SizedBox(height: 16),
                  Text('No recent batches found.', style: TextStyle(color: PikopTheme.grey)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCard(BuildContext context, String title, String subtitle, IconData icon, VoidCallback onTap) {
    return Card(
      color: Colors.white.withOpacity(0.05),
      child: ListTile(
        leading: Icon(icon, color: PikopTheme.gold),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}
