import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'api_client.dart';

/// Model representing a custom skin from the backend.
class CustomSkin {
  const CustomSkin({
    required this.id,
    required this.shoeId,
    required this.skinName,
    this.textureUrl,
    this.createdAt,
  });

  final String id;
  final String shoeId;
  final String skinName;
  final String? textureUrl;
  final DateTime? createdAt;

  factory CustomSkin.fromJson(Map<String, dynamic> json) {
    final String skinId = (json['_id'] as String?) ?? (json['id'] as String? ?? '');
    final shoeData = json['shoeId'];
    
    return CustomSkin(
      id: skinId,
      shoeId: shoeData is Map<String, dynamic>
          ? ((shoeData['_id'] as String?) ?? (shoeData['id'] as String? ?? ''))
          : (shoeData as String? ?? ''),
      skinName: json['skinName'] as String? ?? 'Untitled',
      textureUrl: json['textureUrl'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'shoeId': shoeId,
      'skinName': skinName,
      'textureUrl': textureUrl,
    };
  }
}

/// Service for managing custom skins via backend API.
class SkinApiService {
  SkinApiService._();

  static final SkinApiService _instance = SkinApiService._();
  factory SkinApiService() => _instance;

  final ApiClient _api = ApiClient();

  /// Creates a new custom skin.
  Future<CustomSkin?> createSkin({
    required String shoeId,
    required String skinName,
    String? textureUrl,
    Uint8List? textureFileBytes,
    String? textureFileName,
  }) async {
    try {
      List<http.MultipartFile>? files;
      if (textureFileBytes != null) {
        files = [
          http.MultipartFile.fromBytes(
            'texture',
            textureFileBytes,
            filename: textureFileName ?? 'texture.png',
          ),
        ];
      }

      final response = await _api.postMultipart(
        '/skins',
        fields: {
          'shoeId': shoeId,
          'skinName': skinName,
          // If textureUrl is provided instead of file, backend handles it? 
          // Current backend logic prefers file > textureUrl.
          if (textureUrl != null) 'textureUrl': textureUrl,
        },
        files: files,
        requiresAuth: true,
      );

      if (response.statusCode == 201) {
        final responseBody = await response.stream.bytesToString();
        final Map<String, dynamic> data =
            json.decode(responseBody) as Map<String, dynamic>;
        return CustomSkin.fromJson(data);
      } else {
        debugPrint('Create skin failed: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      debugPrint('Create skin error: $e');
      return null;
    }
  }

  /// Gets all custom skins for the current user.
  Future<List<CustomSkin>> getSkins() async {
    try {
      final response = await _api.get('/skins', requiresAuth: true);

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body) as List<dynamic>;
        return data
            .whereType<Map<String, dynamic>>()
            .map((e) => CustomSkin.fromJson(e))
            .toList(growable: false);
      } else {
        debugPrint('Get skins failed: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      debugPrint('Get skins error: $e');
      return [];
    }
  }

  /// Updates an existing custom skin.
  Future<CustomSkin?> updateSkin({
    required String id,
    String? skinName,
    String? textureUrl,
  }) async {
    try {
      final response = await _api.put(
        '/skins/$id',
        {
          if (skinName != null) 'skinName': skinName,
          if (textureUrl != null) 'textureUrl': textureUrl,
        },
        requiresAuth: true,
      );

      if (response.statusCode == 200) {
        final Map<String, dynamic> data =
            json.decode(response.body) as Map<String, dynamic>;
        return CustomSkin.fromJson(data);
      } else {
        debugPrint('Update skin failed: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      debugPrint('Update skin error: $e');
      return null;
    }
  }

  /// Deletes a custom skin.
  Future<bool> deleteSkin(String id) async {
    try {
      final response = await _api.delete('/skins/$id', requiresAuth: true);
      return response.statusCode == 204;
    } catch (e) {
      debugPrint('Delete skin error: $e');
      return false;
    }
  }
}
