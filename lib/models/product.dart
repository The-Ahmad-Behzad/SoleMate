import 'package:flutter/foundation.dart';

@immutable
class Product {
  const Product({
    required this.id,
    required this.name,
    required this.brand,
    required this.price,
    required this.category,
    this.thumbnailUrl,
    this.modelUrl,
    this.textureUrl,
    this.arLensId,
    this.arLensGroupId,
    this.isPopular = false,
    this.lastTriedAt,
  });

  final String id;
  final String name;
  final String brand;
  final double price;
  final String category;
  final String? thumbnailUrl;
  final String? modelUrl;
  final String? textureUrl;
  final String? arLensId;
  final String? arLensGroupId;
  final bool isPopular;
  final DateTime? lastTriedAt;

  Product copyWith({
    String? id,
    String? name,
    String? brand,
    double? price,
    String? category,
    String? thumbnailUrl,
    String? modelUrl,
    String? textureUrl,
    String? arLensId,
    String? arLensGroupId,
    bool? isPopular,
    DateTime? lastTriedAt,
  }) {
    return Product(
      id: id ?? this.id,
      name: name ?? this.name,
      brand: brand ?? this.brand,
      price: price ?? this.price,
      category: category ?? this.category,
      thumbnailUrl: thumbnailUrl ?? this.thumbnailUrl,
      modelUrl: modelUrl ?? this.modelUrl,
      textureUrl: textureUrl ?? this.textureUrl,
      arLensId: arLensId ?? this.arLensId,
      arLensGroupId: arLensGroupId ?? this.arLensGroupId,
      isPopular: isPopular ?? this.isPopular,
      lastTriedAt: lastTriedAt ?? this.lastTriedAt,
    );
  }

  factory Product.fromJson(Map<String, dynamic> json) {
    // Handle MongoDB _id field mapping to id
    final String productId = (json['_id'] as String?) ?? (json['id'] as String);
    
    return Product(
      id: productId,
      name: json['name'] as String,
      brand: json['brand'] as String,
      price: (json['price'] as num).toDouble(),
      category: json['category'] as String,
      thumbnailUrl: json['thumbnailUrl'] as String?,
      modelUrl: json['modelUrl'] as String?,
      textureUrl: json['textureUrl'] as String?,
      arLensId: json['arLensId'] as String?,
      arLensGroupId: json['arLensGroupId'] as String?,
      isPopular: (json['isPopular'] as bool?) ?? false,
      lastTriedAt: json['lastTriedAt'] != null
          ? DateTime.tryParse(json['lastTriedAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'id': id,
      'name': name,
      'brand': brand,
      'price': price,
      'category': category,
      'thumbnailUrl': thumbnailUrl,
      'modelUrl': modelUrl,
      'textureUrl': textureUrl,
      'arLensId': arLensId,
      'arLensGroupId': arLensGroupId,
      'isPopular': isPopular,
      'lastTriedAt': lastTriedAt?.toIso8601String(),
    };
  }

  @override
  String toString() {
    return 'Product(id: ' + id + ', name: ' + name + ', brand: ' + brand + ')';
  }
}



