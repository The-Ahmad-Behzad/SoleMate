import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../models/product.dart';
import 'api_client.dart';

/// Model representing an outfit match entry from the backend.
class OutfitMatch {
  const OutfitMatch({
    required this.id,
    this.outfitImageUrl,
    this.dominantColors = const [],
    this.recommendedShoes = const [],
    this.createdAt,
  });

  final String id;
  final String? outfitImageUrl;
  final List<String> dominantColors;
  final List<Product> recommendedShoes;
  final DateTime? createdAt;

  factory OutfitMatch.fromJson(Map<String, dynamic> json) {
    final String matchId = (json['_id'] as String?) ?? (json['id'] as String? ?? '');
    
    // Parse recommended shoes if populated
    List<Product> shoes = [];
    final shoeIds = json['recommendedShoeIds'];
    if (shoeIds is List) {
      shoes = shoeIds
          .whereType<Map<String, dynamic>>()
          .map((e) => Product.fromJson(e))
          .toList(growable: false);
    }

    return OutfitMatch(
      id: matchId,
      outfitImageUrl: json['outfitImageUrl'] as String?,
      dominantColors: (json['dominantColors'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList(growable: false) ??
          [],
      recommendedShoes: shoes,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }
}

/// Service for outfit matching via backend API.
class OutfitApiService {
  OutfitApiService._();

  static final OutfitApiService _instance = OutfitApiService._();
  factory OutfitApiService() => _instance;

  final ApiClient _api = ApiClient();

  /// Analyzes an outfit image and stores the result.
  Future<OutfitMatch?> analyzeOutfit({
    required String outfitImageUrl,
    List<String> dominantColors = const [],
  }) async {
    try {
      final response = await _api.post(
        '/outfit/analyze',
        {
          'outfitImageUrl': outfitImageUrl,
          'dominantColors': dominantColors,
        },
        requiresAuth: true,
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final Map<String, dynamic> data =
            json.decode(response.body) as Map<String, dynamic>;
        return OutfitMatch.fromJson(data);
      } else {
        debugPrint('Analyze outfit failed: ${response.statusCode} - ${response.body}');
        return null;
      }
    } catch (e) {
      debugPrint('Analyze outfit error: $e');
      return null;
    }
  }

  /// Gets shoe recommendations based on colors.
  Future<List<Product>> getRecommendations(List<String> colors) async {
    try {
      final response = await _api.post(
        '/outfit/recommend',
        {'colors': colors},
        requiresAuth: true,
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body) as List<dynamic>;
        return data
            .whereType<Map<String, dynamic>>()
            .map((e) => Product.fromJson(e))
            .toList(growable: false);
      } else {
        debugPrint('Get recommendations failed: ${response.statusCode} - ${response.body}');
        return [];
      }
    } catch (e) {
      debugPrint('Get recommendations error: $e');
      return [];
    }
  }

  /// Gets the user's outfit match history.
  Future<List<OutfitMatch>> getHistory() async {
    try {
      final response = await _api.get('/outfit/history', requiresAuth: true);

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body) as List<dynamic>;
        return data
            .whereType<Map<String, dynamic>>()
            .map((e) => OutfitMatch.fromJson(e))
            .toList(growable: false);
      } else {
        debugPrint('Get outfit history failed: ${response.statusCode} - ${response.body}');
        return [];
      }
    } catch (e) {
      debugPrint('Get outfit history error: $e');
      return [];
    }
  }
}
