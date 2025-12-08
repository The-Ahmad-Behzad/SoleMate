# Redis Database Creation - Quick Checklist

Use this checklist when creating your new Redis database in Redis Cloud.

## ✅ Database Configuration Checklist

### **Basic Settings:**
- [ ] **Name:** `solemate-redis` (or your preferred name)
- [ ] **Type:** Select **"Redis"** (NOT Redis Cluster)
- [ ] **Region:** Choose closest to your backend (e.g., `ap-south-1`)
- [ ] **Version:** Select **7.x** or **6.x** (any recent stable version)

### **Memory & Performance:**
- [ ] **Memory:** Minimum **30 MB** (Free tier) or **100 MB+** (Recommended)
- [ ] **Replication:** Disabled (Free tier) or Enabled (Paid - optional)
- [ ] **Persistence:** **Disabled** (volatile cache only)

### **Security:**
- [ ] **Default User:** `default` (or custom username)
- [ ] **Password:** Generate and **SAVE SECURELY** (you'll need it for connection string)
- [ ] **ACL:** Disabled (Free tier) or Optional (Paid)

### **Network:**
- [ ] **Public Endpoint:** **ENABLED** (required)
- [ ] **Port:** Note the port number (usually 6379 or custom)
- [ ] **TLS/SSL:** Optional (not required but recommended for production)

## 📝 Connection String Format

After creation, you'll receive:
- **Hostname:** `redis-XXXXX.c264.ap-south-1-1.ec2.redns.redis-cloud.com`
- **Port:** `XXXXX`
- **Password:** `your_password`

**Connection String:**
```
redis://default:your_password@redis-XXXXX.c264.ap-south-1-1.ec2.redns.redis-cloud.com:XXXXX
```

## 🔧 Update Your .env File

```env
REDIS_URL=redis://default:your_password@redis-XXXXX.c264.ap-south-1-1.ec2.redns.redis-cloud.com:XXXXX
```

## ✅ Verification

1. [ ] Run: `cd backend && npm run keep-redis-active`
2. [ ] Should see: `✅ Connected to Redis`
3. [ ] Start backend: `npm run dev`
4. [ ] Should see: `✅ Redis connected` in logs

## ❌ What You DON'T Need

- ❌ Redis Cluster mode
- ❌ Redis Modules (RediSearch, RedisJSON, etc.)
- ❌ Persistence/AOF (volatile cache only)
- ❌ Advanced data structures (hashes, lists, sets) - only STRING type needed

## 📚 Full Guide

See `REDIS_SETUP_GUIDE.md` for detailed instructions and troubleshooting.

