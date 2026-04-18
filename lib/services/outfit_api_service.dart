import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../models/product.dart';
import 'api_client.dart';

/// Model representing an outfit match entry from the backend.
class OutfitMatch {
  final String id;
  final String? outfitImageUrl;
  final String? category;
  final String? description;
  final List<String> dominantColors;
  final List<Product> recommendedShoes;
  final DateTime? createdAt;

  const OutfitMatch({
    required this.id,
    this.outfitImageUrl,
    this.category,
    this.description,
    this.dominantColors = const [],
    this.recommendedShoes = const [],
    this.createdAt,
  });

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
      category: (json['category'] as String?) ?? 'Outfit Match',
      description: json['description'] as String?,
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
    String? outfitImageUrl,
    String? imagePath,
    List<String> dominantColors = const [],
    String? category,
    String? description,
    List<String>? recommendedShoeIds,
  }) async {
    try {
      if (imagePath != null) {
        // Use multipart for real image uploads
        final response = await _api.postMultipart(
          '/outfit/analyze',
          fields: {
            if (dominantColors.isNotEmpty) 'dominantColors': jsonEncode(dominantColors),
            if (category != null) 'category': category,
            if (description != null) 'description': description,
            if (recommendedShoeIds != null) 'recommendedShoeIds': jsonEncode(recommendedShoeIds),
          },
          files: [await http.MultipartFile.fromPath('outfitImage', imagePath)],
          requiresAuth: true,
        );

        if (response.statusCode == 201 || response.statusCode == 200) {
          final responseBody = await response.stream.bytesToString();
          return OutfitMatch.fromJson(json.decode(responseBody) as Map<String, dynamic>);
        } else {
          final errorBody = await response.stream.bytesToString();
          debugPrint('Analyze outfit (multipart) failed: ${response.statusCode} - $errorBody');
          return null;
        }
      } else {
        // Fallback to JSON for updates or URL-based analysis
        final response = await _api.post(
          '/outfit/analyze',
          {
            if (outfitImageUrl != null) 'outfitImageUrl': outfitImageUrl,
            'dominantColors': dominantColors,
            if (category != null) 'category': category,
            if (description != null) 'description': description,
            if (recommendedShoeIds != null) 'recommendedShoeIds': recommendedShoeIds,
          },
          requiresAuth: true,
        );

        if (response.statusCode == 200 || response.statusCode == 201) {
          final Map<String, dynamic> data = json.decode(response.body) as Map<String, dynamic>;
          return OutfitMatch.fromJson(data);
        } else {
          debugPrint('Analyze outfit (JSON) failed: ${response.statusCode} - ${response.body}');
          return null;
        }
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

  /// NEW: Gets shoe recommendations based on an uploaded outfit image file.
  Future<Map<String, dynamic>?> recommendShoesFromImage(String imagePath, {String gender = 'unisex'}) async {
    try {
      final response = await _api.postMultipart(
        '/outfit/recommend-shoes',
        fields: {
          'gender': gender,
        },
        files: [await http.MultipartFile.fromPath('outfitImage', imagePath)],
        requiresAuth: true,
      );

      if (response.statusCode == 200) {
        final responseBody = await response.stream.bytesToString();
        return json.decode(responseBody) as Map<String, dynamic>;
      } else {
        final errorBody = await response.stream.bytesToString();
        debugPrint('Recommend shoes failed: ${response.statusCode} - $errorBody');
        return null;
      }
    } catch (e) {
      debugPrint('Recommend shoes error: $e');
      return null;
    }
  }

  /// NEW: Gets an outfit recommendation (text) for an uploaded shoe image.
  Future<Map<String, dynamic>?> getOutfitRecommendationForShoe(String imagePath, {String gender = 'unisex'}) async {
    try {
      final response = await _api.postMultipart(
        '/outfit/recommend-outfit-for-shoe',
        fields: {
          'gender': gender,
        },
        files: [await http.MultipartFile.fromPath('file', imagePath)],
        requiresAuth: true,
      );

      if (response.statusCode == 200) {
        final responseBody = await response.stream.bytesToString();
        return json.decode(responseBody) as Map<String, dynamic>;
      } else {
        final errorBody = await response.stream.bytesToString();
        debugPrint('Get outfit recommendation failed: ${response.statusCode} - $errorBody');
        return null;
      }
    } catch (e) {
      debugPrint('Get outfit recommendation error: $e');
      return null;
    }
  }

  /// NEW: Checks for style harmony (mismatch) in an uploaded outfit image.
  Future<Map<String, dynamic>?> checkOutfitMismatch(String imagePath) async {
    try {
      final response = await _api.postMultipart(
        '/outfit/check-mismatch',
        files: [await http.MultipartFile.fromPath('image', imagePath)],
        requiresAuth: true,
      );

      if (response.statusCode == 200) {
        final responseBody = await response.stream.bytesToString();
        return json.decode(responseBody) as Map<String, dynamic>;
      } else {
        final errorBody = await response.stream.bytesToString();
        debugPrint('Check outfit mismatch failed: ${response.statusCode} - $errorBody');
        return null;
      }
    } catch (e) {
      debugPrint('Check outfit mismatch error: $e');
      return null;
    }
  }
}

