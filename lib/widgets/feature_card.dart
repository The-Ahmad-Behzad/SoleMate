import 'package:flutter/material.dart';
import '../theme/theme_config.dart';

/// Feature card widget for the landing page features section
class FeatureCard extends StatelessWidget {
  const FeatureCard({
    super.key,
    required this.icon,
    required this.title,
    required this.description,
    this.onTap,
  });

  final IconData icon;
  final String title;
  final String description;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    
    return Card(
      elevation: 0,
      shadowColor: isDark 
          ? AppColors.lightForeground.withOpacity(0.15)
          : AppColors.lightForeground.withOpacity(0.15),
      shape: RoundedRectangleBorder(
        borderRadius: AppRadius.radiusLarge,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: AppRadius.radiusLarge,
        child: Container(
          padding: AppSpacing.paddingLarge,
          decoration: BoxDecoration(
            borderRadius: AppRadius.radiusLarge,
            gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Icon container with accent background
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: AppColors.accent10,
                  borderRadius: AppRadius.radiusLarge,
                ),
                child: Icon(
                  icon,
                  color: AppColors.accent,
                  size: 24,
                ),
              ),
              
              const SizedBox(height: AppSpacing.lg),
              
              // Title
              Text(
                title,
                style: AppTypography.headline4.copyWith(
                  color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                ),
              ),
              
              const SizedBox(height: AppSpacing.sm),
              
              // Description
              Text(
                description,
                style: AppTypography.bodyMedium.copyWith(
                  color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Grid of feature cards with responsive layout
class FeatureCardsGrid extends StatelessWidget {
  const FeatureCardsGrid({
    super.key,
    required this.features,
    this.onFeatureTap,
  });

  final List<FeatureCardData> features;
  final Function(int index)? onFeatureTap;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        // Determine column count based on screen width
        int crossAxisCount;
        if (constraints.maxWidth < AppBreakpoints.md) {
          crossAxisCount = 1; // Mobile: single column
        } else if (constraints.maxWidth < AppBreakpoints.lg) {
          crossAxisCount = 2; // Tablet: 2 columns
        } else {
          crossAxisCount = 4; // Desktop: 4 columns
        }

        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            crossAxisSpacing: AppSpacing.xl2,
            mainAxisSpacing: AppSpacing.xl2,
            childAspectRatio: 1.0,
          ),
          itemCount: features.length,
          itemBuilder: (context, index) {
            final feature = features[index];
            return FeatureCard(
              icon: feature.icon,
              title: feature.title,
              description: feature.description,
              onTap: () => onFeatureTap?.call(index),
            );
          },
        );
      },
    );
  }
}

/// Data class for feature card information
class FeatureCardData {
  const FeatureCardData({
    required this.icon,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final String title;
  final String description;
}

/// Predefined feature cards for SoleMate app
class SoleMateFeatures {
  static const List<FeatureCardData> features = [
    FeatureCardData(
      icon: Icons.camera_alt,
      title: 'AR Try-On',
      description: 'Experience shoes in augmented reality before you buy',
    ),
    FeatureCardData(
      icon: Icons.shopping_bag,
      title: 'My Closet',
      description: 'Save and organize your favorite shoe collections',
    ),
    FeatureCardData(
      icon: Icons.auto_awesome,
      title: 'Outfit Match',
      description: 'Get AI-powered outfit suggestions for your shoes',
    ),
    FeatureCardData(
      icon: Icons.palette,
      title: 'Customize',
      description: 'Design and personalize shoes to match your style',
    ),
  ];
}
