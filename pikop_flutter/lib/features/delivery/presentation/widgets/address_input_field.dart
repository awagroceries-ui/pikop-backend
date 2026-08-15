import 'package:flutter/material.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../screens/address_search_screen.dart';

class AddressInputField extends StatelessWidget {
  final String label;
  final String? value;
  final IconData icon;
  final Function(Map<String, dynamic>) onSelected;

  const AddressInputField({
    super.key,
    required this.label,
    required this.icon,
    required this.onSelected,
    this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
      ),
      child: ListTile(
        leading: Icon(icon, color: PikopTheme.gold),
        title: Text(
          label,
          style: const TextStyle(color: PikopTheme.grey, fontSize: 12),
        ),
        subtitle: Text(
          value ?? 'Select an address...',
          style: TextStyle(
            color: value != null ? Colors.white : Colors.white.withOpacity(0.3),
            fontWeight: FontWeight.w500,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: const Icon(Icons.chevron_right, size: 20, color: PikopTheme.grey),
        onTap: () async {
          final result = await Navigator.push<Map<String, dynamic>>(
            context,
            MaterialPageRoute(
              builder: (context) => AddressSearchScreen(title: label),
            ),
          );

          if (result != null) {
            onSelected(result);
          }
        },
      ),
    );
  }
}
