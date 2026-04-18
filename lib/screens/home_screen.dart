import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../repositories/catalog_repository.dart';
import '../repositories/closet_repository.dart';
import '../models/product.dart';
import '../models/try_on_history.dart';
import '../widgets/product_card.dart';
import '../theme/theme_config.dart';
import 'auth/login_screen.dart';
import 'catalog_screen.dart';
import 'ar_tryon_screen.dart';
import '../ar/ar_main.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final AuthService _authService = AuthService();
  final CatalogRepository _catalog = CatalogRepository();
  final ClosetRepository _closet = ClosetRepository();
  final ARMain _arMain = ARMain();

  late Future<List<Product>> _popularFuture;
  late Future<List<TryOnHistory>> _historyFuture;

  @override
  void initState() {
    super.initState();
    _refreshData();
  }

  void _refreshData() {
    setState(() {
      _popularFuture = _catalog.getProducts().then((list) => list.take(2).toList());
      _historyFuture = _closet.getTryOnHistory();
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('SoleMate'),
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const CatalogScreen()),
              );
            },
          ),
          PopupMenuButton<String>(
            onSelected: (value) async {
              if (value == 'logout') {
                await _authService.logout();
                if (mounted) {
                  Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(builder: (_) => const LoginScreen()),
                  );
                }
              }
            },
            itemBuilder: (BuildContext context) => [
              const PopupMenuItem<String>(
                value: 'logout',
                child: Row(
                  children: [
                    Icon(Icons.logout),
                    SizedBox(width: 8),
                    Text('Logout'),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => _refreshData(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeroSection(context, isDark),
              const SizedBox(height: AppSpacing.xl2),
              _buildSectionHeader(context, 'Popular Now', isDark, onSeeAll: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const CatalogScreen()),
                );
              }),
              _buildPopularList(context),
              const SizedBox(height: AppSpacing.xl2),
              _buildSectionHeader(context, 'Recent Try-Ons', isDark),
              _buildHistoryList(context, isDark),
              const SizedBox(height: AppSpacing.xl3),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _arMain.checkPermissionsAndOpenAR(context),
        icon: const Icon(Icons.camera_alt),
        label: const Text('Quick AR'),
      ),
    );
  }

  Widget _buildHeroSection(BuildContext context, bool isDark) {
    return Container(
      width: double.infinity,
      padding: AppSpacing.paddingLarge,
      decoration: BoxDecoration(
        gradient: isDark ? AppGradients.heroDark : AppGradients.heroLight,
        borderRadius: const BorderRadius.only(
          bottomLeft: Radius.circular(30),
          bottomRight: Radius.circular(30),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Find Your\nPerfect SoleMate',
            style: AppTypography.headline1.copyWith(color: Colors.white),
          ),
          const SizedBox(height: AppSpacing.md),
          Text(
            'Try on shoes virtually with Snap AR',
            style: AppTypography.bodyLarge.copyWith(color: Colors.white70),
          ),
          const SizedBox(height: AppSpacing.lg),
          ElevatedButton(
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const CatalogScreen()),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.white,
              foregroundColor: AppColors.primary,
              padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
            ),
            child: const Text('Browse Catalog'),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(
    BuildContext context,
    String title,
    bool isDark, {
    VoidCallback? onSeeAll,
  }) {
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: AppTypography.headline3.copyWith(
              color: isDark ? AppColors.darkForeground : AppColors.lightForeground,
            ),
          ),
          if (onSeeAll != null)
            TextButton(
              onPressed: onSeeAll,
              child: const Text('See All'),
            ),
        ],
      ),
    );
  }

  Widget _buildPopularList(BuildContext context) {
    return SizedBox(
      height: 280,
      child: FutureBuilder<List<Product>>(
        future: _popularFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError || !snapshot.hasData || snapshot.data!.isEmpty) {
            return _buildEmptyState('No popular items yet');
          }

          final products = snapshot.data!;
          return ListView.separated(
            padding: EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.md),
            scrollDirection: Axis.horizontal,
            itemCount: products.length,
            separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.md),
            itemBuilder: (context, index) {
              final p = products[index];
              return SizedBox(
                width: 160,
                child: ProductCard(
                  imagePath: p.thumbnailUrl ?? '',
                  title: p.name,
                  price: p.price,
                  aspectRatio: 1.0,
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => ARTryOnScreen(selectedProduct: p),
                      ),
                    );
                  },
                ),
              );
            },
          );
        },
      ),
    );
  }

  Widget _buildHistoryList(BuildContext context, bool isDark) {
    return FutureBuilder<List<TryOnHistory>>(
      future: _historyFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: Padding(
            padding: EdgeInsets.all(16.0),
            child: CircularProgressIndicator(),
          ));
        }
        if (snapshot.hasError || !snapshot.hasData || snapshot.data!.isEmpty) {
          return Padding(
            padding: EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.md),
            child: Text(
              'No recent try-ons',
              style: AppTypography.bodyMedium.copyWith(color: Colors.grey),
            ),
          );
        }

        final history = snapshot.data!;
        return ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          padding: EdgeInsets.symmetric(horizontal: AppSpacing.lg),
          itemCount: history.length > 2 ? 2 : history.length,
          itemBuilder: (context, index) {
            final entry = history[index];
            final shoeName = entry.shoe.name;
            
            return Card(
              margin: const EdgeInsets.only(bottom: AppSpacing.sm),
              child: ListTile(
                leading: const Icon(Icons.history),
                title: Text(shoeName),
                subtitle: Text(entry.createdAt?.toString().split('.')[0] ?? ''),
                trailing: const Icon(Icons.chevron_right),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildEmptyState(String message) {
    return Center(
      child: Text(message, style: const TextStyle(color: Colors.grey)),
    );
  }
}
