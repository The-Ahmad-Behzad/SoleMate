import 'package:flutter/material.dart';

/// Design tokens extracted from DESIGN_SPECIFICATIONS.md
/// Provides centralized access to colors, typography, spacing, and shadows
class AppColors {
  // Private constructor to prevent instantiation
  AppColors._();

  // Light Mode Colors
  static const Color lightBackground = Color(0xFFFAFAFA); // HSL(0, 0%, 98%)
  static const Color lightForeground = Color(0xFF403121); // HSL(30, 35%, 25%)
  static const Color lightCard = Color(0xFFFFFFFF); // HSL(0, 0%, 100%)
  static const Color lightCardForeground = Color(0xFF403121); // HSL(30, 35%, 25%)
  static const Color lightPopover = Color(0xFFFFFFFF); // HSL(0, 0%, 100%)
  static const Color lightPopoverForeground = Color(0xFF403121); // HSL(30, 35%, 25%)

  // Brand Colors (same for both themes)
  static const Color primary = Color(0xFF7A9F6B); // HSL(85, 25%, 45%) - Olive Green
  static const Color primaryForeground = Color(0xFFFAFAFA); // HSL(0, 0%, 98%)
  static const Color secondary = Color(0xFFB8895F); // HSL(30, 40%, 50%) - Warm Brown
  static const Color secondaryForeground = Color(0xFFFAFAFA); // HSL(0, 0%, 98%)
  static const Color accent = Color(0xFF5D7F4E); // HSL(85, 30%, 40%) - Dark Olive
  static const Color accentForeground = Color(0xFFFAFAFA); // HSL(0, 0%, 98%)

  // Neutral Colors (Light)
  static const Color lightMuted = Color(0xFFEBE5DE); // HSL(30, 20%, 90%)
  static const Color lightMutedForeground = Color(0xFF8A7A66); // HSL(30, 20%, 45%)

  // UI Utilities (Light)
  static const Color lightBorder = Color(0xFFDDD5CC); // HSL(30, 15%, 85%)
  static const Color lightInput = Color(0xFFDDD5CC); // HSL(30, 15%, 85%)
  static const Color lightRing = Color(0xFF7A9F6B); // HSL(85, 25%, 45%) - same as primary

  // Semantic Colors
  static const Color destructive = Color(0xFFE53935); // HSL(0, 84.2%, 60.2%)
  static const Color destructiveForeground = Color(0xFFFAFAFA); // HSL(0, 0%, 98%)

  // Dark Mode Colors
  static const Color darkBackground = Color(0xFF1F1914); // HSL(30, 20%, 12%)
  static const Color darkForeground = Color(0xFFF2F2F2); // HSL(0, 0%, 95%)
  static const Color darkCard = Color(0xFF2D2419); // HSL(30, 25%, 18%)
  static const Color darkCardForeground = Color(0xFFF2F2F2); // HSL(0, 0%, 95%)
  static const Color darkPopover = Color(0xFF2D2419); // HSL(30, 25%, 18%)
  static const Color darkPopoverForeground = Color(0xFFF2F2F2); // HSL(0, 0%, 95%)

  // Neutral Colors (Dark)
  static const Color darkMuted = Color(0xFF3D3329); // HSL(30, 20%, 25%)
  static const Color darkMutedForeground = Color(0xFFA6A6A6); // HSL(0, 0%, 65%)

  // UI Utilities (Dark)
  static const Color darkBorder = Color(0xFF3D3329); // HSL(30, 20%, 25%)
  static const Color darkInput = Color(0xFF3D3329); // HSL(30, 20%, 25%)
  static const Color darkRing = Color(0xFF7A9F6B); // HSL(85, 25%, 45%) - same as primary

  // Dark Semantic Colors
  static const Color darkDestructive = Color(0xFF7D2927); // HSL(0, 62.8%, 30.6%)
  static const Color darkDestructiveForeground = Color(0xFFFAFAFA); // HSL(0, 0%, 98%)

  // Gradient Colors (with opacity)
  static const Color primary5 = Color(0x0D7A9F6B); // Primary at 5% opacity
  static const Color primary10 = Color(0x1A7A9F6B); // Primary at 10% opacity
  static const Color primary20 = Color(0x337A9F6B); // Primary at 20% opacity
  static const Color accent5 = Color(0x0D5D7F4E); // Accent at 5% opacity
  static const Color accent10 = Color(0x1A5D7F4E); // Accent at 10% opacity
  static const Color accent20 = Color(0x335D7F4E); // Accent at 20% opacity
  static const Color secondary20 = Color(0x33B8895F); // Secondary at 20% opacity
}

