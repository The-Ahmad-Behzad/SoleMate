import 'dart:io';
import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../ar/ar_main.dart';
import '../models/image_filter.dart';
import '../models/image_action.dart';

/// CamScanner-inspired Image Manager for viewing and managing captured AR snaps.
/// Features:
/// - Carousel view with swipe navigation (default)
/// - Pinch-zoom on individual images
/// - Grid view with 2 columns (pinch-out to switch)
/// - Top navbar with undo/redo/share buttons
/// - Bottom navbar with filter mode toggle
/// - Non-destructive filters using ColorFiltered
/// - Undo/redo for delete, filter, and reorder actions
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
  bool _isGridView = true; // Start in grid view by default (no preselection)
  bool _isFilterMode = false;
  
  // Selection mode state for multi-select in grid view
  bool _isSelectionMode = false;
  final Set<String> _selectedPaths = {};
  
  // Track applied filter for each image path
  final Map<String, ImageFilter> _appliedFilters = {};
  
  // Currently previewing filter (before confirmation)
  ImageFilter? _previewingFilter;
  
  // Undo/Redo action history manager
  final ActionHistoryManager _historyManager = ActionHistoryManager();
  
  // For pinch-to-zoom gesture detection
  double _lastScale = 1.0;
  
  @override
  void initState() {
    super.initState();
    _imagePaths = List.from(widget.imagePaths);
    _pageController = PageController(initialPage: 0);
    
    // Initialize all images with no filter, then load saved filters
    for (final path in _imagePaths) {
      _appliedFilters[path] = ImageFilter.none;
    }
    
    // Load persisted filters from native
    _loadFiltersFromNative();
  }
  
  /// Load filters from native ScreenshotManager
  Future<void> _loadFiltersFromNative() async {
    final arMain = ARMain();
    final savedFilters = await arMain.getFilters();
    
    setState(() {
      for (final entry in savedFilters.entries) {
        final path = entry.key;
        final filterId = entry.value;
        if (filterId != null && _appliedFilters.containsKey(path)) {
          // Find matching filter by id
          final filter = ImageFilter.availableFilters.firstWhere(
            (f) => f.id == filterId,
            orElse: () => ImageFilter.none,
          );
          _appliedFilters[path] = filter;
        }
      }
    });
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
    
    return PopScope(
      canPop: false, // Prevent default back behavior
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) {
          _handleBackNavigation();
        }
      },
      child: Scaffold(
        backgroundColor: isDark ? AppColors.darkBackground : AppColors.lightBackground,
        appBar: _buildAppBar(isDark),
        body: Column(
          children: [
            // Main content area
            Expanded(
              child: _imagePaths.isEmpty
                  ? _buildEmptyState(isDark)
                  : _isGridView
                      ? _buildGridView(isDark)
                      : _buildCarouselView(isDark),
            ),
            // Filter carousel (shown when filter mode is active)
            if (_isFilterMode && _imagePaths.isNotEmpty)
              _buildFilterCarousel(isDark),
          ],
        ),
        bottomNavigationBar: _imagePaths.isNotEmpty ? _buildBottomBar(isDark) : null,
      ),
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
        onPressed: _handleBackNavigation,
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
            color: _historyManager.canUndo 
                ? (isDark ? AppColors.darkForeground : AppColors.lightForeground)
                : (isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground),
          ),
          tooltip: _historyManager.canUndo 
              ? 'Undo ${_historyManager.nextUndoAction?.description ?? ""}' 
              : 'Nothing to undo',
          onPressed: _historyManager.canUndo ? _performUndo : null,
        ),
        // Redo button
        IconButton(
          icon: Icon(
            Icons.redo,
            color: _historyManager.canRedo 
                ? (isDark ? AppColors.darkForeground : AppColors.lightForeground)
                : (isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground),
          ),
          tooltip: _historyManager.canRedo 
              ? 'Redo ${_historyManager.nextRedoAction?.description ?? ""}' 
              : 'Nothing to redo',
          onPressed: _historyManager.canRedo ? _performRedo : null,
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
    
    // Get the filter to apply
    // Preview filter only applies to current image in carousel mode
    final ImageFilter currentFilter;
    if (!_isGridView && index == _currentIndex && _previewingFilter != null) {
      currentFilter = _previewingFilter!;
    } else {
      currentFilter = _appliedFilters[path] ?? ImageFilter.none;
    }
    
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
              ? _buildFilteredImage(file, currentFilter, isDark)
              : _buildImageError(isDark),
        ),
      ),
    );
  }
  
  /// Wrap image with ColorFiltered for non-destructive filter application
  Widget _buildFilteredImage(
    File file, 
    ImageFilter filter, 
    bool isDark, {
    BoxFit fit = BoxFit.contain,
  }) {
    final image = Image.file(
      file,
      fit: fit,
      errorBuilder: (_, __, ___) => _buildImageError(isDark),
    );
    
    if (filter.colorFilter != null) {
      return ColorFiltered(
        colorFilter: filter.colorFilter!,
        child: image,
      );
    }
    return image;
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
    final isSelected = _selectedPaths.contains(path);
    final appliedFilter = _appliedFilters[path] ?? ImageFilter.none;
    
    return GestureDetector(
      onTap: () {
        if (_isSelectionMode) {
          // In selection mode: toggle selection
          setState(() {
            if (isSelected) {
              _selectedPaths.remove(path);
              // Exit selection mode if nothing selected
              if (_selectedPaths.isEmpty) {
                _isSelectionMode = false;
              }
            } else {
              _selectedPaths.add(path);
            }
          });
        } else {
          // Normal mode: open carousel at this image
          setState(() {
            _currentIndex = index;
            _isGridView = false;
            _previewingFilter = null;
          });
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (_pageController.hasClients) {
              _pageController.jumpToPage(index);
            }
          });
        }
      },
      onLongPress: () {
        // Enter selection mode and select this image
        setState(() {
          _isSelectionMode = true;
          _selectedPaths.add(path);
        });
      },
      child: Stack(
        fit: StackFit.expand,
        children: [
          // Image thumbnail with filter applied
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
                  ? _buildFilteredImage(
                      file, 
                      appliedFilter, 
                      isDark,
                      fit: BoxFit.cover,
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
          // Selection checkmark overlay
          if (isSelected)
            Positioned(
              top: AppSpacing.xs,
              left: AppSpacing.xs,
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 2),
                ),
                child: const Icon(
                  Icons.check,
                  color: Colors.white,
                  size: 16,
                ),
              ),
            ),
          // Selection overlay tint
          if (isSelected)
            Container(
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusMedium,
                color: AppColors.primary.withOpacity(0.2),
                border: Border.all(color: AppColors.primary, width: 3),
              ),
            ),
        ],
      ),
    );
  }
  
  /// Build horizontal filter carousel with preview thumbnails
  Widget _buildFilterCarousel(bool isDark) {
    if (_imagePaths.isEmpty) return const SizedBox.shrink();
    
    final currentPath = _imagePaths[_currentIndex];
    final currentAppliedFilter = _appliedFilters[currentPath] ?? ImageFilter.none;
    
    return Container(
      height: 100,
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkCard : AppColors.lightCard,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm),
        itemCount: ImageFilter.availableFilters.length,
        itemBuilder: (context, index) {
          final filter = ImageFilter.availableFilters[index];
          final isSelected = currentAppliedFilter.id == filter.id;
          final isPreviewing = _previewingFilter?.id == filter.id;
          
          return GestureDetector(
            onTap: () {
              setState(() {
                _previewingFilter = filter;
              });
              // Show confirmation popup
              _showApplyFilterConfirmation(filter, isDark);
            },
            child: Container(
              width: 70,
              margin: const EdgeInsets.symmetric(horizontal: AppSpacing.xs),
              decoration: BoxDecoration(
                borderRadius: AppRadius.radiusMedium,
                border: Border.all(
                  color: isPreviewing 
                      ? AppColors.secondary 
                      : isSelected 
                          ? AppColors.primary 
                          : Colors.transparent,
                  width: 2,
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Filter preview thumbnail
                  Container(
                    width: 50,
                    height: 50,
                    decoration: BoxDecoration(
                      borderRadius: AppRadius.radiusSmall,
                      color: isDark ? AppColors.darkMuted : AppColors.lightMuted,
                    ),
                    child: ClipRRect(
                      borderRadius: AppRadius.radiusSmall,
                      child: _buildFilterPreviewThumbnail(filter, isDark),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  // Filter name
                  Text(
                    filter.name,
                    style: AppTypography.caption.copyWith(
                      color: isSelected 
                          ? AppColors.primary 
                          : (isDark ? AppColors.darkForeground : AppColors.lightForeground),
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                    ),
                    textAlign: TextAlign.center,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
  
  /// Build a small preview thumbnail with the filter applied
  Widget _buildFilterPreviewThumbnail(ImageFilter filter, bool isDark) {
    final baseColor = filter.id == 'none' 
        ? (isDark ? AppColors.darkMuted : AppColors.lightMuted)
        : filter.id == 'bw' || filter.id == 'contrast'
            ? Colors.grey
            : filter.id == 'sepia' || filter.id == 'warm' || filter.id == 'vintage'
                ? Colors.brown.shade200
                : Colors.blue.shade200;
    
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [baseColor, baseColor.withOpacity(0.6)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Center(
        child: Icon(
          filter.id == 'none' ? Icons.image : Icons.filter,
          color: Colors.white.withOpacity(0.8),
          size: 20,
        ),
      ),
    );
  }
  
  /// Show confirmation popup to apply a filter
  void _showApplyFilterConfirmation(ImageFilter filter, bool isDark) {
    final isApplyToAll = _isGridView;
    final title = isApplyToAll 
        ? 'Apply ${filter.name} to All?' 
        : 'Apply ${filter.name} Filter?';
    
    final content = isApplyToAll
        ? filter.id == 'none' 
            ? 'This will remove filters from all ${_imagePaths.length} images.'
            : 'This will apply the ${filter.name} filter to all ${_imagePaths.length} images.'
        : filter.id == 'none' 
            ? 'This will remove the filter from this image.'
            : 'This will apply the ${filter.name} filter to this image.';
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              setState(() {
                _previewingFilter = null;
              });
            },
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              if (isApplyToAll) {
                _applyFilterToAll(filter);
              } else {
                _applyFilter(filter);
              }
            },
            style: TextButton.styleFrom(
              foregroundColor: AppColors.primary,
            ),
            child: Text(isApplyToAll ? 'Apply to All' : 'Apply'),
          ),
        ],
      ),
    );
  }
  
  /// Apply a filter to the current image only
  Future<void> _applyFilter(ImageFilter filter) async {
    if (_imagePaths.isEmpty) return;
    
    final currentPath = _imagePaths[_currentIndex];
    final previousFilter = _appliedFilters[currentPath] ?? ImageFilter.none;
    
    // Record filter action for undo
    if (filter.id == 'none' && previousFilter.id != 'none') {
      _historyManager.recordAction(ImageAction.filterRemove(
        path: currentPath,
        previousFilter: previousFilter,
      ));
    } else if (filter.id != 'none') {
      _historyManager.recordAction(ImageAction.filterApply(
        path: currentPath,
        previousFilter: previousFilter,
        newFilter: filter,
      ));
    }
    
    setState(() {
      _appliedFilters[currentPath] = filter;
      _previewingFilter = null;
    });
    
    // Persist to native
    final arMain = ARMain();
    await arMain.setFilter(currentPath, filter.id == 'none' ? null : filter.id);
    
    widget.onImagesChanged?.call();
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            filter.id == 'none' 
                ? 'Filter removed' 
                : '${filter.name} filter applied',
          ),
        ),
      );
    }
  }
  
  /// Apply a filter to all images (grid mode)
  Future<void> _applyFilterToAll(ImageFilter filter) async {
    if (_imagePaths.isEmpty) return;
    
    // Record filter action for each image (for undo support)
    for (final path in _imagePaths) {
      final previousFilter = _appliedFilters[path] ?? ImageFilter.none;
      if (filter.id == 'none' && previousFilter.id != 'none') {
        _historyManager.recordAction(ImageAction.filterRemove(
          path: path,
          previousFilter: previousFilter,
        ));
      } else if (filter.id != 'none') {
        _historyManager.recordAction(ImageAction.filterApply(
          path: path,
          previousFilter: previousFilter,
          newFilter: filter,
        ));
      }
    }
    
    setState(() {
      for (final path in _imagePaths) {
        _appliedFilters[path] = filter;
      }
      _previewingFilter = null;
    });
    
    // Persist to native
    final arMain = ARMain();
    for (final path in _imagePaths) {
      await arMain.setFilter(path, filter.id == 'none' ? null : filter.id);
    }
    
    widget.onImagesChanged?.call();
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            filter.id == 'none' 
                ? 'Filters removed from all images' 
                : '${filter.name} filter applied to all images',
          ),
        ),
      );
    }
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
              },
              isDark: isDark,
            ),
            // Delete current or selected items
            _buildBottomBarButton(
              icon: Icons.delete_outline,
              label: _isSelectionMode && _selectedPaths.isNotEmpty 
                  ? 'Delete (${_selectedPaths.length})' 
                  : 'Delete',
              onTap: () {
                if (_isSelectionMode && _selectedPaths.isNotEmpty) {
                  _showDeleteSelectedConfirmation();
                } else {
                  _showDeleteConfirmation(_currentIndex);
                }
              },
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
    bool isPrimary = false,
  }) {
    final color = isDestructive 
        ? AppColors.destructive 
        : isPrimary 
            ? AppColors.primary
            : isActive 
                ? AppColors.secondary 
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

  /// Show delete confirmation for multiple selected images
  void _showDeleteSelectedConfirmation() {
    final count = _selectedPaths.length;
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Delete $count Snaps?'),
        content: const Text('This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _deleteSelectedImages();
            },
            style: TextButton.styleFrom(
              foregroundColor: AppColors.destructive,
            ),
            child: const Text('Delete All'),
          ),
        ],
      ),
    );
  }

  /// Delete all selected images
  Future<void> _deleteSelectedImages() async {
    if (_selectedPaths.isEmpty) return;
    
    // Get paths to delete (make a copy since we'll modify the set)
    final pathsToDelete = List<String>.from(_selectedPaths);
    
    // Delete in reverse index order to maintain correct indices
    final indicesToDelete = <int>[];
    for (final path in pathsToDelete) {
      final idx = _imagePaths.indexOf(path);
      if (idx >= 0) {
        indicesToDelete.add(idx);
      }
    }
    indicesToDelete.sort((a, b) => b.compareTo(a)); // Sort descending
    
    final arMain = ARMain();
    
    for (final idx in indicesToDelete) {
      final path = _imagePaths[idx];
      
      // Record delete action for undo
      _historyManager.recordAction(ImageAction.delete(
        path: path,
        index: idx,
      ));
      
      // Sync with native
      await arMain.deleteSnapByPath(path);
      
      setState(() {
        _imagePaths.removeAt(idx);
        _appliedFilters.remove(path);
      });
    }
    
    // Clear selection and exit selection mode
    setState(() {
      _selectedPaths.clear();
      _isSelectionMode = false;
      if (_currentIndex >= _imagePaths.length) {
        _currentIndex = _imagePaths.isEmpty ? 0 : _imagePaths.length - 1;
      }
    });
    
    widget.onImagesChanged?.call();
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${pathsToDelete.length} snaps deleted')),
      );
    }
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

  Future<void> _deleteImage(int index) async {
    if (index < 0 || index >= _imagePaths.length) return;
    
    final pathToDelete = _imagePaths[index];
    
    // Record delete action BEFORE deleting (for undo)
    _historyManager.recordAction(ImageAction.delete(
      path: pathToDelete,
      index: index,
    ));
    
    // Sync deletion with native ScreenshotManager
    final arMain = ARMain();
    await arMain.deleteSnapByPath(pathToDelete);
    
    setState(() {
      _imagePaths.removeAt(index);
      _appliedFilters.remove(pathToDelete);
      if (_currentIndex >= _imagePaths.length) {
        _currentIndex = _imagePaths.isEmpty ? 0 : _imagePaths.length - 1;
      }
    });
    
    widget.onImagesChanged?.call();
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Snap deleted')),
      );
    }
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
                onTap: () async {
                  Navigator.pop(context);
                  await _saveToGallery();
                },
              ),
              ListTile(
                leading: const Icon(Icons.camera_alt, color: Color(0xFFE4405F)),
                title: const Text('Share to Instagram'),
                onTap: () async {
                  Navigator.pop(context);
                  await _shareToInstagram();
                },
              ),
              ListTile(
                leading: const Icon(Icons.facebook, color: Color(0xFF1877F2)),
                title: const Text('Share to Facebook'),
                onTap: () async {
                  Navigator.pop(context);
                  await _shareToFacebook();
                },
              ),
              ListTile(
                leading: Icon(Icons.share, color: AppColors.accent),
                title: const Text('More Options...'),
                onTap: () async {
                  Navigator.pop(context);
                  await _nativeShare();
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  /// Save current image to gallery (or all selected in selection mode)
  Future<void> _saveToGallery() async {
    if (_imagePaths.isEmpty) return;
    
    final arMain = ARMain();
    
    // Get paths to save
    final List<String> pathsToSave;
    if (_isSelectionMode && _selectedPaths.isNotEmpty) {
      pathsToSave = _selectedPaths.toList();
    } else {
      pathsToSave = [_imagePaths[_currentIndex]];
    }
    
    int successCount = 0;
    for (final path in pathsToSave) {
      final result = await arMain.saveToGallery(path);
      if (result != null) {
        successCount++;
      }
    }
    
    // Clear selection after save
    if (_isSelectionMode) {
      setState(() {
        _selectedPaths.clear();
        _isSelectionMode = false;
      });
    }
    
    if (mounted) {
      if (successCount > 0) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('✓ Saved $successCount image${successCount > 1 ? 's' : ''} to Gallery'),
            backgroundColor: Colors.green,
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Failed to save to gallery'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
  
  /// Share image(s) to Instagram
  Future<void> _shareToInstagram() async {
    if (_imagePaths.isEmpty) return;
    
    // Get paths to share
    final List<String> pathsToShare;
    if (_isSelectionMode && _selectedPaths.isNotEmpty) {
      pathsToShare = _selectedPaths.toList();
    } else {
      pathsToShare = [_imagePaths[_currentIndex]];
    }
    
    final arMain = ARMain();
    
    try {
      // Instagram only supports single image, use first one
      await arMain.shareToInstagram(pathsToShare.first);
      
      // Clear selection after share
      if (_isSelectionMode) {
        setState(() {
          _selectedPaths.clear();
          _isSelectionMode = false;
        });
      }
    } catch (e) {
      if (mounted) {
        final message = e.toString().contains('APP_NOT_FOUND')
            ? 'Instagram is not installed'
            : 'Failed to share to Instagram';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message), backgroundColor: Colors.red),
        );
      }
    }
  }
  
  /// Share image(s) to Facebook
  Future<void> _shareToFacebook() async {
    if (_imagePaths.isEmpty) return;
    
    // Get paths to share
    final List<String> pathsToShare;
    if (_isSelectionMode && _selectedPaths.isNotEmpty) {
      pathsToShare = _selectedPaths.toList();
    } else {
      pathsToShare = [_imagePaths[_currentIndex]];
    }
    
    final arMain = ARMain();
    
    try {
      // Facebook only supports single image, use first one
      await arMain.shareToFacebook(pathsToShare.first);
      
      // Clear selection after share
      if (_isSelectionMode) {
        setState(() {
          _selectedPaths.clear();
          _isSelectionMode = false;
        });
      }
    } catch (e) {
      if (mounted) {
        final message = e.toString().contains('APP_NOT_FOUND')
            ? 'Facebook is not installed'
            : 'Failed to share to Facebook';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message), backgroundColor: Colors.red),
        );
      }
    }
  }
  
  /// Share image(s) using native share dialog
  Future<void> _nativeShare() async {
    if (_imagePaths.isEmpty) return;
    
    // Get paths to share
    final List<String> pathsToShare;
    if (_isSelectionMode && _selectedPaths.isNotEmpty) {
      pathsToShare = _selectedPaths.toList();
    } else {
      pathsToShare = [_imagePaths[_currentIndex]];
    }
    
    final arMain = ARMain();
    
    try {
      // Share all selected images
      for (final path in pathsToShare) {
        await arMain.nativeShare(path);
      }
      
      // Clear selection after share
      if (_isSelectionMode) {
        setState(() {
          _selectedPaths.clear();
          _isSelectionMode = false;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Failed to share'), backgroundColor: Colors.red),
        );
      }
    }
  }
  
  /// Handle back navigation:
  /// - If in selection mode, exit selection mode first
  /// - If in carousel view with filter mode, exit filter mode first
  /// - If in carousel view, go back to grid view
  /// - If in grid view with unsaved snaps, show warning dialog
  /// - If in grid view, return to AR session
  void _handleBackNavigation() {
    if (_isSelectionMode) {
      // Exit selection mode first
      setState(() {
        _isSelectionMode = false;
        _selectedPaths.clear();
      });
    } else if (!_isGridView) {
      // In carousel view
      if (_isFilterMode) {
        // Exit filter mode first
        setState(() {
          _isFilterMode = false;
          _previewingFilter = null;
        });
      } else {
        // Go back to grid view
        setState(() {
          _isGridView = true;
        });
      }
    } else {
      // In grid view - check if there are unsaved snaps
      if (_imagePaths.isNotEmpty) {
        _showExitWarningDialog();
      } else {
        _returnToAR();
      }
    }
  }
  
  /// Show warning dialog when exiting with unsaved snaps
  Future<void> _showExitWarningDialog() async {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: isDark ? AppColors.darkCard : AppColors.lightCard,
        title: Text(
          'Unsaved Snaps',
          style: TextStyle(
            color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
          ),
        ),
        content: Text(
          'You have ${_imagePaths.length} unsaved snap${_imagePaths.length > 1 ? 's' : ''}. What would you like to do?',
          style: TextStyle(
            color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, 'cancel'),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, 'discard'),
            style: TextButton.styleFrom(foregroundColor: AppColors.destructive),
            child: const Text('Discard All'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, 'keep'),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary),
            child: const Text('Keep & Exit'),
          ),
        ],
      ),
    );
    
    if (result == 'discard') {
      // Delete all snaps and exit
      final arMain = ARMain();
      await arMain.clearAllSnaps();
      setState(() {
        _imagePaths.clear();
        _appliedFilters.clear();
        _selectedPaths.clear();
      });
      _returnToAR();
    } else if (result == 'keep') {
      // Keep snaps and return to AR
      _returnToAR();
    }
    // 'cancel' or null - do nothing, stay in Image Manager
  }
  
  /// Perform undo operation
  Future<void> _performUndo() async {
    final action = _historyManager.popForUndo();
    if (action == null) return;
    
    switch (action.type) {
      case ImageActionType.delete:
        // Undo delete: restore the image (note: file must still exist on disk)
        if (action.deletedPath != null && action.deletedIndex != null) {
          // Check if file still exists
          if (File(action.deletedPath!).existsSync()) {
            setState(() {
              _imagePaths.insert(action.deletedIndex!, action.deletedPath!);
              _appliedFilters[action.deletedPath!] = ImageFilter.none;
              _currentIndex = action.deletedIndex!;
            });
            
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Delete undone')),
              );
            }
          } else {
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Cannot undo: file no longer exists')),
              );
            }
          }
        }
        break;
        
      case ImageActionType.filterApply:
      case ImageActionType.filterRemove:
        // Undo filter: restore the previous filter
        if (action.imagePath != null && action.previousFilter != null) {
          final arMain = ARMain();
          await arMain.setFilter(
            action.imagePath!, 
            action.previousFilter!.id == 'none' ? null : action.previousFilter!.id,
          );
          
          setState(() {
            _appliedFilters[action.imagePath!] = action.previousFilter!;
          });
          
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('Filter ${action.type == ImageActionType.filterApply ? 'apply' : 'remove'} undone')),
            );
          }
        }
        break;
        
      case ImageActionType.reorder:
        // Undo reorder: swap back
        if (action.fromIndex != null && action.toIndex != null) {
          setState(() {
            final path = _imagePaths.removeAt(action.toIndex!);
            _imagePaths.insert(action.fromIndex!, path);
          });
          
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Reorder undone')),
            );
          }
        }
        break;
    }
    
    widget.onImagesChanged?.call();
  }
  
  /// Perform redo operation
  Future<void> _performRedo() async {
    final action = _historyManager.popForRedo();
    if (action == null) return;
    
    switch (action.type) {
      case ImageActionType.delete:
        // Redo delete: delete again
        if (action.deletedPath != null && action.deletedIndex != null) {
          final idx = _imagePaths.indexOf(action.deletedPath!);
          if (idx >= 0) {
            final arMain = ARMain();
            await arMain.deleteSnapByPath(action.deletedPath!);
            
            setState(() {
              _imagePaths.removeAt(idx);
              _appliedFilters.remove(action.deletedPath);
              if (_currentIndex >= _imagePaths.length) {
                _currentIndex = _imagePaths.isEmpty ? 0 : _imagePaths.length - 1;
              }
            });
            
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Delete redone')),
              );
            }
          }
        }
        break;
        
      case ImageActionType.filterApply:
      case ImageActionType.filterRemove:
        // Redo filter: apply the new filter
        if (action.imagePath != null && action.newFilter != null) {
          final arMain = ARMain();
          await arMain.setFilter(
            action.imagePath!, 
            action.newFilter!.id == 'none' ? null : action.newFilter!.id,
          );
          
          setState(() {
            _appliedFilters[action.imagePath!] = action.newFilter!;
          });
          
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('Filter ${action.type == ImageActionType.filterApply ? 'apply' : 'remove'} redone')),
            );
          }
        }
        break;
        
      case ImageActionType.reorder:
        // Redo reorder: swap again
        if (action.fromIndex != null && action.toIndex != null) {
          setState(() {
            final path = _imagePaths.removeAt(action.fromIndex!);
            _imagePaths.insert(action.toIndex!, path);
          });
          
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Reorder redone')),
            );
          }
        }
        break;
    }
    
    widget.onImagesChanged?.call();
  }
  
  /// Return to AR session (if it's still alive in background)
  Future<void> _returnToAR() async {
    final arMain = ARMain();
    final success = await arMain.returnToAR();
    
    if (success) {
      // AR session is alive, we're returning to it
      if (mounted) {
        Navigator.of(context).pop();
      }
    } else {
      // AR session was destroyed, show message
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('AR session ended. Please restart AR from main screen.')),
        );
        Navigator.of(context).pop();
      }
    }
  }
}
