import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/feature_card.dart';
import 'main_shell.dart';
import '../services/local_catalog_service.dart';
import '../models/product.dart';
import '../widgets/product_card.dart';
import 'catalog_screen.dart';

/// Landing/Welcome screen with hero section and features
class LandingScreen extends StatelessWidget {
  const LandingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final screenSize = MediaQuery.of(context).size;

    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          children: [
            // Hero Section
            _buildHeroSection(context, isDark, screenSize),
            
            // Features Section
            _buildFeaturesSection(context, isDark),
            
          // Popular & Recent Shoes Sections (horizontal lists)
          _buildCatalogTeasers(context, isDark),

            // CTA Section
            _buildCTASection(context, isDark),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroSection(BuildContext context, bool isDark, Size screenSize) {
    final heroGradient = isDark ? AppGradients.heroDark : AppGradients.heroLight;
    final isMobile = screenSize.width < AppBreakpoints.md;
    
    return Container(
      width: double.infinity,
      constraints: BoxConstraints(
        minHeight: isMobile ? screenSize.height * 0.4 : screenSize.height * 0.5,
        maxHeight: isMobile ? screenSize.height * 0.7 : screenSize.height * 0.6,
      ),
      decoration: BoxDecoration(
        gradient: heroGradient,
      ),
      child: Stack(
        children: [
          // Pattern overlay (simplified)
          Positioned.fill(
            child: Container(
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.1),
              ),
            ),
          ),
          
          // Content
          SafeArea(
            child: Padding(
              padding: EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: isMobile ? AppSpacing.lg : AppSpacing.xl2,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(height: isMobile ? AppSpacing.lg : AppSpacing.xl2),
                  
                  // Logo/Title
                  FittedBox(
                    fit: BoxFit.scaleDown,
                    child: Text(
                      'SoleMate',
                      style: AppTypography.headline1.copyWith(
                        color: AppColors.primaryForeground,
                        fontWeight: AppTypography.bold,
                        fontSize: isMobile ? 32 : 48,
                      ),
                    ),
                  ),
                  
                  SizedBox(height: isMobile ? AppSpacing.sm : AppSpacing.md),
                  
                  // Subtitle
                  FittedBox(
                    fit: BoxFit.scaleDown,
                    child: Text(
                      'AR Shoe Try-On Experience',
                      style: AppTypography.headline4.copyWith(
                        color: AppColors.secondary,
                        fontWeight: AppTypography.semibold,
                        fontSize: isMobile ? 18 : 24,
                      ),
                    ),
                  ),
                  
                  SizedBox(height: isMobile ? AppSpacing.md : AppSpacing.lg),
                  
                  // Description
                  Text(
                    'Experience shoes in augmented reality before you buy. Try on, customize, and find the perfect match for your style.',
                    style: AppTypography.bodyLarge.copyWith(
                      color: AppColors.primaryForeground.withOpacity(0.9),
                      fontSize: isMobile ? 14 : 16,
                    ),
                    textAlign: TextAlign.center,
                    maxLines: isMobile ? 3 : 4,
                    overflow: TextOverflow.ellipsis,
                  ),
                  
                  SizedBox(height: isMobile ? AppSpacing.lg : AppSpacing.xl2),
                  
                  // CTA Buttons
                  if (isMobile) ...[
                    // Mobile: Stacked buttons with reduced spacing
                    Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SizedBox(
                          width: double.infinity,
                          child: PrimaryButton(
                            text: 'Start AR Try-On',
                            onPressed: () => _navigateToMainApp(context),
                            size: CustomButtonSize.medium,
                            isFullWidth: true,
                            icon: Icons.camera_alt,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.md),
                        SizedBox(
                          width: double.infinity,
                          child: OutlineButton(
                            text: 'Learn More',
                            onPressed: () => _scrollToFeatures(context),
                            size: CustomButtonSize.medium,
                            isFullWidth: true,
                          ),
                        ),
                      ],
                    ),
                  ] else ...[
                    // Desktop: Side by side buttons
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Flexible(
                          child: PrimaryButton(
                            text: 'Start AR Try-On',
                            onPressed: () => _navigateToMainApp(context),
                            size: CustomButtonSize.large,
                            icon: Icons.camera_alt,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.lg),
                        Flexible(
                          child: OutlineButton(
                            text: 'Learn More',
                            onPressed: () => _scrollToFeatures(context),
                            size: CustomButtonSize.large,
                          ),
                        ),
                      ],
                    ),
                  ],
                  
                  SizedBox(height: isMobile ? AppSpacing.lg : AppSpacing.xl2),
                  
                  // Hero Image with Glow Effect
                  Container(
                    width: isMobile ? 120 : 200,
                    height: isMobile ? 120 : 200,
                    decoration: BoxDecoration(
                      borderRadius: AppRadius.radiusFull,
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.secondary20,
                          blurRadius: isMobile ? 30 : 60,
                          spreadRadius: isMobile ? 10 : 20,
                        ),
                      ],
                    ),
                    child: ClipRRect(
                      borderRadius: AppRadius.radiusFull,
                      child: Image.asset(
                        'assets/images/hero-shoe.jpg',
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          return Container(
                            decoration: BoxDecoration(
                              gradient: AppGradients.elementGradient,
                              borderRadius: AppRadius.radiusFull,
                            ),
                            child: Center(
                              child: Icon(
                                Icons.shopping_bag,
                                size: isMobile ? 40 : 80,
                                color: AppColors.accent,
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeaturesSection(BuildContext context, bool isDark) {
    final screenSize = MediaQuery.of(context).size;
    final isMobile = screenSize.width < AppBreakpoints.md;
    
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: isMobile ? AppSpacing.lg : AppSpacing.xl2,
      ),
      child: Column(
        children: [
          SizedBox(height: isMobile ? AppSpacing.xl2 : AppSpacing.xl4),
          
          // Section Title
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Text(
              'Why Choose SoleMate?',
              style: AppTypography.headline2.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                fontSize: isMobile ? 24 : 32,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          
          SizedBox(height: isMobile ? AppSpacing.lg : AppSpacing.xl2),
          
          // Features Grid
          FeatureCardsGrid(
            features: SoleMateFeatures.features,
            onFeatureTap: (index) {
              // Feature Mapping: 
              // index matches the intended screen index in MainAppShell:
              // 0: AR, 1: Closet, 2: Outfit, 3: Customize
              _navigateToMainApp(context, initialTab: index);
            },
          ),
          
          SizedBox(height: isMobile ? AppSpacing.xl2 : AppSpacing.xl4),
        ],
      ),
    );
  }

  Widget _buildCTASection(BuildContext context, bool isDark) {
    final screenSize = MediaQuery.of(context).size;
    final isMobile = screenSize.width < AppBreakpoints.md;
    
    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: isMobile ? AppSpacing.lg : AppSpacing.xl2,
      ),
      decoration: BoxDecoration(
        color: AppColors.primary5,
      ),
      child: Column(
        children: [
          SizedBox(height: isMobile ? AppSpacing.xl2 : AppSpacing.xl4),
          
          // CTA Content
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 768),
            child: Column(
              children: [
                FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Text(
                    'Ready to Transform Your Shopping Experience?',
                    style: AppTypography.headline3.copyWith(
                      color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                      fontSize: isMobile ? 20 : 28,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
                
                SizedBox(height: isMobile ? AppSpacing.md : AppSpacing.lg),
                
                Text(
                  'Join thousands of users who have revolutionized their shoe shopping with AR technology.',
                  style: AppTypography.bodyLarge.copyWith(
                    color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                    fontSize: isMobile ? 14 : 16,
                  ),
                  textAlign: TextAlign.center,
                  maxLines: isMobile ? 2 : 3,
                  overflow: TextOverflow.ellipsis,
                ),
                
                SizedBox(height: isMobile ? AppSpacing.lg : AppSpacing.xl2),
                
                SizedBox(
                  width: double.infinity,
                  child: PrimaryButton(
                    text: 'Get Started Now',
                    onPressed: () => _navigateToMainApp(context),
                    size: isMobile ? CustomButtonSize.medium : CustomButtonSize.large,
                    isFullWidth: true,
                    icon: Icons.arrow_forward,
                  ),
                ),
              ],
            ),
          ),
          
          SizedBox(height: isMobile ? AppSpacing.xl2 : AppSpacing.xl4),
        ],
      ),
    );
  }

  Widget _buildCatalogTeasers(BuildContext context, bool isDark) {
    final service = LocalCatalogService();
    return FutureBuilder<List<Product>>(
      future: service.loadProducts(),
      builder: (context, snapshot) {
        final List<Product> all = snapshot.data ?? <Product>[];
        final List<Product> popular = all.where((p) => p.isPopular).take(2).toList(growable: false);
        final List<Product> recents = all
            .where((p) => p.lastTriedAt != null)
            .take(2)
            .toList(growable: false);

        return Column(
          children: [
            _HorizontalSection(
              title: 'Popular Shoes',
              isDark: isDark,
              products: popular.isNotEmpty ? popular : all,
              onViewAll: () => _navigateToCatalog(context),
            ),
            _HorizontalSection(
              title: 'Recently Tried',
              isDark: isDark,
              products: recents,
              onViewAll: () => _navigateToCatalog(context),
            ),
          ],
        );
      },
    );
  }

  void _navigateToCatalog(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const CatalogScreen()),
    );
  }

  void _navigateToMainApp(BuildContext context, {int initialTab = 0}) {
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => MainAppShell(initialIndex: initialTab),
      ),
    );
  }

