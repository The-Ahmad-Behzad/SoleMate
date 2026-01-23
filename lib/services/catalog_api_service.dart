import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../models/product.dart';
import 'api_client.dart';
import 'local_catalog_service.dart';

/// Service for fetching catalog data from backend API.
/// Falls back to LocalCatalogService if API is unavailable.
class CatalogApiService {
  CatalogApiService._();

  static final CatalogApiService _instance = CatalogApiService._();
  factory CatalogApiService() => _instance;

  final ApiClient _api = ApiClient();
  final LocalCatalogService _localService = LocalCatalogService();

  List<Product>? _cache;
  DateTime? _cacheTime;
  static const Duration _cacheDuration = Duration(minutes: 5);

  /// Loads all products from API, with fallback to local JSON.
  Future<List<Product>> loadProducts({bool forceRefresh = false}) async {
    // Return cached data if valid
    if (!forceRefresh && _cache != null && _cacheTime != null) {
      final elapsed = DateTime.now().difference(_cacheTime!);
      if (elapsed < _cacheDuration) {
        return _cache!;
      }
    }

    try {
      // Attempt to fetch from API
      final response = await _api.get('/catalog');

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body) as List<dynamic>;
        final List<Product> products = data
            .whereType<Map<String, dynamic>>()
            .map((e) => Product.fromJson(e))
            .toList(growable: false);

        // Update cache
        _cache = products;
        _cacheTime = DateTime.now();

        return products;
      } else {
        // API returned error, fallback to local
        debugPrint('Catalog API returned ${response.statusCode}, falling back to local');
        return _localService.loadProducts(forceRefresh: forceRefresh);
      }
    } catch (e) {
      // Network error or timeout, fallback to local
      debugPrint('Catalog API error: $e, falling back to local');
      return _localService.loadProducts(forceRefresh: forceRefresh);
    }
  }

  /// Gets a single product by ID from API.
  Future<Product?> getById(String id) async {
    try {
      final response = await _api.get('/catalog/$id');

      if (response.statusCode == 200) {
        final Map<String, dynamic> data =
            json.decode(response.body) as Map<String, dynamic>;
        return Product.fromJson(data);
      } else if (response.statusCode == 404) {
        return null;
      } else {
        // Fallback to local
        return _localService.getById(id);
      }
    } catch (e) {
      debugPrint('Get product by ID error: $e');
      return _localService.getById(id);
    }
  }

  /// Searches products by query string.
  Future<List<Product>> search(String query) async {
    final q = query.trim();
    if (q.isEmpty) return loadProducts();

    try {
      final response = await _api.get('/catalog/search?q=${Uri.encodeComponent(q)}');

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body) as List<dynamic>;
        return data
            .whereType<Map<String, dynamic>>()
            .map((e) => Product.fromJson(e))
            .toList(growable: false);
      } else {
        // Fallback to local search
        return _localService.search(query);
      }
    } catch (e) {
      debugPrint('Search API error: $e');
      return _localService.search(query);
    }
  }

  /// Gets popular products (uses cached data or fetches if needed).
  Future<List<Product>> popular() async {
    final products = await loadProducts();
    return products.where((p) => p.isPopular).toList(growable: false);
  }

  /// Gets recently tried products.
  Future<List<Product>> recents({int limit = 10}) async {
    final products = await loadProducts();
    final withDates = products
        .where((p) => p.lastTriedAt != null)
        .toList(growable: false);
    withDates.sort((a, b) {
      return (b.lastTriedAt!)
          .millisecondsSinceEpoch
          .compareTo((a.lastTriedAt!).millisecondsSinceEpoch);
    });
    if (withDates.length <= limit) return withDates;
    return withDates.sublist(0, limit);
  }

  /// Clears the local cache, forcing next load to fetch from API.
  void clearCache() {
    _cache = null;
    _cacheTime = null;
  }
}
