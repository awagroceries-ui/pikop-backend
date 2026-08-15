import 'package:flutter/material.dart';
import '../../../../core/theme/pikop_theme.dart';

class ManageCatalogScreen extends StatelessWidget {
  const ManageCatalogScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Store Management')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.inventory_2_outlined, size: 80, color: PikopTheme.gold),
            const SizedBox(height: 24),
            const Text(
              'Your Catalog is Empty',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Start adding products to your store.',
              style: TextStyle(color: PikopTheme.grey),
            ),
            const SizedBox(height: 40),
            ElevatedButton.icon(
              onPressed: () {
                // TODO: Add Product Form
              },
              icon: const Icon(Icons.add, color: PikopTheme.black),
              label: const Text('ADD FIRST PRODUCT'),
            ),
          ],
        ),
      ),
    );
  }
}
