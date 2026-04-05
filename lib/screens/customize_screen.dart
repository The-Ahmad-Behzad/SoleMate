import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../services/skin_api_service.dart';
import '../services/api_client.dart';
import 'auth/login_screen.dart';
import 'package:image_picker/image_picker.dart';
import 'package:http/http.dart' as http;
import 'dart:io';
import 'package:painter/painter.dart';

/// Customize screen with 3D preview and customization options
class CustomizeScreen extends StatefulWidget {
  const CustomizeScreen({super.key});

  @override
  State<CustomizeScreen> createState() => _CustomizeScreenState();
}

class _CustomizeScreenState extends State<CustomizeScreen> {
  final AuthService _authService = AuthService();
  final SkinApiService _skinService = SkinApiService();
  int _selectedColorIndex = 0;
  int _selectedTextureIndex = 0;
  double _shineValue = 50.0;
  bool _isSaving = false;
  late PainterController _painterController;

  @override
  void initState() {
    super.initState();
    _painterController = _newController();
  }

  PainterController _newController() {
    PainterController controller = PainterController();
    controller.thickness = 5.0;
    controller.backgroundColor = Colors.white;
    return controller;
  }

  // Predefined colors
  final List<Color> _colors = [
    const Color(0xFF000000), // Black
    const Color(0xFF8B4513), // Saddle Brown
    const Color(0xFFDC143C), // Crimson
    const Color(0xFF0000FF), // Blue
    const Color(0xFF228B22), // Forest Green
    const Color(0xFF800080), // Purple
    const Color(0xFFFFFFFF), // White
    const Color(0xFFC0C0C0), // Silver
  ];

  // Texture options
  final List<TextureOption> _textures = [
    const TextureOption(name: 'Leather', icon: Icons.texture),
    const TextureOption(name: 'Suede', icon: Icons.grain),
    const TextureOption(name: 'Canvas', icon: Icons.grid_on),
    const TextureOption(name: 'Synthetic', icon: Icons.polymer),
  ];

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final screenSize = MediaQuery.of(context).size;
    final isMobile = screenSize.width < AppBreakpoints.md;

