import 'package:flutter/material.dart';
import 'theme_config.dart';

/// Custom theme extension for additional properties not covered by ThemeData
@immutable
class AppThemeExtension extends ThemeExtension<AppThemeExtension> {
  const AppThemeExtension({
    required this.heroGradient,
    required this.cardGradient,
    required this.elementGradient,
    required this.overlayGradient,
    required this.cardShadows,
    required this.glowShadows,
  });

  final LinearGradient heroGradient;
  final LinearGradient cardGradient;
  final LinearGradient elementGradient;
  final LinearGradient overlayGradient;
  final List<BoxShadow> cardShadows;
  final List<BoxShadow> glowShadows;

  @override
  AppThemeExtension copyWith({
    LinearGradient? heroGradient,
    LinearGradient? cardGradient,
    LinearGradient? elementGradient,
    LinearGradient? overlayGradient,
    List<BoxShadow>? cardShadows,
    List<BoxShadow>? glowShadows,
  }) {
    return AppThemeExtension(
      heroGradient: heroGradient ?? this.heroGradient,
      cardGradient: cardGradient ?? this.cardGradient,
      elementGradient: elementGradient ?? this.elementGradient,
      overlayGradient: overlayGradient ?? this.overlayGradient,
      cardShadows: cardShadows ?? this.cardShadows,
      glowShadows: glowShadows ?? this.glowShadows,
    );
  }

  @override
  AppThemeExtension lerp(ThemeExtension<AppThemeExtension>? other, double t) {
    if (other is! AppThemeExtension) {
      return this;
    }
    return AppThemeExtension(
      heroGradient: LinearGradient.lerp(heroGradient, other.heroGradient, t)!,
      cardGradient: LinearGradient.lerp(cardGradient, other.cardGradient, t)!,
      elementGradient: LinearGradient.lerp(elementGradient, other.elementGradient, t)!,
      overlayGradient: LinearGradient.lerp(overlayGradient, other.overlayGradient, t)!,
      cardShadows: cardShadows,
      glowShadows: glowShadows,
    );
  }
}

/// Global theme configuration for SoleMate app
class AppTheme {
  // Private constructor to prevent instantiation
  AppTheme._();

  /// Light theme configuration
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      
      // Color scheme
      colorScheme: const ColorScheme.light(
        primary: AppColors.primary,
        onPrimary: AppColors.primaryForeground,
        secondary: AppColors.secondary,
        onSecondary: AppColors.secondaryForeground,
        tertiary: AppColors.accent,
        onTertiary: AppColors.accentForeground,
        surface: AppColors.lightCard,
        onSurface: AppColors.lightCardForeground,
        background: AppColors.lightBackground,
        onBackground: AppColors.lightForeground,
        error: AppColors.destructive,
        onError: AppColors.destructiveForeground,
        outline: AppColors.lightBorder,
        outlineVariant: AppColors.lightMuted,
        surfaceContainerHighest: AppColors.lightMuted,
        onSurfaceVariant: AppColors.lightMutedForeground,
      ),

      // Typography
      textTheme: const TextTheme(
        displayLarge: AppTypography.headline1,
        displayMedium: AppTypography.headline2,
        displaySmall: AppTypography.headline3,
        headlineLarge: AppTypography.headline3,
        headlineMedium: AppTypography.headline4,
        headlineSmall: AppTypography.headline4,
        titleLarge: AppTypography.headline4,
        titleMedium: AppTypography.bodyLarge,
        titleSmall: AppTypography.bodyMedium,
        bodyLarge: AppTypography.bodyLarge,
        bodyMedium: AppTypography.bodyMedium,
        bodySmall: AppTypography.bodySmall,
        labelLarge: AppTypography.buttonLarge,
        labelMedium: AppTypography.buttonMedium,
        labelSmall: AppTypography.caption,
      ),

