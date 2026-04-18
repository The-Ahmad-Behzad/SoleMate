import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { cacheService } from '../services/cacheService.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
dotenv.config({ path: join(__dirname, '../../.env') });

async function clearCache() {
  try {
    console.log('Clearing catalog cache...');
    await cacheService.clearCatalog();
    console.log('Cache cleared successfully.');
    process.exit(0);
  } catch (err) {
    console.error('Error clearing cache:', err);
    process.exit(1);
  }
}

clearCache();