    return Scaffold(
      appBar: AppBar(
        leading: const LogoButton(),
        title: const Text('Customize'),
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
            
            // Main Content
            if (isMobile) ...[
              // Mobile: Stacked layout
              _build3DPreviewCard(context, isDark),
              const SizedBox(height: AppSpacing.xl2),
              _buildDrawingCanvas(context, isDark),
              const SizedBox(height: AppSpacing.xl2),
              _buildCustomizationOptions(context, isDark),
              const SizedBox(height: AppSpacing.xl2),
              const _RedesignRequestForm(),
            ] else ...[
              // Desktop: Side-by-side layout
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Left: 3D Preview
                  Expanded(
                    flex: 1,
                    child: _build3DPreviewCard(context, isDark),
                  ),
                  const SizedBox(width: AppSpacing.xl2),
                  // Right: Customization Options
                  Expanded(
                    flex: 1,
                    child: Column(
                      children: [
                        _buildDrawingCanvas(context, isDark),
                        const SizedBox(height: AppSpacing.xl2),
                        _buildCustomizationOptions(context, isDark),
                        const SizedBox(height: AppSpacing.xl2),
                        const _RedesignRequestForm(),
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
          'Customize Your Shoes',
          style: AppTypography.headline2.copyWith(
            color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          'Design and personalize shoes to match your style',
          style: AppTypography.bodyLarge.copyWith(
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _build3DPreviewCard(BuildContext context, bool isDark) {
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
            // 3D Preview
            Container(
              width: double.infinity,
              height: 300,
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusLarge,
                gradient: AppGradients.overlayGradient,
              ),
              child: Stack(
                children: [
                  // 3D Model placeholder
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 120,
                          height: 120,
                          decoration: BoxDecoration(
                            color: _colors[_selectedColorIndex],
                            borderRadius: AppRadius.radiusLarge,
                            boxShadow: [
                              BoxShadow(
                                color: _colors[_selectedColorIndex].withOpacity(0.3),
                                blurRadius: 20,
                                spreadRadius: 5,
                              ),
                            ],
                          ),
                          child: const Icon(
                            Icons.shopping_bag,
                            size: 60,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.lg),
                        Text(
                          '3D Preview',
                          style: AppTypography.bodyLarge.copyWith(
                            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        Text(
                          '${_textures[_selectedTextureIndex].name} • ${_getShineLabel()}',
                          style: AppTypography.bodyMedium.copyWith(
                            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                          ),
                        ),
                      ],
                    ),
                  ),
                  
                  // Rotation controls
                  Positioned(
                    top: AppSpacing.lg,
                    right: AppSpacing.lg,
                    child: Row(
                      children: [
                        IconButton(
                          onPressed: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Rotating left...')),
                            );
                          },
                          icon: const Icon(Icons.rotate_left),
                          style: IconButton.styleFrom(
                            backgroundColor: AppColors.accent10,
                            foregroundColor: AppColors.accent,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.sm),
                        IconButton(
                          onPressed: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Rotating right...')),
                            );
                          },
                          icon: const Icon(Icons.rotate_right),
                          style: IconButton.styleFrom(
                            backgroundColor: AppColors.accent10,
                            foregroundColor: AppColors.accent,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            
            // Save Button
            PrimaryButton(
              text: _isSaving ? 'Saving...' : 'Save Custom Design',
              onPressed: _isSaving ? null : _saveCustomDesign,
              icon: Icons.save,
              isFullWidth: true,
              isLoading: _isSaving,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDrawingCanvas(BuildContext context, bool isDark) {
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
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Draw Pattern',
                  style: AppTypography.headline4.copyWith(
                    color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.undo),
                  onPressed: () {
                    if (!_painterController.isEmpty) {
                      _painterController.undo();
                    }
                  },
                ),
                IconButton(
                  icon: const Icon(Icons.delete),
                  onPressed: () => _painterController.clear(),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),
            Container(
              height: 200,
              decoration: BoxDecoration(
                border: Border.all(color: AppColors.lightBorder),
                borderRadius: AppRadius.radiusLarge,
              ),
              child: ClipRRect(
                borderRadius: AppRadius.radiusLarge,
                child: Painter(_painterController),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCustomizationOptions(BuildContext context, bool isDark) {
    return Column(
      children: [
        // Color Selection
        _buildColorSelectionCard(context, isDark),
        const SizedBox(height: AppSpacing.xl2),
        
        // Texture Selection
        _buildTextureSelectionCard(context, isDark),
        const SizedBox(height: AppSpacing.xl2),
        
        // Shine Adjustment
        _buildShineAdjustmentCard(context, isDark),
      ],
    );
  }

  Widget _buildColorSelectionCard(BuildContext context, bool isDark) {
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
              'Color Selection',
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            
            // Color Grid
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 4,
                crossAxisSpacing: AppSpacing.md,
                mainAxisSpacing: AppSpacing.md,
                childAspectRatio: 1.0,
              ),
              itemCount: _colors.length,
              itemBuilder: (context, index) {
                final color = _colors[index];
                final isSelected = _selectedColorIndex == index;
                
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _selectedColorIndex = index;
                    });
                  },
                  child: Container(
                    decoration: BoxDecoration(
                      color: color,
                      borderRadius: AppRadius.radiusLarge,
                      border: Border.all(
                        color: isSelected ? AppColors.accent : AppColors.lightBorder,
                        width: isSelected ? 3 : 1,
                      ),
                    ),
                    child: isSelected
                        ? const Center(
                            child: Icon(
                              Icons.check,
                              color: Colors.white,
                              size: 24,
                            ),
                          )
                        : null,
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTextureSelectionCard(BuildContext context, bool isDark) {
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
              'Texture Selection',
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            
            // Texture Grid
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: AppSpacing.lg,
                mainAxisSpacing: AppSpacing.lg,
                childAspectRatio: 2.0,
              ),
              itemCount: _textures.length,
              itemBuilder: (context, index) {
                final texture = _textures[index];
                final isSelected = _selectedTextureIndex == index;
                
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _selectedTextureIndex = index;
                    });
                  },
                  child: Container(
                    padding: AppSpacing.paddingMedium,
                    decoration: BoxDecoration(
                      borderRadius: AppRadius.radiusLarge,
                      border: Border.all(
                        color: isSelected ? AppColors.accent : AppColors.lightBorder,
                        width: isSelected ? 2 : 1,
                      ),
                      color: isSelected ? AppColors.accent10 : Colors.transparent,
                    ),
                    child: Row(
                      children: [
                        Icon(
                          texture.icon,
                          color: isSelected ? AppColors.accent : AppColors.lightMutedForeground,
                          size: 24,
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Text(
                          texture.name,
                          style: AppTypography.bodyMedium.copyWith(
                            fontWeight: isSelected ? AppTypography.semibold : AppTypography.normal,
                            color: isSelected 
                                ? AppColors.accent 
                                : (isDark ? AppColors.darkForeground : AppColors.lightForeground),
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

  Widget _buildShineAdjustmentCard(BuildContext context, bool isDark) {
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
              'Shine Adjustment',
              style: AppTypography.headline4.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            
            // Slider
            Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Matte',
                      style: AppTypography.bodySmall.copyWith(
                        color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                      ),
                    ),
                    Text(
                      '${_shineValue.round()}%',
                      style: AppTypography.bodyMedium.copyWith(
                        fontWeight: AppTypography.semibold,
                        color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                      ),
                    ),
                    Text(
                      'Glossy',
                      style: AppTypography.bodySmall.copyWith(
                        color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.md),
                SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    activeTrackColor: AppColors.accent,
                    inactiveTrackColor: AppColors.lightBorder,
                    thumbColor: AppColors.accent,
                    overlayColor: AppColors.accent.withOpacity(0.2),
                    trackHeight: 4,
                  ),
                  child: Slider(
                    value: _shineValue,
                    min: 0,
                    max: 100,
                    divisions: 100,
                    onChanged: (value) {
                      setState(() {
                        _shineValue = value;
                      });
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _getShineLabel() {
    if (_shineValue < 25) return 'Matte';
    if (_shineValue < 75) return 'Semi-Gloss';
    return 'Glossy';
  }

  /// Save the custom design to the backend
  Future<void> _saveCustomDesign() async {
    setState(() => _isSaving = true);

    try {
      // Build skin name from selected options
      final colorName = _getColorName(_colors[_selectedColorIndex]);
      final textureName = _textures[_selectedTextureIndex].name;
      final skinName = '$colorName $textureName - ${_getShineLabel()}';

      final result = await _skinService.createSkin(
        shoeId: 'default', // Would come from product selection in full implementation
        skinName: skinName,
        textureUrl: null, // Would be texture file URL in full implementation
      );

      if (mounted) {
        if (result != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Custom design "$skinName" saved!'),
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
            content: Text('Error saving design: $e'),
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

  String _getColorName(Color color) {
    if (color == const Color(0xFF000000)) return 'Black';
    if (color == const Color(0xFF8B4513)) return 'Brown';
    if (color == const Color(0xFFDC143C)) return 'Red';
    if (color == const Color(0xFF0000FF)) return 'Blue';
    if (color == const Color(0xFF228B22)) return 'Green';
    if (color == const Color(0xFF800080)) return 'Purple';
    if (color == const Color(0xFFFFFFFF)) return 'White';
    if (color == const Color(0xFFC0C0C0)) return 'Silver';
    return 'Custom';
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

/// Texture option data
class TextureOption {
  const TextureOption({
    required this.name,
    required this.icon,
  });

  final String name;
  final IconData icon;
}

class _RedesignRequestForm extends StatefulWidget {
  const _RedesignRequestForm({Key? key}) : super(key: key);

  @override
  State<_RedesignRequestForm> createState() => _RedesignRequestFormState();
}

class _RedesignRequestFormState extends State<_RedesignRequestForm> {
  final TextEditingController _descController = TextEditingController();
  final List<XFile> _selectedImages = [];
  bool _isSending = false;
  final ApiClient _apiClient = ApiClient();

  Future<void> _pickImages() async {
    final ImagePicker picker = ImagePicker();
    final List<XFile> images = await picker.pickMultiImage();
    if (images.isNotEmpty) {
      setState(() {
        _selectedImages.addAll(images);
      });
    }
  }

  void _removeImage(int index) {
    setState(() {
      _selectedImages.removeAt(index);
    });
  }

  Future<void> _submitRequest() async {
    if (_descController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Please add a description')));
      return;
    }
    
    setState(() => _isSending = true);
    
    try {
      final List<http.MultipartFile> files = [];
      for (var file in _selectedImages) {
        files.add(await http.MultipartFile.fromPath('images', file.path));
      }

      final response = await _apiClient.postMultipart(
        '/skins/request-redesign',
        fields: {
          'shoeId': '000000000000000000000001', // Fallback
          'description': _descController.text,
        },
        files: files.isNotEmpty ? files : null,
        requiresAuth: true,
      );

      if (response.statusCode == 201 && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Redesign requested successfully!')));
        setState(() {
          _selectedImages.clear();
          _descController.clear();
        });
      } else {
        if (mounted) {
           ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed. Ensure you are logged in.')));
        }
      }
    } catch (e) {
      if (mounted) {
         ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    } finally {
      if (mounted) setState(() => _isSending = false);
    }
  }

  @override
  void dispose() {
    _descController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Card(
      elevation: 0,
      shadowColor: AppColors.lightForeground.withOpacity(0.15),
      shape: RoundedRectangleBorder(borderRadius: AppRadius.radiusLarge),
      child: Container(
        padding: AppSpacing.paddingLarge,
        decoration: BoxDecoration(
          borderRadius: AppRadius.radiusLarge,
          gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Request Custom Redesign', style: AppTypography.headline4.copyWith(color: isDark ? AppColors.darkForeground : AppColors.lightForeground)),
            const SizedBox(height: AppSpacing.sm),
            Text('Send references and multiple concept images to our seller', style: AppTypography.bodyMedium),
            const SizedBox(height: AppSpacing.md),
            
            TextField(
              controller: _descController,
              decoration: const InputDecoration(labelText: 'Description / Instructions', border: OutlineInputBorder()),
              maxLines: 3,
            ),
            
            const SizedBox(height: AppSpacing.md),
            
            Wrap(
              spacing: 8.0,
              runSpacing: 8.0,
              children: [
                ..._selectedImages.asMap().entries.map((entry) {
                   int index = entry.key;
                   XFile file = entry.value;
                   return Stack(
                     children: [
                       ClipRRect(
                         borderRadius: BorderRadius.circular(8),
                         child: Image.file(File(file.path), width: 80, height: 80, fit: BoxFit.cover),
                       ),
                       Positioned(
                         right: 0,
                         top: 0,
                         child: GestureDetector(
                           onTap: () => _removeImage(index),
                           child: Container(
                             padding: const EdgeInsets.all(2),
                             color: Colors.black54,
                             child: const Icon(Icons.close, color: Colors.white, size: 16),
                           ),
                         ),
                       )
                     ],
                   );
                }).toList(),
                
                GestureDetector(
                  onTap: _pickImages,
                  child: Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      border: Border.all(color: AppColors.accent),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.add_a_photo, color: AppColors.accent),
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: AppSpacing.lg),
            
            _isSending 
              ? const Center(child: CircularProgressIndicator()) 
              : CustomButton(
                  text: 'Send Request',
                  onPressed: _submitRequest,
                  isFullWidth: true,
                )
          ],
        ),
      ),
    );
  }
}