      // App bar theme
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.primaryForeground,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          fontSize: AppTypography.xl2,
          fontWeight: AppTypography.semibold,
          color: AppColors.primaryForeground,
        ),
        surfaceTintColor: Colors.transparent,
      ),

      // Card theme
      cardTheme: CardThemeData(
        color: AppColors.lightCard,
        elevation: 0,
        shadowColor: AppColors.lightForeground.withOpacity(0.15),
        shape: RoundedRectangleBorder(
          borderRadius: AppRadius.radiusLarge,
        ),
        margin: EdgeInsets.zero,
      ),

      // Elevated button theme
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.secondary,
          foregroundColor: AppColors.secondaryForeground,
          elevation: 0,
          shadowColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: AppRadius.radiusLarge,
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.xl2,
            vertical: AppSpacing.lg,
          ),
          textStyle: AppTypography.buttonMedium,
        ),
      ),

      // Outlined button theme
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.lightForeground,
          side: const BorderSide(color: AppColors.lightBorder),
          shape: RoundedRectangleBorder(
            borderRadius: AppRadius.radiusLarge,
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.xl2,
            vertical: AppSpacing.lg,
          ),
          textStyle: AppTypography.buttonMedium,
        ),
      ),

      // Text button theme
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.primary,
          shape: RoundedRectangleBorder(
            borderRadius: AppRadius.radiusLarge,
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          textStyle: AppTypography.buttonMedium,
        ),
      ),

      // Input decoration theme
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.lightCard,
        border: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.lightBorder),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.lightBorder),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.primary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.destructive),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.destructive, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.lg,
        ),
        labelStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.lightMutedForeground,
        ),
        hintStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.lightMutedForeground,
        ),
      ),

      // Bottom navigation bar theme
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: AppColors.lightCard,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.lightMutedForeground,
        type: BottomNavigationBarType.fixed,
        elevation: 8,
      ),

      // Floating action button theme
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: AppColors.secondary,
        foregroundColor: AppColors.secondaryForeground,
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: AppRadius.radiusLarge,
        ),
      ),

      // Divider theme
      dividerTheme: const DividerThemeData(
        color: AppColors.lightBorder,
        thickness: 1,
        space: 1,
      ),

      // Custom theme extension
      extensions: const [
        AppThemeExtension(
          heroGradient: AppGradients.heroLight,
          cardGradient: AppGradients.cardLight,
          elementGradient: AppGradients.elementGradient,
          overlayGradient: AppGradients.overlayGradient,
          cardShadows: AppShadows.cardShadows,
          glowShadows: AppShadows.glowShadows,
        ),
      ],
    );
  }

  /// Dark theme configuration
  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      
      // Color scheme
      colorScheme: const ColorScheme.dark(
        primary: AppColors.primary,
        onPrimary: AppColors.primaryForeground,
        secondary: AppColors.secondary,
        onSecondary: AppColors.secondaryForeground,
        tertiary: AppColors.accent,
        onTertiary: AppColors.accentForeground,
        surface: AppColors.darkCard,
        onSurface: AppColors.darkCardForeground,
        background: AppColors.darkBackground,
        onBackground: AppColors.darkForeground,
        error: AppColors.darkDestructive,
        onError: AppColors.darkDestructiveForeground,
        outline: AppColors.darkBorder,
        outlineVariant: AppColors.darkMuted,
        surfaceContainerHighest: AppColors.darkMuted,
        onSurfaceVariant: AppColors.darkMutedForeground,
      ),

      // Typography (same as light theme)
      textTheme: const TextTheme(
        displayLarge: AppTypography.headline1,
        displayMedium: AppTypography.headline2,
        displaySmall: AppTypography.headline3,
        headlineLarge: AppTypography.headline3,
        headlineMedium: AppTypography.headline4,
        headlineSmall: AppTypography.headline4,
        titleLarge: AppTypography.headline4,
        titleMedium: AppTypography.bodyLarge,
        titleSmall: AppTypography.bodyMedium,
        bodyLarge: AppTypography.bodyLarge,
        bodyMedium: AppTypography.bodyMedium,
        bodySmall: AppTypography.bodySmall,
        labelLarge: AppTypography.buttonLarge,
        labelMedium: AppTypography.buttonMedium,
        labelSmall: AppTypography.caption,
      ),

      // App bar theme
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.primaryForeground,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          fontSize: AppTypography.xl2,
          fontWeight: AppTypography.semibold,
          color: AppColors.primaryForeground,
        ),
        surfaceTintColor: Colors.transparent,
      ),

      // Card theme
      cardTheme: CardThemeData(
        color: AppColors.darkCard,
        elevation: 0,
        shadowColor: Colors.black.withOpacity(0.5),
        shape: RoundedRectangleBorder(
          borderRadius: AppRadius.radiusLarge,
        ),
        margin: EdgeInsets.zero,
      ),

      // Elevated button theme
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.secondary,
          foregroundColor: AppColors.secondaryForeground,
          elevation: 0,
          shadowColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: AppRadius.radiusLarge,
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.xl2,
            vertical: AppSpacing.lg,
          ),
          textStyle: AppTypography.buttonMedium,
        ),
      ),

      // Outlined button theme
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.darkForeground,
          side: const BorderSide(color: AppColors.darkBorder),
          shape: RoundedRectangleBorder(
            borderRadius: AppRadius.radiusLarge,
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.xl2,
            vertical: AppSpacing.lg,
          ),
          textStyle: AppTypography.buttonMedium,
        ),
      ),

      // Text button theme
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.primary,
          shape: RoundedRectangleBorder(
            borderRadius: AppRadius.radiusLarge,
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          textStyle: AppTypography.buttonMedium,
        ),
      ),

      // Input decoration theme
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.darkCard,
        border: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.darkBorder),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.darkBorder),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.primary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.darkDestructive),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: AppRadius.radiusLarge,
          borderSide: const BorderSide(color: AppColors.darkDestructive, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.lg,
        ),
        labelStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.darkMutedForeground,
        ),
        hintStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.darkMutedForeground,
        ),
      ),

      // Bottom navigation bar theme
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: AppColors.darkCard,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.darkMutedForeground,
        type: BottomNavigationBarType.fixed,
        elevation: 8,
      ),

      // Floating action button theme
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: AppColors.secondary,
        foregroundColor: AppColors.secondaryForeground,
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: AppRadius.radiusLarge,
        ),
      ),

      // Divider theme
      dividerTheme: const DividerThemeData(
        color: AppColors.darkBorder,
        thickness: 1,
        space: 1,
      ),

      // Custom theme extension
      extensions: const [
        AppThemeExtension(
          heroGradient: AppGradients.heroDark,
          cardGradient: AppGradients.cardDark,
          elementGradient: AppGradients.elementGradient,
          overlayGradient: AppGradients.overlayGradient,
          cardShadows: AppShadows.cardShadowsDark,
          glowShadows: AppShadows.glowShadowsDark,
        ),
      ],
    );
  }

  /// Helper method to get custom theme extension
  static AppThemeExtension of(BuildContext context) {
    return Theme.of(context).extension<AppThemeExtension>()!;
  }
}
