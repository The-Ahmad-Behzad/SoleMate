# SoleMate Backend Architecture Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Implementation Status](#implementation-status)
3. [System Components](#system-components)
4. [Setup Steps](#setup-steps)
5. [Verification Guide](#verification-guide)
6. [Frontend Integration Mapping](#frontend-integration-mapping)
7. [Future Implementation Guide](#future-implementation-guide)
8. [API Reference](#api-reference)

---

## Architecture Overview

SoleMate follows a **Hybrid Layered (N-Tier) Architecture** with cloud-integrated extensions, combining the clarity of layered architecture with elastic cloud services for scalability.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Flutter Client                    │
│                (Presentation Layer)                          │
└─────────────────────────────────────────────────────────────┘
                            │
                UI Events / API Calls
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Node.js BFF (Port 8080)                   │
│              (Backend-for-Frontend Layer)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Routes: Catalog, Try-On, Skins, Outfit, User       │  │
│  │  Controllers: Request handling logic                 │  │
│  │  Middleware: Auth, Error handling                    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  MongoDB     │   │  Redis       │   │  Firebase    │
│  Atlas       │   │  Cloud       │   │  Auth +      │
│  (Data)      │   │  (Cache)     │   │  Storage     │
└──────────────┘   └──────────────┘   └──────────────┘
```

### Technology Stack

| Component | Technology | Status |
|-----------|-----------|--------|
| Backend Runtime | Node.js 20 | ✅ Ready |
| Language | TypeScript (ES Modules) | ✅ Ready |
| Framework | Express.js | ✅ Ready |
| Database | MongoDB Atlas | ✅ Connected |
| Cache | Redis Cloud | ✅ Connected |
| Authentication | Firebase Admin SDK | ✅ Configured |
| Storage | Firebase Storage | ✅ Implemented |
| Containerization | Docker + Docker Compose | ✅ Working |
| Logging | Pino | ✅ Active |

---

## Implementation Status

### ✅ Completed Components

#### Phase 1: Project Structure (100%)
- ✅ Backend directory structure created
- ✅ Node.js project initialized with TypeScript
- ✅ Environment configuration management
- ✅ Docker configuration files
- ✅ Build and run scripts

#### Phase 2: MongoDB Integration (100%)
- ✅ MongoDB Atlas connection configured
- ✅ 5 Mongoose schemas created:
  - User
  - Product/Shoe
  - TryOnHistory
  - CustomSkin
  - OutfitMatch
- ✅ Database indexes for performance
- ✅ Seed script with 3 mock products
- ✅ Database successfully seeded

#### Phase 3: Redis Integration (100%)
- ✅ Redis Cloud connection configured
- ✅ Cache service with get/set/del methods
- ✅ Catalog caching implemented
- ✅ User session caching ready

#### Phase 4: Firebase Integration (100%)
- ✅ Firebase Admin SDK initialized
- ✅ Authentication middleware for token validation
- ✅ Firebase Storage service implemented
- ✅ User context extraction from tokens

#### Phase 5: REST API Endpoints (100%)
- ✅ **Catalog Routes**:
  - GET /api/catalog - List all products
  - GET /api/catalog/:id - Get specific product
  - GET /api/catalog/search - Search products

- ✅ **Try-On History Routes**:
  - POST /api/tryon/save - Save try-on session
  - GET /api/tryon/history - Get user's last 5 try-ons
  - DELETE /api/tryon/:id - Remove try-on record

- ✅ **Custom Skins Routes**:
  - POST /api/skins/create - Create custom skin
  - GET /api/skins - List user's skins
  - PUT /api/skins/:id - Update skin
  - DELETE /api/skins/:id - Delete skin

- ✅ **Outfit Matching Routes**:
  - POST /api/outfit/analyze - Analyze outfit colors
  - POST /api/outfit/recommend - Get recommendations
  - GET /api/outfit/history - Get match history

- ✅ **User Profile Routes**:
  - GET /api/user/profile - Get profile
  - PUT /api/user/profile - Update profile
  - GET /api/user/stats - Get statistics

- ✅ Health endpoint: GET /health

#### Phase 6: Cloud Storage (100%)
- ✅ Firebase Storage service implemented
- ✅ File upload/download methods
- ✅ Presigned URL generation
- ✅ File validation utilities

#### Phase 7: Error Handling (100%)
- ✅ Custom error classes
- ✅ Global error handler middleware
- ✅ Structured logging with Pino
- ✅ Request/response logging

#### Phase 8: Docker (100%)
- ✅ Multi-stage Dockerfile
- ✅ Docker Compose configuration
- ✅ Health checks implemented
- ✅ Network configuration
- ✅ Environment variable management

#### Phase 9: Flutter Client (80%)
- ✅ API client service created
- ✅ Authentication interceptor
- ✅ API configuration
- ❌ Repositories not yet integrated with UI

#### Phase 10: Documentation (100%)
- ✅ Backend README created
- ✅ API documentation created
- ✅ Architecture documentation (this file)

### ⚠️ Incomplete Components

#### Frontend Integration (20%)
- ❌ Repositories not connected to Flutter screens
- ❌ Product models not mapped from BFF to UI
- ❌ Error handling in UI not implemented
- ❌ Loading states not handled

#### Kotlin Native Module (Snap Camera Kit)
- ✅ Snap Camera Kit SDK Integrated
- ✅ `ARActivity.kt` utilizing Lens Groups
- ❌ Lens ID Dynamic Fetching (from BFF/Config) not implemented
- ❌ Native-to-Flutter data bridge for specific Shoe Lens selection

#### Additional Features (Not Started)
- ❌ Real-time outfit color extraction
- ❌ Advanced recommendation algorithms
- ❌ Product filtering and sorting
- ❌ Pagination for catalog

---

## System Components

### Backend Structure

```
backend/
├── src/
│   ├── config/              # Configuration modules
│   │   ├── database.ts      # MongoDB connection
│   │   ├── redis.ts         # Redis connection
│   │   └── firebase.ts      # Firebase Admin setup
│   │
│   ├── models/              # Mongoose schemas
│   │   ├── User.ts
│   │   ├── Product.ts
│   │   ├── TryOnHistory.ts
│   │   ├── CustomSkin.ts
│   │   └── OutfitMatch.ts
│   │
│   ├── controllers/         # Request handlers
│   │   ├── catalogController.ts
│   │   ├── tryonController.ts
│   │   ├── skinController.ts
│   │   ├── outfitController.ts
│   │   └── userController.ts
│   │
│   ├── routes/              # API routes
│   │   ├── index.ts         # Route aggregation
│   │   ├── catalogRoutes.ts
│   │   ├── tryonRoutes.ts
│   │   ├── skinRoutes.ts
│   │   ├── outfitRoutes.ts
│   │   └── userRoutes.ts
│   │
│   ├── middleware/          # Express middleware
│   │   ├── authMiddleware.ts
│   │   └── errorHandler.ts
│   │
│   ├── services/            # Business logic
│   │   ├── cacheService.ts
│   │   └── storageService.ts
│   │
│   ├── utils/               # Utilities
│   │   ├── errors.ts
│   │   └── fileValidator.ts
│   │
│   ├── scripts/             # Utility scripts
│   │   └── seed.ts
│   │
│   └── index.ts             # Application entry
│
├── docs/
│   └── API.md               # API documentation
│
├── Dockerfile
├── .dockerignore
├── docker-compose.yml
├── package.json
├── tsconfig.json
├── env.example
└── README.md
```

### Data Flow

1. **Request Flow**:
   ```
   Flutter App → HTTP Request → Express Router → Middleware → Controller → Service → Database
   ```

2. **Authentication Flow**:
   ```
   Flutter (Firebase Auth) → JWT Token → BFF (Verify Token) → Extract User UID → Process Request
   ```

3. **Caching Flow**:
   ```
   Request → Check Redis Cache → If Cache Hit: Return → If Cache Miss: Query MongoDB → Cache Result → Return
   ```

---

## Setup Steps

### Prerequisites

1. **Node.js 20+** installed
2. **Docker Desktop** installed
3. **MongoDB Atlas** account (free tier)
4. **Redis Cloud** account (free tier)
5. **Firebase Project** with Admin SDK enabled

### Step-by-Step Setup

#### 1. Clone and Navigate
```bash
cd c:\Projects\solemate_app
cd backend
```

#### 2. Install Dependencies
```bash
npm install
```

#### 3. Configure Environment
```bash
# Copy environment template
npm run setup

# Or manually
copy env.example .env

# Edit .env and add your credentials:
# - MONGODB_URI
# - REDIS_URL
# - FIREBASE_PROJECT_ID
# - FIREBASE_CLIENT_EMAIL
# - FIREBASE_PRIVATE_KEY
```

#### 4. Seed Database
```bash
npm run seed
```

Expected output:
```
Products already exist, skipping seed.
```

#### 5. Start Development Server
```bash
npm run dev
```

Expected output:
```
BFF listening on port 8080
```

#### 6. Verify Server
```bash
# In another terminal
curl http://localhost:8080/health
```

Expected response:
```json
{"ok":true,"service":"solemate-bff","version":"0.1.0","time":"..."}
```

#### 7. Test Catalog Endpoint
```bash
curl http://localhost:8080/api/catalog
```

Should return array of 3 products.

### Docker Setup

#### Build and Run
```bash
# From project root
docker-compose up --build
```

#### Stop Container
```bash
docker-compose down
```

#### View Logs
```bash
docker-compose logs -f bff
```

---

## Verification Guide

### 1. MongoDB Verification

#### Check Connection
```bash
# Via backend logs
npm run dev

# Should see successful connection
```

#### Verify Seed Data
```bash
# Connect to MongoDB Atlas
# Navigate to Collections
# Check solemate_app database
# Verify products collection has 3 documents
```

#### Test with API
```bash
curl http://localhost:8080/api/catalog | json_pp
```

Should return:
```json
[
  {
    "_id": "...",
    "name": "Air Zoom Runner",
    "brand": "SoleMate",
    "price": 129.99,
    ...
  },
  ...
]
```

### 2. Redis Verification

#### Check Connection
```bash
# Check logs when server starts
# Should see: "✅ Redis connected"
```

#### Test Cache (Implementation Needed)
```bash
# First request hits MongoDB
curl http://localhost:8080/api/catalog

# Second request should hit cache (needs to be verified)
```

### 3. Firebase Verification

#### Test Authentication
```bash
# Get Firebase token from Flutter app
# Then test protected endpoint:

curl -H "Authorization: Bearer <YOUR_TOKEN>" \
  http://localhost:8080/api/user/profile
```

Expected: User profile data or "Unauthorized" if invalid token.

#### Test Storage
```bash
# This requires implementing upload endpoint first
# Will test in Future Implementation section
```

### 4. Docker Verification

#### Check Health
```bash
docker ps
```

Should show `solemate_app-bff-1` with status "Up" and health "healthy".

#### Test from Container
```bash
docker exec solemate_app-bff-1 curl localhost:8080/health
```

### 5. API Verification

#### Test All Endpoints
Use the following collection or individual curl commands:

```bash
# Health
curl http://localhost:8080/health

# Catalog - All products
curl http://localhost:8080/api/catalog

# Catalog - Specific product
curl http://localhost:8080/api/catalog/6900f235777b45b41bafff98

# Catalog - Search
curl "http://localhost:8080/api/catalog/search?q=Air"

# For protected endpoints, use:
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/tryon/history
```

### 6. Flutter Integration Verification

#### Test API Client
```dart
// In Flutter app
import 'package:solemate_app/services/api_client.dart';

final client = ApiClient();
final response = await client.get('/catalog');
print(response.body); // Should print products JSON
```

---

## Frontend Integration Mapping

### Current Flutter Structure

```
lib/
├── screens/
│   ├── home_screen.dart
│   ├── ar_tryon_screen.dart      # Main AR feature
│   ├── outfit_match_screen.dart
│   ├── customize_screen.dart
│   └── closet_screen.dart
│
├── services/
│   └── auth_service.dart         # Firebase Auth
│
└── config/
    └── api_config.dart           # BFF base URL
```

### Integration Flow Mappings

#### 1. Home Screen → Catalog Display

**Current State**: ❌ Not Connected

**How to Connect**:

```dart
// lib/repositories/catalog_repository.dart
import '../services/api_client.dart';

class CatalogRepository {
  final ApiClient _api = ApiClient();

  Future<List<Product>> getProducts() async {
    final response = await _api.get('/graphs');
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Product.fromJson(json)).toList();
    }
    throw Exception('Failed to load products');
  }
}

// Use in home_screen.dart
final catalogRepo = CatalogRepository();
final products = await catalogRepo.getProducts();
```

**API Call**:
- Endpoint: `GET /api/catalog`
- Authentication: Not required
- Response: Array of Product objects

---

#### 2. AR Try-On Screen → Shoe Display

**Current State**: ⚠️ Partially Connected (Native Snap AR Activity exists)

**How to Connect**:

```dart
// lib/screens/ar_tryon_screen.dart

// When user selects a shoe to try on
Future<void> _loadShoeForAR(String shoeId) async {
  final response = await ApiClient().get('/catalog/$shoeId');
  
  if (response.statusCode == 200) {
    final product = Product.fromJson(jsonDecode(response.body));
    
    // Pass Lens ID to Native Module
    // Note: Product model needs to store 'lensId' or 'lensGroupId'
    await _arModule.launchARSession(
      lensId: product.arLensId, 
      lensGroupId: product.arLensGroupId
    );
  }
}
```

**API Call**:
- Endpoint: `GET /api/catalog/:id`
- Response: Product object with `arLensId` and `arLensGroupId`

**Native Bridge**:
```kotlin
// android/app/src/main/kotlin/.../MainActivity.kt

@FlutterMethod
fun launchARSession(call: MethodCall, result: Result) {
    val lensId = call.argument<String>("lensId")
    val groupId = call.argument<String>("lensGroupId")
    
    val intent = Intent(this, ARActivity::class.java).apply {
        putExtra("LENS_ID", lensId)
        putExtra("LENS_GROUP_ID", groupId)
    }
    startActivity(intent)
    result.success(true)
}
```

---

#### 3. Customize Screen → Save Custom Skin

**Current State**: ❌ Not Connected

**How to Connect**:

```dart
// lib/screens/customize_screen.dart

Future<void> _saveCustomSkin() async {
  // 1. Upload texture to Firebase Storage
  final textureUrl = await _uploadTextureToFirebase(
    textureData: _textureBytes,
  );
  
  // 2. Save metadata to BFF
  final response = await ApiClient().post(
    '/skins/create',
    {
      'shoeId': _selectedShoeId,
      'skinName': _skinName,
      'textureUrl': textureUrl,
    },
    requiresAuth: true,
  );
  
  if (response.statusCode == 201) {
    // Success
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Custom skin saved!')),
    );
  }
}
```

**API Calls**:
1. Direct to Firebase Storage (Flutter → Firebase Storage)
   - Upload texture file
   - Get download URL
   
2. Endpoint: `POST /api/skins/create`
   - Authentication: **Required** (Firebase token)
   - Body: `{shoeId, skinName, textureUrl}`
   - Response: Created CustomSkin object

---

#### 4. Closet Screen → Load Try-On History

**Current State**: ❌ Not Connected

**How to Connect**:

```dart
// lib/screens/closet стек.dart

Future<void> _loadHistory() async {
  final response = await ApiClient().get(
    '/tryon/history',
    requiresAuth: true,
  );
  
  if (response.statusCode == 200) {
    final List<dynamic> data = jsonDecode(response.body);
    _history = data.map((json) => TryOnHistory.fromJson(json)).toList();
    setState(() {});
  }
}
```

**API Call**:
- Endpoint: `GET /api/tryon/history`
- Authentication: **Required**
- Response: Array of last 5 TryOnHistory objects

---

#### 5. Outfit Match Screen → Get Recommendations

**Current State**: ❌ Not Connected

**How to Connect**:

```dart
// lib/screens/outfit_match_screen.dart

Future<void> _analyzeOutfit() async {
  // 1. Extract colors from outfit image
  final colors = await _extractColors(_outfitImage);
  
  // 2. Send to BFF for recommendations
  final response = await ApiClient().post(
    '/outfit/recommend',
    {'colors': colors},
    requiresAuth: true,
  );
  
  if (response.statusCode == 200) {
    final List<dynamic> data = jsonDecode(response.body);
    _recommendations = data.map((json) => Product.fromJson(json)).toList();
  }
}

// Upload outfit image for analysis
Future<void> _uploadAndAnalyze() async {
  // Upload to Firebase Storage
  final imageUrl = await _uploadImageToFirebase(_outfitImage);
  
  // Analyze via BFF
  final response = await ApiClient().post(
    '/outfit/analyze',
    {
      'outfitImageUrl': imageUrl,
      'dominantColors': _extractedColors,
    },
    requiresAuth: true,
  );
  
  // Get recommendations
  await _getRecommendations();
}
```

**API Calls**:
1. Endpoint: `POST /api/outfit/analyze`
   - Authentication: **Required**
   - Body: `{outfitImageUrl, dominantColors}`
   - Response: OutfitMatch object

2. Endpoint: `POST /api/outfit/recommend`
   - Authentication: **Required**
   - Body: `{colors: [string]}`
   - Response: Array of recommended Product objects

---

#### 6. Save Try-On Session → BFF

**Current State**: ❌ Not Connected

**How to Connect**:

```dart
// After user tries on shoe in AR

Future<void> _saveTryOn() async {
  // Capture snapshot
  final snapshotUrl = await _captureARSnapshot();
  
  // Save to BFF
  final response = await ApiClient().post(
    '/tryon/save',
    {
      'shoeId': _currentShoe.id,
      'snapshotUrl': snapshotUrl,
      'customSkinApplied': _hasCustomSkin,
    },
    requiresAuth: true,
  );
  
  if (response.statusCode == 201) {
    print('Try-on saved!');
  }
}
```

**API Call**:
- Endpoint: `POST /api/tryon/save`
- Authentication: **Required**
- Body: `{shoeId, snapshotUrl, customSkinApplied}`
- Response: Created TryOnHistory object

---

### Authentication Flow

```
┌─────────────┐
│ Flutter App │
└──────┬──────┘
       │ 1. User Login
       ▼
┌─────────────────┐
│ Firebase Auth   │
│ (Flutter SDK)   │
└──────┬──────────┘
       │ 2. Return ID Token
       ▼
┌─────────────────┐
│ Store in App    │
│ SharedPrefs     │
└──────┬──────────┘
       │ 3. Include in API Call
       │    Authorization: Bearer <token>
       ▼
┌─────────────────┐
│ BFF Middleware  │
│ verifyIdToken() │
└──────┬──────────┘
       │ 4. Extract UID
       ▼
┌─────────────────┐
│ Process Request │
│ with User UID   │
└─────────────────┘
```

**Implementation in Flutter**:

```dart
// lib/services/api_client.dart (Already Implemented)
Future<String?> _getAuthToken() async {
  final user = FirebaseAuth.instance.currentUser;
  return user?.getIdToken();
}

Future<Response> get(String endpoint, {bool requiresAuth = false}) async {
  final headers = {'Content-Type': 'application/json'};
  
  if (requiresAuth) {
    final token = await _getAuthToken();
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
  }
  
  // ... rest of implementation
}
```

---

## Future Implementation Guide

### 1. Repository Pattern Integration

**Status**: ❌ Not Implemented

**Create File**: `lib/repositories/catalog_repository.dart`

```dart
import 'dart:convert';
import '../services/api_client.dart';
import '../models/product_model.dart';

class CatalogRepository {
  final ApiClient _api = ApiClient();

  Future<List<Product>> getAllProducts() async {
    final response = await _api.get('/catalog');
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Product.fromJson(json)).toList();
    }
    
    throw Exception('Failed to load products: ${response.statusCode}');
  }

  Future<Product> getProductById(String id) async {
    final response = await _api.get('/catalog/$id');
    
    if (response.statusCode == 200) {
      return Product.fromJson(jsonDecode(response.body));
    }
    
    throw Exception('Failed to load product');
  }

  Future<List<Product>> searchProducts(String query) async {
    final response = await _api.get('/catalog/search?q=$query');
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Product.fromJson(json)).toList();
    }
    
    throw Exception('Search failed');
  }
}
```

**Implement for Other Domains**:
- `lib/repositories/tryon_repository.dart`
- `lib/repositories/skin_repository.dart`
- `lib/repositories/outfit_repository.dart`
- `lib/repositories/user_repository.dart`

---

### 2. Product Model Implementation

**Status**: ❌ Not Implemented

**Create File**: `lib/models/product_model.dart`

```dart
class Product {
  final String id;
  final String name;
  final String brand;
  final double price;
  final List<String> colors;
  final List<int> sizes;
  final String category;
  final String? modelUrl;
  final String? textureUrl;
  final String? thumbnailUrl;

