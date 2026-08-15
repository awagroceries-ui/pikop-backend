import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../bloc/foods_bloc.dart';

class FoodBrowserScreen extends StatefulWidget {
  const FoodBrowserScreen({super.key});

  @override
  State<FoodBrowserScreen> createState() => _FoodBrowserScreenState();
}

class _FoodBrowserScreenState extends State<FoodBrowserScreen> {
  @override
  void initState() {
    super.initState();
    context.read<FoodsBloc>().add(const KitchensRequested());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Foods & Meals', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: BlocBuilder<FoodsBloc, FoodsState>(
        builder: (context, state) {
          if (state is FoodsLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is KitchensLoaded) {
            if (state.kitchens.isEmpty) {
              return const Center(child: Text('No kitchens available in your area.'));
            }

            return ListView.builder(
              padding: const EdgeInsets.all(24),
              itemCount: state.kitchens.length,
              itemBuilder: (context, index) {
                final kitchen = state.kitchens[index];
                return _buildKitchenCard(context, kitchen);
              },
            );
          }

          if (state is FoodsFailure) {
            return Center(child: Text(state.message));
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }

  Widget _buildKitchenCard(BuildContext context, dynamic kitchen) {
    return Card(
      margin: const EdgeInsets.only(bottom: 20),
      color: Colors.white.withOpacity(0.05),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () {
          // TODO: Navigate to Kitchen Details
        },
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              height: 160,
              decoration: BoxDecoration(
                color: Colors.white10,
                image: kitchen['logo_url'] != null
                    ? DecorationImage(image: NetworkImage(kitchen['logo_url']), fit: BoxFit.cover)
                    : null,
              ),
              child: kitchen['logo_url'] == null
                  ? const Center(child: Icon(Icons.restaurant, size: 40, color: PikopTheme.grey))
                  : null,
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          kitchen['business_name'] ?? '',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                        ),
                        Text(
                          kitchen['cuisine_type'] ?? 'General',
                          style: const TextStyle(color: PikopTheme.grey, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.green.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Text(
                      'OPEN',
                      style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold, fontSize: 10),
                    ),
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
