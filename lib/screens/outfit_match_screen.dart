import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import 'auth/login_screen.dart';

/// Outfit Match screen with current shoe and outfit suggestions
class OutfitMatchScreen extends StatefulWidget {
  const OutfitMatchScreen({super.key});

  @override
  State<OutfitMatchScreen> createState() => _OutfitMatchScreenState();
}

class _OutfitMatchScreenState extends State<OutfitMatchScreen> {
  final AuthService _authService = AuthService();
  int _selectedShoeIndex = 0;
  int _selectedOutfitIndex = 0;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final screenSize = MediaQuery.of(context).size;
    final isMobile = screenSize.width < AppBreakpoints.md;

    return Scaffold(
      appBar: AppBar(
        leading: const LogoButton(),
        title: const Text('Outfit Match'),
        actions: [
          PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'logout') {
                _handleLogout();
              }
            },
            itemBuilder: (BuildContext context) => [
              const PopupMenuItem<String>(
                value: 'logout',
                child: Row(
                  children: [
                    Icon(Icons.logout),
                    SizedBox(width: AppSpacing.sm),
                    Text('Logout'),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: AppSpacing.paddingLarge,
        child: Column(
          children: [
            // Page Header
            _buildPageHeader(context, isDark),
            
            const SizedBox(height: AppSpacing.xl3),
            
            // Top Section: Current Shoe + Outfit Preview
            if (isMobile) ...[
              // Mobile: Stacked layout
              _buildCurrentShoeCard(context, isDark),
              const SizedBox(height: AppSpacing.xl2),
              _buildOutfitPreviewCard(context, isDark),
            ] else ...[
              // Desktop: Side-by-side layout
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Current Shoe Card (1 column)
                  Expanded(
                    flex: 1,
                    child: _buildCurrentShoeCard(context, isDark),
                  ),
                  const SizedBox(width: AppSpacing.xl2),
                  // Outfit Preview Card (2 columns)
                  Expanded(
                    flex: 2,
                    child: _buildOutfitPreviewCard(context, isDark),
                  ),
                ],
              ),
            ],
            
            const SizedBox(height: AppSpacing.xl4),
            
            // Suggestions Section
            _buildSuggestionsSection(context, isDark),
          ],
        ),
      ),
    );
  }

