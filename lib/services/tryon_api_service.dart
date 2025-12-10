import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'api_client.dart';

/// Model representing a try-on history entry from the backend.
class TryOnEntry {
  const TryOnEntry({
    required this.id,
    required this.shoeId,
    this.snapshotUrl,
    this.customSkinApplied = false,
    this.createdAt,
    this.shoe,
  });

  final String id;
  final String shoeId;
  final String? snapshotUrl;
  final bool customSkinApplied;
  final DateTime? createdAt;
  final Map<String, dynamic>? shoe; // Populated shoe data

  factory TryOnEntry.fromJson(Map<String, dynamic> json) {
    // Handle MongoDB _id field
    final String entryId = (json['_id'] as String?) ?? (json['id'] as String? ?? '');
    final shoeData = json['shoeId'];
    
    return TryOnEntry(
      id: entryId,
      shoeId: shoeData is Map<String, dynamic> 
          ? ((shoeData['_id'] as String?) ?? (shoeData['id'] as String? ?? ''))
          : (shoeData as String? ?? ''),
      snapshotUrl: json['snapshotUrl'] as String?,
      customSkinApplied: (json['customSkinApplied'] as bool?) ?? false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
      shoe: shoeData is Map<String, dynamic> ? shoeData : null,
    );
  }
}

/// Service for managing try-on history via backend API.
class TryOnApiService {
  TryOnApiService._();

  static final TryOnApiService _instance = TryOnApiService._();
  factory TryOnApiService() => _instance;

  final ApiClient _api = ApiClient();

  /// Saves a try-on session to the backend.
  /// Returns the created TryOnEntry on success, null on failure.
  Future<TryOnEntry?> saveTryOn({
    required String shoeId,
    String? snapshotUrl,
    bool customSkinApplied = false,
  }) async {
    try {
      final response = await _api.post(
        '/tryon/save',
        {
          'shoeId': shoeId,
          'snapshotUrl': snapshotUrl,
          'customSkinApplied': customSkinApplied,
        },
        requiresAuth: true,
      );

      if (response.statusCode == 201) {
        final Map<String, dynamic> data =
            json.decode(response.body) as Map<String, dynamic>;
        return TryOnEntry.fromJson(data);
      } else {
        debugPrint('Save try-on failed: ${response.statusCode} - ${response.body}');
        return null;
      }
    } catch (e) {
      debugPrint('Save try-on error: $e');
      return null;
    }
  }

  /// Fetches the user's try-on history from the backend.
  Future<List<TryOnEntry>> getHistory() async {
    try {
      final response = await _api.get('/tryon/history', requiresAuth: true);

      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body) as List<dynamic>;
        return data
            .whereType<Map<String, dynamic>>()
            .map((e) => TryOnEntry.fromJson(e))
            .toList(growable: false);
      } else {
        debugPrint('Get history failed: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      debugPrint('Get history error: $e');
      return [];
    }
  }

  /// Deletes a try-on entry by ID.
  Future<bool> deleteTryOn(String id) async {
    try {
      final response = await _api.delete('/tryon/$id', requiresAuth: true);
      return response.statusCode == 204;
    } catch (e) {
      debugPrint('Delete try-on error: $e');
      return false;
    }
  }
}
