import 'package:flutter/material.dart';
import '../../../../core/theme/pikop_theme.dart';

class ProductDetailScreen extends StatelessWidget {
  final Map<String, dynamic> product;

  const ProductDetailScreen({super.key, required this.product});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(product['name'] ?? 'Product Details')),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Container(
                    height: 300,
                    decoration: BoxDecoration(
                      color: Colors.white10,
                      image: product['photo_url'] != null
                          ? DecorationImage(image: NetworkImage(product['photo_url']), fit: BoxFit.cover)
                          : null,
                    ),
                    child: product['photo_url'] == null
                        ? const Icon(Icons.shopping_bag, size: 100, color: PikopTheme.grey)
                        : null,
                  ),
                  Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              '₦${product['price']}',
                              style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900, color: PikopTheme.gold),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                              decoration: BoxDecoration(
                                color: PikopTheme.gold.withOpacity(0.1),
                                borderRadius: BorderRadius.circular(100),
                              ),
                              child: Text(
                                product['category'] ?? 'General',
                                style: const TextStyle(color: PikopTheme.gold, fontSize: 12, fontWeight: FontWeight.bold),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        Text(
                          product['name'] ?? '',
                          style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Sold by: ${product['business_name']}',
                          style: const TextStyle(color: PikopTheme.grey),
                        ),
                        const SizedBox(height: 24),
                        const Text(
                          'DESCRIPTION',
                          style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: PikopTheme.grey, letterSpacing: 1.2),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          product['description'] ?? 'No description provided.',
                          style: const TextStyle(height: 1.6),
                        ),
                        const SizedBox(height: 32),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          _buildBottomBar(context),
        ],
      ),
    );
  }

  Widget _buildBottomBar(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: PikopTheme.black,
        border: Border(top: BorderSide(color: Colors.white.withOpacity(0.05))),
      ),
      child: ElevatedButton(
        onPressed: () {
          // TODO: Navigate to Delivery Quote with pre-filled Pickup (Vendor location)
          // and pre-filled Item Description
          Navigator.pushNamed(context, '/request_delivery', arguments: {
            'itemDescription': product['name'],
            'pickupAddress': product['formatted_address'], // Assuming it's in the payload
            'pickupLat': product['pickup_lat'],
            'pickupLng': product['pickup_lng'],
          });
        },
        style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 18)),
        child: const Text('BUY & ARRANGE PICKUP', style: TextStyle(letterSpacing: 1.2)),
      ),
    );
  }
}