  Product({
    required this.id,
    required this.name,
    required this.brand,
    required this.price,
    required this.colors,
    required this.sizes,
    required this.category,
    this.modelUrl,
    this.textureUrl,
    this.thumbnailUrl,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['_id'],
      name: json['name'],
      brand: json['brand'],
      price: (json['price'] as num).toDouble(),
      colors: List<String>.from(json['colors']),
      sizes: List<int>.from(json['sizes']),
      category: json['category'],
      modelUrl: json['modelUrl'],
      textureUrl: json['textureUrl'],
      thumbnailUrl: json['thumbnailUrl'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      '_id': id,
      'name': name,
      'brand': brand,
      'price': price,
      'colors': colors,
      'sizes': sizes,
      'category': category,
      'modelUrl': modelUrl,
      'textureUrl': textureUrl,
      'thumbnailUrl': thumbnailUrl,
    };
  }
}
```

---

### 3. AR Integration with BFF

**Status**: ❌ Not Connected

**Steps**:

1. **Create Method Channel Bridge**
```dart
// lib/services/ar_service.dart

import 'package:flutter/services.dart';

class ARService {
  static const platform = MethodChannel('solemate.ar/native');

  Future<void> loadShoeModel({
    required String modelUrl,
    String? textureUrl,
  }) async {
    try {
      await platform.invokeMethod('loadShoeModel', {
        'modelUrl': modelUrl,
        'textureUrl': textureUrl,
      });
    } catch (e) {
      print('Failed to load shoe model: $e');
    }
  }
}
```

2. **Update Kotlin Side**
```kotlin
// android/app/src/main/kotlin/.../MainActivity.kt

class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "solemate.ar/native")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "loadShoeModel" -> {
                        val modelUrl = call.argument<String>("modelUrl")
                        val textureUrl = call.argument<String>("textureUrl")
                        
