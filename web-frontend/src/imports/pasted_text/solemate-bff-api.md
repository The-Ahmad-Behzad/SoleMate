# SoleMate Platform: BFF API Specification

This document details the Backend-for-Frontend (BFF) endpoints, their JSON input/output structures, and their mapping to frontend components.

---

## 1. Consumer API (Mobile App)

### 1.1 Catalog Service
**Mapping**: Connects to `CatalogScreen` and `LandingScreen`.

#### `GET /api/catalog`
- **Purpose**: Fetch all shoes for browsing.
- **Output**:
```json
[
  {
    "id": "65f1a2b3c4d5e6f7a8b9c0d1",
    "name": "Air Max 270",
    "brand": "Nike",
    "price": 150.0,
    "thumbnailUrl": "https://s3.amazonaws.com/solemate/thumbnails/nike_air_max.png",
    "modelUrl": "https://s3.amazonaws.com/solemate/models/nike_air_max.glb",
    "category": "Running"
  }
]
```

#### `GET /api/catalog/:id`
- **Purpose**: Fetch details for a specific shoe.
- **Output**:
```json
{
  "id": "65f1a2b3c4d5e6f7a8b9c0d1",
  "name": "Air Max 270",
  "brand": "Nike",
  "description": "Premium cushioning for daily comfort.",
  "price": 150.0,
  "colors": ["Black", "White", "Volt"],
  "modelUrl": "https://s3.amazonaws.com/solemate/models/nike_air_max.glb"
}
```

---

### 1.2 Try-On & Closet Service
**Mapping**: Connects to `ARTryOnScreen` and `ClosetScreen`.

#### `POST /api/tryon/save`
- **Purpose**: Save a try-on session with a snapshot.
- **Input (Multipart/Form-Data)**:
```json
{
  "shoeId": "65f1a2b3c4d5e6f7a8b9c0d1",
  "customSkinApplied": false,
  "snapshot": "BinaryFileContent"
}
```
- **Output**:
```json
{
  "id": "75g2b3c4d5e6f7a8b9c0d1e2",
  "userId": "user_123",
  "shoeId": "65f1a2b3c4d5e6f7a8b9c0d1",
  "snapshotUrl": "https://s3.amazonaws.com/solemate/snapshots/user_123/1234567.png",
  "createdAt": "2024-05-12T15:00:00Z"
}
```

#### `GET /api/tryon/history`
- **Purpose**: Fetch user's saved try-ons.
- **Output**:
```json
[
  {
    "id": "75g2b3c4d5e6f7a8b9c0d1e2",
    "shoeId": {
      "name": "Air Max 270",
      "thumbnailUrl": "..."
    },
    "snapshotUrl": "...",
    "createdAt": "2024-05-12T15:00:00Z"
  }
]
```

---

### 1.3 AI Outfit Match Service
**Mapping**: Connects to `OutfitMatchScreen`.

#### `POST /api/outfit/recommend-shoes`
- **Purpose**: Analyze outfit image to find matching shoes.
- **Input (Multipart/Form-Data)**:
```json
{
  "outfitImage": "BinaryFileContent",
  "gender": "unisex"
}
```
- **Output**:
```json
{
  "detectedStyle": "Streetwear",
  "detectedColors": ["black", "grey"],
  "suggestedShoeColor": "white",
  "recommendations": [
    {
      "id": "...",
      "name": "Yeezy Boost 350",
      "brand": "Adidas",
      "matchScore": 0.95
    }
  ]
}
```

#### `POST /api/outfit/recommend-outfit-for-shoe`
- **Purpose**: Generate style advice for a specific shoe.
- **Input (Multipart/Form-Data)**:
```json
{
  "file": "BinaryFileContent"
}
```
- **Output**:
```json
{
  "styleAdvice": "This shoe pairs perfectly with slim-fit indigo jeans and a neutral oversized hoodie.",
  "suggestedColors": ["navy", "beige", "white"],
  "outfitCategory": "Casual"
}
```

---

### 1.4 Custom Skin Service
**Mapping**: Connects to `CustomizeScreen`.

