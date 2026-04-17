import mongoose from 'mongoose';
import dotenv from 'dotenv';
import { ProductModel } from '../src/models/Product.js';

dotenv.config();

const MONGO_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/solemate';

const randomColors = ['black', 'white', 'grey', 'blue', 'red', 'green', 'brown', 'yellow'];
const randomStyles = ['casual', 'sporty', 'formal', 'boots', 'sandals'];

async function seedColors() {
    try {
        console.log('Connecting to MongoDB...');
        await mongoose.connect(MONGO_URI);
        console.log('Connected.');

        const products = await ProductModel.find();
        console.log(`Found ${products.length} products to update.`);

const randomGenders = ['male', 'female', 'unisex'];
        for (const product of products) {
            const pColor = randomColors[Math.floor(Math.random() * randomColors.length)];
            const sColor = randomColors[Math.floor(Math.random() * randomColors.length)];
            const style = randomStyles[Math.floor(Math.random() * randomStyles.length)];
            const gender = randomGenders[Math.floor(Math.random() * randomGenders.length)];

            await ProductModel.updateOne(
                { _id: product._id },
                { 
                    $set: { 
                        primaryColor: pColor, 
                        secondaryColor: sColor,
                        style: style,
                        gender: gender,
                        colors: [pColor, sColor],
                        sizes: [40, 41, 42, 43, 44]
                    } 
                }
            );

        }

        console.log('Successfully updated all products with random color/style values.');
    } catch (err) {
        console.error('Seed error:', err);
    } finally {
        await mongoose.disconnect();
    }
}

seedColors();
