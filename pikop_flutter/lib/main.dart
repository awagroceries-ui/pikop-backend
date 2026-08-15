import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/theme/pikop_theme.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/auth/presentation/bloc/auth_bloc.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/otp_screen.dart';
import 'features/auth/presentation/screens/signup_screen.dart';

import 'features/delivery/data/delivery_repository.dart';
import 'features/delivery/presentation/bloc/delivery_bloc.dart';
import 'features/delivery/presentation/screens/request_delivery_screen.dart';

void main() {
  runApp(const PikopApp());
}

class PikopApp extends StatelessWidget {
  const PikopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiRepositoryProvider(
      providers: [
        RepositoryProvider(create: (context) => AuthRepository()),
        RepositoryProvider(create: (context) => DeliveryRepository()),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (context) => AuthBloc(authRepository: context.read<AuthRepository>()),
          ),
          BlocProvider(
            create: (context) => DeliveryBloc(deliveryRepository: context.read<DeliveryRepository>()),
          ),
        ],
        child: MaterialApp(
          title: 'Pikop',
          debugShowCheckedModeBanner: false,
          theme: PikopTheme.darkTheme,
          initialRoute: '/',
          routes: {
            '/': (context) => const SplashScreen(),
            '/login': (context) => const LoginScreen(),
            '/signup': (context) => const SignupScreen(role: 'CUSTOMER'),
            '/request_delivery': (context) => const RequestDeliveryScreen(),
          },
          onGenerateRoute: (settings) {
            if (settings.name == '/otp') {
              final email = settings.arguments as String;
              return MaterialPageRoute(builder: (context) => OtpScreen(email: email));
            }
            return null;
          },
        ),
      ),
    );
  }
}

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _checkAuth();
  }

  Future<void> _checkAuth() async {
    await Future.delayed(const Duration(seconds: 3));
    if (mounted) {
      Navigator.pushReplacementNamed(context, '/login');
    }
  }

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