#### `POST /api/skins/request-redesign`
- **Purpose**: Submit a custom redesign request with reference images.
- **Input (Multipart/Form-Data)**:
```json
{
  "shoeId": "65f1a2b3c4d5e6f7a8b9c0d1",
  "description": "Please add a galaxy pattern to the sole.",
  "primaryColor": "#000033",
  "secondaryColor": "#FF00FF",
  "images": ["File1", "File2"]
}
```
- **Output**:
```json
{
  "id": "85h3b4c5d6e7f8a9b0c1d2e3",
  "status": "new",
  "imageUrls": ["https://s3.../img1.png"],
  "createdAt": "2024-05-12T15:30:00Z"
}
```

---

## 2. Seller API (Dashboard)

### 2.1 Inventory (GLB) Service
**Mapping**: Connects to `UploadPage` and `ModelsPage`.

#### `POST /api/seller/upload-url`
- **Purpose**: Get presigned S3 URL for GLB upload.
- **Input**:
```json
{
  "fileName": "shoe_v1.glb",
  "contentType": "model/gltf-binary",
  "shoeName": "Jordan 1",
  "brand": "Nike"
}
```
- **Output**:
```json
{
  "presignedUrl": "https://s3.amazon.../sig=...",
  "s3Key": "shoes/nike/jordan-1-12345.glb",
  "s3Url": "https://s3.amazon.../shoes/nike/jordan-1-12345.glb"
}
```

#### `POST /api/seller/uploads/confirm`
- **Purpose**: Confirm successful S3 upload to DB.
- **Input**:
```json
{
  "s3Key": "shoes/nike/jordan-1-12345.glb",
  "s3Url": "https://s3.amazon.../jordan-1-12345.glb",
  "shoeName": "Jordan 1",
  "brand": "Nike",
  "description": "Iconic basketball shoe.",
  "scale": [1.0, 1.0, 1.0],
  "positionOffset": [0, 0, 0],
  "rotationOffset": [0, 0, 0]
}
```
- **Output**:
```json
{
  "id": "95i4b5c6d7e8f9a0b1c2d3e4",
  "status": "pending",
  "createdAt": "2024-05-12T16:00:00Z"
}
```

---

### 2.2 Design Request Service
**Mapping**: Connects to `SkinRequestsPage` and `SkinRequestDetailPage`.

#### `GET /api/skin-requests`
- **Purpose**: Fetch all design requests for the seller.
- **Output**:
```json
{
  "requests": [
    {
      "id": "85h3b4c5d6e7f8a9b0c1d2e3",
      "userEmail": "user@example.com",
      "shoeName": "Air Max 270",
      "status": "new",
      "createdAt": "..."
    }
  ],
  "total": 1,
  "statusCounts": { "new": 1, "viewed": 0 }
}
```

#### `PATCH /api/skin-requests/:id`
- **Purpose**: Update request status and add seller notes.
- **Input**:
```json
{
  "status": "in_progress",
  "sellerNotes": "Starting work on the galaxy texture."
}
```
- **Output**:
```json
{
  "id": "85h3b4c5d6e7f8a9b0c1d2e3",
  "status": "in_progress",
  "sellerNotes": "...",
  "updatedAt": "2024-05-12T16:30:00Z"
}
```

---

## 3. Component-API Dependency Map

| Component / Feature | API Endpoint | Responsibility |
| :--- | :--- | :--- |
| **LandingScreen Hero** | `GET /api/catalog` | Provides initial "Popular" shoe data. |
| **AR Camera View** | `POST /api/tryon/save` | Persists the snapshot and shoe ID after try-on. |
| **Closet Grid** | `GET /api/tryon/history` | Hydrates the personal shoe collection list. |
| **AI Analyze Button** | `POST /api/outfit/recommend-shoes` | Sends outfit photo for Vision analysis and shoe matching. |
| **Rec Outfit Button** | `POST /api/outfit/recommend-outfit-for-shoe` | Sends shoe photo for style generation. |
| **Customize Drawing** | `POST /api/skins/request-redesign` | Submits annotations and colors to the seller. |
| **Seller Dashboard Stats**| `GET /api/skin-requests` | Provides aggregated counts (New/Pending) for cards. |
| **S3 Upload Wizard** | `POST /api/seller/upload-url` | Negotiates secure S3 access for heavy GLB files. |
| **Fulfillment Management**| `PATCH /api/skin-requests/:id` | Syncs seller workflow status back to the mobile user. |