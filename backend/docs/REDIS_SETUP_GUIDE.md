# Redis Database Setup Guide

This guide will help you create a new Redis database that matches your current SoleMate application configuration.

## Current Redis Usage Analysis

Based on your codebase, here's what your application uses:

### **Redis Commands Used:**
- `GET` - Retrieve string values
- `SET` with `EX` (expiration) - Store string values with TTL
- `DEL` - Delete keys
- `INFO` - Get server information

### **Data Structures:**
- **Only STRING type** - No hashes, lists, sets, or sorted sets
- Values are JSON strings (serialized objects)
- Keys use namespace patterns:
  - `catalog:all` - Cached product catalog
  - `session:{userId}` - User session data
  - `keepalive:*` - Keep-alive ping data

### **Connection Configuration:**
- Library: `ioredis` (Node.js)
- Connection format: `redis://[username]:[password]@[host]:[port]`
- Default TTL: 3600 seconds (1 hour)
- Lazy connection: enabled
- Max retries: 3

---

## Redis Cloud Setup Instructions

### **Step 1: Create New Database**

1. Log in to [Redis Cloud](https://redis.com/try-free/)
2. Navigate to **Databases** → **New Database**
3. Select **Free** or **Fixed** plan (depending on your needs)

### **Step 2: Database Configuration**

#### **Basic Settings:**

| Setting | Value | Notes |
|---------|-------|-------|
| **Name** | `solemate-redis` | (or any name you prefer) |
| **Region** | `ap-south-1` | (or closest to your backend) |
| **Type** | **Redis** | (not Redis Cluster - your app uses single instance) |
| **Version** | **7.x** or **6.x** | (any recent stable version) |

#### **Memory & Performance:**

| Setting | Recommended Value | Notes |
|---------|-------------------|-------|
| **Memory** | **30 MB** (Free tier) or **100 MB+** | Your app only caches small JSON strings |
| **Replication** | **Disabled** (Free tier) or **Enabled** (Paid) | Not required for caching |
| **Persistence** | **Disabled** | Your app uses Redis as volatile cache only |

#### **Security Settings:**

| Setting | Value | Notes |
|---------|-------|-------|
| **Default User** | `default` | (or create custom username) |
| **Password** | Generate strong password | Save this - you'll need it for connection string |
| **ACL** | **Disabled** (Free tier) | Not needed for basic setup |

#### **Network Settings:**

| Setting | Value | Notes |
|---------|-------|-------|
| **Public Endpoint** | **Enabled** | Required for external connections |
| **Private Endpoint** | Optional | Only if backend is in same cloud |
| **TLS/SSL** | **Optional** | Your current config doesn't require it, but recommended for production |

### **Step 3: Get Connection Details**

After creating the database, you'll receive:

1. **Public Endpoint** (hostname)
2. **Port** (usually 6379 or custom)
3. **Default User Password**

### **Step 4: Build Connection String**

Format: `redis://[username]:[password]@[host]:[port]`

**Example:**
```
redis://default:your_password_here@redis-12345.c264.ap-south-1-1.ec2.redns.redis-cloud.com:12345
```

**Components:**
- `redis://` - Protocol
- `default` - Username (or your custom username)
- `your_password_here` - Password from Redis Cloud
- `redis-12345...` - Public endpoint hostname
- `12345` - Port number

### **Step 5: Update Environment Variables**

Update your `.env` file in the `backend` directory:

```env
REDIS_URL=redis://default:your_new_password@your_new_host:your_new_port
```

**Important:** Replace with your actual credentials from Step 3.

---

## Verification Steps

### **1. Test Connection**

Run the keep-alive script to verify connection:

```bash
cd backend
npm run keep-redis-active
```

Expected output:
```
🔄 Connecting to Redis...
✅ Connected to Redis
📤 Pushing data to Redis...
  ✓ Set key: keepalive:ping
  ✓ Set key: keepalive:timestamp
  ✓ Set key: keepalive:status
  ✓ Set key: keepalive:last_activity
✅ Verification successful - data retrieved from Redis
📊 Redis version: 7.x.x
✅ Successfully pushed data to Redis. Instance should remain active.
```

### **2. Test Application**

Start your backend server:

```bash
cd backend
npm run dev
```

Check for Redis connection message:
```
✅ Redis connected
```

### **3. Test Cache Functionality**

Make a request to your catalog endpoint:
```bash
curl http://localhost:8080/api/catalog
```

The first request should fetch from MongoDB and cache in Redis. Subsequent requests should be served from Redis cache.

---

## Minimum Requirements Summary

When creating your new Redis database, ensure:

✅ **Required:**
- Redis (not Redis Cluster)
- STRING data type support (standard)
- TTL/Expiration support (standard)
- Public endpoint enabled
- Password authentication

❌ **Not Required:**
- Redis Modules (RediSearch, RedisJSON, etc.)
- Persistence/AOF (volatile cache only)
- Replication (optional)
- TLS/SSL (optional but recommended)
- Redis Cluster mode
- Advanced data structures (hashes, lists, sets)

---

## Cost Considerations

### **Free Tier (Redis Cloud):**
- 30 MB memory
- 1 database
- No persistence
- Suitable for development/testing

### **Paid Plans:**
- More memory (100 MB - several GB)
- Multiple databases
- Persistence options
- Better performance
- Recommended for production

---

## Troubleshooting

### **Connection Issues:**

1. **"ENOTFOUND" error:**
   - Verify hostname is correct
   - Check if database is active (not suspended)
   - Wait 1-2 minutes after creation for DNS propagation

2. **"Authentication failed":**
   - Verify password is correct (no extra spaces)
   - Check username (usually "default")
   - Ensure password is URL-encoded if it contains special characters

3. **"Connection refused":**
   - Verify port number
   - Check if public endpoint is enabled
   - Verify firewall/network settings

### **Performance Issues:**

- If cache is slow, consider upgrading memory/plan
- Monitor memory usage in Redis Cloud dashboard
- Set appropriate TTL values (current: 3600 seconds)

---

## Migration Notes

If migrating from old Redis instance:

1. **No data migration needed** - Redis is used as volatile cache
2. **Update REDIS_URL** in `.env` file
3. **Restart backend server**
4. **Cache will rebuild** automatically as requests come in

---

## Keep Redis Active

To prevent Redis from being suspended due to inactivity, run periodically:

```bash
cd backend
npm run keep-redis-active
```

Or set up a cron job/scheduled task to run this weekly.

---

## Additional Resources

- [Redis Cloud Documentation](https://docs.redis.com/)
- [ioredis Documentation](https://github.com/redis/ioredis)
- [Redis Commands Reference](https://redis.io/commands/)

