import 'package:flutter/material.dart';
import '../theme/theme_config.dart';

/// Custom button variants following the design system
class CustomButton extends StatelessWidget {
  const CustomButton({
    super.key,
    required this.text,
    this.onPressed,
    this.variant = CustomButtonVariant.primary,
    this.size = CustomButtonSize.medium,
    this.isLoading = false,
    this.icon,
    this.isFullWidth = false,
  });

  final String text;
  final VoidCallback? onPressed;
  final CustomButtonVariant variant;
  final CustomButtonSize size;
  final bool isLoading;
  final IconData? icon;
  final bool isFullWidth;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    
    // Determine button colors based on variant
    Color backgroundColor;
    Color foregroundColor;
    Color? borderColor;
    List<BoxShadow>? shadows;

    switch (variant) {
      case CustomButtonVariant.primary:
        backgroundColor = AppColors.secondary;
        foregroundColor = AppColors.secondaryForeground;
        shadows = AppShadows.glowShadows;
        break;
      case CustomButtonVariant.secondary:
        backgroundColor = AppColors.accent;
        foregroundColor = AppColors.accentForeground;
        break;
      case CustomButtonVariant.outline:
        backgroundColor = Colors.transparent;
        foregroundColor = isDark ? AppColors.darkForeground : AppColors.lightForeground;
        borderColor = isDark ? AppColors.darkBorder : AppColors.lightBorder;
        break;
      case CustomButtonVariant.destructive:
        backgroundColor = isDark ? AppColors.darkDestructive : AppColors.destructive;
        foregroundColor = AppColors.destructiveForeground;
        break;
    }

    // Determine button size
    EdgeInsets padding;
    TextStyle textStyle;
    BoxConstraints constraints;

    switch (size) {
      case CustomButtonSize.small:
        padding = const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.sm,
        );
        textStyle = AppTypography.buttonMedium;
        constraints = const BoxConstraints(minHeight: 36.0);
        break;
      case CustomButtonSize.medium:
        padding = const EdgeInsets.symmetric(
          horizontal: AppSpacing.xl2,
          vertical: AppSpacing.lg,
        );
        textStyle = AppTypography.buttonMedium;
        constraints = const BoxConstraints(minHeight: 40.0);
        break;
      case CustomButtonSize.large:
        padding = const EdgeInsets.symmetric(
          horizontal: AppSpacing.xl3,
          vertical: AppSpacing.xl,
        );
        textStyle = AppTypography.buttonLarge;
        constraints = const BoxConstraints(minHeight: 44.0);
        break;
    }

    Widget buttonChild = isLoading
        ? SizedBox(
            height: textStyle.fontSize! * 1.2,
            width: textStyle.fontSize! * 1.2,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              valueColor: AlwaysStoppedAnimation<Color>(foregroundColor),
            ),
          )
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (icon != null) ...[
                Icon(icon, size: textStyle.fontSize! * 1.2),
                const SizedBox(width: AppSpacing.sm),
              ],
              Flexible(
                child: Text(
                  text, 
                  style: textStyle.copyWith(color: foregroundColor),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                ),
              ),
            ],
          );

    return Container(
      width: isFullWidth ? double.infinity : null,
      constraints: constraints,
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: AppRadius.radiusLarge,
        border: borderColor != null
            ? Border.all(color: borderColor, width: 1)
            : null,
        boxShadow: shadows,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: isLoading ? null : onPressed,
          borderRadius: AppRadius.radiusLarge,
          child: Container(
            padding: padding,
            child: Center(child: buttonChild),
          ),
        ),
      ),
    );
  }
}

/// Button variants matching the design system
enum CustomButtonVariant {
  primary,    // Secondary color with glow shadow
  secondary,  // Accent color
  outline,    // Transparent with border
  destructive, // Error/destructive color
}

/// Button sizes matching the design system
enum CustomButtonSize {
  small,   // h-9 px-3
  medium,  // h-10 px-4 (default)
  large,   // h-11 px-8
}

/// Convenience constructors for common button patterns
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.text,
    this.onPressed,
    this.size = CustomButtonSize.medium,
    this.isLoading = false,
    this.icon,
    this.isFullWidth = false,
  });

  final String text;
  final VoidCallback? onPressed;
  final CustomButtonSize size;
  final bool isLoading;
  final IconData? icon;
  final bool isFullWidth;

  @override
  Widget build(BuildContext context) {
    return CustomButton(
      text: text,
      onPressed: onPressed,
      variant: CustomButtonVariant.primary,
      size: size,
      isLoading: isLoading,
      icon: icon,
      isFullWidth: isFullWidth,
    );
  }
}

class SecondaryButton extends StatelessWidget {
  const SecondaryButton({
    super.key,
    required this.text,
    this.onPressed,
    this.size = CustomButtonSize.medium,
    this.isLoading = false,
    this.icon,
    this.isFullWidth = false,
  });

  final String text;
  final VoidCallback? onPressed;
  final CustomButtonSize size;
  final bool isLoading;
  final IconData? icon;
  final bool isFullWidth;

  @override
  Widget build(BuildContext context) {
    return CustomButton(
      text: text,
      onPressed: onPressed,
      variant: CustomButtonVariant.secondary,
      size: size,
      isLoading: isLoading,
      icon: icon,
      isFullWidth: isFullWidth,
    );
  }
}

class OutlineButton extends StatelessWidget {
  const OutlineButton({
    super.key,
    required this.text,
    this.onPressed,
    this.size = CustomButtonSize.medium,
    this.isLoading = false,
    this.icon,
    this.isFullWidth = false,
  });

  final String text;
  final VoidCallback? onPressed;
  final CustomButtonSize size;
  final bool isLoading;
  final IconData? icon;
  final bool isFullWidth;

  @override
  Widget build(BuildContext context) {
    return CustomButton(
      text: text,
      onPressed: onPressed,
      variant: CustomButtonVariant.outline,
      size: size,
      isLoading: isLoading,
      icon: icon,
      isFullWidth: isFullWidth,
    );
  }
}

class DestructiveButton extends StatelessWidget {
  const DestructiveButton({
    super.key,
    required this.text,
    this.onPressed,
    this.size = CustomButtonSize.medium,
    this.isLoading = false,
    this.icon,
    this.isFullWidth = false,
  });

  final String text;
  final VoidCallback? onPressed;
  final CustomButtonSize size;
  final bool isLoading;
  final IconData? icon;
  final bool isFullWidth;

  @override
  Widget build(BuildContext context) {
    return CustomButton(
      text: text,
      onPressed: onPressed,
      variant: CustomButtonVariant.destructive,
      size: size,
      isLoading: isLoading,
      icon: icon,
      isFullWidth: isFullWidth,
    );
  }
}
