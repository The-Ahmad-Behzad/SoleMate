import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;
import '../models/product.dart';

class LocalCatalogService {
  LocalCatalogService._();

  static final LocalCatalogService _instance = LocalCatalogService._();
  factory LocalCatalogService() => _instance;

  static const String _productsAssetPath = 'assets/data/products.json';

  List<Product>? _cache;

  Future<List<Product>> loadProducts({bool forceRefresh = false}) async {
    if (!forceRefresh && _cache != null) {
      return _cache!;
    }

    final String jsonStr = await rootBundle.loadString(_productsAssetPath);
    final List<dynamic> data = json.decode(jsonStr) as List<dynamic>;
    final List<Product> products = data
        .whereType<Map<String, dynamic>>()
        .map((e) => Product.fromJson(e))
        .toList(growable: false);

    _cache = products;
    return products;
  }

  Future<Product?> getById(String id) async {
    final List<Product> products = await loadProducts();
    for (final Product p in products) {
      if (p.id == id) return p;
    }
    return null;
  }

  Future<List<Product>> search(String query) async {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) return loadProducts();

    final List<Product> products = await loadProducts();
    return products.where((p) {
      final name = p.name.toLowerCase();
      final brand = p.brand.toLowerCase();
      final category = p.category.toLowerCase();
      return name.contains(q) || brand.contains(q) || category.contains(q);
    }).toList(growable: false);
  }

  Future<List<Product>> popular() async {
    final List<Product> products = await loadProducts();
    return products.where((p) => p.isPopular).toList(growable: false);
  }

  Future<List<Product>> recents({int limit = 10}) async {
    final List<Product> products = await loadProducts();
    final List<Product> withDates = products
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
}



