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
    final token = await user?.getIdToken();
    print('ApiClient Auth Token present: ${token != null}');
    return token;
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

    print('ApiClient GET: $uri');
    try {
      final response = await http.get(uri, headers: headers).timeout(ApiConfig.timeout);
      print('ApiClient GET Status: ${response.statusCode}');
      if (response.statusCode >= 400) {
        print('ApiClient GET Error Body: ${response.body}');
      }
      return response;
    } catch (e) {
      print('ApiClient GET Error: $e');
      rethrow;
    }
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

    print('ApiClient POST: $uri');
    try {
      final response = await http.post(uri, body: jsonEncode(body), headers: headers).timeout(ApiConfig.timeout);
      print('ApiClient POST Status: ${response.statusCode}');
      if (response.statusCode >= 400) {
        print('ApiClient POST Error Body: ${response.body}');
      }
      return response;
    } catch (e) {
      print('ApiClient POST Error: $e');
      rethrow;
    }
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

    print('ApiClient PUT: $uri');
    try {
      final response = await http.put(uri, body: jsonEncode(body), headers: headers).timeout(ApiConfig.timeout);
      print('ApiClient PUT Status: ${response.statusCode}');
      return response;
    } catch (e) {
      print('ApiClient PUT Error: $e');
      rethrow;
    }
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

    print('ApiClient DELETE: $uri');
    try {
      final response = await http.delete(uri, headers: headers).timeout(ApiConfig.timeout);
      print('ApiClient DELETE Status: ${response.statusCode}');
      return response;
    } catch (e) {
      print('ApiClient DELETE Error: $e');
      rethrow;
    }
  }

  Future<http.StreamedResponse> postMultipart(
    String endpoint, {
    Map<String, String>? fields,
    List<http.MultipartFile>? files,
    bool requiresAuth = false,
  }) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$endpoint');
    final request = http.MultipartRequest('POST', uri);

    if (requiresAuth) {
      final token = await _getAuthToken();
      if (token != null) {
        request.headers['Authorization'] = 'Bearer $token';
      }
    }

    if (fields != null) {
      request.fields.addAll(fields);
    }

    if (files != null) {
      request.files.addAll(files);
    }

    print('ApiClient MULTIPART: $uri');
    try {
      final response = await request.send().timeout(ApiConfig.timeout);
      print('ApiClient MULTIPART Status: ${response.statusCode}');
      if (response.statusCode >= 400) {
        final respStr = await response.stream.bytesToString();
        print('ApiClient MULTIPART Error Body: $respStr');
        // Return a new response with the string body so it can be read again if needed
        return http.StreamedResponse(
          Stream.value(utf8.encode(respStr)),
          response.statusCode,
          headers: response.headers,
        );
      }
      return response;
    } catch (e) {
      print('ApiClient MULTIPART Error: $e');
      rethrow;
    }
  }
}
