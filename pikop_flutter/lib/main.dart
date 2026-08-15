import 'package:flutter/material.dart';
import 'core/theme/pikop_theme.dart';

void main() {
  runApp(const PikopApp());
}

class PikopApp extends StatelessWidget {
  const PikopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Pikop',
      debugShowCheckedModeBanner: false,
      theme: PikopTheme.darkTheme,
      home: const SplashScreen(),
    );
  }
}

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.rocket_launch, size: 80, color: PikopTheme.gold),
            const SizedBox(height: 24),
            Text(
              'PIKOP V3',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 8),
            const Text(
              'Multi-Platform Rebuild',
              style: TextStyle(color: PikopTheme.grey, letterSpacing: 1.5),
            ),
          ],
        ),
      ),
    );
  }
}
