import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/marketplace_bloc.dart';

class ShopBrowserScreen extends StatefulWidget {
  const ShopBrowserScreen({super.key});

  @override
  State<ShopBrowserScreen> createState() => _ShopBrowserScreenState();
}

class _ShopBrowserScreenState extends State<ShopBrowserScreen> {
  @override
  void initState() {
    super.initState();
    context.read<MarketplaceBloc>().add(const MarketplaceProductsRequested());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Marketplace', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: BlocBuilder<MarketplaceBloc, MarketplaceState>(
        builder: (context, state) {
          if (state is MarketplaceLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is MarketplaceLoaded) {
            if (state.products.isEmpty) {
              return const Center(child: Text('No products available yet.'));
            }

            return GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                childAspectRatio: 0.75,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
              ),
              itemCount: state.products.length,
              itemBuilder: (context, index) {
                final product = state.products[index];
                return _buildProductCard(context, product);
              },
            );
          }

          if (state is MarketplaceFailure) {
            return Center(child: Text(state.message));
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }

  Widget _buildProductCard(BuildContext context, dynamic product) {
    return Card(
      color: PikopTheme.grey.withOpacity(0.05),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.pushNamed(context, '/product_detail', arguments: product),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white10,
                  image: product['photo_url'] != null
                      ? DecorationImage(image: NetworkImage(product['photo_url']), fit: BoxFit.cover)
                      : null,
                ),
                child: product['photo_url'] == null
                    ? const Center(child: Icon(Icons.shopping_bag, color: PikopTheme.grey))
                    : null,
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product['name'] ?? '',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '₦${product['price']}',
                    style: const TextStyle(color: PikopTheme.gold, fontWeight: FontWeight.w900),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    product['business_name'] ?? '',
                    style: const TextStyle(fontSize: 10, color: PikopTheme.grey),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
