import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../../../../core/services/socket_service.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../../../fulfiller/presentation/bloc/fulfiller_bloc.dart';
import '../../../fulfiller/presentation/widgets/mission_offer_dialog.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    final authState = context.read<AuthBloc>().state;
    if (authState is AuthSuccess) {
      final userId = authState.userData['user']['id'];
      context.read<SocketService>().connect(userId);
    }
  }

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
            icon: const Icon(Icons.person_outline),
            onPressed: () => Navigator.pushNamed(context, '/account'),
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
              () => Navigator.pushNamed(context, '/marketplace'),
            ),
            const SizedBox(height: 16),
            _buildActionCard(
              context,
              'Foods & Meals',
              'Order fresh meals from kitchens.',
              Icons.restaurant,
              PikopTheme.teal ?? Colors.teal,
              () => Navigator.pushNamed(context, '/foods'),
            ),
            const SizedBox(height: 32),
            const Divider(color: Colors.white10),
            const SizedBox(height: 16),
            ListTile(
              leading: const Icon(Icons.storefront, color: PikopTheme.gold),
              title: const Text('Sell on Pikop', style: TextStyle(fontWeight: FontWeight.bold)),
              subtitle: const Text('Register as a Vendor or Kitchen', style: TextStyle(fontSize: 10)),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.pushNamed(context, '/vendor_onboarding'),
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
  List<dynamic> _queue = [];

  @override
  void initState() {
    super.initState();
    // Listen for socket offers
    context.read<SocketService>().on('new_mission_offer', (data) {
      if (_isOnline) {
        context.read<FulfillerBloc>().add(MissionOfferReceived(data));
      }
    });

    // Listen for queue updates (v3.6.0)
    context.read<SocketService>().on('status_updated', (data) {
        if (data['status'] == 'QUEUED') {
            _refreshQueue();
        }
    });
  }

  void _refreshQueue() async {
    // TODO: Fetch queued missions from API
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('FLEET COMMAND'),
        leading: IconButton(
          icon: const Icon(Icons.person_outline),
          onPressed: () => Navigator.pushNamed(context, '/account'),
        ),
        actions: [
          Switch(
            value: _isOnline,
            onChanged: (v) {
              setState(() => _isOnline = v);
              context.read<FulfillerBloc>().add(
                FulfillerStatusUpdated(status: v ? 'ONLINE' : 'OFFLINE'),
              );
            },
            activeColor: PikopTheme.green,
          )
        ],
      ),
      body: BlocListener<FulfillerBloc, FulfillerState>(
        listener: (context, state) {
          if (state is NewMissionOffer) {
            _showOfferDialog(state.offer);
          } else if (state is MissionAcceptSuccess) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Mission Secured!'), backgroundColor: PikopTheme.green),
            );
          } else if (state is FulfillerFailure) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          }
        },
        child: SingleChildScrollView(
            child: Column(
                children: [
                    const SizedBox(height: 60),
                    Center(
                        child: Column(
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
                    if (_isOnline && _queue.isNotEmpty) ...[
                        const Padding(
                            padding: EdgeInsets.fromLTRB(24, 60, 24, 16),
                            child: Align(
                                alignment: Alignment.centerLeft,
                                child: Text('UPCOMING QUEUE', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, letterSpacing: 1.2, color: PikopTheme.grey)),
                            ),
                        ),
                        // List queued items here
                    ]
                ],
            ),
        ),
      ),
    );
  }

  void _showOfferDialog(Map<String, dynamic> offer) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => MissionOfferDialog(
        offer: offer,
        onAccept: () {
          Navigator.pop(dialogContext);
          context.read<FulfillerBloc>().add(MissionAccepted(offer['order_id']));
        },
        onDecline: () => Navigator.pop(dialogContext),
      ),
    );
  }
}

extension on PikopTheme {
  static const Color teal = Color(0xFF00BCD4);
}
