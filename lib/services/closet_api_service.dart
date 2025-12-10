import 'package:flutter/foundation.dart';
import '../models/product.dart';
import 'tryon_api_service.dart';

/// Service for managing the user's closet (saved try-ons).
/// This is a wrapper around TryOnApiService that provides closet-specific functionality.
class ClosetApiService {
  ClosetApiService._();

  static final ClosetApiService _instance = ClosetApiService._();
  factory ClosetApiService() => _instance;

  final TryOnApiService _tryOnService = TryOnApiService();

  List<TryOnEntry>? _cache;

  /// Gets all closet items (try-on history) from the backend.
  Future<List<TryOnEntry>> getClosetItems({bool forceRefresh = false}) async {
    if (!forceRefresh && _cache != null) {
      return _cache!;
    }

    try {
      final history = await _tryOnService.getHistory();
      _cache = history;
      return history;
    } catch (e) {
      debugPrint('Get closet items error: $e');
      return _cache ?? [];
    }
  }

  /// Converts try-on entries to Product objects for display.
  List<Product> entriesToProducts(List<TryOnEntry> entries) {
    return entries
        .where((e) => e.shoe != null)
        .map((e) => Product.fromJson(e.shoe!))
        .toList(growable: false);
  }

  /// Gets favorite items (placeholder for future implementation).
  Future<List<TryOnEntry>> getFavorites() async {
    // For now, return all items - favorites would require backend support
    final items = await getClosetItems();
    return items;
  }

  /// Gets recent items (last 5).
  Future<List<TryOnEntry>> getRecent({int limit = 5}) async {
    final items = await getClosetItems();
    if (items.length <= limit) return items;
    return items.sublist(0, limit);
  }

  /// Removes an item from the closet.
  Future<bool> removeItem(String id) async {
    final result = await _tryOnService.deleteTryOn(id);
    if (result) {
      _cache?.removeWhere((e) => e.id == id);
    }
    return result;
  }

  /// Clears the local cache.
  void clearCache() {
    _cache = null;
  }
}
