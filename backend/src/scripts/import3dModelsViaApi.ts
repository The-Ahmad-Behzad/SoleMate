import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: path.join(__dirname, '../../.env') });

const API_URL = process.env.API_URL || 'http://localhost:8080/api/catalog/upload-3d';

const modelsToUpload = [
    {
        name: 'Airmax 270',
        brand: 'Nike',
        price: '150',
        category: 'Sneakers',
        filepath: '../../../android/app/src/main/assets/models/shoes/airmax_270_left.glb'
    },
    {
        name: 'Caterpillar Work Boot',
        brand: 'Caterpillar',
        price: '120',
        category: 'Boots',
        filepath: '../../../android/app/src/main/assets/models/shoes/caterpillar_work_boot_left.glb'
    },
    {
        name: 'Nike Journey Run',
        brand: 'Nike',
        price: '130',
        category: 'Sneakers',
        filepath: '../../../android/app/src/main/assets/models/shoes/nike_journey_run_left.glb'
    },
    {
        name: 'Puma Winter Shoe',
        brand: 'Puma',
        price: '100',
        category: 'Sneakers',
        filepath: '../../../android/app/src/main/assets/models/shoes/puma_winter_shoe_left.glb'
    }
];

async function run() {
    for (const shoe of modelsToUpload) {
        const fullPath = path.resolve(__dirname, shoe.filepath);
        if (!fs.existsSync(fullPath)) {
            console.error(`File not found: ${fullPath}`);
            continue;
        }

        console.log(`Uploading ${shoe.name}...`);
        
        const form = new FormData();
        form.append('name', shoe.name);
        form.append('brand', shoe.brand);
        form.append('price', shoe.price);
        form.append('category', shoe.category);
        
        const fileBuffer = fs.readFileSync(fullPath);
        const fileBlob = new Blob([fileBuffer], { type: 'model/gltf-binary' });
        form.append('model', fileBlob as any, path.basename(fullPath));

        try {
            const res = await fetch(API_URL, {
                method: 'POST',
                body: form as any
            });
            const data = await res.json();
            
            if (res.ok) {
                console.log(`✅ Successfully uploaded ${shoe.name}`);
                console.log(`   ID: ${data._id}`);
                console.log(`   Model URL: ${data.modelUrl}`);
            } else {
                console.error(`❌ Failed to upload ${shoe.name}:`, data);
            }
        } catch (error) {
            console.error(`❌ Request failed for ${shoe.name}:`, error);
        }
        
    }
}

run();
