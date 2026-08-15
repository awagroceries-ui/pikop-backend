import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/pikop_theme.dart';

class TransactionTile extends StatelessWidget {
  final Map<String, dynamic> transaction;

  const TransactionTile({super.key, required this.transaction});

  @override
  Widget build(BuildContext context) {
    final bool isCredit = transaction['entry_type'] == 'CREDIT';
    final amount = double.parse(transaction['amount'].toString());
    final date = DateTime.parse(transaction['created_at']);

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.03),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: (isCredit ? PikopTheme.green : Colors.red).withOpacity(0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(
              isCredit ? Icons.add : Icons.remove,
              color: isCredit ? PikopTheme.green : Colors.red,
              size: 20,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  transaction['purpose'].toString().replaceAll('_', ' '),
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
                Text(
                  DateFormat('MMM dd, yyyy · HH:mm').format(date),
                  style: const TextStyle(color: PikopTheme.grey, fontSize: 11),
                ),
              ],
            ),
          ),
          Text(
            '${isCredit ? "+" : "-"} ₦${amount.toStringAsFixed(0)}',
            style: TextStyle(
              fontWeight: FontWeight.w900,
              color: isCredit ? PikopTheme.green : Colors.red,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }
}
