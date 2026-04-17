import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import 'voice_mic_button.dart';

/// Bottom navigation items for the main app
enum AppTab {
  arTryOn,
  closet,
  outfitMatch,
  customize,
}

/// App scaffold with bottom navigation bar
class AppScaffold extends StatefulWidget {
  const AppScaffold({
    super.key,
    required this.children,
    this.currentIndex = 0,
    this.onTabChanged,
  });

  final List<Widget> children;
  final int currentIndex;
  final Function(int)? onTabChanged;

  @override
  State<AppScaffold> createState() => _AppScaffoldState();
}

class _AppScaffoldState extends State<AppScaffold> {
  late int _currentIndex;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.currentIndex;
  }

  @override
  void didUpdateWidget(AppScaffold oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.currentIndex != oldWidget.currentIndex) {
      _currentIndex = widget.currentIndex;
    }
  }

  void _onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
    widget.onTabChanged?.call(index);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: widget.children,
      ),
      floatingActionButton: const VoiceMicButton(),
      bottomNavigationBar: Container(
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
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.sm,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildNavItem(
                  context: context,
                  index: 0,
                  icon: Icons.camera_alt,
                  label: 'AR Try-On',
                ),
                _buildNavItem(
                  context: context,
                  index: 1,
                  icon: Icons.shopping_bag,
                  label: 'My Closet',
                ),
                _buildNavItem(
                  context: context,
                  index: 2,
                  icon: Icons.auto_awesome,
                  label: 'Outfit Match',
                ),
                _buildNavItem(
                  context: context,
                  index: 3,
                  icon: Icons.palette,
                  label: 'Customize',
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem({
    required BuildContext context,
    required int index,
    required IconData icon,
    required String label,
  }) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final isSelected = _currentIndex == index;

    return GestureDetector(
      onTap: () => _onTabTapped(index),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.sm,
        ),
        decoration: BoxDecoration(
          color: isSelected 
              ? AppColors.primary.withOpacity(0.1)
              : Colors.transparent,
          borderRadius: AppRadius.radiusLarge,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              color: isSelected 
                  ? AppColors.primary
                  : (isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground),
              size: 24,
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              label,
              style: AppTypography.caption.copyWith(
                color: isSelected 
                    ? AppColors.primary
                    : (isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground),
                fontWeight: isSelected ? AppTypography.medium : AppTypography.normal,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

