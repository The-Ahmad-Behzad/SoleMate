import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import 'custom_button.dart';

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

/// Main app shell that wraps the bottom navigation with feature screens
class MainAppShell extends StatefulWidget {
  const MainAppShell({super.key});

  @override
  State<MainAppShell> createState() => _MainAppShellState();
}

class _MainAppShellState extends State<MainAppShell> {
  int _currentIndex = 0;
  late List<Widget> _screens;

  @override
  void initState() {
    super.initState();
    _initializeScreens();
  }

  void _initializeScreens() {
    // Import screens here to avoid circular dependencies
    _screens = [
      const PlaceholderScreen(title: 'AR Try-On', icon: Icons.camera_alt),
      const PlaceholderScreen(title: 'My Closet', icon: Icons.shopping_bag),
      const PlaceholderScreen(title: 'Outfit Match', icon: Icons.auto_awesome),
      const PlaceholderScreen(title: 'Customize', icon: Icons.palette),
    ];
  }

  void _onTabChanged(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return AppScaffold(
      children: _screens,
      currentIndex: _currentIndex,
      onTabChanged: _onTabChanged,
    );
  }
}

/// Placeholder screen for features not yet implemented
class PlaceholderScreen extends StatelessWidget {
  const PlaceholderScreen({
    super.key,
    required this.title,
    required this.icon,
  });

  final String title;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Logout',
            onPressed: () {
              // TODO: Implement logout functionality
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Logout functionality coming soon')),
              );
            },
          ),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                color: AppColors.accent10,
                borderRadius: AppRadius.radiusFull,
              ),
              child: Icon(
                icon,
                size: 60,
                color: AppColors.accent,
              ),
            ),
            const SizedBox(height: AppSpacing.xl2),
            Text(
              title,
              style: AppTypography.headline3.copyWith(
                color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              'This feature is coming soon!',
              style: AppTypography.bodyMedium.copyWith(
                color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
              ),
            ),
            const SizedBox(height: AppSpacing.xl3),
            CustomButton(
              text: 'Go Back',
              onPressed: () => Navigator.of(context).pop(),
              variant: CustomButtonVariant.outline,
            ),
          ],
        ),
      ),
    );
  }
}
