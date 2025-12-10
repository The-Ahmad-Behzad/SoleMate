import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import 'custom_button.dart';

/// Product card widget for displaying shoes in grids
class ProductCard extends StatelessWidget {
  const ProductCard({
    super.key,
    required this.imagePath,
    required this.title,
    this.subtitle,
    this.price,
    this.isFavorite = false,
    this.onTap,
    this.onFavoriteToggle,
    this.onAction,
    this.actionText,
    this.aspectRatio = 1.0,
    this.showActions = false,
  });

  final String imagePath;
  final String title;
  final String? subtitle;
  final double? price;
  final bool isFavorite;
  final VoidCallback? onTap;
  final VoidCallback? onFavoriteToggle;
  final VoidCallback? onAction;
  final String? actionText;
  final double aspectRatio;
  final bool showActions;

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
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Image container with aspect ratio
            Expanded(
              flex: (aspectRatio * 100).round(),
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
                    // Product image
                    ClipRRect(
                      borderRadius: const BorderRadius.only(
                        topLeft: Radius.circular(AppRadius.lg),
                        topRight: Radius.circular(AppRadius.lg),
                      ),
                      child: _buildImage(imagePath),
                    ),
                    
                    // Favorite star (top-right)
                    if (onFavoriteToggle != null)
                      Positioned(
                        top: AppSpacing.md,
                        right: AppSpacing.md,
                        child: GestureDetector(
                          onTap: onFavoriteToggle,
                          child: Container(
                            padding: const EdgeInsets.all(AppSpacing.xs),
                            decoration: BoxDecoration(
                              color: isDark 
                                  ? AppColors.darkCard.withOpacity(0.8)
                                  : AppColors.lightCard.withOpacity(0.8),
                              borderRadius: AppRadius.radiusFull,
                            ),
                            child: Icon(
                              isFavorite ? Icons.star : Icons.star_border,
                              color: AppColors.secondary,
                              size: 20,
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            
            // Content area
            Padding(
              padding: AppSpacing.paddingMedium,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Title
                  Text(
                    title,
                    style: AppTypography.bodyMedium.copyWith(
                      fontWeight: AppTypography.semibold,
                      color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),

                  // Price (optional)
                  if (price != null) ...[
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      _formatPrice(price!),
                      style: AppTypography.bodyMedium.copyWith(
                        color: AppColors.secondary,
                        fontWeight: AppTypography.semibold,
                      ),
                    ),
                  ],
                  
                  // Subtitle (optional)
                  if (subtitle != null) ...[
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      subtitle!,
                      style: AppTypography.caption.copyWith(
                        color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                  
                  // Action buttons (optional)
                  if (showActions && onAction != null && actionText != null) ...[
                    const SizedBox(height: AppSpacing.md),
                    SizedBox(
                      width: double.infinity,
                      child: CustomButton(
                        text: actionText!,
                        onPressed: onAction,
                        variant: CustomButtonVariant.secondary,
                        size: CustomButtonSize.small,
                        isFullWidth: true,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildImage(String path) {
    final bool isNetwork = path.startsWith('http://') || path.startsWith('https://');
    final Widget placeholder = Container(
      decoration: BoxDecoration(
        gradient: AppGradients.elementGradient,
      ),
      child: const Center(
        child: Icon(
          Icons.image_not_supported,
          color: AppColors.accent,
          size: 48,
        ),
      ),
    );

    if (isNetwork) {
      return Image.network(
        path,
        fit: BoxFit.cover,
        width: double.infinity,
        height: double.infinity,
        errorBuilder: (context, error, stackTrace) => placeholder,
      );
    }
    return Image.asset(
      path,
      fit: BoxFit.cover,
      width: double.infinity,
      height: double.infinity,
      errorBuilder: (context, error, stackTrace) => placeholder,
    );
  }

  static String _formatPrice(double value) {
    // Simple currency formatting without intl to avoid extra deps
    return 'Rs. ' + value.toStringAsFixed(2);
  }
}

/// Grid of product cards with responsive layout
class ProductCardsGrid extends StatelessWidget {
  const ProductCardsGrid({
    super.key,
    required this.products,
    this.onProductTap,
    this.onFavoriteToggle,
    this.onAction,
    this.actionText,
    this.showActions = false,
    this.aspectRatio = 1.0,
  });

  final List<ProductCardData> products;
  final Function(int index)? onProductTap;
  final Function(int index)? onFavoriteToggle;
  final Function(int index)? onAction;
  final String? actionText;
  final bool showActions;
  final double aspectRatio;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        // Determine column count based on screen width
        int crossAxisCount = AppBreakpoints.getColumnCount(context);

        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            crossAxisSpacing: AppSpacing.xl2,
            mainAxisSpacing: AppSpacing.xl2,
            childAspectRatio: aspectRatio,
          ),
          itemCount: products.length,
          itemBuilder: (context, index) {
            final product = products[index];
            return ProductCard(
              imagePath: product.imagePath,
              title: product.title,
              subtitle: product.subtitle,
              isFavorite: product.isFavorite,
              aspectRatio: aspectRatio,
              showActions: showActions,
              actionText: actionText,
              onTap: () => onProductTap?.call(index),
              onFavoriteToggle: () => onFavoriteToggle?.call(index),
              onAction: () => onAction?.call(index),
            );
          },
        );
      },
    );
  }
}

/// Data class for product card information
class ProductCardData {
  const ProductCardData({
    required this.imagePath,
    required this.title,
    this.subtitle,
    this.isFavorite = false,
    this.modelUrl,
    this.price,
    this.brand,
    this.category,
  });

  final String imagePath;
  final String title;
  final String? subtitle;
  final bool isFavorite;
  final String? modelUrl;
  final double? price;
  final String? brand;
  final String? category;
  
  /// Create a copy with updated values
  ProductCardData copyWith({
    String? imagePath,
    String? title,
    String? subtitle,
    bool? isFavorite,
    String? modelUrl,
    double? price,
    String? brand,
    String? category,
  }) {
    return ProductCardData(
      imagePath: imagePath ?? this.imagePath,
      title: title ?? this.title,
      subtitle: subtitle ?? this.subtitle,
      isFavorite: isFavorite ?? this.isFavorite,
      modelUrl: modelUrl ?? this.modelUrl,
      price: price ?? this.price,
      brand: brand ?? this.brand,
      category: category ?? this.category,
    );
  }
}

/// Database of available shoes with real model paths
class ShoeDatabase {
  static const List<ProductCardData> allShoes = [
    ProductCardData(
      imagePath: 'assets/images/shoes/airmax_270.png',
      title: 'Air Max 270',
      subtitle: 'Nike',
      brand: 'Nike',
      category: 'Running',
      price: 150.00,
      modelUrl: 'models/shoes/airmax_270_left.glb',
      isFavorite: true,
    ),
    ProductCardData(
      imagePath: 'assets/images/shoes/caterpillar_work_boot.png',
      title: 'Work Boot',
      subtitle: 'Caterpillar',
      brand: 'Caterpillar',
      category: 'Boots',
      price: 120.00,
      modelUrl: 'models/shoes/caterpillar_work_boot_left.glb',
      isFavorite: false,
    ),
    ProductCardData(
      imagePath: 'assets/images/shoes/nike_journey_run.png',
      title: 'Journey Run',
      subtitle: 'Nike',
      brand: 'Nike',
      category: 'Running',
      price: 130.00,
      modelUrl: 'models/shoes/nike_journey_run_left.glb',
      isFavorite: true,
    ),
    ProductCardData(
      imagePath: 'assets/images/shoes/puma_winter_shoe.png',
      title: 'Winter Shoe',
      subtitle: 'Puma',
      brand: 'Puma',
      category: 'Winter',
      price: 110.00,
      modelUrl: 'models/shoes/puma_winter_shoe_left.glb',
      isFavorite: false,
    ),
  ];
  
  /// Get all unique brands
  static List<String> get brands {
    return allShoes
        .map((s) => s.brand)
        .where((b) => b != null)
        .cast<String>()
        .toSet()
        .toList();
  }
  
  /// Get all unique categories
  static List<String> get categories {
    return allShoes
        .map((s) => s.category)
        .where((c) => c != null)
        .cast<String>()
        .toSet()
        .toList();
  }
}

/// Sample shoe data for the app (legacy support)
class SampleShoes {
  static List<ProductCardData> get shoes => ShoeDatabase.allShoes;

  // Shoe selection for AR Try-On (first 4 shoes)
  static List<ProductCardData> get arSelection => ShoeDatabase.allShoes.take(4).toList();
}

