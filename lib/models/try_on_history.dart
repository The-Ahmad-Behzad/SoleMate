import 'package:flutter/foundation.dart';
import 'product.dart';

@immutable
class TryOnHistory {
  const TryOnHistory({
    required this.id,
    required this.shoe,
    this.snapshotUrl,
    this.customSkinApplied = false,
    this.createdAt,
  });

  final String id;
  final Product shoe;
  final String? snapshotUrl;
  final bool customSkinApplied;
  final DateTime? createdAt;

  factory TryOnHistory.fromJson(Map<String, dynamic> json) {
    return TryOnHistory(
      id: json['_id'] ?? json['id'] ?? '',
      shoe: Product.fromJson((json['shoeId'] as Map<String, dynamic>?) ?? {}),
      snapshotUrl: json['snapshotUrl'] as String?,
      customSkinApplied: json['customSkinApplied'] as bool? ?? false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }
}
