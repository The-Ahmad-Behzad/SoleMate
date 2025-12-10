import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../services/closet_api_service.dart';
import '../services/tryon_api_service.dart';
import '../models/product.dart';
import '../ar/ar_main.dart';
import 'auth/login_screen.dart';
import 'ar_tryon_screen.dart';

/// My Closet screen with shoe grid, search, and filters
class ClosetScreen extends StatefulWidget {
  const ClosetScreen({super.key});

  @override
  State<ClosetScreen> createState() => _ClosetScreenState();
}

class _ClosetScreenState extends State<ClosetScreen> {
  final AuthService _authService = AuthService();
  final ClosetApiService _closetService = ClosetApiService();
  final TextEditingController _searchController = TextEditingController();
  
  ClosetFilter _currentFilter = ClosetFilter.all;
  List<ProductCardData> _shoes = List.from(ShoeDatabase.allShoes);
  List<TryOnEntry> _apiShoes = [];
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
      final items = await _closetService.getClosetItems();
      if (mounted) {
        setState(() {
          _apiShoes = items;
        });
      }
    } catch (e) {
      // Fallback to sample data on error
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

    return Scaffold(
      appBar: AppBar(
        leading: const LogoButton(),
        title: const Text('My Closet'),
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
    List<ProductCardData> result = List.from(_shoes);
    
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
          final shoe = filteredShoes[index];
          final originalIndex = _shoes.indexWhere((s) => s.title == shoe.title);
          if (originalIndex != -1) {
            _shoes[originalIndex] = _shoes[originalIndex].copyWith(
              isFavorite: !_shoes[originalIndex].isFavorite,
            );
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
              _getEmptyStateTitle(),
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              _getEmptyStateSubtitle(),
              style: AppTypography.bodyMedium.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
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