class AppTypography {
  // Private constructor to prevent instantiation
  AppTypography._();

  // Font sizes (matching Tailwind scale)
  static const double xs = 12.0; // 0.75rem
  static const double sm = 14.0; // 0.875rem
  static const double base = 16.0; // 1rem
  static const double lg = 18.0; // 1.125rem
  static const double xl = 20.0; // 1.25rem
  static const double xl2 = 24.0; // 1.5rem
  static const double xl3 = 30.0; // 1.875rem
  static const double xl4 = 36.0; // 2.25rem
  static const double xl5 = 48.0; // 3rem
  static const double xl6 = 60.0; // 3.75rem

  // Font weights
  static const FontWeight normal = FontWeight.w400;
  static const FontWeight medium = FontWeight.w500;
  static const FontWeight semibold = FontWeight.w600;
  static const FontWeight bold = FontWeight.w700;

  // Text styles for different component types
  static const TextStyle headline1 = TextStyle(
    fontSize: xl5,
    fontWeight: bold,
    height: 1.2,
  );

  static const TextStyle headline2 = TextStyle(
    fontSize: xl4,
    fontWeight: bold,
    height: 1.2,
  );

  static const TextStyle headline3 = TextStyle(
    fontSize: xl3,
    fontWeight: bold,
    height: 1.3,
  );

  static const TextStyle headline4 = TextStyle(
    fontSize: xl2,
    fontWeight: semibold,
    height: 1.3,
  );

  static const TextStyle bodyLarge = TextStyle(
    fontSize: lg,
    fontWeight: normal,
    height: 1.5,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontSize: base,
    fontWeight: normal,
    height: 1.5,
  );

  static const TextStyle bodySmall = TextStyle(
    fontSize: sm,
    fontWeight: normal,
    height: 1.5,
  );

  static const TextStyle buttonLarge = TextStyle(
    fontSize: base,
    fontWeight: medium,
    height: 1.2,
  );

  static const TextStyle buttonMedium = TextStyle(
    fontSize: sm,
    fontWeight: medium,
    height: 1.2,
  );

  static const TextStyle caption = TextStyle(
    fontSize: xs,
    fontWeight: normal,
    height: 1.4,
  );
}

class AppSpacing {
  // Private constructor to prevent instantiation
  AppSpacing._();

  // Spacing scale (8px base unit)
  static const double xs = 4.0; // 0.25rem
  static const double sm = 8.0; // 0.5rem
  static const double md = 12.0; // 0.75rem
  static const double lg = 16.0; // 1rem
  static const double xl = 20.0; // 1.25rem
  static const double xl2 = 24.0; // 1.5rem
  static const double xl3 = 32.0; // 2rem
  static const double xl4 = 48.0; // 3rem
  static const double xl5 = 64.0; // 4rem
  static const double xl6 = 80.0; // 5rem

  // Common spacing patterns
  static const EdgeInsets paddingSmall = EdgeInsets.all(sm);
  static const EdgeInsets paddingMedium = EdgeInsets.all(lg);
  static const EdgeInsets paddingLarge = EdgeInsets.all(xl2);
  static const EdgeInsets paddingXLarge = EdgeInsets.all(xl3);

  static const EdgeInsets paddingHorizontalSmall = EdgeInsets.symmetric(horizontal: sm);
  static const EdgeInsets paddingHorizontalMedium = EdgeInsets.symmetric(horizontal: lg);
  static const EdgeInsets paddingHorizontalLarge = EdgeInsets.symmetric(horizontal: xl2);

  static const EdgeInsets paddingVerticalSmall = EdgeInsets.symmetric(vertical: sm);
  static const EdgeInsets paddingVerticalMedium = EdgeInsets.symmetric(vertical: lg);
  static const EdgeInsets paddingVerticalLarge = EdgeInsets.symmetric(vertical: xl2);
}

class AppRadius {
  // Private constructor to prevent instantiation
  AppRadius._();

  // Border radius scale
  static const double sm = 8.0; // --radius - 4px
  static const double md = 10.0; // --radius - 2px
  static const double lg = 12.0; // --radius (default)
  static const double full = 9999.0; // Circular

