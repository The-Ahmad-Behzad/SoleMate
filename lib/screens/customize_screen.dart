import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import 'auth/login_screen.dart';

/// Customize screen with 3D preview and customization options
class CustomizeScreen extends StatefulWidget {
  const CustomizeScreen({super.key});

  @override
  State<CustomizeScreen> createState() => _CustomizeScreenState();
}

class _CustomizeScreenState extends State<CustomizeScreen> {
  final AuthService _authService = AuthService();
  int _selectedColorIndex = 0;
  int _selectedTextureIndex = 0;
  double _shineValue = 50.0;

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
              _buildCustomizationOptions(context, isDark),
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
                    child: _buildCustomizationOptions(context, isDark),
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
              text: 'Save Custom Design',
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Custom design saved!')),
                );
              },
              icon: Icons.save,
              isFullWidth: true,
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