  Widget _buildPageHeader(BuildContext context, bool isDark) {
    return Column(
      children: [
        Text(
          'AI Outfit Matching',
          style: AppTypography.headline2.copyWith(
            color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          'Get AI-powered outfit suggestions for your shoes',
          style: AppTypography.bodyLarge.copyWith(
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildCurrentShoeCard(BuildContext context, bool isDark) {
    final selectedShoe = SampleShoes.shoes[_selectedShoeIndex];
    
    return Card(
      elevation: 0,
      shadowColor: isDark 
          ? AppColors.lightForeground.withOpacity(0.15)
          : AppColors.lightForeground.withOpacity(0.15),
      shape: RoundedRectangleBorder(
        borderRadius: AppRadius.radiusLarge,
      ),
      child: Container(
        padding: AppSpacing.paddingLarge,
        decoration: BoxDecoration(
          borderRadius: AppRadius.radiusLarge,
          gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              children: [
                const Icon(
                  Icons.shopping_bag,
                  color: AppColors.accent,
                  size: 24,
                ),
                const SizedBox(width: AppSpacing.sm),
                Text(
                  'Current Shoe',
                  style: AppTypography.headline4.copyWith(
                    color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            // Shoe Preview
            Container(
              width: double.infinity,
              height: 200,
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusLarge,
                gradient: AppGradients.elementGradient,
              ),
              child: ClipRRect(
                borderRadius: AppRadius.radiusLarge,
                child: Image.asset(
                  selectedShoe.imagePath,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      decoration: BoxDecoration(
                        gradient: AppGradients.elementGradient,
                        borderRadius: AppRadius.radiusLarge,
                      ),
                      child: const Center(
                        child: Icon(
                          Icons.shopping_bag,
                          color: AppColors.accent,
                          size: 60,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            // Shoe Info
            Text(
              selectedShoe.title,
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: AppTypography.semibold,
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              selectedShoe.subtitle ?? 'Try-on date: 2024-01-15',
              style: AppTypography.bodySmall.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Change Shoe Button
            OutlineButton(
              text: 'Change Shoe',
              onPressed: _showShoeSelector,
              isFullWidth: true,
              icon: Icons.swap_horiz,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOutfitPreviewCard(BuildContext context, bool isDark) {
    return Card(
      elevation: 0,
      shadowColor: isDark 
          ? AppColors.lightForeground.withOpacity(0.15)
          : AppColors.lightForeground.withOpacity(0.15),
      shape: RoundedRectangleBorder(
        borderRadius: AppRadius.radiusLarge,
      ),
      child: Container(
        padding: AppSpacing.paddingLarge,
        decoration: BoxDecoration(
          borderRadius: AppRadius.radiusLarge,
          gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              children: [
                const Icon(
                  Icons.auto_awesome,
                  color: AppColors.accent,
                  size: 24,
                ),
                const SizedBox(width: AppSpacing.sm),
                Text(
                  'Outfit Preview',
                  style: AppTypography.headline4.copyWith(
                    color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            // Outfit Preview
            Container(
              width: double.infinity,
              height: 200,
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusLarge,
                gradient: AppGradients.overlayGradient,
              ),
              child: Stack(
                children: [
                  // Outfit placeholder
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.checkroom,
                          size: 60,
                          color: AppColors.accent,
                        ),
                        const SizedBox(height: AppSpacing.lg),
                        Text(
                          'AI Generated Outfit',
                          style: AppTypography.bodyLarge.copyWith(
                            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        Text(
                          'Tap "Generate" to create',
                          style: AppTypography.bodyMedium.copyWith(
                            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                          ),
                        ),
                      ],
                    ),
                  ),
                  
                  // Generate button overlay
                  Positioned(
                    top: AppSpacing.lg,
                    right: AppSpacing.lg,
                    child: PrimaryButton(
                      text: 'Generate',
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Generating outfit...')),
                        );
                      },
                      size: CustomButtonSize.small,
                      icon: Icons.auto_awesome,
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            // Outfit Info
            Text(
              'Casual Street Style',
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: AppTypography.semibold,
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              'Perfect for everyday wear',
              style: AppTypography.bodySmall.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Action Buttons
            Row(
              children: [
                Expanded(
                  child: PrimaryButton(
                    text: 'Save Outfit',
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Outfit saved!')),
                      );
                    },
                    icon: Icons.bookmark,
                  ),
                ),
                const SizedBox(width: AppSpacing.lg),
                Expanded(
                  child: OutlineButton(
                    text: 'Try On',
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Try-on coming soon!')),
                      );
                    },
                    icon: Icons.camera_alt,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSuggestionsSection(BuildContext context, bool isDark) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Section Header
        Text(
          'More Outfit Suggestions',
          style: AppTypography.headline3.copyWith(
            color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          'Discover more styles that match your shoe',
          style: AppTypography.bodyLarge.copyWith(
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
        ),
        
        const SizedBox(height: AppSpacing.xl2),
        
        // Outfit Suggestions Grid
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: AppBreakpoints.getColumnCount(context),
            crossAxisSpacing: AppSpacing.xl2,
            mainAxisSpacing: AppSpacing.xl2,
            childAspectRatio: 0.75, // 3:4 aspect ratio
          ),
          itemCount: _outfitSuggestions.length,
          itemBuilder: (context, index) {
            final outfit = _outfitSuggestions[index];
            final isSelected = _selectedOutfitIndex == index;
            
            return GestureDetector(
              onTap: () {
                setState(() {
                  _selectedOutfitIndex = index;
                });
              },
              child: Card(
                elevation: 0,
                shadowColor: isDark 
                    ? AppColors.lightForeground.withOpacity(0.15)
                    : AppColors.lightForeground.withOpacity(0.15),
                shape: RoundedRectangleBorder(
                  borderRadius: AppRadius.radiusLarge,
                  side: BorderSide(
                    color: isSelected ? AppColors.secondary : Colors.transparent,
                    width: 2,
                  ),
                ),
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: AppRadius.radiusLarge,
                    gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Outfit Preview
                      Expanded(
                        flex: 3,
                        child: Container(
                          width: double.infinity,
                          decoration: BoxDecoration(
                            borderRadius: const BorderRadius.only(
                              topLeft: Radius.circular(AppRadius.lg),
                              topRight: Radius.circular(AppRadius.lg),
                            ),
                            gradient: AppGradients.elementGradient,
                          ),
                          child: Stack(
                            children: [
                              Center(
                                child: Icon(
                                  outfit.icon,
                                  size: 60,
                                  color: AppColors.accent,
                                ),
                              ),
                              if (isSelected)
                                Positioned(
                                  top: AppSpacing.sm,
                                  right: AppSpacing.sm,
                                  child: Container(
                                    padding: const EdgeInsets.all(AppSpacing.xs),
                                    decoration: BoxDecoration(
                                      color: AppColors.secondary,
                                      borderRadius: AppRadius.radiusFull,
                                    ),
                                    child: const Icon(
                                      Icons.check,
                                      color: AppColors.secondaryForeground,
                                      size: 16,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ),
                      
                      // Outfit Info
                      Expanded(
                        flex: 2,
                        child: Padding(
                          padding: AppSpacing.paddingMedium,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                outfit.title,
                                style: AppTypography.bodyMedium.copyWith(
                                  fontWeight: AppTypography.semibold,
                                  color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: AppSpacing.xs),
                              Text(
                                outfit.description,
                                style: AppTypography.caption.copyWith(
                                  color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                                ),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ],
    );
  }

  void _showShoeSelector() {
    showModalBottomSheet(
      context: context,
      builder: (context) => Container(
        padding: AppSpacing.paddingLarge,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Select Shoe',
              style: AppTypography.headline4,
            ),
            const SizedBox(height: AppSpacing.lg),
            GridView.builder(
              shrinkWrap: true,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: AppSpacing.md,
                mainAxisSpacing: AppSpacing.md,
                childAspectRatio: 1.0,
              ),
              itemCount: SampleShoes.shoes.length,
              itemBuilder: (context, index) {
                final shoe = SampleShoes.shoes[index];
                final isSelected = _selectedShoeIndex == index;
                
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _selectedShoeIndex = index;
                    });
                    Navigator.pop(context);
                  },
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: AppRadius.radiusLarge,
                      border: Border.all(
                        color: isSelected ? AppColors.secondary : AppColors.lightBorder,
                        width: isSelected ? 2 : 1,
                      ),
                    ),
                    child: ClipRRect(
                      borderRadius: AppRadius.radiusLarge,
                      child: Image.asset(
                        shoe.imagePath,
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          return Container(
                            decoration: BoxDecoration(
                              gradient: AppGradients.elementGradient,
                              borderRadius: AppRadius.radiusLarge,
                            ),
                            child: const Center(
                              child: Icon(
                                Icons.shopping_bag,
                                color: AppColors.accent,
                                size: 30,
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _handleLogout() async {
    try {
      await _authService.logout();
      if (mounted) {
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(builder: (_) => const LoginScreen()),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Logout failed: ${e.toString()}')),
        );
      }
    }
  }
}

/// Outfit suggestion data
class OutfitSuggestion {
  const OutfitSuggestion({
    required this.title,
    required this.description,
    required this.icon,
  });

  final String title;
  final String description;
  final IconData icon;
}

/// Sample outfit suggestions
final List<OutfitSuggestion> _outfitSuggestions = [
  const OutfitSuggestion(
    title: 'Business Casual',
    description: 'Perfect for office meetings',
    icon: Icons.business_center,
  ),
  const OutfitSuggestion(
    title: 'Weekend Vibes',
    description: 'Relaxed and comfortable',
    icon: Icons.wb_sunny,
  ),
  const OutfitSuggestion(
    title: 'Date Night',
    description: 'Elegant and sophisticated',
    icon: Icons.favorite,
  ),
  const OutfitSuggestion(
    title: 'Gym Ready',
    description: 'Active and sporty',
    icon: Icons.fitness_center,
  ),
];
