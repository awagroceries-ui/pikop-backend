import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthBloc, AuthState>(
      builder: (context, state) {
        if (state is AuthSuccess) {
          final user = state.userData['user'];
          final role = user['role'] ?? 'CUSTOMER';

          return role == 'FULFILLER'
            ? const FulfillerDashboard()
            : const CustomerDashboard();
        }
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      },
    );
  }
}

class CustomerDashboard extends StatelessWidget {
  const CustomerDashboard({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('PIKOP', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 2)),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => context.read<AuthBloc>().add(LogoutRequested()),
          )
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('What are we moving today?', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 24),
            _buildActionCard(
              context,
              'Request Delivery',
              'Send parcels, documents or groceries.',
              Icons.local_shipping,
              PikopTheme.orange,
              () => Navigator.pushNamed(context, '/request_delivery'),
            ),
            const SizedBox(height: 16),
            _buildActionCard(
              context,
              'Marketplace',
              'Buy items from verified vendors.',
              Icons.shopping_bag,
              PikopTheme.green,
              () {}, // TODO: Marketplace
            ),
            const SizedBox(height: 16),
            _buildActionCard(
              context,
              'Foods & Meals',
              'Order fresh meals from kitchens.',
              Icons.restaurant,
              PikopTheme.teal ?? Colors.teal,
              () {}, // TODO: Foods
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionCard(BuildContext context, String title, String subtitle, IconData icon, Color color, VoidCallback onTap) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                child: Icon(icon, color: color, size: 30),
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    Text(subtitle, style: const TextStyle(color: PikopTheme.grey, fontSize: 12)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: PikopTheme.grey),
            ],
          ),
        ),
      ),
    );
  }
}

class FulfillerDashboard extends StatefulWidget {
  const FulfillerDashboard({super.key});

  @override
  State<FulfillerDashboard> createState() => _FulfillerDashboardState();
}

class _FulfillerDashboardState extends State<FulfillerDashboard> {
  bool _isOnline = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('FLEET COMMAND'),
        actions: [
          Switch(
            value: _isOnline,
            onChanged: (v) => setState(() => _isOnline = v),
            activeColor: PikopTheme.green,
          )
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              _isOnline ? Icons.radar : Icons.power_settings_new,
              size: 100,
              color: _isOnline ? PikopTheme.green : PikopTheme.grey,
            ),
            const SizedBox(height: 24),
            Text(
              _isOnline ? 'SCANNING FOR MISSIONS' : 'YOU ARE OFFLINE',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                letterSpacing: 1.5,
                color: _isOnline ? PikopTheme.green : PikopTheme.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

extension on PikopTheme {
  static const Color teal = Color(0xFF00BCD4);
}
