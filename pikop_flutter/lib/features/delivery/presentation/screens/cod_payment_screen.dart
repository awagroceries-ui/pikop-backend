import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../../../../core/theme/pikop_theme.dart';

class CodPaymentScreen extends StatefulWidget {
  final String checkoutUrl;
  final int orderId;

  const CodPaymentScreen({super.key, required this.checkoutUrl, required this.orderId});

  @override
  State<CodPaymentScreen> createState() => _CodPaymentScreenState();
}

class _CodPaymentScreenState extends State<CodPaymentScreen> {
  late final WebViewController _controller;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(PikopTheme.black)
      ..setNavigationDelegate(
        NavigationDelegate(
          onUrlChange: (change) {
             // Handle redirect if needed
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.checkoutUrl));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Recipient Collection'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: WebViewWidget(controller: _controller),
    );
  }
}
