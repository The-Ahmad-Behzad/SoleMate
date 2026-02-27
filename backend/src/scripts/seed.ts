import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// Load .env file explicitly
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
dotenv.config({ path: join(__dirname, '../../.env') });

import { connectToDatabase, disconnectFromDatabase } from '../config/database.js';
import { ProductModel } from '../models/Product.js';

async function seed() {
  await connectToDatabase();

  const count = await ProductModel.estimatedDocumentCount();
  if (count > 0) {
    console.log('Products already exist, skipping seed.');
    await disconnectFromDatabase();
    return;
  }

  const mockProducts = [
    {
      name: 'Air Zoom Runner',
      brand: 'SoleMate',
      price: 129.99,
      colors: ['black', 'white'],
      sizes: [39, 40, 41, 42, 43],
      category: 'running',
      thumbnailUrl: 'https://solemate-assets-placeholder.s3.amazonaws.com/shoes/shoe1.jpg',
      modelUrl: 'https://solemate-assets-placeholder.s3.amazonaws.com/models/shoe1.glb',
      arLensId: 'placeholder_lens_id_1',
      arLensGroupId: 'placeholder_group_id_1'
    },
    {
      name: 'Street Classic',
      brand: 'SoleMate',
      price: 89.99,
      colors: ['blue', 'white'],
      sizes: [40, 41, 42, 43, 44],
      category: 'casual',
      thumbnailUrl: 'https://solemate-assets-placeholder.s3.amazonaws.com/shoes/shoe2.jpg',
      modelUrl: 'https://solemate-assets-placeholder.s3.amazonaws.com/models/shoe2.glb',
      arLensId: 'placeholder_lens_id_2',
      arLensGroupId: 'placeholder_group_id_1'
    },
    {
      name: 'Court Pro',
      brand: 'SoleMate',
      price: 109.99,
      colors: ['red', 'black'],
      sizes: [39, 40, 41, 42, 43, 44],
      category: 'sports',
      thumbnailUrl: 'https://solemate-assets-placeholder.s3.amazonaws.com/shoes/shoe3.jpg',
      modelUrl: 'https://solemate-assets-placeholder.s3.amazonaws.com/models/shoe3.glb',
      arLensId: 'placeholder_lens_id_3',
      arLensGroupId: 'placeholder_group_id_1'
    }
  ];

  await ProductModel.insertMany(mockProducts);
  console.log('Seeded products:', mockProducts.length);
  await disconnectFromDatabase();
}

seed().catch(async (err) => {
  console.error(err);
  await disconnectFromDatabase();
  process.exit(1);
});


