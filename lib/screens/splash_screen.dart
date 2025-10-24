import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../theme/theme_config.dart';
import 'main_shell.dart';
import 'auth/login_screen.dart';

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: BoxDecoration(
          gradient: isDark ? AppGradients.heroDark : AppGradients.heroLight,
        ),
        child: StreamBuilder<User?>(
          stream: FirebaseAuth.instance.authStateChanges(),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Logo
                    Container(
                      width: 120,
                      height: 120,
                      decoration: BoxDecoration(
                        color: AppColors.primaryForeground.withOpacity(0.1),
                        borderRadius: AppRadius.radiusFull,
                      ),
                      child: const Icon(
                        Icons.shopping_bag,
                        size: 60,
                        color: AppColors.primaryForeground,
                      ),
                    ),
                    
                    const SizedBox(height: AppSpacing.xl3),
                    
                    // App Name
                    Text(
                      'SoleMate',
                      style: AppTypography.headline1.copyWith(
                        color: AppColors.primaryForeground,
                        fontWeight: AppTypography.bold,
                      ),
                    ),
                    
                    const SizedBox(height: AppSpacing.sm),
                    
                    Text(
                      'AR Shoe Try-On Experience',
                      style: AppTypography.bodyLarge.copyWith(
                        color: AppColors.primaryForeground.withOpacity(0.9),
                      ),
                    ),
                    
                    const SizedBox(height: AppSpacing.xl4),
                    
                    // Loading Indicator
                    const CircularProgressIndicator(
                      valueColor: AlwaysStoppedAnimation<Color>(AppColors.primaryForeground),
                    ),
                  ],
                ),
              );
            } else if (snapshot.hasData) {
              return const MainAppShell();
            } else {
              return const LoginScreen();
            }
          },
        ),
      ),
    );
  }
}