                        // Load from URL into AR scene
                        loadModelIntoAR(modelUrl, textureUrl)
                        
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }
    }
    
    private fun loadModelIntoAR(modelUrl: String?, textureUrl: String?) {
        // Implement ARCore model loading
        // Download and place 3D model in scene
    }
}
```

3. **Use in AR Screen**
```dart
// lib/screens/ar_tryon_screen.dart

Future<void> _loadShoeForAR(Product shoe) async {
  await ARService().loadShoeModel(
    modelUrl: shoe.modelUrl!,
    textureUrl: shoe.textureUrl,
  );
}
```

---

### 4. File Upload Implementation

**Status**: ⚠️ Partial (BFF Ready, Flutter Missing)

**BFF Status**: ✅ Firebase Storage service implemented

**Flutter Implementation Needed**:

```dart
// lib/services/storage_service.dart

import 'package:firebase_storage/firebase_storage.dart';
import 'dart:io';

class StorageService {
  final FirebaseStorage _storage = FirebaseStorage.instance;

  Future<String> uploadTexture({
    required File file,
    required String shoeId,
    required String userId,
  }) async {
    final ref = _storage.ref()
      .child('skins')
      .child(userId)
      .child('${shoeId}_${DateTime.now().millisecondsSinceEpoch}.png');
    
    final uploadTask = ref.putFile(file);
    
    final snapshot = await uploadTask;
    final downloadUrl = await snapshot.ref.getDownloadURL();
    
    return downloadUrl;
  }

