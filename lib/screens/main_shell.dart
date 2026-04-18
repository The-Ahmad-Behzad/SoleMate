import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../widgets/app_scaffold.dart';
import '../widgets/custom_button.dart';
import '../services/auth_service.dart';
import '../services/user_service.dart';
import '../services/navigation_service.dart';
import 'package:provider/provider.dart';
import 'auth/login_screen.dart';
import 'ar_tryon_screen.dart';
import 'closet_screen.dart';
import 'outfit_match_screen.dart';
import 'customize_screen.dart';

/// Main app shell that wraps the bottom navigation with feature screens
class MainAppShell extends StatefulWidget {
  final int initialIndex;
  const MainAppShell({super.key, this.initialIndex = 0});

  @override
  State<MainAppShell> createState() => _MainAppShellState();
}

class _MainAppShellState extends State<MainAppShell> {
  late List<Widget> _screens;
  final AuthService _authService = AuthService();
  final UserService _userService = UserService();
  bool _isSyncing = true;

  @override
  void initState() {
    super.initState();
    _initializeScreens();
    _syncUser();
    
    // Set initial tab if provided
    if (widget.initialIndex != 0) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          final navService = Provider.of<NavigationService>(context, listen: false);
          navService.setIndex(widget.initialIndex);
        }
      });
    }
  }

  Future<void> _syncUser() async {
    try {
      final user = _authService.currentUser;
      if (user != null) {
        await _userService.syncProfile(user.displayName ?? 'SoleMate User');
      }
    } catch (e) {
      debugPrint('Failed to sync user: $e');
    } finally {
      if (mounted) {
        setState(() {
          _isSyncing = false;
        });
      }
    }
  }

  void _initializeScreens() {
    _screens = const [
      ARTryOnScreen(),
      ClosetScreen(),
      OutfitMatchScreen(),
      CustomizeScreen(),
    ];
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

  @override
  Widget build(BuildContext context) {
    final navService = Provider.of<NavigationService>(context);

    if (_isSyncing) {
      final isDark = Theme.of(context).brightness == Brightness.dark;
      return Scaffold(
        body: Container(
          decoration: BoxDecoration(
            gradient: isDark ? AppGradients.heroDark : AppGradients.heroLight,
          ),
          child: const Center(
            child: CircularProgressIndicator(),
          ),
        ),
      );
    }

    return AppScaffold(
      children: _screens,
      currentIndex: navService.currentIndex,
      onTabChanged: (index) => navService.setIndex(index),
    );
  }
}

/// Placeholder screen for features not yet implemented
class PlaceholderScreen extends StatelessWidget {
  const PlaceholderScreen({
    super.key,
    required this.title,
    required this.icon,
    required this.description,
  });

  final String title;
  final IconData icon;
  final String description;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'logout') {
                _showLogoutDialog(context);
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
      body: Center(
        child: Padding(
          padding: AppSpacing.paddingLarge,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Feature Icon
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
              
              const SizedBox(height: AppSpacing.xl3),
              
              // Title
              Text(
                title,
                style: AppTypography.headline3.copyWith(
                  color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
                ),
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: AppSpacing.lg),
              
              // Description
              Text(
                description,
                style: AppTypography.bodyLarge.copyWith(
                  color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground,
                ),
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: AppSpacing.xl4),
              
              // Coming Soon Badge
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.lg,
                  vertical: AppSpacing.sm,
                ),
                decoration: BoxDecoration(
                  color: AppColors.accent10,
                  borderRadius: AppRadius.radiusLarge,
                  border: Border.all(color: AppColors.accent),
                ),
                child: Text(
                  'Coming Soon',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.accent,
                    fontWeight: AppTypography.semibold,
                  ),
                ),
              ),
              
              const SizedBox(height: AppSpacing.xl3),
              
              // Action Buttons
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  OutlineButton(
                    text: 'Back to Home',
                    onPressed: () => Navigator.of(context).pop(),
                    size: CustomButtonSize.medium,
                  ),
                  const SizedBox(width: AppSpacing.lg),
                  PrimaryButton(
                    text: 'Learn More',
                    onPressed: () => _showFeatureInfo(context),
                    size: CustomButtonSize.medium,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showLogoutDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Logout'),
          content: const Text('Are you sure you want to logout?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                // The logout will be handled by the parent widget
                if (context.mounted) {
                  Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(builder: (_) => const LoginScreen()),
                  );
                }
              },
              child: const Text('Logout'),
            ),
          ],
        );
      },
    );
  }

  void _showFeatureInfo(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('$title Feature'),
          content: Text(description),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Got it'),
            ),
          ],
        );
      },
    );
  }
}
