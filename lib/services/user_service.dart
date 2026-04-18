import 'dart:convert';
import 'api_client.dart';

class UserService {
  final ApiClient _apiClient = ApiClient();

  /// Syncs profile with the backend (upserts) and returns the profile data.
  Future<Map<String, dynamic>> syncProfile(String name) async {
    final response = await _apiClient.put(
      '/user/profile',
      {'name': name},
      requiresAuth: true,
    );

    if (response.statusCode == 200 || response.statusCode == 201) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to sync profile: ${response.body}');
    }
  }

  /// Fetches the profile to get current role.
  Future<Map<String, dynamic>> getProfile() async {
    final response = await _apiClient.get('/user/profile', requiresAuth: true);
    
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get profile: ${response.body}');
    }
  }
}
