import dotenv from 'dotenv';
import mongoose from 'mongoose';
import Redis from 'ioredis';
import { S3Client, ListObjectsV2Command } from '@aws-sdk/client-s3';

dotenv.config();

async function testConnections() {
    console.log('--- BFF Connection Diagnostics ---');

    console.log('\n[MongoDB] Testing connection...');
    try {
        await mongoose.connect(process.env.MONGODB_URI);
        console.log('[MongoDB] ✅ Connected successfully.');
        await mongoose.disconnect();
    } catch (err) {
        console.error('[MongoDB] ❌ Connection failed:', err.message);
    }

    console.log('\n[Redis] Testing connection...');
    try {
        const redis = new Redis(process.env.REDIS_URL, {
            connectTimeout: 5000,
            maxRetriesPerRequest: 1
        });
        
        await new Promise((resolve, reject) => {
            redis.on('ready', resolve);
            redis.on('error', reject);
        });
        console.log('[Redis] ✅ Connected successfully.');
        await redis.quit();
    } catch (err) {
        console.error('[Redis] ❌ Connection failed:', err.message);
    }

    console.log('\n[S3] Testing connection...');
    const isMockS3 = (process.env.AWS_ACCESS_KEY_ID || 'PLACEHOLDER_KEY') === 'PLACEHOLDER_KEY';
    if (isMockS3) {
        console.log('[S3] ⚠️ Using local mock (PLACEHOLDER_KEY detected).');
    } else {
        try {
            const s3Client = new S3Client({
                region: process.env.AWS_REGION || 'us-east-1',
                credentials: {
                    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
                    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
                },
            });
            const command = new ListObjectsV2Command({ Bucket: process.env.AWS_BUCKET_NAME, MaxKeys: 1 });
            await s3Client.send(command);
            console.log(`[S3] ✅ Connected and verified access to bucket: ${process.env.AWS_BUCKET_NAME}`);
        } catch (err) {
            console.error('[S3] ❌ Connection failed:', err.message);
        }
    }
    
    console.log('\n--- Diagnostics Complete ---');
    process.exit(0);
}

testConnections();
