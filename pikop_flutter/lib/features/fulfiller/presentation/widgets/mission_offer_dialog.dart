import 'package:flutter/material.dart';
import '../../../../core/theme/pikop_theme.dart';

class MissionOfferDialog extends StatelessWidget {
  final Map<String, dynamic> offer;
  final VoidCallback onAccept;
  final VoidCallback onDecline;

  const MissionOfferDialog({
    super.key,
    required this.offer,
    required this.onAccept,
    required this.onDecline,
  });

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: PikopTheme.black,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(Icons.flash_on, color: PikopTheme.gold),
                const SizedBox(width: 8),
                const Text(
                  'NEW MISSION OFFER',
                  style: TextStyle(fontWeight: FontWeight.w900, color: PikopTheme.gold),
                ),
                const Spacer(),
                Text(
                  '${offer['distance_km']} km away',
                  style: const TextStyle(fontSize: 10, color: PikopTheme.grey),
                ),
              ],
            ),
            const SizedBox(height: 24),
            Text(
              '₦${offer['total_fare']}',
              style: const TextStyle(fontSize: 40, fontWeight: FontWeight.w900, color: Colors.white),
            ),
            const SizedBox(height: 8),
            const Text(
              'Est. Earnings (75%)',
              style: TextStyle(color: PikopTheme.grey, fontSize: 12),
            ),
            const SizedBox(height: 24),
            const Divider(color: Colors.white12),
            const SizedBox(height: 16),
            const Text('PICKUP FROM', style: TextStyle(color: PikopTheme.grey, fontSize: 10, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(offer['pickup_address'] ?? 'Loading...', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            const Text('ITEM', style: TextStyle(color: PikopTheme.grey, fontSize: 10, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(offer['item_description'] ?? 'Package', maxLines: 1, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 32),
            Row(
              children: [
                Expanded(
                  child: TextButton(
                    onPressed: onDecline,
                    child: const Text('DECLINE', style: TextStyle(color: Colors.red)),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: ElevatedButton(
                    onPressed: onAccept,
                    style: ElevatedButton.styleFrom(backgroundColor: PikopTheme.green),
                    child: const Text('ACCEPT'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
