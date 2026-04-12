import 'package:flutter/material.dart';
import '../theme/theme_config.dart';
import '../services/auth_service.dart';
import '../services/api_client.dart';
import 'auth/login_screen.dart';
import 'package:url_launcher/url_launcher.dart';
import 'dart:convert';

class SellerDashboardScreen extends StatefulWidget {
  const SellerDashboardScreen({super.key});

  @override
  State<SellerDashboardScreen> createState() => _SellerDashboardScreenState();
}

class _SellerDashboardScreenState extends State<SellerDashboardScreen> {
  final AuthService _authService = AuthService();

  Future<void> _logout() async {
    await _authService.logout();
    if (mounted) {
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const LoginScreen()));
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Seller Dashboard'),
          actions: [
            IconButton(icon: const Icon(Icons.logout), onPressed: _logout),
          ],
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Redesign Requests'),
              Tab(text: 'Shoe Uploads'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            RedesignRequestsTab(),
            ShoeUploadRequestTab(),
          ],
        ),
      ),
    );
  }
}

class RedesignRequestsTab extends StatefulWidget {
  const RedesignRequestsTab({super.key});

  @override
  State<RedesignRequestsTab> createState() => _RedesignRequestsTabState();
}

class _RedesignRequestsTabState extends State<RedesignRequestsTab> {
  final ApiClient _apiClient = ApiClient();
  List<dynamic> _requests = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _fetchRequests();
  }

  Future<void> _fetchRequests() async {
    try {
      final response = await _apiClient.get('/skins/redesign-requests');
      if (response.statusCode == 200) {
        setState(() {
          _requests = jsonDecode(response.body);
          _loading = false;
        });
      }
    } catch (e) {
      setState(() => _loading = false);
    }
  }

  void _emailCustomer(String email, String shoeName) {
    final Uri emailLaunchUri = Uri(
      scheme: 'mailto',
      path: email,
      query: 'subject=Update on your Redesign Request: $shoeName&body=Hello,',
    );
    launchUrl(emailLaunchUri);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_requests.isEmpty) {
      return const Center(child: Text('No requests currently.'));
    }

    return ListView.builder(
      itemCount: _requests.length,
      itemBuilder: (context, index) {
        final req = _requests[index];
        final user = req['userId'] ?? {};
        final email = user['email'] ?? '';
        final shoe = req['shoeId'] ?? {};
        
        return Card(
          margin: const EdgeInsets.all(8.0),
          child: ListTile(
            title: Text('Shoe: ${shoe['name'] ?? 'Unknown'}'),
            subtitle: Text('Details: ${req['description']}\nFrom: $email'),
            isThreeLine: true,
            trailing: IconButton(
              icon: const Icon(Icons.email, color: AppColors.primary),
              onPressed: () => _emailCustomer(email, shoe['name'] ?? ''),
            ),
          ),
        );
      },
    );
  }
}

class ShoeUploadRequestTab extends StatefulWidget {
  const ShoeUploadRequestTab({super.key});

  @override
  State<ShoeUploadRequestTab> createState() => _ShoeUploadRequestTabState();
}

class _ShoeUploadRequestTabState extends State<ShoeUploadRequestTab> {
  final _shoeNameController = TextEditingController();
  final _brandController = TextEditingController();
  final _descController = TextEditingController();
  final ApiClient _apiClient = ApiClient();
  bool _isSubmitting = false;

  Future<void> _submitRequest() async {
    if (_shoeNameController.text.isEmpty || _brandController.text.isEmpty || _descController.text.isEmpty) return;

    setState(() => _isSubmitting = true);
    try {
      final response = await _apiClient.post(
        '/catalog/request-shoe',
        {
          'shoeName': _shoeNameController.text,
          'brand': _brandController.text,
          'description': _descController.text,
        },
      );
      if (response.statusCode == 201 && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Request Submitted!')));
        _shoeNameController.clear();
        _brandController.clear();
        _descController.clear();
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        children: [
          TextField(controller: _shoeNameController, decoration: const InputDecoration(labelText: 'Shoe Name')),
          const SizedBox(height: 10),
          TextField(controller: _brandController, decoration: const InputDecoration(labelText: 'Brand')),
          const SizedBox(height: 10),
          TextField(controller: _descController, decoration: const InputDecoration(labelText: 'Description/Features'), maxLines: 3),
          const SizedBox(height: 20),
          ElevatedButton(
            onPressed: _isSubmitting ? null : _submitRequest,
            child: _isSubmitting ? const CircularProgressIndicator() : const Text('Request AR Lens Addition'),
          )
        ],
      ),
    );
  }
}
