import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:firebase_auth/firebase_auth.dart';
import '../config/api_config.dart';

class ApiClient {
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;
  ApiClient._internal();

  Future<String?> _getAuthToken() async {
    final user = FirebaseAuth.instance.currentUser;
    return user?.getIdToken();
  }

  Future<http.Response> get(String endpoint, {bool requiresAuth = false}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$endpoint');
    var headers = {'Content-Type': 'application/json'};

    if (requiresAuth) {
      final token = await _getAuthToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return http.get(uri, headers: headers).timeout(ApiConfig.timeout);
  }

  Future<http.Response> post(String endpoint, dynamic body, {bool requiresAuth = false}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$endpoint');
    var headers = {'Content-Type': 'application/json'};

    if (requiresAuth) {
      final token = await _getAuthToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return http.post(uri, body: jsonEncode(body), headers: headers).timeout(ApiConfig.timeout);
  }

  Future<http.Response> put(String endpoint, dynamic body, {bool requiresAuth = false}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$endpoint');
    var headers = {'Content-Type': 'application/json'};

    if (requiresAuth) {
      final token = await _getAuthToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return http.put(uri, body: jsonEncode(body), headers: headers).timeout(ApiConfig.timeout);
  }

  Future<http.Response> delete(String endpoint, {bool requiresAuth = false}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$endpoint');
    var headers = {'Content-Type': 'application/json'};

    if (requiresAuth) {
      final token = await _getAuthToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return http.delete(uri, headers: headers).timeout(ApiConfig.timeout);
  }
}

