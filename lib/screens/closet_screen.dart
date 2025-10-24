import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import 'auth/login_screen.dart';

/// My Closet screen with shoe grid and filters
class ClosetScreen extends StatefulWidget {
  const ClosetScreen({super.key});

  @override
  State<ClosetScreen> createState() => _ClosetScreenState();
}

class _ClosetScreenState extends State<ClosetScreen> {
  final AuthService _authService = AuthService();
  ClosetFilter _currentFilter = ClosetFilter.all;
  final List<ProductCardData> _shoes = SampleShoes.shoes;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        leading: const LogoButton(),
        title: const Text('My Closet'),
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
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Page Header
            Text(
              'My Shoe Collection',
              style: AppTypography.headline2.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              '${_getFilteredShoes().length} shoes in your collection',
              style: AppTypography.bodyMedium.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Filter Badges
            _buildFilterBadges(context, isDark),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Shoe Grid
            _buildShoeGrid(context, isDark),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterBadges(BuildContext context, bool isDark) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          _buildFilterBadge(
            context: context,
            label: 'All',
            filter: ClosetFilter.all,
            isDark: isDark,
          ),
          const SizedBox(width: AppSpacing.md),
          _buildFilterBadge(
            context: context,
            label: 'Favorites',
            filter: ClosetFilter.favorites,
            isDark: isDark,
          ),
          const SizedBox(width: AppSpacing.md),
          _buildFilterBadge(
            context: context,
            label: 'Recent',
            filter: ClosetFilter.recent,
            isDark: isDark,
          ),
        ],
      ),
    );
  }

  Widget _buildFilterBadge({
    required BuildContext context,
    required String label,
    required ClosetFilter filter,
    required bool isDark,
  }) {
    final isActive = _currentFilter == filter;
    
    return GestureDetector(
      onTap: () {
        setState(() {
          _currentFilter = filter;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.sm,
        ),
        decoration: BoxDecoration(
          color: isActive 
              ? AppColors.accent 
              : Colors.transparent,
          borderRadius: AppRadius.radiusLarge,
          border: Border.all(
            color: isActive 
                ? AppColors.accent 
                : (isDark ? AppColors.darkBorder : AppColors.lightBorder),
          ),
        ),
        child: Text(
          label,
          style: AppTypography.bodyMedium.copyWith(
            color: isActive 
                ? AppColors.accentForeground
                : (isDark ? AppColors.darkForeground : AppColors.lightForeground),
            fontWeight: isActive ? AppTypography.semibold : AppTypography.normal,
          ),
        ),
      ),
    );
  }

  Widget _buildShoeGrid(BuildContext context, bool isDark) {
    final filteredShoes = _getFilteredShoes();
    
    if (filteredShoes.isEmpty) {
      return _buildEmptyState(context, isDark);
    }

    return ProductCardsGrid(
      products: filteredShoes,
      aspectRatio: 1.0,
      showActions: true,
      actionText: 'Try Again',
      onProductTap: (index) {
        final shoe = filteredShoes[index];
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Selected: ${shoe.title}')),
        );
      },
      onFavoriteToggle: (index) {
        setState(() {
          final shoe = filteredShoes[index];
          final originalIndex = _shoes.indexWhere((s) => s.title == shoe.title);
          if (originalIndex != -1) {
            _shoes[originalIndex] = ProductCardData(
              imagePath: _shoes[originalIndex].imagePath,
              title: _shoes[originalIndex].title,
              subtitle: _shoes[originalIndex].subtitle,
              isFavorite: !_shoes[originalIndex].isFavorite,
            );
          }
        });
      },
      onAction: (index) {
        final shoe = filteredShoes[index];
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Trying on: ${shoe.title}')),
        );
      },
    );
  }

  Widget _buildEmptyState(BuildContext context, bool isDark) {
    return Center(
      child: Padding(
        padding: AppSpacing.paddingLarge,
        child: Column(
          children: [
            Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                color: AppColors.accent10,
                borderRadius: AppRadius.radiusFull,
              ),
              child: const Icon(
                Icons.shopping_bag_outlined,
                size: 60,
                color: AppColors.accent,
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            Text(
              'No shoes found',
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            
            const SizedBox(height: AppSpacing.sm),
            
            Text(
              _getEmptyStateMessage(),
              style: AppTypography.bodyLarge.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
              textAlign: TextAlign.center,
            ),
            
            const SizedBox(height: AppSpacing.xl3),
            
            PrimaryButton(
              text: 'Try AR Try-On',
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Navigate to AR Try-On')),
                );
              },
              icon: Icons.camera_alt,
            ),
          ],
        ),
      ),
    );
  }

  List<ProductCardData> _getFilteredShoes() {
    switch (_currentFilter) {
      case ClosetFilter.all:
        return _shoes;
      case ClosetFilter.favorites:
        return _shoes.where((shoe) => shoe.isFavorite).toList();
      case ClosetFilter.recent:
        // Simulate recent by taking first 4 shoes
        return _shoes.take(4).toList();
    }
  }

  String _getEmptyStateMessage() {
    switch (_currentFilter) {
      case ClosetFilter.all:
        return 'Start building your collection by trying on shoes with AR!';
      case ClosetFilter.favorites:
        return 'No favorite shoes yet. Try on some shoes and mark them as favorites!';
      case ClosetFilter.recent:
        return 'No recent shoes. Try on some shoes to see them here!';
    }
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

/// Filter options for the closet
enum ClosetFilter {
  all,
  favorites,
  recent,
}
