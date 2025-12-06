import Redis from 'ioredis';

let redis: Redis | null = null;
let redisInitialized = false;

function initializeRedis() {
  if (redisInitialized) {
    return redis;
  }

  const redisUrl = process.env.REDIS_URL;

  if (!redisUrl) {
    console.warn('⚠️  REDIS_URL not set in environment variables. Redis functionality will be disabled.');
    redisInitialized = true;
    return null;
  }

  try {
    redis = new Redis(redisUrl, {
      maxRetriesPerRequest: 3,
      retryStrategy: (times) => {
        const delay = Math.min(times * 50, 2000);
        return delay;
      },
      enableOfflineQueue: true, // Allow queuing commands while connecting
      lazyConnect: false, // Connect immediately
    });

    redis.on('connect', () => {
      console.log('✅ Redis connected');
    });

    redis.on('error', (err) => {
      console.error('Redis error:', err);
    });

    redis.on('close', () => {
      console.warn('Redis connection closed');
    });

    redisInitialized = true;
    return redis;
  } catch (err) {
    console.error('Failed to initialize Redis:', err);
    redisInitialized = true;
    return null;
  }
}

// Lazy initialization wrapper
export default {
  get instance() {
    return initializeRedis();
  },
  
  async get(key: string): Promise<string | null> {
    const client = initializeRedis();
    if (!client) return null;
    try {
      return await client.get(key);
    } catch (err) {
      console.error('Redis get error:', err);
      return null;
    }
  },

  async set(key: string, value: string, ...args: unknown[]): Promise<string | null> {
    const client = initializeRedis();
    if (!client) return null;
    try {
      return await client.set(key, value, ...args);
    } catch (err) {
      console.error('Redis set error:', err);
      return null;
    }
  },

  async del(key: string): Promise<number> {
    const client = initializeRedis();
    if (!client) return 0;
    try {
      return await client.del(key);
    } catch (err) {
      console.error('Redis del error:', err);
      return 0;
    }
  },
};