  Future<String> uploadOutfitImage({
    required File file,
    required String userId,
  }) async {
    final ref = _storage.ref()
      .child('outfits')
      .child(userId)
      .child('${DateTime.now().millisecondsSinceEpoch}.jpg');
    
    final uploadTask = ref.putFile(file);
    
    final snapshot = await uploadTask;
    final downloadUrl = await snapshot.ref.getDownloadURL();
    
    return downloadUrl;
  }

  Future<String> uploadSnapshot({
    required File file,
    required String userId,
  }) async {
    final ref = _storage.ref()
      .child('snapshots')
      .child(userId)
      .child('${DateTime.now().millisecondsSinceEpoch}.jpg');
    
    final uploadTask = ref.putFile(file);
    
    final snapshot = await uploadTask;
    final downloadUrl = await snapshot.ref.getDownloadURL();
    
    return downloadUrl;
  }
}
```

---

### 5. Color Extraction Implementation

**Status**: ❌ Not Implemented

**Option 1: Flutter-Side Processing**
```dart
// lib/services/color_extractor.dart

import 'package:image/image.dart' as img;
import 'dart:typed_data';
import 'package:flutter/services.dart';

class ColorExtractor {
  Future<List<String>> extractDominantColors(Uint8List imageBytes) async {
    final image = img.decodeImage(imageBytes);
    
    if (image == null) return [];
    
    // Extract color palette
    final colors = _extractColorsFromImage(image);
    
    // Convert to hex strings
    return colors.map((color) => _rgbToHex(color)).toList();
  }

