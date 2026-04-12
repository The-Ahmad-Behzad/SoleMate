import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/product_card.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../services/outfit_api_service.dart';
import '../models/product.dart';
import '../repositories/catalog_repository.dart';
import '../services/tryon_api_service.dart';
import '../ar/ar_main.dart';
import 'auth/login_screen.dart';
import 'ar_tryon_screen.dart';
import 'dart:io';
import 'package:image_picker/image_picker.dart';

/// Outfit Match screen with current shoe and outfit suggestions
class OutfitMatchScreen extends StatefulWidget {
  final Product? baseShoe;
  const OutfitMatchScreen({super.key, this.baseShoe});

  @override
  State<OutfitMatchScreen> createState() => _OutfitMatchScreenState();
}

class _OutfitMatchScreenState extends State<OutfitMatchScreen> {
  final AuthService _authService = AuthService();
  final OutfitApiService _outfitService = OutfitApiService();
  final CatalogRepository _catalogRepository = CatalogRepository();
  final TryOnApiService _tryOnService = TryOnApiService();
  List<Product> _catalog = [];
  bool _isLoadingCatalog = true;

  int _selectedShoeIndex = 0;
  int _selectedOutfitIndex = 0;
  bool _isGenerating = false;
  List<Product> _recommendedShoes = [];
  Product? _currentShoe;
  File? _uploadedOutfitImage;
  String _generatedOutfitDetails = 'Casual Street Style\nPerfect for everyday wear';
  String _outfitCategory = 'Casual';
  List<String> _currentColors = ['black', 'white', 'grey'];