  void _scrollToFeatures(BuildContext context) {
    // Scroll to features section
    Scrollable.ensureVisible(
      context,
      duration: const Duration(milliseconds: 500),
      curve: Curves.easeInOut,
    );
  }
}

class _HorizontalSection extends StatelessWidget {
  const _HorizontalSection({
    required this.title,
    required this.isDark,
    required this.products,
    required this.onViewAll,
  });

  final String title;
  final bool isDark;
  final List<Product> products;
  final VoidCallback onViewAll;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.lg,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: AppTypography.headline4.copyWith(
                  color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                ),
              ),
              TextButton(
                onPressed: onViewAll,
                child: const Text('View all'),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          SizedBox(
            height: 240,
            child: products.isEmpty
                ? Center(
                    child: Text(
                      'No items to show',
                      style: AppTypography.bodyMedium.copyWith(
                        color: isDark
                            ? AppColors.darkMutedForeground
                            : AppColors.lightMutedForeground,
                      ),
                    ),
                  )
                : ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: products.length,
                    separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.lg),
                    itemBuilder: (context, index) {
                      final p = products[index];
                      return SizedBox(
                        width: 180,
                        child: ProductCard(
                          imagePath: p.thumbnailUrl ?? '',
                          title: p.name,
                          price: p.price,
                          onTap: onViewAll,
                          showActions: false,
                          aspectRatio: 0.85,
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
