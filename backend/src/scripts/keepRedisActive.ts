import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// Load .env file explicitly
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
dotenv.config({ path: join(__dirname, '../../.env') });

import redis from '../config/redis.js';

async function keepRedisActive() {
  console.log('🔄 Checking Redis connection...');
  
  const client = redis.instance;
  if (!client) {
    console.error('❌ Redis client not available. Check REDIS_URL in .env file.');
    process.exit(1);
  }

  try {
    // Check if already connected
    const status = client.status;
    if (status === 'ready' || status === 'connect') {
      console.log('✅ Redis already connected');
    } else {
      // Try to connect if not already connected
      console.log('🔄 Connecting to Redis...');
      let connected = false;
      let retries = 3;
      
      while (!connected && retries > 0) {
        try {
          // Check current status before connecting
          const currentStatus = client.status;
          if (currentStatus === 'ready' || currentStatus === 'connect') {
            connected = true;
            console.log('✅ Redis already connected');
            break;
          }
          
          // Only try to connect if not already connecting
          if (currentStatus !== 'connecting') {
            await client.connect();
          }
          connected = true;
          console.log('✅ Connected to Redis');
        } catch (connectErr: any) {
          // If already connecting/connected, that's fine - check status
          if (connectErr.message?.includes('already connecting') || 
              connectErr.message?.includes('already connected') ||
              client.status === 'ready' || 
              client.status === 'connect') {
            connected = true;
            console.log('✅ Redis connection already established');
            break;
          }
          retries--;
          if (retries > 0) {
            console.log(`⚠️  Connection attempt failed, retrying... (${retries} attempts left)`);
            await new Promise(resolve => setTimeout(resolve, 2000)); // Wait 2 seconds
          } else {
            throw connectErr;
          }
        }
      }
    }

    // Push some test data to keep Redis active
    const timestamp = new Date().toISOString();
    const testData = {
      timestamp,
      message: 'Keep-alive ping',
      service: 'solemate-backend',
      version: '1.0.0'
    };

    // Set multiple keys to ensure activity
    const keys = [
      'keepalive:ping',
      'keepalive:timestamp',
      'keepalive:status'
    ];

    console.log('📤 Pushing data to Redis...');
    
    for (const key of keys) {
      await client.set(key, JSON.stringify({
        ...testData,
        key,
        updatedAt: timestamp
      }), 'EX', 86400); // 24 hour TTL
      console.log(`  ✓ Set key: ${key}`);
    }

    // Also set a simple string value
    await client.set('keepalive:last_activity', timestamp, 'EX', 86400);
    console.log('  ✓ Set key: keepalive:last_activity');

    // Verify by reading back one key
    const verifyKey = await client.get('keepalive:ping');
    if (verifyKey) {
      console.log('✅ Verification successful - data retrieved from Redis');
      console.log(`   Sample data: ${verifyKey.substring(0, 100)}...`);
    }

    // Get Redis info to show it's active
    const info = await client.info('server');
    const versionMatch = info.match(/redis_version:([^\r\n]+)/);
    if (versionMatch) {
      console.log(`📊 Redis version: ${versionMatch[1]}`);
    }

    console.log('✅ Successfully pushed data to Redis. Instance should remain active.');
    
    // Don't close connection - it might be shared with the main server
    // The connection will remain open for the server to use
    console.log('✅ Keep-alive complete. Connection remains open for server use.');
    
  } catch (err) {
    console.error('❌ Error interacting with Redis:', err);
    // Don't close connection on error - it might be shared
    process.exit(1);
  }
}

keepRedisActive().catch((err) => {
  console.error('Fatal error:', err);
  process.exit(1);
});

