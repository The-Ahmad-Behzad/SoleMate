import 'dart:ui';

/// Model class for image filters using Flutter's ColorFilter.
/// Provides non-destructive filter application via ColorFiltered widget.
class ImageFilter {
  final String id;
  final String name;
  final ColorFilter? colorFilter;
  final String iconPath; // Optional icon, can be empty

  const ImageFilter({
    required this.id,
    required this.name,
    this.colorFilter,
    this.iconPath = '',
  });

  /// No filter applied (original image)
  static const ImageFilter none = ImageFilter(
    id: 'none',
    name: 'Original',
  );

  /// Black & White filter using saturation matrix
  static const ImageFilter blackAndWhite = ImageFilter(
    id: 'bw',
    name: 'B&W',
    colorFilter: ColorFilter.matrix(<double>[
      0.2126, 0.7152, 0.0722, 0, 0,
      0.2126, 0.7152, 0.0722, 0, 0,
      0.2126, 0.7152, 0.0722, 0, 0,
      0,      0,      0,      1, 0,
    ]),
  );

  /// Sepia tone filter
  static const ImageFilter sepia = ImageFilter(
    id: 'sepia',
    name: 'Sepia',
    colorFilter: ColorFilter.matrix(<double>[
      0.393, 0.769, 0.189, 0, 0,
      0.349, 0.686, 0.168, 0, 0,
      0.272, 0.534, 0.131, 0, 0,
      0,     0,     0,     1, 0,
    ]),
  );

  /// Warm tone filter (slight orange/yellow tint)
  static const ImageFilter warm = ImageFilter(
    id: 'warm',
    name: 'Warm',
    colorFilter: ColorFilter.matrix(<double>[
      1.2,  0,    0,    0, 20,
      0,    1.1,  0,    0, 10,
      0,    0,    0.9,  0, 0,
      0,    0,    0,    1, 0,
    ]),
  );

  /// Cool tone filter (slight blue tint)
  static const ImageFilter cool = ImageFilter(
    id: 'cool',
    name: 'Cool',
    colorFilter: ColorFilter.matrix(<double>[
      0.9,  0,    0,    0, 0,
      0,    1.0,  0,    0, 10,
      0,    0,    1.2,  0, 20,
      0,    0,    0,    1, 0,
    ]),
  );

  /// Vintage filter (desaturated with slight yellow)
  static const ImageFilter vintage = ImageFilter(
    id: 'vintage',
    name: 'Vintage',
    colorFilter: ColorFilter.matrix(<double>[
      0.6,  0.3,  0.1,  0, 30,
      0.2,  0.7,  0.1,  0, 20,
      0.2,  0.3,  0.4,  0, 10,
      0,    0,    0,    1, 0,
    ]),
  );

  /// High contrast filter
  static const ImageFilter contrast = ImageFilter(
    id: 'contrast',
    name: 'Contrast',
    colorFilter: ColorFilter.matrix(<double>[
      1.5,  0,    0,    0, -40,
      0,    1.5,  0,    0, -40,
      0,    0,    1.5,  0, -40,
      0,    0,    0,    1, 0,
    ]),
  );

  /// List of all available filters
  static const List<ImageFilter> availableFilters = [
    none,
    blackAndWhite,
    sepia,
    warm,
    cool,
    vintage,
    contrast,
  ];

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ImageFilter &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
