import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/pikop_theme.dart';
import '../../auth/presentation/bloc/auth_bloc.dart';
import '../bloc/delivery_bloc.dart';
import '../widgets/address_input_field.dart';
import 'checkout_webview.dart';

class RequestDeliveryScreen extends StatefulWidget {
  const RequestDeliveryScreen({super.key});

  @override
  State<RequestDeliveryScreen> createState() => _RequestDeliveryScreenState();
}

class _RequestDeliveryScreenState extends State<RequestDeliveryScreen> {
  final _descriptionController = TextEditingController();

  String? _pickupAddress;
  double? _pickupLat, _pickupLng;

  String? _deliveryAddress;
  double? _deliveryLat, _deliveryLng;

  Map<String, dynamic>? _currentQuote;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final args = ModalRoute.of(context)?.settings.arguments as Map<String, dynamic>?;
      if (args != null) {
        setState(() {
          if (args['itemDescription'] != null) _descriptionController.text = args['itemDescription'];
          if (args['pickupAddress'] != null) _pickupAddress = args['pickupAddress'];
          if (args['pickupLat'] != null) _pickupLat = args['pickupLat'];
          if (args['pickupLng'] != null) _pickupLng = args['pickupLng'];
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Request Delivery', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: BlocConsumer<DeliveryBloc, DeliveryState>(
        listener: (context, state) {
          if (state is QuoteSuccess) {
            _currentQuote = state.quoteData;
            _showQuoteModal(state.quoteData);
          } else if (state is PaymentInitSuccess) {
            _launchPayment(state.checkoutUrl);
          } else if (state is DeliveryFailure) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          }
        },
        builder: (context, state) {
          return SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                AddressInputField(
                  label: 'Pickup Location',
                  icon: Icons.location_on,
                  value: _pickupAddress,
                  onSelected: (data) {
                    setState(() {
                      _pickupAddress = data['address'];
                      _pickupLat = data['lat'];
                      _pickupLng = data['lng'];
                    });
                  },
                ),
                const SizedBox(height: 16),
                AddressInputField(
                  label: 'Delivery Location',
                  icon: Icons.flag,
                  value: _deliveryAddress,
                  onSelected: (data) {
                    setState(() {
                      _deliveryAddress = data['address'];
                      _deliveryLat = data['lat'];
                      _deliveryLng = data['lng'];
                    });
                  },
                ),
                const SizedBox(height: 24),
                TextField(
                  controller: _descriptionController,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    labelText: 'Item Description',
                    hintText: 'What are we moving?',
                    border: OutlineInputBorder(),
                    alignLabelWithHint: true,
                  ),
                  onChanged: (v) => setState(() {}),
                ),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: (_pickupLat == null || _deliveryLat == null || _descriptionController.text.isEmpty || state is DeliveryLoading)
                      ? null
                      : () {
                          context.read<DeliveryBloc>().add(
                                QuoteRequested(
                                  pickupAddress: _pickupAddress!,
                                  deliveryAddress: _deliveryAddress!,
                                  itemDescription: _descriptionController.text,
                                  pickupLat: _pickupLat!,
                                  pickupLng: _pickupLng!,
                                  deliveryLat: _deliveryLat!,
                                  deliveryLng: _deliveryLng!,
                                ),
                              );
                        },
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 18),
                    disabledBackgroundColor: Colors.white.withOpacity(0.05),
                  ),
                  child: state is DeliveryLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black),
                        )
                      : const Text('GET FARE QUOTE', style: TextStyle(letterSpacing: 1.2)),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  void _showQuoteModal(Map<String, dynamic> data) {
    showModalBottomSheet(
      context: context,
      backgroundColor: PikopTheme.black,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (modalContext) {
        return Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Mission Summary',
                style: TextStyle(color: PikopTheme.grey, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '₦${data['total_fare']}',
                    style: const TextStyle(fontSize: 32, fontWeight: FontWeight.w900, color: PikopTheme.gold),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: PikopTheme.gold.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(100),
                    ),
                    child: Text(
                      data['size_tier'],
                      style: const TextStyle(color: PikopTheme.gold, fontWeight: FontWeight.bold, fontSize: 12),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'Distance: ${data['distance_km']} km',
                style: const TextStyle(color: PikopTheme.grey),
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: () {
                  Navigator.pop(modalContext);
                  final authState = context.read<AuthBloc>().state;
                  if (authState is AuthSuccess) {
                    context.read<DeliveryBloc>().add(
                      PaymentInitialized(
                        quoteId: data['quote_id'],
                        amount: (data['total_fare'] as num).toDouble(),
                        email: authState.userData['email'] ?? '',
                      ),
                    );
                  }
                },
                child: const Text('PROCEED TO PAYMENT'),
              ),
            ],
          ),
        );
      },
    );
  }

  void _launchPayment(String url) async {
    final success = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => CheckoutWebView(url: url),
      ),
    );

    if (success == true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Mission Activated!'), backgroundColor: PikopTheme.green),
      );
      Navigator.pop(context); // Back to dashboard
    }
  }
}
