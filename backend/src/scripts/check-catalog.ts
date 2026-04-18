import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import mongoose from 'mongoose';
import { ProductModel } from '../models/Product.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
dotenv.config({ path: join(__dirname, '../../.env') });

async function checkCatalog() {
  try {
    const mongoUri = process.env.MONGODB_URI;
    if (!mongoUri) {
      console.error('MONGODB_URI not found in environment');
      process.exit(1);
    }

    console.log('Connecting to MongoDB...');
    await mongoose.connect(mongoUri);
    console.log('Connected.');

    const count = await ProductModel.countDocuments();
    console.log(`\nTotal products in database: ${count}`);

    if (count > 0) {
      const products = await ProductModel.find().limit(5).lean();
      console.log('\nLast 5 products:');
      products.forEach((p: any, i) => {
        console.log(`${i+1}. ${p.name} (${p.brand}) - Price: ${p.price}`);
      });
    } else {
      console.log('\nNo products found. Seed script might have failed or connected to a different DB.');
    }

    await mongoose.disconnect();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err);
    process.exit(1);
  }
}

checkCatalog();
