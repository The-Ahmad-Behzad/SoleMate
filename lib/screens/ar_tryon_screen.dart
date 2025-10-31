import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../ar/ar_main.dart';
import '../models/product.dart';
import 'auth/login_screen.dart';
import 'catalog_screen.dart';

/// AR Try-On screen with camera preview and shoe selection
class ARTryOnScreen extends StatefulWidget {
  const ARTryOnScreen({super.key, this.selectedProduct});

  final Product? selectedProduct;

  @override
  State<ARTryOnScreen> createState() => _ARTryOnScreenState();
}

class _ARTryOnScreenState extends State<ARTryOnScreen> {
  final AuthService _authService = AuthService();
  final ARMain _arMain = ARMain();
  int _selectedShoeIndex = 0;
  bool _isARActive = false;
  Product? _selectedProduct;

  @override
  void initState() {
    super.initState();
    _selectedProduct = widget.selectedProduct;
  }

  /// Opens AR Camera using ARMain widget
  Future<void> _openARView() async {
    setState(() => _isARActive = true);
    await _arMain.checkPermissionsAndOpenAR(context);
    setState(() => _isARActive = false);
  }

  /// Reset AR session
  Future<void> _resetAR() async {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('AR session reset')),
    );
  }

  /// Save AR session
  Future<void> _saveAR() async {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('AR session saved to closet')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final screenSize = MediaQuery.of(context).size;
    final isMobile = screenSize.width < AppBreakpoints.md;

    return Scaffold(
      appBar: AppBar(
        leading: const LogoButton(),
        title: const Text('AR Try-On'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const CatalogScreen()),
              );
            },
            child: const Text('View all'),
          ),
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
            
            // Main Content
            if (isMobile) ...[
              // Mobile: Stacked layout
              _buildCameraCard(context, isDark, screenSize),
              const SizedBox(height: AppSpacing.xl2),
              _buildShoeSelectionCard(context, isDark),
              const SizedBox(height: AppSpacing.xl2),
              _buildQuickActionsCard(context, isDark),
            ] else ...[
              // Desktop: Side-by-side layout
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Left: Camera Card
                  Expanded(
                    flex: 1,
                    child: _buildCameraCard(context, isDark, screenSize),
                  ),
                  const SizedBox(width: AppSpacing.xl2),
                  // Right: Shoe Selection and Actions
                  Expanded(
                    flex: 1,
                    child: Column(
                      children: [
                        _buildShoeSelectionCard(context, isDark),
                        const SizedBox(height: AppSpacing.xl2),
                        _buildQuickActionsCard(context, isDark),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPageHeader(BuildContext context, bool isDark) {
    return Column(
      children: [
        Text(
          'AR Shoe Try-On',
          style: AppTypography.headline2.copyWith(
            color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          'Experience shoes in augmented reality before you buy',
          style: AppTypography.bodyLarge.copyWith(
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
          textAlign: TextAlign.center,
        ),
        if (_selectedProduct != null) ...[
          const SizedBox(height: AppSpacing.md),
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.xs,
            ),
            decoration: BoxDecoration(
              color: AppColors.accent10,
              borderRadius: AppRadius.radiusLarge,
              border: Border.all(color: AppColors.accent),
            ),
            child: Text(
              'Selected: ' + _selectedProduct!.name,
              style: AppTypography.bodyMedium.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                fontWeight: AppTypography.semibold,
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildCameraCard(BuildContext context, bool isDark, Size screenSize) {
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
          children: [
            // Camera Preview
            Container(
              width: double.infinity,
              height: 300,
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusLarge,
                gradient: AppGradients.overlayGradient,
              ),
              child: Stack(
                children: [
                  // Camera placeholder
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.camera_alt,
                          size: 80,
                          color: AppColors.accent,
                        ),
                        const SizedBox(height: AppSpacing.lg),
                        Text(
                          'Camera Preview',
                          style: AppTypography.bodyLarge.copyWith(
                            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        Text(
                          'Tap "Enable AR" to start',
                          style: AppTypography.bodyMedium.copyWith(
                            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                          ),
                        ),
                      ],
                    ),
                  ),
                  
                  // AR Status indicator
                  Positioned(
                    top: AppSpacing.lg,
                    right: AppSpacing.lg,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppSpacing.md,
                        vertical: AppSpacing.xs,
                      ),
                      decoration: BoxDecoration(
                        color: _isARActive ? AppColors.accent : AppColors.lightMuted,
                        borderRadius: AppRadius.radiusLarge,
                      ),
                      child: Text(
                        _isARActive ? 'AR Active' : 'AR Ready',
                        style: AppTypography.caption.copyWith(
                          color: _isARActive ? AppColors.accentForeground : AppColors.lightMutedForeground,
                          fontWeight: AppTypography.medium,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Action Buttons
            if (screenSize.width < AppBreakpoints.md) ...[
              // Mobile: Stacked buttons
              Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: OutlineButton(
                          text: 'Reset',
                          onPressed: _resetAR,
                          icon: Icons.refresh,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.lg),
                      Expanded(
                        child: PrimaryButton(
                          text: 'Enable AR',
                          onPressed: _openARView,
                          icon: Icons.camera_alt,
                          isLoading: _isARActive,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),
                  SecondaryButton(
                    text: 'Save',
                    onPressed: _saveAR,
                    icon: Icons.download,
                    isFullWidth: true,
                  ),
                ],
              ),
            ] else ...[
              // Desktop: All in one row
              Row(
                children: [
                  Expanded(
                    child: OutlineButton(
                      text: 'Reset',
                      onPressed: _resetAR,
                      icon: Icons.refresh,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.lg),
                  Expanded(
                    child: PrimaryButton(
                      text: 'Enable AR',
                      onPressed: _openARView,
                      icon: Icons.camera_alt,
                      isLoading: _isARActive,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.lg),
                  Expanded(
                    child: SecondaryButton(
                      text: 'Save',
                      onPressed: _saveAR,
                      icon: Icons.download,
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildShoeSelectionCard(BuildContext context, bool isDark) {
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
            Text(
              'Select Shoe',
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            
            // Shoe Grid
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: AppSpacing.lg,
                mainAxisSpacing: AppSpacing.lg,
                childAspectRatio: 1.0,
              ),
              itemCount: SampleShoes.arSelection.length,
              itemBuilder: (context, index) {
                final shoe = SampleShoes.arSelection[index];
                final isSelected = _selectedShoeIndex == index;
                
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _selectedShoeIndex = index;
                    });
                  },
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: AppRadius.radiusLarge,
                      border: Border.all(
                        color: isSelected ? AppColors.secondary : AppColors.lightBorder,
                        width: isSelected ? 2 : 1,
                      ),
                      gradient: AppGradients.elementGradient,
                    ),
                    child: Stack(
                      children: [
                        // Shoe Image
                        ClipRRect(
                          borderRadius: AppRadius.radiusLarge,
                          child: Image.asset(
                            shoe.imagePath,
                            fit: BoxFit.cover,
                            width: double.infinity,
                            height: double.infinity,
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
                                    size: 40,
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                        
                        // Selection indicator
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
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuickActionsCard(BuildContext context, bool isDark) {
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
            Text(
              'Quick Actions',
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            
            // Action Buttons
            Column(
              children: [
                PrimaryButton(
                  text: 'Add to Closet',
                  onPressed: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Added to closet!')),
                    );
                  },
                  icon: Icons.add_shopping_cart,
                  isFullWidth: true,
                ),
                const SizedBox(height: AppSpacing.lg),
                SecondaryButton(
                  text: 'View in 3D',
                  onPressed: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('3D view coming soon!')),
                    );
                  },
                  icon: Icons.view_in_ar,
                  isFullWidth: true,
                ),
                const SizedBox(height: AppSpacing.lg),
                OutlineButton(
                  text: 'Match with Outfit',
                  onPressed: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Outfit matching coming soon!')),
                    );
                  },
                  icon: Icons.auto_awesome,
                  isFullWidth: true,
                ),
              ],
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