  List<Color> _extractColorsFromImage(img.Image image) {
    // Implement k-means clustering or similar algorithm
    // Return top 3-5 dominant colors
  }

  String _rgbToHex(Color color) {
    // Convert RGB to hex
  }
}
```

**Option 2: BFF-Side Processing** (Requires Cloud Function)
```typescript
// backend/src/services/colorAnalysisService.ts

export async function analyzeImageColors(imageUrl: string): Promise<string[]> {
  // Download image from URL
  // Process with image analysis library
  // Return dominant colors
}
```

---

### 6. Error Handling in UI

**Status**: ❌ Not Implemented

**Implementation**:

```dart
// lib/widgets/api_error_handler.dart

class APIErrorHandler {
  static void handleError(BuildContext context, Object error) {
    String message = 'An error occurred';
    
    if (error is SocketException) {
      message = 'No internet connection';
    } else if (error is HttpException) {
      message = 'Server error: ${error.message}';
    } else if (error is FormatException) {
      message = 'Invalid data format';
    }
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }
}

// Usage in screens
try {
  final products = await catalogRepo.getAllProducts();
  setState(() => _products = products);
} catch (modal) {
  APIErrorHandler.handleError(context, e);
}
```

---

## API Reference

### Base URL
```
http://localhost:8080/api
```

### Authentication
Include Firebase ID token in Authorization header:
```
Authorization: Bearer <firebase_id_token>
```

### Complete Endpoint List

See `backend/docs/API.md` for detailed API documentation.

### Example Flutter Implementation

```dart
// Complete example: Loading products with error handling

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final CatalogRepository _repo = CatalogRepository();
  List<Product> _products = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  Future<void> _loadProducts() async {
    try {
      setState(() => _loading = true);
      final products = await _repo.getAllProducts();
      setState(() {
        _products = products;
        _loading = false;
      });
    } catch (e) {
      setState(() => _loading = false);
      APIErrorHandler.handleError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Center(child: CircularProgressIndicator());
    }

    return ListView.builder(
      itemCount: _products.length,
      itemBuilder: (context, index) {
        final product = _products[index];
        return ProductCard(product: product);
      },
    );
  }
}
```

---

## Troubleshooting

### Common Issues

1. **MongoDB Connection Failed**
   - Check MONGODB_URI in .env
   - Verify IP whitelist in MongoDB Atlas
   - Ensure credentials are correct

2. **Redis Connection Failed**
   - Check REDIS_URL in .env
   - Verify Redis Cloud credentials

3. **Firebase Auth Errors**
   - Ensure private key has escaped newlines (\n)
   - Verify all three Firebase env variables are set

4. **Port Already in Use**
   - Change PORT in .env
   - Or stop other services on port 8080

5. **Docker Build Fails**
   - Check TypeScript compilation errors
   - Verify all files are in correct locations

---

## Next Steps

1. ✅ Backend infrastructure complete
2. ⬜ Implement Flutter repositories
3. ⬜ Connect AR screen to BFF
4. ⬜ Implement file uploads
5. ⬜ Add color extraction
6. ⬜ Integrate error handling
7. ⬜ Deploy to production

---

**Last Updated**: October 28, 2025

---

## Future Update Instructions

To keep this documentation current as development progresses, follow this process:

### Update Prompt for AI Assistant

Copy and paste this prompt when requesting updates to the architecture documentation:

```
Please analyze the current state of the SoleMate project and update the BACKEND_ARCHITECTURE.md file with the following:

