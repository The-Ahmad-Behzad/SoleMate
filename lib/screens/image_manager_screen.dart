import 'dart:io';
import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../ar/ar_main.dart';

/// CamScanner-inspired Image Manager for viewing and managing captured AR snaps.
/// Features:
/// - Carousel view with swipe navigation (default)
/// - Pinch-zoom on individual images
/// - Grid view with 2 columns (pinch-out to switch)
/// - Top navbar with undo/redo/share buttons
/// - Bottom navbar with filter mode toggle
class ImageManagerScreen extends StatefulWidget {
  /// List of image file paths to display
  final List<String> imagePaths;
  
  /// Callback when images are modified (deleted, filtered, etc.)
  final VoidCallback? onImagesChanged;

  const ImageManagerScreen({
    super.key,
    required this.imagePaths,
    this.onImagesChanged,
  });

  @override
  State<ImageManagerScreen> createState() => _ImageManagerScreenState();
}

class _ImageManagerScreenState extends State<ImageManagerScreen> {
  late List<String> _imagePaths;
  late PageController _pageController;
  int _currentIndex = 0;
  bool _isGridView = false;
  bool _isFilterMode = false;
  
  // For pinch-to-zoom gesture detection
  double _lastScale = 1.0;
  
  @override
  void initState() {
    super.initState();
    _imagePaths = List.from(widget.imagePaths);
    _pageController = PageController(initialPage: 0);
  }
  
  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    
    return Scaffold(
      backgroundColor: isDark ? AppColors.darkBackground : AppColors.lightBackground,
      appBar: _buildAppBar(isDark),
      body: _imagePaths.isEmpty
          ? _buildEmptyState(isDark)
          : _isGridView
              ? _buildGridView(isDark)
              : _buildCarouselView(isDark),
      bottomNavigationBar: _imagePaths.isNotEmpty ? _buildBottomBar(isDark) : null,
    );
  }

  PreferredSizeWidget _buildAppBar(bool isDark) {
    return AppBar(
      backgroundColor: isDark ? AppColors.darkCard : AppColors.lightCard,
      elevation: 0,
      leading: IconButton(
        icon: Icon(
          Icons.arrow_back,
          color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
        ),
        onPressed: () => Navigator.of(context).pop(),
      ),
      title: Text(
        _imagePaths.isEmpty 
            ? 'Image Manager' 
            : '${_currentIndex + 1} of ${_imagePaths.length}',
        style: AppTypography.headline4.copyWith(
          color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
        ),
      ),
      centerTitle: true,
      actions: [
        // Undo button
        IconButton(
          icon: Icon(
            Icons.undo,
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
          onPressed: () {
            // TODO: Implement undo in Phase 5
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Undo coming soon')),
            );
          },
        ),
        // Redo button
        IconButton(
          icon: Icon(
            Icons.redo,
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
          onPressed: () {
            // TODO: Implement redo in Phase 5
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Redo coming soon')),
            );
          },
        ),
        // Share button
        IconButton(
          icon: Icon(
            Icons.share,
            color: AppColors.primary,
          ),
          onPressed: _imagePaths.isNotEmpty ? _showShareOverlay : null,
        ),
      ],
    );
  }

  Widget _buildEmptyState(bool isDark) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.photo_library_outlined,
            size: 80,
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
          const SizedBox(height: AppSpacing.lg),
          Text(
            'No snaps yet',
            style: AppTypography.headline4.copyWith(
              color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            'Capture some AR snaps to view them here',
            style: AppTypography.bodyMedium.copyWith(
              color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCarouselView(bool isDark) {
    return GestureDetector(
      onScaleStart: (_) {
        _lastScale = 1.0;
      },
      onScaleUpdate: (details) {
        // Detect pinch-out to switch to grid view
        if (details.scale < 0.8 && _lastScale >= 0.8) {
          setState(() {
            _isGridView = true;
          });
        }
        _lastScale = details.scale;
      },
      child: PageView.builder(
        controller: _pageController,
        onPageChanged: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        itemCount: _imagePaths.length,
        itemBuilder: (context, index) {
          return _buildImageCard(index, isDark);
        },
      ),
    );
  }

  Widget _buildImageCard(int index, bool isDark) {
    final path = _imagePaths[index];
    final file = File(path);
    
    return Container(
      margin: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        borderRadius: AppRadius.radiusLarge,
        boxShadow: isDark ? AppShadows.cardShadowsDark : AppShadows.cardShadows,
      ),
      child: ClipRRect(
        borderRadius: AppRadius.radiusLarge,
        child: InteractiveViewer(
          minScale: 0.5,
          maxScale: 4.0,
          child: file.existsSync()
              ? Image.file(
                  file,
                  fit: BoxFit.contain,
                  errorBuilder: (_, __, ___) => _buildImageError(isDark),
                )
              : _buildImageError(isDark),
        ),
      ),
    );
  }

  Widget _buildImageError(bool isDark) {
    return Container(
      color: isDark ? AppColors.darkCard : AppColors.lightCard,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.broken_image,
              size: 64,
              color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              'Image not found',
              style: AppTypography.bodyMedium.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGridView(bool isDark) {
    return GestureDetector(
      onScaleUpdate: (details) {
        // Detect pinch-in to switch back to carousel view
        if (details.scale > 1.3 && _lastScale <= 1.3) {
          setState(() {
            _isGridView = false;
          });
        }
        _lastScale = details.scale;
      },
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: GridView.builder(
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            crossAxisSpacing: AppSpacing.md,
            mainAxisSpacing: AppSpacing.md,
            childAspectRatio: 0.75,
          ),
          itemCount: _imagePaths.length,
          itemBuilder: (context, index) {
            return _buildGridItem(index, isDark);
          },
        ),
      ),
    );
  }

  Widget _buildGridItem(int index, bool isDark) {
    final path = _imagePaths[index];
    final file = File(path);
    final isSelected = _currentIndex == index;
    
    return GestureDetector(
      onTap: () {
        setState(() {
          _currentIndex = index;
          _isGridView = false;
        });
        _pageController.jumpToPage(index);
      },
      child: Stack(
        fit: StackFit.expand,
        children: [
          // Image thumbnail
          Container(
            decoration: BoxDecoration(
              borderRadius: AppRadius.radiusMedium,
              border: isSelected
                  ? Border.all(color: AppColors.primary, width: 3)
                  : null,
              boxShadow: isDark ? AppShadows.cardShadowsDark : AppShadows.cardShadows,
            ),
            child: ClipRRect(
              borderRadius: AppRadius.radiusMedium,
              child: file.existsSync()
                  ? Image.file(
                      file,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _buildImageError(isDark),
                    )
                  : _buildImageError(isDark),
            ),
          ),
          // Delete button overlay
          Positioned(
            top: AppSpacing.xs,
            right: AppSpacing.xs,
            child: GestureDetector(
              onTap: () => _showDeleteConfirmation(index),
              child: Container(
                padding: const EdgeInsets.all(AppSpacing.xs),
                decoration: BoxDecoration(
                  color: AppColors.destructive.withOpacity(0.9),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.close,
                  color: Colors.white,
                  size: 18,
                ),
              ),
            ),
          ),
          // Index badge
          Positioned(
            bottom: AppSpacing.xs,
            left: AppSpacing.xs,
            child: Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.sm,
                vertical: AppSpacing.xs,
              ),
              decoration: BoxDecoration(
                color: isDark 
                    ? AppColors.darkCard.withOpacity(0.8) 
                    : AppColors.lightCard.withOpacity(0.8),
                borderRadius: AppRadius.radiusSmall,
              ),
              child: Text(
                '${index + 1}',
                style: AppTypography.caption.copyWith(
                  color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomBar(bool isDark) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : AppColors.lightCard,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            // Grid/Carousel toggle
            _buildBottomBarButton(
              icon: _isGridView ? Icons.view_carousel : Icons.grid_view,
              label: _isGridView ? 'Carousel' : 'Grid',
              onTap: () {
                setState(() {
                  _isGridView = !_isGridView;
                });
              },
              isDark: isDark,
            ),
            // Filter toggle
            _buildBottomBarButton(
              icon: Icons.filter,
              label: 'Filter',
              isActive: _isFilterMode,
              onTap: () {
                setState(() {
                  _isFilterMode = !_isFilterMode;
                });
                // TODO: Show filter carousel in Phase 4
                if (_isFilterMode) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Filters coming in Phase 4')),
                  );
                }
              },
              isDark: isDark,
            ),
            // Delete current
            _buildBottomBarButton(
              icon: Icons.delete_outline,
              label: 'Delete',
              onTap: () => _showDeleteConfirmation(_currentIndex),
              isDark: isDark,
              isDestructive: true,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBottomBarButton({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
    required bool isDark,
    bool isActive = false,
    bool isDestructive = false,
  }) {
    final color = isDestructive 
        ? AppColors.destructive 
        : isActive 
            ? AppColors.primary 
            : (isDark ? AppColors.darkForeground : AppColors.lightForeground);
    
    return InkWell(
      onTap: onTap,
      borderRadius: AppRadius.radiusMedium,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.sm,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: AppSpacing.xs),
            Text(
              label,
              style: AppTypography.caption.copyWith(color: color),
            ),
          ],
        ),
      ),
    );
  }

  void _showDeleteConfirmation(int index) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Snap?'),
        content: const Text('This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _deleteImage(index);
            },
            style: TextButton.styleFrom(
              foregroundColor: AppColors.destructive,
            ),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }

  void _deleteImage(int index) {
    if (index < 0 || index >= _imagePaths.length) return;
    
    setState(() {
      _imagePaths.removeAt(index);
      if (_currentIndex >= _imagePaths.length) {
        _currentIndex = _imagePaths.isEmpty ? 0 : _imagePaths.length - 1;
      }
    });
    
    widget.onImagesChanged?.call();
    
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Snap deleted')),
    );
  }

  void _showShareOverlay() {
    // TODO: Implement share overlay in Phase 6
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        padding: const EdgeInsets.all(AppSpacing.lg),
        decoration: BoxDecoration(
          color: Theme.of(context).brightness == Brightness.dark
              ? AppColors.darkCard
              : AppColors.lightCard,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: AppSpacing.lg),
                decoration: BoxDecoration(
                  color: Colors.grey[400],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              Text(
                'Share Snap',
                style: AppTypography.headline4,
              ),
              const SizedBox(height: AppSpacing.lg),
              ListTile(
                leading: const Icon(Icons.save_alt, color: AppColors.primary),
                title: const Text('Save to Gallery'),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Save to Gallery coming in Phase 6')),
                  );
                },
              ),
              ListTile(
                leading: const Icon(Icons.camera_alt, color: Color(0xFFE4405F)),
                title: const Text('Share to Instagram'),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Instagram share coming in Phase 6')),
                  );
                },
              ),
              ListTile(
                leading: const Icon(Icons.facebook, color: Color(0xFF1877F2)),
                title: const Text('Share to Facebook'),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Facebook share coming in Phase 6')),
                  );
                },
              ),
              ListTile(
                leading: Icon(Icons.share, color: AppColors.accent),
                title: const Text('More Options...'),
                onTap: () {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Native share coming in Phase 6')),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
