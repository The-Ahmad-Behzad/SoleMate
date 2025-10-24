import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../screens/landing_screen.dart';

/// Logo button widget for app bars
class LogoButton extends StatelessWidget {
  const LogoButton({super.key});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => const LandingScreen()),
        );
      },
      child: Container(
        height: 40,
        width: 40,
        margin: const EdgeInsets.only(left: AppSpacing.sm),
        child: Image.asset(
          'assets/images/logo.png',
          fit: BoxFit.contain,
          errorBuilder: (context, error, stackTrace) {
            // Fallback to icon if image fails to load
            return Container(
              decoration: BoxDecoration(
                color: AppColors.accent10,
                borderRadius: AppRadius.radiusLarge,
              ),
              child: const Icon(
                Icons.shopping_bag,
                color: AppColors.accent,
                size: 24,
              ),
            );
          },
        ),
      ),
    );
  }
}
