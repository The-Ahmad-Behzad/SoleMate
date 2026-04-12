import redis from '../config/redis.js';

const DEFAULT_TTL = 3600;

export class CacheService {
  async get<T>(key: string): Promise<T | null> {
    try {
      const value = await redis.get(key);
      return value ? (JSON.parse(value) as T) : null;
    } catch (err) {
      console.error('Redis get error:', err);
      return null;
    }
  }

  async set(key: string, value: unknown, ttl = DEFAULT_TTL): Promise<void> {
    try {
      await redis.set(key, JSON.stringify(value), 'EX', ttl);
    } catch (err) {
      console.error('Redis set error:', err);
    }
  }

  async del(key: string): Promise<void> {
    try {
      await redis.del(key);
    } catch (err) {
      console.error('Redis del error:', err);
    }
  }

  async getCatalog(): Promise<unknown[] | null> {
    return this.get('catalog:all');
  }

  async setCatalog(data: unknown[], ttl = DEFAULT_TTL): Promise<void> {
    await this.set('catalog:all', data, ttl);
  }

  async clearCatalog(): Promise<void> {
    await this.del('catalog:all');
  }

  async getUserSession(userId: string): Promise<unknown | null> {
    return this.get(`session:${userId}`);
  }

  async setUserSession(userId: string, data: unknown, ttl = DEFAULT_TTL): Promise<void> {
    await this.set(`session:${userId}`, data, ttl);
  }

  async warmCache(): Promise<void> {
    console.log('Cache warming not implemented yet');
  }
}

export const cacheService = new CacheService();


