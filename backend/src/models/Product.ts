import { Schema, model } from 'mongoose';

export interface ProductDoc {
  name: string;
  brand: string;
  price: number;
  colors: string[];
  sizes: number[];
  category: string;
  modelUrl?: string;
  textureUrl?: string;
  thumbnailUrl?: string;
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
    sizes: { type: [Number], default: [] },
    category: { type: String, required: true, index: true },
    modelUrl: { type: String },
    textureUrl: { type: String },
    thumbnailUrl: { type: String },
    metadata: { type: Schema.Types.Mixed }
  },
  { timestamps: true }
);

productSchema.index({ brand: 1, category: 1 });
productSchema.index({ name: 'text' });

export const ProductModel = model<ProductDoc>('Product', productSchema);



