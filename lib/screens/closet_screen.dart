import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../services/closet_api_service.dart';
import '../services/tryon_api_service.dart';
import '../models/product.dart';
import '../services/outfit_api_service.dart';
import '../ar/ar_main.dart';
import 'auth/login_screen.dart';
import 'ar_tryon_screen.dart';
import 'catalog_screen.dart';

/// My Closet screen with shoe grid, search, and filters
class ClosetScreen extends StatefulWidget {
  const ClosetScreen({super.key});

  @override
  State<ClosetScreen> createState() => _ClosetScreenState();
}

class _ClosetScreenState extends State<ClosetScreen> {
  final AuthService _authService = AuthService();
  final ClosetApiService _closetService = ClosetApiService();
  final OutfitApiService _outfitService = OutfitApiService();
  final TextEditingController _searchController = TextEditingController();
  
  ClosetFilter _currentFilter = ClosetFilter.all;
  List<TryOnEntry> _apiShoes = [];
  List<OutfitMatch> _apiOutfits = [];
  Set<String> _favoriteIds = {};
  bool _isLoading = false;
  String _searchQuery = '';
  
  // Filter state
  double? _maxPrice;
  String? _selectedBrand;
  String? _selectedCategory;

  @override
  void initState() {
    super.initState();
    _loadClosetFromApi();
  }
  
  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadClosetFromApi() async {
    setState(() => _isLoading = true);
    try {
      // Clear cache to ensure we get the latest try-ons from AI matching
      _closetService.clearCache();
      
      final results = await Future.wait([
        _closetService.getClosetItems(forceRefresh: true),
        _outfitService.getHistory(),
      ]);
      
      if (mounted) {
        setState(() {
          _apiShoes = results[0] as List<TryOnEntry>;
          _apiOutfits = results[1] as List<OutfitMatch>;
        });
      }
    } catch (e) {
      debugPrint('Error loading closet: $e');
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          leading: const LogoButton(),
          title: const Text('My Closet'),
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.shopping_bag), text: 'Shoes'),
              Tab(icon: Icon(Icons.checkroom), text: 'Outfits'),
            ],
          ),
          actions: [
            // Filter button
            IconButton(
              icon: const Icon(Icons.filter_list),
              onPressed: () => _showFilterSheet(context, isDark),
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
        body: TabBarView(
          children: [
            // Shoes Tab
            SingleChildScrollView(
              padding: AppSpacing.paddingLarge,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Page Header
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'My Shoes',
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
                        ],
                      ),
                      IconButton(
                        icon: const Icon(Icons.refresh),
                        onPressed: _loadClosetFromApi,
                        tooltip: 'Refresh Collection',
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: AppSpacing.xl),
                  
                  // Search Bar
                  _buildSearchBar(isDark),
                  
                  const SizedBox(height: AppSpacing.xl),
                  
                  // Filter Badges
                  _buildFilterBadges(context, isDark),
                  
                  const SizedBox(height: AppSpacing.xl2),
                  
                  // Shoe Grid
                  _buildShoeGrid(context, isDark),
                ],
              ),
            ),
            // Outfits Tab
            SingleChildScrollView(
              padding: AppSpacing.paddingLarge,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Saved Outfits',
                            style: AppTypography.headline2,
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          Text(
                            '${_apiOutfits.length} outfits in your collection',
                            style: AppTypography.bodyMedium.copyWith(
                              color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                            ),
                          ),
                        ],
                      ),
                      IconButton(
                        icon: const Icon(Icons.refresh),
                        onPressed: _loadClosetFromApi,
                        tooltip: 'Refresh Outfits',
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xl2),
                  
                  if (_apiOutfits.isEmpty && !_isLoading)
                    _buildEmptyState(context, isDark, title: 'No Saved Outfits', subtitle: 'Use AI matching to generate and save your best looks.')
                  else if (_isLoading)
                     const Center(child: Padding(padding: EdgeInsets.all(40), child: CircularProgressIndicator()))
                  else
                    _buildOutfitGrid(context, isDark),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildSearchBar(bool isDark) {
    return TextField(
      controller: _searchController,
      decoration: InputDecoration(
        hintText: 'Search shoes... (e.g. "nike \$150" for Nike under \$150)',
        prefixIcon: const Icon(Icons.search),
        suffixIcon: _searchQuery.isNotEmpty
            ? IconButton(
                icon: const Icon(Icons.clear),
                onPressed: () {
                  _searchController.clear();
                  setState(() => _searchQuery = '');
                },
              )
            : null,
        filled: true,
        fillColor: isDark ? AppColors.darkCard : AppColors.lightCard,
        border: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: BorderSide.none,
        ),
      ),
      onChanged: (value) {
        setState(() => _searchQuery = value);
      },
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

  List<ProductCardData> _getFilteredShoes() {
    List<ProductCardData> result = _apiShoes
        .where((e) => e.shoe != null)
        .map((e) {
          final product = Product.fromJson(e.shoe!);
          return ProductCardData(
            imagePath: product.thumbnailUrl ?? 'assets/images/shoes/nike_journey_run.png',
            title: product.name,
            subtitle: product.category,
            price: product.price,
            brand: product.brand,
            category: product.category,
            modelUrl: product.modelUrl,
            isFavorite: _favoriteIds.contains(product.id),
          );
        }).toList();
    
    // Apply tab filter
    switch (_currentFilter) {
      case ClosetFilter.favorites:
        result = result.where((s) => s.isFavorite).toList();
        break;
      case ClosetFilter.recent:
        result = result.take(4).toList();
        break;
      case ClosetFilter.all:
      default:
        break;
    }
    
    // Apply brand filter
    if (_selectedBrand != null) {
      result = result.where((s) => s.brand == _selectedBrand).toList();
    }
    
    // Apply category filter
    if (_selectedCategory != null) {
      result = result.where((s) => s.category == _selectedCategory).toList();
    }
    
    // Apply max price filter
    if (_maxPrice != null) {
      result = result.where((s) => (s.price ?? 0) <= _maxPrice!).toList();
    }
    
    // Apply search query
    if (_searchQuery.isNotEmpty) {
      result = _applySmartSearch(result, _searchQuery);
    }
    
    return result;
  }
  
  /// Smart search with price parsing
  /// "$250" or "$250" searches for shoes under $250
  /// "nike 250" searches "250" in name AND shows shoes under $250
  List<ProductCardData> _applySmartSearch(List<ProductCardData> shoes, String query) {
    final q = query.trim().toLowerCase();
    
    // Check for explicit price search with $
    final priceOnlyMatch = RegExp(r'^\$(\d+(?:\.\d+)?)$').firstMatch(q);
    if (priceOnlyMatch != null) {
      final maxPrice = double.tryParse(priceOnlyMatch.group(1)!) ?? 0;
      return shoes.where((s) => (s.price ?? 0) <= maxPrice).toList();
    }
    
    // Check for combined search like "nike $250"
    final combinedMatch = RegExp(r'(.+?)\s*\$(\d+(?:\.\d+)?)').firstMatch(q);
    if (combinedMatch != null) {
      final textQuery = combinedMatch.group(1)!.trim();
      final maxPrice = double.tryParse(combinedMatch.group(2)!) ?? 0;
      
      return shoes.where((s) {
        final matchesPrice = (s.price ?? 0) <= maxPrice;
        final matchesText = _matchesTextQuery(s, textQuery);
        return matchesPrice && matchesText;
      }).toList();
    }
    
    // Check for text + number like "nike 250"
    final textNumberMatch = RegExp(r'(.+?)\s+(\d+(?:\.\d+)?)$').firstMatch(q);
    if (textNumberMatch != null) {
      final textQuery = textNumberMatch.group(1)!.trim();
      final number = double.tryParse(textNumberMatch.group(2)!) ?? 0;
      
      // First: shoes matching text AND having number in name
      final nameMatches = shoes.where((s) {
        final matchesText = _matchesTextQuery(s, textQuery);
        final hasNumberInName = s.title.toLowerCase().contains(textNumberMatch.group(2)!);
        return matchesText && hasNumberInName;
      }).toList();
      
      // Second: shoes matching text AND price under number
      final priceMatches = shoes.where((s) {
        final matchesText = _matchesTextQuery(s, textQuery);
        final matchesPrice = (s.price ?? 0) <= number;
        return matchesText && matchesPrice && !nameMatches.contains(s);
      }).toList();
      
      return [...nameMatches, ...priceMatches];
    }
    
    // Plain text search
    return shoes.where((s) => _matchesTextQuery(s, q)).toList();
  }
  
  bool _matchesTextQuery(ProductCardData shoe, String query) {
    final q = query.toLowerCase();
    return shoe.title.toLowerCase().contains(q) ||
           (shoe.brand?.toLowerCase().contains(q) ?? false) ||
           (shoe.category?.toLowerCase().contains(q) ?? false) ||
           (shoe.subtitle?.toLowerCase().contains(q) ?? false);
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
        _navigateToARWithShoe(shoe);
      },
      onFavoriteToggle: (index) {
        setState(() {
          // Find the corresponding TryOnEntry
          if (index < _apiShoes.length && _apiShoes[index].shoe != null) {
            final productId = Product.fromJson(_apiShoes[index].shoe!).id;
            if (_favoriteIds.contains(productId)) {
              _favoriteIds.remove(productId);
            } else {
              _favoriteIds.add(productId);
            }
          }
        });
      },
      onAction: (index) {
        final shoe = filteredShoes[index];
        _navigateToARWithShoe(shoe);
      },
    );
  }

  /// Navigate to AR Try-On screen with the selected shoe
  void _navigateToARWithShoe(ProductCardData shoe) {
    // Get model URL from shoe data with fallback
    final modelUrl = shoe.modelUrl ?? 'models/shoes/nike_journey_run_left.glb';
    
    // Use ARMain to open AR with the specific shoe model
    // This sets the model before opening AR so the renderer loads the correct GLB
    final arMain = ARMain();
    arMain.openARViewWithShoe(context, modelUrl);
    
    // Show which shoe is being loaded
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Loading ${shoe.title} in AR...'),
        duration: const Duration(seconds: 1),
      ),
    );
  }
  
  void _showFilterSheet(BuildContext context, bool isDark) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.5,
        decoration: BoxDecoration(
          color: isDark ? AppColors.darkCard : AppColors.lightCard,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          children: [
            // Handle bar
            Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(top: AppSpacing.md),
              decoration: BoxDecoration(
                color: Colors.grey[400],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            
            // Title
            Padding(
              padding: AppSpacing.paddingLarge,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Filters', style: AppTypography.headline4),
                  TextButton(
                    onPressed: () {
                      setState(() {
                        _maxPrice = null;
                        _selectedBrand = null;
                        _selectedCategory = null;
                      });
                      Navigator.pop(context);
                    },
                    child: const Text('Clear All'),
                  ),
                ],
              ),
            ),
            
            Expanded(
              child: ListView(
                padding: AppSpacing.paddingLarge,
                children: [
                  // Price Range
                  Text('Max Price', style: AppTypography.bodyLarge),
                  const SizedBox(height: AppSpacing.sm),
                  Wrap(
                    spacing: AppSpacing.sm,
                    children: [50, 100, 150, 200, 300].map((price) {
                      final isSelected = _maxPrice == price.toDouble();
                      return FilterChip(
                        label: Text('\$$price'),
                        selected: isSelected,
                        onSelected: (selected) {
                          setState(() {
                            _maxPrice = selected ? price.toDouble() : null;
                          });
                        },
                      );
                    }).toList(),
                  ),
                  
                  const SizedBox(height: AppSpacing.xl),
                  
                  // Brand
                  Text('Brand', style: AppTypography.bodyLarge),
                  const SizedBox(height: AppSpacing.sm),
                  Wrap(
                    spacing: AppSpacing.sm,
                    children: ShoeDatabase.brands.map((brand) {
                      final isSelected = _selectedBrand == brand;
                      return FilterChip(
                        label: Text(brand),
                        selected: isSelected,
                        onSelected: (selected) {
                          setState(() {
                            _selectedBrand = selected ? brand : null;
                          });
                        },
                      );
                    }).toList(),
                  ),
                  
                  const SizedBox(height: AppSpacing.xl),
                  
                  // Category
                  Text('Category', style: AppTypography.bodyLarge),
                  const SizedBox(height: AppSpacing.sm),
                  Wrap(
                    spacing: AppSpacing.sm,
                    children: ShoeDatabase.categories.map((category) {
                      final isSelected = _selectedCategory == category;
                      return FilterChip(
                        label: Text(category),
                        selected: isSelected,
                        onSelected: (selected) {
                          setState(() {
                            _selectedCategory = selected ? category : null;
                          });
                        },
                      );
                    }).toList(),
                  ),
                ],
              ),
            ),
            
            // Apply button
            Padding(
              padding: AppSpacing.paddingLarge,
              child: PrimaryButton(
                text: 'Apply Filters',
                onPressed: () => Navigator.pop(context),
                isFullWidth: true,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context, bool isDark, {String? title, String? subtitle}) {
    return Center(
      child: Padding(
        padding: AppSpacing.paddingLarge,
        child: Column(
          children: [
            Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                color: AppColors.accent.withOpacity(0.1),
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
              title ?? _getEmptyStateTitle(),
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              subtitle ?? _getEmptyStateSubtitle(),
              style: AppTypography.bodyMedium.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.xl2),
            PrimaryButton(
              text: 'Explore Shoe Catalog',
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => CatalogScreen()),
                );
              },
              icon: Icons.search,
              size: CustomButtonSize.medium,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOutfitGrid(BuildContext context, bool isDark) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: AppSpacing.lg,
        mainAxisSpacing: AppSpacing.lg,
        childAspectRatio: 0.8,
      ),
      itemCount: _apiOutfits.length,
      itemBuilder: (context, index) {
        final outfit = _apiOutfits[index];
        return GestureDetector(
          onTap: () => _showOutfitDetails(context, outfit, isDark),
          child: Card(
            elevation: 0,
            shape: RoundedRectangleBorder(borderRadius: AppRadius.radiusLarge),
            child: Container(
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusLarge,
                gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    flex: 3,
                    child: ClipRRect(
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(AppRadius.lg)),
                      child: (outfit.outfitImageUrl != null && outfit.outfitImageUrl!.startsWith('http'))
                          ? Image.network(outfit.outfitImageUrl!, fit: BoxFit.cover)
                          : const Icon(Icons.checkroom, size: 40, color: AppColors.accent),
                    ),
                  ),
                  Expanded(
                    flex: 2,
                    child: Padding(
                      padding: AppSpacing.paddingSmall,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(outfit.category ?? 'Outfit Match', style: AppTypography.bodyMedium.copyWith(fontWeight: FontWeight.bold), maxLines: 1, overflow: TextOverflow.ellipsis),
                          if (outfit.description != null) ...[
                            const SizedBox(height: AppSpacing.xs),
                            Text(
                              outfit.description!,
                              style: AppTypography.caption.copyWith(color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
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
    );
  }

  void _showOutfitDetails(BuildContext context, OutfitMatch outfit, bool isDark) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: isDark ? AppColors.darkCard : AppColors.lightCard,
        shape: RoundedRectangleBorder(borderRadius: AppRadius.radiusLarge),
        title: Text(outfit.category ?? 'Outfit Match', style: AppTypography.headline3),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (outfit.outfitImageUrl != null)
                ClipRRect(
                  borderRadius: AppRadius.radiusMedium,
                  child: Image.network(outfit.outfitImageUrl!, fit: BoxFit.contain),
                ),
              const SizedBox(height: AppSpacing.lg),
              Text('AI Recommendation', style: AppTypography.bodyLarge.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: AppSpacing.sm),
              Text(outfit.description ?? 'No detailed description available.', style: AppTypography.bodyMedium),
              const SizedBox(height: AppSpacing.xl),
              Text('Recommended Shoes', style: AppTypography.bodyLarge.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: AppSpacing.sm),
              if (outfit.recommendedShoes.isEmpty)
                const Text('No shoes recommended.')
              else
                Wrap(
                  spacing: AppSpacing.sm,
                  runSpacing: AppSpacing.sm,
                  children: outfit.recommendedShoes.map((shoe) => Chip(
                    label: Text(shoe.name),
                    avatar: const Icon(Icons.shopping_bag, size: 16),
                  )).toList(),
                ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  String _getEmptyStateTitle() {
    if (_searchQuery.isNotEmpty) {
      return 'No results found';
    }
    switch (_currentFilter) {
      case ClosetFilter.favorites:
        return 'No favorites yet';
      case ClosetFilter.recent:
        return 'No recent shoes';
      case ClosetFilter.all:
      default:
        return 'Your closet is empty';
    }
  }

  String _getEmptyStateSubtitle() {
    if (_searchQuery.isNotEmpty) {
      return 'Try a different search term';
    }
    switch (_currentFilter) {
      case ClosetFilter.favorites:
        return 'No favorite shoes yet. Try on some shoes and mark them as favorites!';
      case ClosetFilter.recent:
        return 'No recent shoes. Try on some shoes to see them here!';
      case ClosetFilter.all:
      default:
        return 'Try on some shoes to add them to your collection';
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

/// Closet filter options
enum ClosetFilter {
  all,
  favorites,
  recent,
}
