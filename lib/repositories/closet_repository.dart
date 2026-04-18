import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../models/try_on_history.dart';
import '../models/product.dart';
import '../services/api_client.dart';
import '../services/outfit_api_service.dart';

class ClosetRepository {
  static final ClosetRepository _instance = ClosetRepository._internal();
  factory ClosetRepository() => _instance;
  ClosetRepository._internal();

  final ApiClient _api = ApiClient();
  final OutfitApiService _outfitService = OutfitApiService();

  /// Fetch user's AR try-on history
  Future<List<TryOnHistory>> getTryOnHistory() async {
    try {
      final response = await _api.get('/tryon/history', requiresAuth: true);
      
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.whereType<Map<String, dynamic>>().map((e) => TryOnHistory.fromJson(e)).toList();
      } else {
        debugPrint('Failed to load try-on history: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      debugPrint('Error fetching try-on history: $e');
      return [];
    }
  }

  /// Delete a try-on history record
  Future<bool> deleteTryOnHistory(String id) async {
    try {
      final response = await _api.delete('/tryon/$id', requiresAuth: true);
      return response.statusCode == 204 || response.statusCode == 200;
    } catch (e) {
      debugPrint('Error deleting try-on history: $e');
      return false;
    }
  }

  /// Get saved outfits (from Outfit Match feature)
  Future<List<OutfitMatch>> getSavedOutfits() async {
    return await _outfitService.getHistory();
  }
}
