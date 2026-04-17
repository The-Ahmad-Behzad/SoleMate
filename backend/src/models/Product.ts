import { Schema, model } from 'mongoose';

export interface ProductDoc {
  name: string;
  brand: string;
  price: number;
  colors: string[];
  primaryColor?: string;
  secondaryColor?: string;
  style?: string; // e.g., 'casual', 'sporty', 'formal'
  sizes: number[];
  category: string;
  gender: 'male' | 'female' | 'unisex';
  modelUrl?: string; // S3 URL
  textureUrl?: string;
  thumbnailUrl?: string;
  arLensId?: string; // Snap Lens ID
  arLensGroupId?: string; // Snap Lens Group ID
  metadata?: Record<string, unknown>;
  createdAt: Date;
  updatedAt: Date;
}


const productSchema = new Schema<ProductDoc>(
  {
    name: { type: String, required: true },
    brand: { type: String, required: true, index: true },
    price: { type: Number, required: true },
    colors: { type: [String], default: [] },
    primaryColor: { type: String, index: true },
    secondaryColor: { type: String, index: true },
    style: { type: String, index: true },
    sizes: { type: [Number], default: [] },

    category: { type: String, required: true, index: true },
    gender: { type: String, enum: ['male', 'female', 'unisex'], default: 'unisex', index: true },
    modelUrl: { type: String },
    textureUrl: { type: String },
    thumbnailUrl: { type: String },
    arLensId: { type: String },
    arLensGroupId: { type: String },
    metadata: { type: Schema.Types.Mixed }
  },
  { timestamps: true }
);

productSchema.index({ brand: 1, category: 1 });
productSchema.index({ name: 'text' });

export const ProductModel = model<ProductDoc>('Product', productSchema);



