import 'package:flutter/material.dart';
import 'dart:async';
import 'package:provider/provider.dart';
import '../services/navigation_service.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../services/tryon_api_service.dart';
import '../ar/ar_main.dart';
import '../models/product.dart';
import '../repositories/catalog_repository.dart';
import 'auth/login_screen.dart';
import 'catalog_screen.dart';
import 'outfit_match_screen.dart';

/// AR Try-On screen with camera preview and shoe selection
class ARTryOnScreen extends StatefulWidget {
  const ARTryOnScreen({super.key, this.selectedProduct});

  final Product? selectedProduct;

  @override
  State<ARTryOnScreen> createState() => _ARTryOnScreenState();
}

class _ARTryOnScreenState extends State<ARTryOnScreen> {
  final AuthService _authService = AuthService();
  final TryOnApiService _tryOnService = TryOnApiService();
  final ARMain _arMain = ARMain();
  int _selectedShoeIndex = 0;
  bool _isARActive = false;
  bool _isSaving = false;
  final CatalogRepository _catalogRepository = CatalogRepository();
  List<Product> _products = [];
  bool _isLoadingProducts = true;
  Product? _selectedProduct;
  StreamSubscription<String>? _voiceSubscription;

  @override
  void initState() {
    super.initState();
    _selectedProduct = widget.selectedProduct;
    _loadProducts();
  }

  Future<void> _loadProducts() async {
    setState(() => _isLoadingProducts = true);
    try {
      final products = await _catalogRepository.getProducts();
      if (mounted) {
        setState(() {
          _products = products;
          _isLoadingProducts = false;
          // Pre-select first product if requested product is null
          if (_selectedProduct == null && products.isNotEmpty) {
            _selectedProduct = products[0];
          }
        });
      }
    } catch (e) {
      debugPrint('Error loading products for AR selection: $e');
      if (mounted) {
        setState(() => _isLoadingProducts = false);
      }
    }
  }

  /// Opens AR Camera using ARMain widget
  Future<void> _openARView() async {
    setState(() => _isARActive = true);
    final lensId = _selectedProduct?.arLensId;
    final groupId = _selectedProduct?.arLensGroupId;
    await _arMain.checkPermissionsAndOpenAR(context, lensId: lensId, groupId: groupId);
    setState(() => _isARActive = false);
  }

  /// Reset AR session
  Future<void> _resetAR() async {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('AR session reset')),
    );
  }

  /// Save AR session to backend
  Future<void> _saveAR() async {
    // Need a selected product to save
    if (_selectedProduct == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select a shoe first')),
      );
      return;
    }

    setState(() => _isSaving = true);

    try {
      final result = await _tryOnService.saveTryOn(
        shoeId: _selectedProduct!.id,
        snapshotUrl: null, // TODO: Add actual snapshot URL when AR capture is implemented
        customSkinApplied: false,
      );

      if (mounted) {
        if (result != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Try-on saved to your closet!'),
              backgroundColor: Colors.green,
            ),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Failed to save. Please log in and try again.'),
              backgroundColor: Colors.orange,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error saving: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
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
                  MaterialPageRoute(builder: (_) => CatalogScreen()),
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
            
            // Shoe Selection
            if (_isLoadingProducts)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(AppSpacing.xl),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (_products.isEmpty)
              Padding(
                padding: const EdgeInsets.all(AppSpacing.xl),
                child: Column(
                  children: [
                    const Icon(Icons.info_outline, size: 48, color: AppColors.accent),
                    const SizedBox(height: AppSpacing.md),
                    Text(
                      'No shoes found in catalog.',
                      style: AppTypography.bodyLarge,
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    OutlineButton(
                      text: 'Retry',
                      onPressed: _loadProducts,
                      size: CustomButtonSize.small,
                    ),
                  ],
                ),
              )
            else
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  crossAxisSpacing: AppSpacing.lg,
                  mainAxisSpacing: AppSpacing.lg,
                  childAspectRatio: 1.0,
                ),
                itemCount: _products.length,
                itemBuilder: (context, index) {
                  final shoe = _products[index];
                  final isSelected = _selectedProduct?.id == shoe.id;
                  
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedProduct = shoe;
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
                          Positioned.fill(
                            child: ClipRRect(
                              borderRadius: AppRadius.radiusLarge,
                              child: (shoe.thumbnailUrl != null && shoe.thumbnailUrl!.startsWith('http'))
                                ? Image.network(
                                    shoe.thumbnailUrl!,
                                    fit: BoxFit.cover,
                                    errorBuilder: (_, __, ___) => const Center(child: Icon(Icons.shopping_bag, size: 40)),
                                  )
                                : Image.asset(
                                    shoe.thumbnailUrl ?? 'assets/images/shoes/nike_journey_run.png',
                                    fit: BoxFit.cover,
                                    errorBuilder: (_, __, ___) => const Center(child: Icon(Icons.shopping_bag, size: 40)),
                                  ),
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
                          
                          // Name overlay
                          Positioned(
                            bottom: 0,
                            left: 0,
                            right: 0,
                            child: Container(
                              padding: const EdgeInsets.all(AppSpacing.xs),
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.bottomCenter,
                                  end: Alignment.topCenter,
                                  colors: [Colors.black.withOpacity(0.6), Colors.transparent],
                                ),
                                borderRadius: const BorderRadius.vertical(bottom: Radius.circular(AppRadius.lg)),
                              ),
                              child: Text(
                                shoe.name,
                                style: AppTypography.caption.copyWith(color: Colors.white),
                                textAlign: TextAlign.center,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
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
                  onPressed: _saveAR,
                  icon: Icons.add_shopping_cart,
                  isFullWidth: true,
                ),
                const SizedBox(height: AppSpacing.lg),
                SecondaryButton(
                  text: 'View in 3D',
                  onPressed: () => _openARView(),
                  icon: Icons.view_in_ar,
                  isFullWidth: true,
                ),
                const SizedBox(height: AppSpacing.lg),
                OutlineButton(
                  text: 'Match with Outfit',
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const OutfitMatchScreen(),
                      ),
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
