import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/theme/pikop_theme.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/auth/presentation/bloc/auth_bloc.dart';
import 'features/auth/presentation/screens/account_screen.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/policy_screen.dart';
import 'features/auth/presentation/screens/otp_screen.dart';
import 'features/auth/presentation/screens/signup_screen.dart';
import 'core/services/socket_service.dart';
import 'features/dashboard/presentation/screens/home_screen.dart';
import 'features/delivery/data/delivery_repository.dart';

import 'features/fulfiller/data/fulfiller_repository.dart';
import 'features/fulfiller/presentation/bloc/fulfiller_bloc.dart';
import 'features/fulfiller/presentation/screens/fulfiller_onboarding_screen.dart';

import 'features/foods/data/kitchen_repository.dart';
import 'features/foods/presentation/bloc/foods_bloc.dart';
import 'features/foods/presentation/screens/food_browser_screen.dart';
import 'features/growth/data/growth_repository.dart';
import 'features/growth/presentation/bloc/growth_bloc.dart';
import 'features/growth/presentation/screens/loyalty_hub_screen.dart';
import 'features/marketplace/data/marketplace_repository.dart';
import 'features/marketplace/presentation/bloc/marketplace_bloc.dart';
import 'features/marketplace/presentation/screens/manage_catalog_screen.dart';
import 'features/marketplace/presentation/screens/product_detail_screen.dart';
import 'features/marketplace/presentation/screens/shop_browser_screen.dart';
import 'features/marketplace/presentation/screens/vendor_onboarding_screen.dart';
import 'features/merchant/data/merchant_repository.dart';
import 'features/merchant/presentation/bloc/merchant_bloc.dart';
import 'features/merchant/presentation/screens/merchant_dashboard_screen.dart';
import 'features/support/data/support_repository.dart';
import 'features/support/presentation/bloc/support_bloc.dart';
import 'features/support/presentation/screens/support_hub_screen.dart';

import 'features/wallet/data/wallet_repository.dart';
import 'features/wallet/presentation/bloc/wallet_bloc.dart';
import 'features/wallet/presentation/screens/wallet_screen.dart';

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
        RepositoryProvider(create: (context) => MarketplaceRepository()),
        RepositoryProvider(create: (context) => KitchenRepository()),
        RepositoryProvider(create: (context) => WalletRepository()),
        RepositoryProvider(create: (context) => MerchantRepository()),
        RepositoryProvider(create: (context) => GrowthRepository()),
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
          BlocProvider(
            create: (context) => MarketplaceBloc(marketplaceRepository: context.read<MarketplaceRepository>()),
          ),
          BlocProvider(
            create: (context) => FoodsBloc(kitchenRepository: context.read<KitchenRepository>()),
          ),
          BlocProvider(
            create: (context) => WalletBloc(walletRepository: context.read<WalletRepository>()),
          ),
          BlocProvider(
            create: (context) => MerchantBloc(merchantRepository: context.read<MerchantRepository>()),
          ),
          BlocProvider(
            create: (context) => GrowthBloc(growthRepository: context.read<GrowthRepository>()),
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
            '/account': (context) => const AccountScreen(),
            '/request_delivery': (context) => const RequestDeliveryScreen(),
            '/fulfiller_onboarding': (context) => const FulfillerOnboardingScreen(),
            '/support_hub': (context) => const SupportHubScreen(),
            '/marketplace': (context) => const ShopBrowserScreen(),
            '/vendor_onboarding': (context) => const VendorOnboardingScreen(),
            '/manage_catalog': (context) => const ManageCatalogScreen(),
            '/foods': (context) => const FoodBrowserScreen(),
            '/wallet': (context) => const WalletScreen(),
            '/merchant_dashboard': (context) => const MerchantDashboardScreen(),
            '/loyalty_hub': (context) => const LoyaltyHubScreen(),
          },
          onGenerateRoute: (settings) {
            if (settings.name == '/otp') {
              final email = settings.arguments as String;
              return MaterialPageRoute(builder: (context) => OtpScreen(email: email));
            }
            if (settings.name == '/product_detail') {
              final product = settings.arguments as Map<String, dynamic>;
              return MaterialPageRoute(builder: (context) => ProductDetailScreen(product: product));
            }
            if (settings.name == '/cod_payment') {
              final args = settings.arguments as Map<String, dynamic>;
              return MaterialPageRoute(
                builder: (context) => CodPaymentScreen(checkoutUrl: args['url'], orderId: args['orderId']),
              );
            }
            if (settings.name == '/policy') {
              final args = settings.arguments as Map<String, dynamic>;
              return MaterialPageRoute(
                builder: (context) => PolicyScreen(title: args['title'], url: args['url']),
              );
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
