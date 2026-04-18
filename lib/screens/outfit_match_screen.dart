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
import 'dart:convert';
import 'dart:developer' as developer;
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'package:http/http.dart' as http;

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
  // Result state
  String? _analysisResult;
  List<Product> _recommendations = [];
  bool _isGenerating = false;
  List<Product> _recommendedShoes = [];
  Product? _currentShoe;
  File? _uploadedOutfitImage;
  
  String _outfitAnalysisText = ''; // For Outfit -> Shoe (Analyze button)
  String _shoeRecText = '';        // For Shoe -> Outfit (Rec Outfit button)
  File? _uploadedShoeImage;        // For manual shoe upload
  
  // NEW: State for Outfit MisMatch
  File? _mismatchImage;
  String _mismatchResult = "";
  bool _isCheckingMismatch = false;

  String _outfitCategory = 'Casual';
  String _selectedGender = 'unisex'; // Default gender filter
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
        _outfitAnalysisText = 'Image Uploaded. Tap Analyze to get shoe matches.';
        _outfitCategory = 'Ready for Analysis';
        _recommendedShoes = []; // Clear old results
      });
    }
  }

  Future<void> _pickShoeImage() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);
    if (image != null) {
      setState(() {
        _uploadedShoeImage = File(image.path);
        _currentShoe = null; // Clear catalog shoe if manual upload exists
        _shoeRecText = 'Shoe Uploaded. Tap Rec Outfit for style advice.';
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
            
            // Gender Selection Section
            _buildGenderSelector(context, isDark),
            
            const SizedBox(height: AppSpacing.xl2),
            
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
            // NEW: Outfit MisMatch Checker Section
            _buildOutfitMismatchSection(context, isDark),
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
            Stack(
              children: [
                Container(
                  width: double.infinity,
                  height: 200,
                  decoration: BoxDecoration(
                    borderRadius: AppRadius.radiusLarge,
                    gradient: AppGradients.elementGradient,
                  ),
                  child: ClipRRect(
                    borderRadius: AppRadius.radiusLarge,
                    child: _uploadedShoeImage != null
                        ? Image.file(
                            _uploadedShoeImage!,
                            fit: BoxFit.cover,
                          )
                        : (selectedShoe.imagePath.startsWith('http') || selectedShoe.imagePath.startsWith('https'))
                            ? Image.network(
                                selectedShoe.imagePath,
                                fit: BoxFit.cover,
                                errorBuilder: (context, error, stackTrace) => _buildPlaceholderIcon(),
                              )
                            : Image.asset(
                                selectedShoe.imagePath,
                                fit: BoxFit.cover,
                                errorBuilder: (context, error, stackTrace) => _buildPlaceholderIcon(),
                              ),
                  ),
                ),
                // Clear Button over image
                if (_uploadedShoeImage != null || _currentShoe != null)
                  Positioned(
                    top: AppSpacing.sm,
                    left: AppSpacing.sm,
                    child: CircleAvatar(
                      backgroundColor: Colors.black54,
                      radius: 16,
                      child: IconButton(
                        icon: const Icon(Icons.close, color: Colors.white, size: 16),
                        padding: EdgeInsets.zero,
                        onPressed: () {
                          setState(() {
                            _uploadedShoeImage = null;
                            _currentShoe = null;
                            _shoeRecText = "";
                            // Optionally reset to a default from catalog if needed, 
                            // but currently user wants it cleared.
                          });
                        },
                      ),
                    ),
                  ),
              ],
            ),

            
            const SizedBox(height: AppSpacing.lg),
            
            // Shoe Info
            Text(
              _uploadedShoeImage != null ? "Custom Shoe" : selectedShoe.title,
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
            
            const SizedBox(height: AppSpacing.lg),
            
            // Prominent Rec Outfit Button
            PrimaryButton(
              text: 'Rec Outfit',
              onPressed: _recommendOutfitForSelectedShoe,
              icon: Icons.auto_awesome,
              isFullWidth: true,
              isLoading: _isGenerating && (_uploadedShoeImage != null || _currentShoe != null),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Action Buttons
            Row(
              children: [
                Expanded(
                  child: OutlineButton(
                    text: 'Change Shoe',
                    onPressed: _showShoeSelector,
                    icon: Icons.swap_horiz,
                    size: CustomButtonSize.small,
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Expanded(
                  child: OutlineButton(
                    text: 'Upload Shoe',
                    onPressed: _pickShoeImage,
                    icon: Icons.upload_file,
                    size: CustomButtonSize.small,
                  ),
                ),
              ],
            ),
            
            // NEW: Relocated Result specifically for Shoe -> Outfit
            if (_shoeRecText.isNotEmpty) ...[
              const SizedBox(height: AppSpacing.lg),
              _buildResultCard(
                context,
                isDark,
                title: 'Style Recommendation',
                content: _shoeRecText,
                icon: Icons.auto_awesome,
                accentColor: AppColors.secondary,
              ),
            ],

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

                // Clear Button
                if (_uploadedOutfitImage != null)
                  Positioned(
                    top: AppSpacing.lg,
                    left: AppSpacing.lg,
                    child: CircleAvatar(
                      backgroundColor: Colors.black54,
                      child: IconButton(
                        icon: const Icon(Icons.close, color: Colors.white),
                        onPressed: () {
                          setState(() {
                            _uploadedOutfitImage = null;
                            _outfitAnalysisText = "";
                            _recommendedShoes = [];
                          });
                        },
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            // 2. AI Analysis Results (Recommendations Only)
            if (_recommendedShoes.isNotEmpty) ...[
              const SizedBox(height: AppSpacing.lg),
              SizedBox(
                height: 520, // Increased height for the direct list
                child: _buildRecommendationsTab(context, isDark),
              ),
            ],

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
                          description: _outfitAnalysisText,
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
                          _outfitAnalysisText = "Ready to analyze...";
                          _shoeRecText = "";
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
      
      // Filter recommendations locally for this case or ensure backend handles i
      
      // 2. Generate Description based on the shoe
      String category = "Analysis Result";
      
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
          _outfitAnalysisText = description;
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
      // 1. Fetch live shoe recommendations from the catalog based on outfit image
      var aiResponse = await _outfitService.recommendShoesFromImage(
        _uploadedOutfitImage!.path,
        gender: _selectedGender,
      );
      
      if (aiResponse == null) throw Exception('AI Recommendation failed');

      // Adaptive mapping for recommendations
      List<dynamic> recData = aiResponse['recommendations'] ?? 
                             aiResponse['recommendation'] ?? 
                             aiResponse['recommended_shoes'] ?? [];

      // FALLBACK A: Try unisex if specific gender returned 0
      if (recData.isEmpty && _selectedGender != 'unisex') {
        debugPrint('RESILIENCE: 0 matches for $_selectedGender, retrying with unisex...');
        final fallbackRes = await _outfitService.recommendShoesFromImage(
          _uploadedOutfitImage!.path,
          gender: 'unisex',
        );
        if (fallbackRes != null) {
          aiResponse = fallbackRes; // Update response for color extraction
          recData = aiResponse!['recommendations'] ?? 
                    aiResponse!['recommendation'] ?? 
                    aiResponse!['recommended_shoes'] ?? [];
        }
      }

      // DIAGNOSTIC LOG
      debugPrint('AI RESPONSE KEYS: ${aiResponse?.keys.toList()}');
      
      final style = aiResponse?['detectedStyle'] ?? aiResponse?['detected_style'] ?? 'Coordinated';
      final suggestedColor = aiResponse?['suggestedShoeColor'] ?? aiResponse?['suggested_shoe_color'] ?? '';
      
      // PURE AI RECOMMENDATIONS: Solely based on AI response list
      List<Product> recommendations = recData.map((e) {
        final p = Product.fromJson(e);
        debugPrint('FINAL TITLE FOR RECOMMENDATION: ${p.name}');
        return p;
      }).toList();
      
      if (recommendations.isEmpty) {
        debugPrint('STRICT: AI returned 0 recommendations. No local fallback will be used.');
      }

      // 2. First, calculate dominant shirt and pant colors with adaptive logic
      String shirtColor = 'primary';
      String pantColor = 'secondary';
      
      final detectedObjects = List<dynamic>.from(aiResponse?['detected_colors'] ?? aiResponse?['detectedColors'] ?? []);
      final detectedStrings = List<String>.from(aiResponse?['detectedColors'] ?? aiResponse?['detected_colors'] ?? []);

      // Logic A: Try robust part identification (from objects) - Choosing highest dominance per source
      final shirtList = detectedObjects.where((c) => 
        c is Map && (c['source']?.toString().toLowerCase() == 'shirt')
      ).toList();
      
      final pantList = detectedObjects.where((c) => 
        c is Map && (c['source']?.toString().toLowerCase() == 'pant')
      ).toList();
      
      if (shirtList.isNotEmpty) {
        // Sort by dominance desc and pick first
        shirtList.sort((a, b) => (double.tryParse(b['dominance']?.toString() ?? '0') ?? 0)
            .compareTo(double.tryParse(a['dominance']?.toString() ?? '0') ?? 0));
        shirtColor = shirtList[0]['name'] ?? 'primary';
      } else if (detectedStrings.isNotEmpty) {
        shirtColor = detectedStrings[0];
      }

      if (pantList.isNotEmpty) {
        // Sort by dominance desc and pick first
        pantList.sort((a, b) => (double.tryParse(b['dominance']?.toString() ?? '0') ?? 0)
            .compareTo(double.tryParse(a['dominance']?.toString() ?? '0') ?? 0));
        pantColor = pantList[0]['name'] ?? 'secondary';
      } else if (detectedStrings.length > 1) {
        pantColor = detectedStrings[1];
      }

      // FALLBACK C: Synthesize reasons for all recommendations if they are missing
      for (int i = 0; i < recommendations.length; i++) {
        if (recommendations[i].matchReason == null || recommendations[i].matchReason!.contains('Available soon')) {
          final colorMatch = recommendations[i].name.toLowerCase().contains(suggestedColor.toLowerCase()) || 
                             recommendations[i].brand.toLowerCase().contains(suggestedColor.toLowerCase());
          
          final colorReason = suggestedColor.isNotEmpty 
            ? (colorMatch ? "perfectly aligns with the suggested $suggestedColor palette" : "provides a sophisticated alternative to the suggested $suggestedColor tone")
            : "offers a stylish complement to your outfit's structure";
            
          final synthReason = "This $style footwear $colorReason, adding a balanced focal point that elevates your $shirtColor shirt and $pantColor pant combination.";
          
          recommendations[i] = recommendations[i].copyWith(matchReason: synthReason);
        }
      }
      
      debugPrint('MAPPING SUCCESS: Finalized ${recommendations.length} recommendations');

      // 2. Format the personalized description
      final formattedDescription = "A $style outfit with dominant \"$shirtColor\" and \"$pantColor\" colors contrasts nicely with these recommended shoes.";
      debugPrint('FINAL OUTFIT DESCRIPTION: $formattedDescription');

      // 3. Save matching to database with personalized description
      String category = style;
      final match = await _outfitService.analyzeOutfit(
        imagePath: _uploadedOutfitImage!.path,
        dominantColors: detectedStrings,
        category: category,
        description: formattedDescription, // Use personalized text
        recommendedShoeIds: recommendations.map((s) => s.id).toList(),
      );

      if (mounted) {
        setState(() {
          _recommendedShoes = recommendations;
          _outfitCategory = category;
          _outfitAnalysisText = formattedDescription;
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
          SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isGenerating = false);
      }
    }
  }

  /// Unified Recommendation Logic (Always based on Image)
  Future<void> _recommendOutfitForSelectedShoe() async {
    String? imagePath;

    if (_uploadedShoeImage != null) {
      imagePath = _uploadedShoeImage!.path;
    } else if (_uploadedShoeImage == null) {
      // Fallback: Current catalog shoe or sample shoe
      final shoe = _currentShoe;
      final thumbnailUrl = shoe?.thumbnailUrl;
      
      if (thumbnailUrl != null) {
        setState(() => _isGenerating = true);
        try {
          final response = await http.get(Uri.parse(thumbnailUrl));
          final documentDirectory = await getTemporaryDirectory();
          final file = File(p.join(documentDirectory.path, 'temp_shoe.jpg'));
          await file.writeAsBytes(response.bodyBytes);
          imagePath = file.path;
        } catch (e) {
          debugPrint('Error downloading shoe thumbnail: $e');
        }
      }
    }

    if (imagePath == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select a shoe or upload an image first')),
      );
      setState(() => _isGenerating = false);
      return;
    }

    setState(() => _isGenerating = true);

    try {
      final aiResponse = await _outfitService.getOutfitRecommendationForShoe(
        imagePath,
        gender: _selectedGender,
      );
      
      if (aiResponse != null) {
        final description = aiResponse['recommendation_text'] ??
                            aiResponse['text'] ?? 
                            aiResponse['description'] ?? 
                            aiResponse['recommendation'] ?? 
                            aiResponse['recommendations'] ?? 
                            aiResponse['outfit_description'] ?? 
                            (aiResponse.containsKey('outfit') && aiResponse['outfit'] is String ? aiResponse['outfit'] : 'A stylish ensemble.');

        if (mounted) {
          setState(() {
            _shoeRecText = description;
            _isGenerating = false;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error generating outfit: $e'), backgroundColor: Colors.red),
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

  Widget _buildGenderSelector(BuildContext context, bool isDark) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildGenderChip('male', Icons.male),
            const SizedBox(width: AppSpacing.md),
            _buildGenderChip('female', Icons.female),
          ],
        ),
      ],
    );
  }

  Widget _buildGenderChip(String value, IconData icon) {
    final isSelected = _selectedGender == value;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return ChoiceChip(
      label: Text(value.toUpperCase()),
      avatar: Icon(
        icon, 
        size: 18, 
        color: isSelected ? Colors.white : (isDark ? Colors.white70 : Colors.black54)
      ),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) {
          setState(() {
            _selectedGender = value;
            // Clear current results to encourage re-analysis with new gender filter
            _recommendedShoes = [];
            _outfitAnalysisText = '';
            _shoeRecText = '';
          });
        }
      },
      selectedColor: AppColors.secondary,
      backgroundColor: isDark ? Colors.grey[900] : Colors.grey[200],
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : (isDark ? Colors.white70 : Colors.black54),
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.xs),
    );
  }

  Widget _buildResultCard(
    BuildContext context, 
    bool isDark, 
    {required String title, required String content, required IconData icon, required Color accentColor}
  ) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.sm),
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
              Icon(icon, color: accentColor, size: 20),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Text(
                  title,
                  style: AppTypography.headline4.copyWith(color: accentColor),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            content,
            style: AppTypography.bodyMedium.copyWith(
              height: 1.5,
              color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendationsTab(BuildContext context, bool isDark) {
    if (_recommendedShoes.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.xl),
          child: Text(
            'No recommendations available yet.',
            style: AppTypography.bodyMedium.copyWith(
              color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
            ),
          ),
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(AppSpacing.lg),
      itemCount: _recommendedShoes.length,
      separatorBuilder: (context, index) => const SizedBox(height: AppSpacing.md),
      itemBuilder: (context, index) {
        final shoe = _recommendedShoes[index];
        return Container(
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
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      shoe.name,
                      style: AppTypography.headline4.copyWith(
                        color: AppColors.accent,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const Icon(
                    Icons.auto_awesome,
                    color: AppColors.accent,
                    size: 20,
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.md),
              const Divider(height: 1, color: Colors.white12),
              const SizedBox(height: AppSpacing.md),
              Text(
                shoe.matchReason ?? 'High-confidence style match for this outfit composition.',
                style: AppTypography.bodyLarge.copyWith(
                  height: 1.6,
                  color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                  fontStyle: FontStyle.italic,
                  letterSpacing: 0.2,
                ),
              ),
              const SizedBox(height: AppSpacing.sm),
              // Brand/Source tag
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.accent.withOpacity(0.1),
                  borderRadius: AppRadius.radiusSmall,
                ),
                child: Text(
                  shoe.brand != 'SoleMate' ? shoe.brand : 'AI STYLING SUGGESTION',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.accent,
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildPlaceholderIcon() {
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
  }


  /// Section for Outfit Harmony Check (MisMatch)
  Widget _buildOutfitMismatchSection(BuildContext context, bool isDark) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Header
        Row(
          children: [
            const Icon(
              Icons.balance,
              color: AppColors.secondary,
              size: 28,
            ),
            const SizedBox(width: AppSpacing.sm),
            Text(
              'Outfit MisMatch',
              style: AppTypography.headline3.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          'Check if your head-to-toe look is perfectly matched',
          style: AppTypography.bodyLarge.copyWith(
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
        ),
        
        const SizedBox(height: AppSpacing.lg),
        
        // The MisMatch Card
        Card(
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: AppRadius.radiusLarge),
          child: Container(
            padding: AppSpacing.paddingLarge,
            decoration: BoxDecoration(
              borderRadius: AppRadius.radiusLarge,
              gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
            ),
            child: Column(
              children: [
                // Image Analysis Area
                Container(
                  width: double.infinity,
                  height: 250,
                  decoration: BoxDecoration(
                    borderRadius: AppRadius.radiusLarge,
                    color: isDark ? Colors.black26 : Colors.white54,
                    border: Border.all(
                      color: isDark ? Colors.white12 : Colors.black12,
                      width: 1,
                    ),
                  ),
                  child: Stack(
                    children: [
                      if (_mismatchImage != null)
                        ClipRRect(
                          borderRadius: AppRadius.radiusLarge,
                          child: Image.file(
                            _mismatchImage!,
                            width: double.infinity,
                            height: double.infinity,
                            fit: BoxFit.cover,
                          ),
                        )
                      else
                        Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                Icons.add_a_photo_outlined,
                                size: 48,
                                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                              ),
                              const SizedBox(height: AppSpacing.md),
                              Text(
                                'Upload your full outfit image',
                                style: AppTypography.bodyMedium.copyWith(
                                  color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                                ),
                              ),
                            ],
                          ),
                        ),
                      
                      // Action buttons overlay
                      Positioned(
                        bottom: AppSpacing.md,
                        right: AppSpacing.md,
                        child: Row(
                          children: [
                            if (_mismatchImage != null) ...[
                              FloatingActionButton.small(
                                backgroundColor: Colors.redAccent,
                                onPressed: () => setState(() {
                                  _mismatchImage = null;
                                  _mismatchResult = "";
                                }),
                                child: const Icon(Icons.close, color: Colors.white),
                              ),
                              const SizedBox(width: AppSpacing.sm),
                            ],
                            FloatingActionButton.extended(
                              backgroundColor: AppColors.secondary,
                              onPressed: _isCheckingMismatch ? null : (_mismatchImage == null ? _pickMismatchImage : _checkOutfitMismatch),
                              label: Text(_isCheckingMismatch ? 'Analyzing...' : (_mismatchImage == null ? 'Upload' : 'Check Harmony')),
                              icon: Icon(_isCheckingMismatch ? Icons.hourglass_empty : Icons.auto_awesome),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                
                // Results area
                if (_mismatchResult.isNotEmpty) ...[
                  const SizedBox(height: AppSpacing.lg),
                  _buildResultCard(
                    context, 
                    isDark, 
                    title: 'Harmony Result', 
                    content: _mismatchResult, 
                    icon: Icons.reviews,
                    accentColor: AppColors.secondary,
                  ),
                ],
              ],
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _pickMismatchImage() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery);
    if (image != null) {
      setState(() {
        _mismatchImage = File(image.path);
        _mismatchResult = "";
      });
    }
  }

  Future<void> _checkOutfitMismatch() async {
    if (_mismatchImage == null) return;
    setState(() => _isCheckingMismatch = true);

    try {
      final res = await _outfitService.checkOutfitMismatch(_mismatchImage!.path);
      if (res != null) {
        final feedback = res['feedback'];
        if (feedback != null) {
          final issue = feedback['issue_detected'] ?? 'None';
          final fix = feedback['suggested_fix'] ?? 'N/A';
          final reason = feedback['reason'] ?? 'N/A';
          
          setState(() {
            _mismatchResult = "Issue detected: $issue,\nSuggested fix: $fix,\nReason: $reason";
          });
        }
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error checking harmony: $e'), backgroundColor: Colors.red),
      );
    } finally {
      setState(() => _isCheckingMismatch = false);
    }
  }
}
