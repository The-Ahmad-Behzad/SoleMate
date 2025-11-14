# SoleMate Backend API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication

Most endpoints require Firebase authentication. Include the Firebase ID token in the Authorization header:

```
Authorization: Bearer <firebase_id_token>
```

## Endpoints

### Health Check

#### GET /health
Check server health status

**Response:**
```json
{
  "ok": true,
  "service": "solemate-bff",
  "version": "0.1.0",
  "time": "2025-10-28T17:00:33.877Z"
}
```

---

### Catalog

#### GET /api/catalog
Get all products/shoes

**Response:**
```json
[
  {
    "_id": "6900f235777b45b41bafff98",
    "name": "Air Zoom Runner",
    "brand": "SoleMate",
    "price": 129.99,
    "colors": ["black", "white"],
    "sizes": [39, 40, 41, 42, 43],
    "category": "running",
    "thumbnailUrl": "/assets/images/shoes/shoe1.jpg",
    "createdAt": "2025-10-28T16:41:25.939Z",
    "updatedAt": "2025-10-28T16:41:25.939Z"
  }
]
```

#### GET /api/catalog/:id
Get specific shoe by ID

**Response:**
```json
{
  "_id": "6900f235777b45b41bafff98",
  "name": "Air Zoom Runner",
  "brand": "SoleMate",
  "price": 129.99,
  ...
}
```

#### GET /api/catalog/search?q=query
Search shoes by name

**Query Parameters:**
- `q` (required) - Search query

**Response:** Array of matching shoes

---

### Try-On History

#### POST /api/tryon/save
Save a try-on session

**Authentication:** Required

**Request Body:**
```json
{
  "shoeId": "6900f235777b45b41bafff98",
  "snapshotUrl": "https://...",
  "customSkinApplied": false
}
```

**Response:**
```json
{
  "_id": "...",
  "userId": "...",
  "shoeId": "6900f235777b45b41bafff98",
  "snapshotUrl": "https://...",
  "customSkinApplied": false,
  "createdAt": "2025-10-28T..."
}
```

#### GET /api/tryon/history
Get user's last 5 try-ons

**Authentication:** Required

**Response:** Array of try-on history records with populated shoeId

#### DELETE /api/tryon/:id
Delete a try-on record

**Authentication:** Required

**Response:** 204 No Content

---

### Custom Skins

#### POST /api/skins/create
Create a custom shoe skin

**Authentication:** Required

**Request Body:**
```json
{
  "shoeId": "6900f235777b45b41bafff98",
  "skinName": "My Custom Design",
  "textureUrl": "https://..."
}
```

**Response:** Created skin object

#### GET /api/skins
Get user's custom skins

**Authentication:** Required

**Response:** Array of custom skins with populated shoeId

#### PUT /api/skins/:id
Update a custom skin

**Authentication:** Required

**Request Body:**
```json
{
  "skinName": "Updated Name",
  "textureUrl": "https://..."
}
```

**Response:** Updated skin object

#### DELETE /api/skins/:id
Delete a custom skin

**Authentication:** Required

**Response:** 204 No Content

---

### Outfit Matching

#### POST /api/outfit/analyze
Analyze outfit colors

**Authentication:** Required

**Request Body:**
```json
{
  "outfitImageUrl": "https://...",
  "dominantColors": ["red", "blue"]
}
```

**Response:** Outfit match record

#### POST /api/outfit/recommend
Get shoe recommendations based on colors

**Authentication:** Required

**Request Body:**
```json
{
  "colors": ["red", "blue"]
}
```

**Response:** Array of recommended shoes

#### GET /api/outfit/history
Get past outfit matches

**Authentication:** Required

**Response:** Array of outfit match records with populated shoe recommendations

---

### User Profile

#### GET /api/user/profile
Get user profile

**Authentication:** Required

**Response:**
```json
{
  "_id": "...",
  "uid": "firebase_uid",
  "email": "user@example.com",
  "name": "John Doe",
  "preferences": {},
  "createdAt": "2025-10-28T...",
  "updatedAt": "2025-10-28T..."
}
```

#### PUT /api/user/profile
Update user profile

**Authentication:** Required

**Request Body:**
```json
{
  "name": "John Updated",
  "preferences": {
    "theme": "dark"
  }
}
```

**Response:** Updated user object

#### GET /api/user/stats
Get user statistics

**Authentication:** Required

**Response:**
```json
{
  "tryOnCount": 15,
  "userId": "firebase_uid"
}
```

---

## Error Responses

All endpoints may return these error responses:

### 400 Bad Request
```json
{
  "error": "Validation error message",
  "statusCode": 400
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "statusCode": 401
}
```

### 404 Not Found
```json
{
  "error": "Resource not found",
  "statusCode": 404
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error",
  "statusCode": 500
}
```

