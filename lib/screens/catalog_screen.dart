import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../models/product.dart';
import '../repositories/catalog_repository.dart';
import '../widgets/product_card.dart';
import '../widgets/custom_button.dart';
import 'ar_tryon_screen.dart';
import 'customize_screen.dart';
import 'outfit_match_screen.dart';

class CatalogScreen extends StatefulWidget {
  const CatalogScreen({super.key});

  @override
  State<CatalogScreen> createState() => _CatalogScreenState();
}

class _CatalogScreenState extends State<CatalogScreen> {
  final CatalogRepository _catalog = CatalogRepository();
  final TextEditingController _searchController = TextEditingController();

  late Future<List<Product>> _future;
  List<Product> _all = <Product>[];
  List<Product> _shown = <Product>[];

  @override
  void initState() {
    super.initState();
    _future = _load();
    _searchController.addListener(_onSearchChanged);
  }

  @override
  void dispose() {
    _searchController.removeListener(_onSearchChanged);
    _searchController.dispose();
    super.dispose();
  }

  Future<List<Product>> _load({bool refresh = false}) async {
    final products = await _catalog.getProducts();
    _all = products;
    _shown = products;
    return products;
  }

  Future<void> _onSearchChanged() async {
    final text = _searchController.text.trim().toLowerCase();
    
    if (text.isEmpty) {
      if (mounted) setState(() => _shown = _all);
      return;
    }
    
    // Call backend API for search
    final results = await _catalog.searchProducts(text);
    if (mounted) {
      setState(() {
        _shown = results;
      });
    }
  }

  Future<void> _onRefresh() async {
    await _load(refresh: true);
    setState(() {});
  }

  void _openTryOn(Product product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ARTryOnScreen(selectedProduct: product),
      ),
    );
  }

  void _openCustomize(Product product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        // Assuming CustomizeScreen takes a shoe context or we pass it (will update CustomizeScreen later)
        builder: (_) => const CustomizeScreen(),
      ),
    );
  }

  void _openOutfitRecommendation(Product product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        // Pass base colors when opening outfit match
        builder: (_) => OutfitMatchScreen(baseShoe: product),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: _SearchField(controller: _searchController),
      ),
      body: FutureBuilder<List<Product>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(
              child: Padding(
                padding: AppSpacing.paddingLarge,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.error_outline, color: AppColors.accent),
                    const SizedBox(height: AppSpacing.md),
                    Text(
                      'Failed to load catalog',
                      style: AppTypography.bodyLarge.copyWith(
                        color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    OutlinedButton(
                      onPressed: () {
                        setState(() {
                          _future = _load(refresh: true);
                        });
                      },
                      child: const Text('Retry'),
                    ),
                  ],
                ),
              ),
            );
          }

          if (_shown.isEmpty) {
            return RefreshIndicator(
              onRefresh: _onRefresh,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                children: [
                  const SizedBox(height: 120),
                  Center(
                    child: Text(
                      'No shoes found',
                      style: AppTypography.bodyLarge.copyWith(
                        color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                      ),
                    ),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: _onRefresh,
            child: Padding(
              padding: EdgeInsets.all(AppSpacing.lg),
              child: _buildGrid(context, _shown),
            ),
          );
        },
      ),
    );
  }

  Widget _buildGrid(BuildContext context, List<Product> products) {
    final int crossAxisCount = AppBreakpoints.getColumnCount(context);
    return GridView.builder(
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: crossAxisCount,
        crossAxisSpacing: AppSpacing.xl2,
        mainAxisSpacing: AppSpacing.xl2,
        childAspectRatio: 0.55,
      ),
      itemCount: products.length,
      itemBuilder: (context, index) {
        final p = products[index];
        return ProductCard(
          imagePath: p.thumbnailUrl ?? '',
          title: p.name,
          price: p.price,
          onTap: () => _openTryOn(p),
          actions: [
            CustomButton(
              text: 'Try On',
              onPressed: () => _openTryOn(p),
              variant: CustomButtonVariant.primary,
              size: CustomButtonSize.small,
              isFullWidth: true,
            ),
            CustomButton(
              text: 'Customize',
              onPressed: () => _openCustomize(p),
              variant: CustomButtonVariant.secondary,
              size: CustomButtonSize.small,
              isFullWidth: true,
            ),
            CustomButton(
              text: 'Outfit Match',
              onPressed: () => _openOutfitRecommendation(p),
              variant: CustomButtonVariant.outline,
              size: CustomButtonSize.small,
              isFullWidth: true,
            ),
          ],
        );
      },
    );
  }
}

class _SearchField extends StatelessWidget {
  const _SearchField({required this.controller});

  final TextEditingController controller;

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return TextField(
      controller: controller,
      decoration: InputDecoration(
        hintText: 'Search shoes, brands, categories',
        hintStyle: AppTypography.bodyMedium.copyWith(
          color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
        ),
        border: InputBorder.none,
        prefixIcon: const Icon(Icons.search),
        isDense: true,
      ),
      textInputAction: TextInputAction.search,
    );
  }
}