1. **Review Current Implementation Status**:
   - Check which phases/todos in docs/BACKEND_ARCHITECTURE.md are still marked as incomplete
   - Examine the actual codebase to verify current implementation state
   - Check for any new files, features, or integrations that have been added

2. **Update Implementation Status Section**:
   - Change completion percentages based on actual progress
   - Move items from "❌ Not Implemented" to "✅ ] Implemented" if completed
   - Add any new components that have been added since the last update
   - Update file listings if new files were created

3. **Update Frontend Integration Mapping**:
   - Review lib/repositories/, lib/models/, lib/services/ for new implementations
   - Check if repository pattern has been implemented
   - Verify if models have been created and connected
   - Update API call mappings if endpoints have changed

4. **Update Future Implementation Guide**:
   - Mark completed implementation steps with ✅
   - Remove or update code examples for completed features
   - Add new TODOs for any newly discovered gaps
   - Update estimated completion percentages

5. **Update Verification Guide**:
   - Add new verification steps for any newly implemented features
   - Update test commands if APIs have changed
   - Add troubleshooting for common issues encountered

6. **Review and Update**:
   - Check if all code examples are still accurate
   - Ensure file paths are correct
   - Verify API endpoints match actual implementation
   - Update architecture diagrams if structure changed

7. **Final Check**:
   - Ensure the "Last Updated" date is accurate
   - Verify all links and references are working
   - Make sure status indicators (✅, ⬜, ❌, ⚠️) are consistent

