import 'package:flutter/material.dart';
import '../theme/pikop_theme.dart';

class ErrorBoundary extends StatelessWidget {
  final Widget child;

  const ErrorBoundary({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    ErrorWidget.builder = (FlutterErrorDetails details) {
      return Scaffold(
        backgroundColor: PikopTheme.black,
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(32.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.error_outline, color: Colors.red, size: 80),
                const SizedBox(height: 24),
                const Text(
                  'SYSTEM INTERRUPTION',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white, letterSpacing: 2),
                ),
                const SizedBox(height: 16),
                Text(
                  details.exception.toString(),
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: PikopTheme.grey, fontSize: 12),
                ),
                const SizedBox(height: 40),
                ElevatedButton(
                  onPressed: () => Navigator.of(context).pushNamedAndRemoveUntil('/', (route) => false),
                  child: const Text('RESTART APP'),
                ),
              ],
            ),
          ),
        ),
      );
    };
    return child;
  }
}
