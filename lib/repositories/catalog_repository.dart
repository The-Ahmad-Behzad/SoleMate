import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../models/product.dart';
import '../services/api_client.dart';

class CatalogRepository {
  static final CatalogRepository _instance = CatalogRepository._internal();
  factory CatalogRepository() => _instance;
  CatalogRepository._internal();

  final ApiClient _api = ApiClient();

  /// Fetch all products from the catalog
  Future<List<Product>> getProducts() async {
    try {
      final response = await _api.get('/catalog');
      
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.whereType<Map<String, dynamic>>().map((e) => Product.fromJson(e)).toList();
      } else {
        debugPrint('Failed to load catalog: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      debugPrint('Error fetching catalog: $e');
      return [];
    }
  }

  /// Search products by query
  Future<List<Product>> searchProducts(String query) async {
    try {
      final response = await _api.get('/catalog/search?q=$query');
      
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.whereType<Map<String, dynamic>>().map((e) => Product.fromJson(e)).toList();
      } else {
        debugPrint('Failed to search catalog: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      debugPrint('Error searching catalog: $e');
      return [];
    }
  }

  /// Fetch a single product by ID
  Future<Product?> getProductById(String id) async {
    try {
      final response = await _api.get('/catalog/$id');
      
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        return Product.fromJson(data);
      } else {
        debugPrint('Failed to load product details: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      debugPrint('Error fetching product: $e');
      return null;
    }
  }
}