  // Common border radius patterns
  static const BorderRadius radiusSmall = BorderRadius.all(Radius.circular(sm));
  static const BorderRadius radiusMedium = BorderRadius.all(Radius.circular(md));
  static const BorderRadius radiusLarge = BorderRadius.all(Radius.circular(lg));
  static const BorderRadius radiusFull = BorderRadius.all(Radius.circular(full));
}

class AppShadows {
  // Private constructor to prevent instantiation
  AppShadows._();

  // Elegant shadow (card resting state)
  static const BoxShadow elegant = BoxShadow(
    color: Color(0x26403121), // HSL(30, 35%, 25%) at 15% opacity
    blurRadius: 40.0,
    offset: Offset(0, 10),
    spreadRadius: -10,
  );

  // Elegant shadow (dark mode)
  static const BoxShadow elegantDark = BoxShadow(
    color: Color(0x80000000), // Black at 50% opacity
    blurRadius: 40.0,
    offset: Offset(0, 10),
    spreadRadius: -10,
  );

  // Glow shadow (interactive hover state)
  static const BoxShadow glow = BoxShadow(
    color: Color(0x4D7A9F6B), // HSL(85, 25%, 45%) at 30% opacity
    blurRadius: 30.0,
    offset: Offset(0, 0),
  );

  // Glow shadow (dark mode)
  static const BoxShadow glowDark = BoxShadow(
    color: Color(0x667A9F6B), // HSL(85, 25%, 45%) at 40% opacity
    blurRadius: 30.0,
    offset: Offset(0, 0),
  );

  // Shadow lists for easy application
  static const List<BoxShadow> cardShadows = [elegant];
  static const List<BoxShadow> cardShadowsDark = [elegantDark];
  static const List<BoxShadow> glowShadows = [glow];
  static const List<BoxShadow> glowShadowsDark = [glowDark];
}

class AppGradients {
  // Private constructor to prevent instantiation
  AppGradients._();

  // Hero gradient (light mode)
  static const LinearGradient heroLight = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF7A9F6B), // HSL(85, 25%, 45%)
      Color(0xFF5A7A4D), // HSL(85, 30%, 35%)
    ],
  );

  // Hero gradient (dark mode)
  static const LinearGradient heroDark = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF5A7A4D), // HSL(85, 25%, 35%)
      Color(0xFF3D5A32), // HSL(85, 30%, 25%)
    ],
  );

  // Card gradient (light mode)
  static const LinearGradient cardLight = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [
      Color(0xFFFFFFFF), // HSL(0, 0%, 100%)
      Color(0xFFFAFAFA), // HSL(0, 0%, 98%)
    ],
  );

  // Card gradient (dark mode)
  static const LinearGradient cardDark = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [
      Color(0xFF2D2419), // HSL(30, 25%, 18%)
      Color(0xFF262018), // HSL(30, 20%, 15%)
    ],
  );

  // Element gradients (for placeholders)
  static const LinearGradient elementGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      AppColors.primary10,
      AppColors.accent10,
    ],
  );

  // Overlay gradients
  static const LinearGradient overlayGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      AppColors.primary5,
      AppColors.accent5,
    ],
  );
}

class AppBreakpoints {
  // Private constructor to prevent instantiation
  AppBreakpoints._();

  // Breakpoints matching Tailwind CSS
  static const double sm = 640.0; // Small tablets
  static const double md = 768.0; // Tablets
  static const double lg = 1024.0; // Small laptops
  static const double xl = 1280.0; // Desktops
  static const double xl2 = 1536.0; // Large screens

  // Helper methods for responsive design
  static bool isMobile(BuildContext context) {
    return MediaQuery.of(context).size.width < md;
  }

  static bool isTablet(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    return width >= md && width < lg;
  }

  static bool isDesktop(BuildContext context) {
    return MediaQuery.of(context).size.width >= lg;
  }

  // Get responsive column count for grids
  static int getColumnCount(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    if (width < md) return 1;
    if (width < lg) return 2;
    return 4;
  }

  // Get responsive spacing
  static double getResponsiveSpacing(BuildContext context, {
    required double mobile,
    required double tablet,
    required double desktop,
  }) {
    final width = MediaQuery.of(context).size.width;
    if (width < md) return mobile;
    if (width < lg) return tablet;
    return desktop;
  }
}
