import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/auth_bloc.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthBloc, AuthState>(
      builder: (context, state) {
        if (state is AuthSuccess) {
          final user = state.userData['user'];
          return Scaffold(
            appBar: AppBar(title: const Text('My Account')),
            body: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  const CircleAvatar(
                    radius: 40,
                    backgroundColor: PikopTheme.gold,
                    child: Icon(Icons.person, size: 40, color: PikopTheme.black),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    user['full_name'] ?? 'User',
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  Text(
                    user['email'] ?? '',
                    style: const TextStyle(color: PikopTheme.grey),
                  ),
                  const SizedBox(height: 32),
                  _buildOption(
                    context,
                    'Wallet & Earnings',
                    Icons.account_balance_wallet_outlined,
                    () => Navigator.pushNamed(context, '/wallet'),
                  ),
                  _buildOption(
                    context,
                    'Rewards & Referrals',
                    Icons.card_giftcard,
                    () => Navigator.pushNamed(context, '/loyalty_hub'),
                  ),
                  _buildOption(
                    context,
                    'Mission History',
                    Icons.history,
                    () {}, // TODO
                  ),
                  _buildOption(
                    context,
                    'Help & Support',
                    Icons.help_outline,
                    () => Navigator.pushNamed(context, '/support_hub'),
                  ),
                  _buildOption(
                    context,
                    'Terms & Conditions',
                    Icons.gavel_outlined,
                    () => Navigator.pushNamed(context, '/policy', arguments: {
                      'title': 'Terms & Conditions',
                      'url': 'https://api.awa.name.ng/legal/terms'
                    }),
                  ),
                  _buildOption(
                    context,
                    'Privacy Policy',
                    Icons.security_outlined,
                    () => Navigator.pushNamed(context, '/policy', arguments: {
                      'title': 'Privacy Policy',
                      'url': 'https://api.awa.name.ng/legal/privacy'
                    }),
                  ),
                  _buildOption(
                    context,
                    'Merchant Portal',
                    Icons.business_center_outlined,
                    () => Navigator.pushNamed(context, '/merchant_dashboard'),
                  ),
                  const SizedBox(height: 32),
                  ElevatedButton.icon(
                    onPressed: () => context.read<AuthBloc>().add(LogoutRequested()),
                    icon: const Icon(Icons.logout),
                    label: const Text('SIGN OUT'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red.withOpacity(0.1),
                      foregroundColor: Colors.red,
                    ),
                  ),
                ],
              ),
            ),
          );
        }
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      },
    );
  }

  Widget _buildOption(BuildContext context, String title, IconData icon, VoidCallback onTap) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: Colors.white.withOpacity(0.05),
      child: ListTile(
        leading: Icon(icon, color: PikopTheme.gold),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        trailing: const Icon(Icons.chevron_right, size: 20, color: PikopTheme.grey),
        onTap: onTap,
      ),
    );
  }
}
