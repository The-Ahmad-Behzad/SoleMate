import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../models/product.dart';
import '../services/catalog_api_service.dart';
import '../widgets/product_card.dart';
import 'ar_tryon_screen.dart';

class CatalogScreen extends StatefulWidget {
  const CatalogScreen({super.key});

  @override
  State<CatalogScreen> createState() => _CatalogScreenState();
}

class _CatalogScreenState extends State<CatalogScreen> {
  final CatalogApiService _catalog = CatalogApiService();
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
    final products = await _catalog.loadProducts(forceRefresh: refresh);
    _all = products;
    _shown = products;
    return products;
  }

  void _onSearchChanged() {
    final text = _searchController.text.trim().toLowerCase();
    setState(() {
      if (text.isEmpty) {
        _shown = _all;
      } else {
        _shown = _all.where((p) {
          return p.name.toLowerCase().contains(text) ||
              p.brand.toLowerCase().contains(text) ||
              p.category.toLowerCase().contains(text);
        }).toList(growable: false);
      }
    });
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
        childAspectRatio: 1.0,
      ),
      itemCount: products.length,
      itemBuilder: (context, index) {
        final p = products[index];
        return ProductCard(
          imagePath: p.thumbnailUrl ?? '',
          title: p.name,
          price: p.price,
          showActions: true,
          actionText: 'Try On',
          onAction: () => _openTryOn(p),
          onTap: () => _openTryOn(p),
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


