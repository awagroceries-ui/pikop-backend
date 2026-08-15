import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/theme/pikop_theme.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/auth/presentation/bloc/auth_bloc.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/otp_screen.dart';
import 'features/auth/presentation/screens/signup_screen.dart';
import 'core/services/socket_service.dart';
import 'features/dashboard/presentation/screens/home_screen.dart';
import 'features/delivery/data/delivery_repository.dart';

import 'features/fulfiller/data/fulfiller_repository.dart';
import 'features/fulfiller/presentation/bloc/fulfiller_bloc.dart';
import 'features/fulfiller/presentation/screens/fulfiller_onboarding_screen.dart';

import 'features/support/data/support_repository.dart';
import 'features/support/presentation/bloc/support_bloc.dart';
import 'features/support/presentation/screens/support_hub_screen.dart';

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
        RepositoryProvider(create: (context) => FulfillerRepository()),
        RepositoryProvider(create: (context) => SocketService()),
        RepositoryProvider(create: (context) => SupportRepository()),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (context) => AuthBloc(authRepository: context.read<AuthRepository>()),
          ),
          BlocProvider(
            create: (context) => DeliveryBloc(deliveryRepository: context.read<DeliveryRepository>()),
          ),
          BlocProvider(
            create: (context) => FulfillerBloc(fulfillerRepository: context.read<FulfillerRepository>()),
          ),
          BlocProvider(
            create: (context) => SupportBloc(supportRepository: context.read<SupportRepository>()),
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
            '/home': (context) => const HomeScreen(),
            '/request_delivery': (context) => const RequestDeliveryScreen(),
            '/fulfiller_onboarding': (context) => const FulfillerOnboardingScreen(),
            '/support_hub': (context) => const SupportHubScreen(),
          },
          onGenerateRoute: (settings) {
            if (settings.name == '/otp') {
              final email = settings.arguments as String;
              return MaterialPageRoute(builder: (context) => OtpScreen(email: email));
            }
            if (settings.name == '/mission_tracking') {
              final args = settings.arguments as Map<String, dynamic>;
              return MaterialPageRoute(
                builder: (context) => ActiveMissionScreen(
                  missionId: args['missionId'],
                  isFulfiller: args['isFulfiller'] ?? false,
                ),
              );
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
