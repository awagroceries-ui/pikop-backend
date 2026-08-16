import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/growth_bloc.dart';

class LoyaltyHubScreen extends StatefulWidget {
  const LoyaltyHubScreen({super.key});

  @override
  State<LoyaltyHubScreen> createState() => _LoyaltyHubScreenState();
}

class _LoyaltyHubScreenState extends State<LoyaltyHubScreen> {
  @override
  void initState() {
    super.initState();
    context.read<GrowthBloc>().add(GrowthStatsRequested());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Rewards & Referrals')),
      body: BlocBuilder<GrowthBloc, GrowthState>(
        builder: (context, state) {
          if (state is GrowthLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is GrowthStatsLoaded) {
            final stats = state.stats;
            return SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _buildPointsCard(stats['total_points']),
                  const SizedBox(height: 32),
                  _buildReferralSection(context, stats['referral_code'], stats['referral_count']),
                ],
              ),
            );
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }

  Widget _buildPointsCard(int points) {
    return Container(
      padding: const EdgeInsets.all(32),
      decoration: BoxDecoration(
        color: PikopTheme.orange,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: PikopTheme.orange.withOpacity(0.3), blurRadius: 15, offset: const Offset(0, 8))],
      ),
      child: Column(
        children: [
          const Text('LOYALTY POINTS', style: TextStyle(color: Colors.black54, fontWeight: FontWeight.bold, fontSize: 10, letterSpacing: 1.5)),
          const SizedBox(height: 12),
          Text('$points', style: const TextStyle(color: Colors.black, fontSize: 48, fontWeight: FontWeight.black)),
          const SizedBox(height: 8),
          const Text('Earn more by completing missions.', style: TextStyle(color: Colors.black45, fontSize: 12)),
        ],
      ),
    );
  }

  Widget _buildReferralSection(BuildContext context, String code, int count) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Refer and Earn', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        const Text('Invite friends to Pikop. You both get rewarded after their first mission.', style: TextStyle(color: PikopTheme.grey, fontSize: 13)),
        const SizedBox(height: 24),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(color: Colors.white.withOpacity(0.05), borderRadius: BorderRadius.circular(16), border: Border.all(color: Colors.white10)),
          child: Column(
            children: [
              const Text('YOUR REFERRAL CODE', style: TextStyle(color: PikopTheme.grey, fontSize: 10, fontWeight: FontWeight.bold, letterSpacing: 1)),
              const SizedBox(height: 12),
              GestureDetector(
                onTap: () {
                  Clipboard.setData(ClipboardData(text: code));
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Code Copied!')));
                },
                child: Text(code, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.black, color: PikopTheme.gold, letterSpacing: 5)),
              ),
              const SizedBox(height: 24),
              const Divider(color: Colors.white10),
              const SizedBox(height: 12),
              Text('$count Successful Referrals', style: const TextStyle(fontWeight: FontWeight.bold)),
            ],
          ),
        ),
      ],
    );
  }
}
