# SoleMate Backend-for-Frontend (BFF)

Node.js/Express backend API for the SoleMate AR footwear try-on application.

## Features

- RESTful API for catalog, try-on history, custom skins, outfit matching, and user profiles
- MongoDB Atlas integration for persistent data storage
- Redis Cloud for caching and session management
- Firebase Admin for authentication
- TypeScript with Express
- Docker containerization support

## Prerequisites

Before running the backend, you need:

1. **MongoDB Atlas Account** - Get connection URI
2. **Redis Cloud Account** - Get connection URL
3. **Firebase Admin SDK** - Service account credentials

## Environment Setup

1. Copy the environment template:
```bash
cp env.example .env
```

2. Fill in your credentials in `.env`:
```env
MONGODB_URI=mongodb+srv://...
REDIS_URL=redis://...
FIREBASE_PROJECT_ID=solemate-app-d4560
FIREBASE_CLIENT_EMAIL=...
FIREBASE_PRIVATE_KEY="..."
```

## Installation

```bash
cd backend
npm install
```

## Running the Server

### Development Mode
```bash
npm run dev
```

### Production Mode
```bash
npm run build
npm start
```

### With Docker
```bash
docker-compose up
```

## Seeding Database

To populate with mock shoe data:

```bash
npm run seed
```

Finished: backend now provides REST endpoints for the Flutter app.

## API Endpoints

### Catalog
- `GET /api/catalog` - Get all shoes
- `GET /api/catalog/:id` - Get specific shoe
- `GET /api/catalog/search?q=query` - Search shoes

### Try-On History
- `POST /api/tryon/save` - Save try-on session
- `GET /api/tryon/history` - Get user's last 5 try-ons
- `DELETE /api/tryon/:id` - Remove from history

### Custom Skins
- `POST /api/skins/create` - Create custom skin
- `GET /api/skins` - Get user's skins
- `PUT /api/skins/:id` - Update skin
- `DELETE /api/skins/:id` - Delete skin

### Outfit Matching
- `POST /api/outfit/analyze` - Analyze outfit colors
- `POST /api/outfit/recommend` - Get shoe recommendations
- `GET /api/outfit/history` - Get past matches

### User Profile
- `GET /api/user/profile` - Get user profile
- `PUT /api/user/profile` - Update profile
- `GET /api/user/stats` - Get usage statistics

## Health Check

```bash
curl http://localhost:8080/health
```

## Running with Docker

### Build and Run
```bash
docker-compose up --build
```

### Stop
```bash
docker-compose down
```

### View Logs
```bash
docker-compose logs -f bff
```

### Health Check
The container includes automatic health checks that verify the service is running.

## Project Structure

```
backend/
├── src/
│   ├── config/          # Configuration files (database, redis, firebase)
│   ├── controllers/     # Request handlers
│   ├── middleware/      # Express middleware (auth, error handling)
│   ├── models/          # Mongoose schemas
│   ├── routes/          # API routes
│   ├── services/        # Business logic
│   ├── scripts/         # Utility scripts
│   ├── utils/           # Utility functions
│   └── index.ts         # Application entry point
├── docs/
│   └── API.md           # Complete API documentation
├── Dockerfile
├── .dockerignore
├── env.example
├── package.json
├── tsconfig.json
└── README.md
```

## Troubleshooting

### MongoDB Connection Issues
- Verify `MONGODB_URI` in `.env` is correct
- Check IP whitelist in MongoDB Atlas
- Ensure database user has proper permissions

### Redis Connection Issues
- Verify `REDIS_URL` in `.env` is correct
- Check Redis Cloud credentials

### Firebase Auth Issues
- Verify all three Firebase credentials are set in `.env`
- Ensure private key includes escaped newlines (`\n`)

### Port Already in Use
- Change `PORT` in `.env` to another value
- Or stop other services using port 8080


