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
      isPopular: isPopular ?? this.isPopular,
      lastTriedAt: lastTriedAt ?? this.lastTriedAt,
    );
  }

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id'] as String,
      name: json['name'] as String,
      brand: json['brand'] as String,
      price: (json['price'] as num).toDouble(),
      category: json['category'] as String,
      thumbnailUrl: json['thumbnailUrl'] as String?,
      modelUrl: json['modelUrl'] as String?,
      textureUrl: json['textureUrl'] as String?,
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
      'isPopular': isPopular,
      'lastTriedAt': lastTriedAt?.toIso8601String(),
    };
  }

  @override
  String toString() {
    return 'Product(id: ' + id + ', name: ' + name + ', brand: ' + brand + ')';
  }
}