  @override
  void initState() {
    super.initState();
    _currentShoe = widget.baseShoe;
    _loadCatalog();
    
    // Automatically generate if coming from catalog with a shoe
    if (widget.baseShoe != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _generateOutfitRecommendations();
      });
    }
  }

  Future<void> _loadCatalog() async {
    setState(() => _isLoadingCatalog = true);
    try {
      final products = await _catalogRepository.getProducts();
      if (mounted) {
        setState(() {
          _catalog = products;
          _isLoadingCatalog = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading catalog: $e');
      if (mounted) setState(() => _isLoadingCatalog = false);
    }
  }

  Future<void> _pickOutfitImage() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);
    if (image != null) {
      setState(() {
        _uploadedOutfitImage = File(image.path);
        _generatedOutfitDetails = 'Image Uploaded. Tap Analyze to get shoe matches.';
        _outfitCategory = 'Ready for Analysis';
        _recommendedShoes = []; // Clear old results
      });
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
    final selectedShoe = _currentShoe != null ? ProductCardData(
      imagePath: _currentShoe!.thumbnailUrl ?? 'assets/images/shoes/nike_journey_run.png',
      title: _currentShoe!.name,
      subtitle: _currentShoe!.category,
      brand: _currentShoe!.brand,
      price: _currentShoe!.price,
      modelUrl: _currentShoe!.modelUrl,
    ) : SampleShoes.shoes[_selectedShoeIndex];
    
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
                  if (_uploadedOutfitImage != null)
                    Positioned.fill(
                      child: ClipRRect(
                        borderRadius: AppRadius.radiusLarge,
                        child: Image.file(_uploadedOutfitImage!, fit: BoxFit.cover),
                      ),
                    )
                  else
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
                            widget.baseShoe != null ? 'AI Generated Outfit' : 'Upload Outfit Image',
                            style: AppTypography.bodyLarge.copyWith(
                              color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          Text(
                            widget.baseShoe != null ? 'Tap "Generate" to create' : 'Tap to upload reference outfit',
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
                      text: _isGenerating 
                          ? 'Processing...' 
                          : (_uploadedOutfitImage != null && _recommendedShoes.isEmpty 
                              ? 'Analyze' 
                              : (widget.baseShoe != null ? 'Generate' : 'Upload Image')),
                      onPressed: _isGenerating 
                          ? null 
                          : (_uploadedOutfitImage != null && _recommendedShoes.isEmpty
                              ? _analyzeUploadedOutfit
                              : (widget.baseShoe != null ? _generateOutfitRecommendations : _pickOutfitImage)),
                      size: CustomButtonSize.small,
                      icon: (widget.baseShoe != null || _uploadedOutfitImage != null) ? Icons.auto_awesome : Icons.upload,
                      isLoading: _isGenerating,
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            // AI Recommendation Card
            if (_generatedOutfitDetails != 'Ready to analyze...')
              Container(
                margin: const EdgeInsets.only(bottom: AppSpacing.lg),
                padding: AppSpacing.paddingLarge,
                decoration: BoxDecoration(
                  gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
                  borderRadius: AppRadius.radiusLarge,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.05),
                      blurRadius: 10,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.auto_awesome, color: AppColors.secondary, size: 20),
                        const SizedBox(width: AppSpacing.sm),
                        Text(
                          _outfitCategory,
                          style: AppTypography.headline4.copyWith(color: AppColors.secondary),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      _generatedOutfitDetails.contains('\n') 
                        ? _generatedOutfitDetails.split('\n').sublist(1).join('\n')
                        : _generatedOutfitDetails,
                      style: AppTypography.bodyMedium.copyWith(
                        height: 1.5,
                        color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                      ),
                    ),
                  ],
                ),
              ),

            // Action Buttons
            Row(
              children: [
                Expanded(
                  child: PrimaryButton(
                    text: 'Save Outfit',
                    onPressed: () async {
                      try {
                        // Call backend API to save the outfit
                        // Record the analysis with real results from the screen
                        await _outfitService.analyzeOutfit(
                          imagePath: _uploadedOutfitImage?.path,
                          dominantColors: _currentColors,
                          category: _outfitCategory,
                          description: _generatedOutfitDetails.contains('\n') 
                              ? _generatedOutfitDetails.split('\n').sublist(1).join('\n')
                              : _generatedOutfitDetails,
                          recommendedShoeIds: _recommendedShoes.map((s) => s.id).toList(),
                        );
                        if (mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Outfit saved securely to Closet!'),
                              backgroundColor: AppColors.secondary,
                            ),
                          );
                        }
                      } catch (e) {
                        if (mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('Failed to save outfit: $e')),
                          );
                        }
                      }
                    },
                    icon: Icons.bookmark,
                  ),
                ),
                const SizedBox(width: AppSpacing.lg),
                Expanded(
                  child: OutlineButton(
                    text: 'Try On',
                    onPressed: () => _navigateToARTryOn(),
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
        
        if (_recommendedShoes.isNotEmpty) ...[
          // Render actual API results
          Text(
            'Recommended Matches',
            style: AppTypography.headline4.copyWith(
              color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          ProductCardsGrid(
            products: _recommendedShoes.map((product) {
              return ProductCardData(
                imagePath: product.thumbnailUrl ?? 'assets/images/shoes/nike_journey_run.png', // Fallback image
                title: product.name,
                subtitle: product.category,
                brand: product.brand,
                price: product.price,
                modelUrl: product.modelUrl,
                isFavorite: false,
              );
            }).toList(),
            onProductTap: (index) {
              // Optionally handle tap on recommended shoe (e.g., show details or update current shoe)
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Selected ${_recommendedShoes[index].name}')),
              );
            },
            onAction: (index) {
               // Load into AR
               final selectedShoe = _recommendedShoes[index];
               final modelUrl = selectedShoe.modelUrl ?? 'models/shoes/nike_journey_run_left.glb';
               final arMain = ARMain();
               arMain.openARViewWithShoe(context, modelUrl);
            },
            showActions: true,
            actionText: 'Try On',
          ),
        ] else ...[
          // Default placeholder suggestions when no recommendations exist
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
      ],
    );
  }

  void _showShoeSelector() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.7,
        decoration: BoxDecoration(
          color: Theme.of(context).scaffoldBackgroundColor,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(AppRadius.lg)),
        ),
        padding: AppSpacing.paddingLarge,
        child: Column(
          children: [
            Container(
              width: 40, height: 4,
              margin: const EdgeInsets.only(bottom: AppSpacing.lg),
              decoration: BoxDecoration(color: Colors.grey.withOpacity(0.3), borderRadius: AppRadius.radiusFull),
            ),
            Text('Select Current Shoe', style: AppTypography.headline3),
            const SizedBox(height: AppSpacing.lg),
            if (_isLoadingCatalog)
              const Expanded(child: Center(child: CircularProgressIndicator()))
            else if (_catalog.isEmpty)
              const Expanded(child: Center(child: Text('No shoes found in catalog.')))
            else
              Expanded(
                child: GridView.builder(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    crossAxisSpacing: AppSpacing.md,
                    mainAxisSpacing: AppSpacing.md,
                    childAspectRatio: 0.8,
                  ),
                  itemCount: _catalog.length,
                  itemBuilder: (context, index) {
                    final shoe = _catalog[index];
                    final isSelected = _currentShoe?.id == shoe.id;
                    return GestureDetector(
                      onTap: () {
                        setState(() {
                          _currentShoe = shoe;
                          _generatedOutfitDetails = "Ready to analyze...";
                        });
                        Navigator.pop(context);
                      },
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: AppRadius.radiusLarge,
                          border: Border.all(color: isSelected ? AppColors.secondary : Colors.grey.withOpacity(0.2), width: isSelected ? 2 : 1),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Expanded(
                              child: ClipRRect(
                                borderRadius: const BorderRadius.vertical(top: Radius.circular(AppRadius.lg - 1)),
                                child: (shoe.thumbnailUrl != null && shoe.thumbnailUrl!.startsWith('http'))
                                    ? Image.network(shoe.thumbnailUrl!, fit: BoxFit.cover)
                                    : Image.asset(shoe.thumbnailUrl ?? 'assets/images/shoes/nike_journey_run.png', fit: BoxFit.cover),
                              ),
                            ),
                            Padding(
                              padding: AppSpacing.paddingSmall,
                              child: Text(shoe.name, style: AppTypography.bodySmall.copyWith(fontWeight: FontWeight.bold), textAlign: TextAlign.center, maxLines: 1, overflow: TextOverflow.ellipsis),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }

  /// Generates outfit recommendations for a specific shoe
  Future<void> _generateOutfitRecommendations() async {
    setState(() => _isGenerating = true);

    try {
      // Logic for shoe-to-outfit:
      // Start with the shoe's primary colors
      _currentColors = ['black', 'white', 'grey'];
      
      // 1. Fetch live AI recommendations
      final recommendations = await _outfitService.getRecommendations(_currentColors);
      
      // 2. Generate Description based on the shoe
      String category = "Modern Ensemble";
      if (_currentShoe?.category == 'running') category = "Sporty Chic";
      if (_currentShoe?.category == 'casual') category = "Relaxed Urban";
      
      String description = "This outfit is designed to complement the unique profile of your ${_currentShoe?.name ?? 'selected shoe'}. We've focused on ${_currentColors.join(' and ')} tones to create a balanced, high-fashion aesthetic.";

      // 3. Record the analysis in the backend history with rich data
      await _outfitService.analyzeOutfit(
        imagePath: _uploadedOutfitImage?.path,
        dominantColors: _currentColors,
        category: category,
        description: description,
        recommendedShoeIds: recommendations.map((s) => s.id).toList(),
      );

      // 4. Also record a Try-On event if a shoe is selected to populate "My Closet"
      if (_currentShoe != null) {
        await _tryOnService.saveTryOn(
          shoeId: _currentShoe!.id,
          customSkinApplied: false,
        );
      }
      
      if (mounted) {
        setState(() {
          _recommendedShoes = recommendations;
          _outfitCategory = category;
          _generatedOutfitDetails = "$category\n$description";
        });
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Found ${recommendations.length} matching items!'),
            backgroundColor: AppColors.secondary,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isGenerating = false);
      }
    }
  }

  /// Analyzes an uploaded outfit image to find matching shoes
  Future<void> _analyzeUploadedOutfit() async {
    if (_uploadedOutfitImage == null) return;
    setState(() => _isGenerating = true);

    try {
      // Simulated dominant color extraction
      _currentColors = ['white', 'grey', 'accent']; 
      
      // 1. Fetch live shoe recommendations from the catalog based on outfit colors
      final recommendations = await _outfitService.getRecommendations(_currentColors);
      
      // 2. Perform Analysis & History creation (This uploads image to S3)
      String category = "Coordinated Look";
      String description = "Based on your uploaded outfit, we've identified key neutral tones that work best with our classic silhouettes. These shoe recommendations provide the perfect finishing touch for a clean, cohesive style.";

      final match = await _outfitService.analyzeOutfit(
        imagePath: _uploadedOutfitImage!.path,
        dominantColors: _currentColors,
        category: category,
        description: description,
        recommendedShoeIds: recommendations.map((s) => s.id).toList(),
      );

      if (mounted) {
        setState(() {
          _recommendedShoes = recommendations;
          _outfitCategory = category;
          _generatedOutfitDetails = description;
        });
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(match != null ? 'Analysis complete & saved to Closet!' : 'Analysis complete!'),
            backgroundColor: AppColors.secondary,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error analyzing outfit: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isGenerating = false);
      }
    }
  }

  /// Navigate to AR Try-On screen with the current selected shoe
  void _navigateToARTryOn() {
    final modelUrl = _currentShoe?.modelUrl ?? SampleShoes.arSelection[_selectedShoeIndex].modelUrl ?? 'models/shoes/nike_journey_run_left.glb';
    final name = _currentShoe?.name ?? SampleShoes.arSelection[_selectedShoeIndex].title;
    
    // Use ARMain to open AR with the specific shoe model
    final arMain = ARMain();
    arMain.openARViewWithShoe(context, modelUrl);
    
    // Show which shoe is being loaded
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Loading $name in AR...'),
        duration: const Duration(seconds: 1),
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