Please provide a summary of changes made and highlight any major implementation milestones achieved.
```

### Manual Update Checklist

Before updating, verify:

- [ ] Checked `lib/repositories/` for new repository files
- [ ] Checked `lib/models/` for new model files
- [ ] Checked `backend/src/` for new controllers/routes/services
- [ ] Tested any new API endpoints
- [ ] Verified Docker setup still works
- [ ] Checked if any new dependencies were added
- [ ] Reviewed git history for recent changes
- [ ] Tested Flutter integration with BFF
- [ ] Verified authentication flow still works
- [ ] Checked if any features from "Future Implementation" were completed

### Areas to Monitor for Changes

1. **Backend**: `backend/src/` directory structure
2. **Models**: New Mongoose schemas or TypeScript interfaces
3. **Controllers**: New API endpoints added
4. **Flutter**: New repositories, models, or services in `lib/`
5. **Integration**: Flutter screens connecting to BFF
6. **AR Module**: Kotlin integration with backend
7. **Storage**: Firebase Storage implementation progress
8. **Testing**: New test files or coverage improvements
9. **Documentation**: New READMEs, API docs, or guides

### Suggested Update Frequency

- **After completing each major feature**: Update immediately
- **After each sprint**: Weekly review
- **Before major releases**: Full audit
- **When onboarding new developers**: Ensure documentation is current

---

**Documentation Maintenance**: This file should be treated as a living document and updated continuously as the project evolves.

