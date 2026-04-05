import { Schema, model, Types } from 'mongoose';

export interface ShoeUploadRequestDoc {
  sellerId: Types.ObjectId;
  shoeName: string;
  brand: string;
  description: string;
  imageUrls: string[];
  status: 'pending' | 'approved' | 'rejected';
  createdAt: Date;
  updatedAt: Date;
}

const shoeUploadRequestSchema = new Schema<ShoeUploadRequestDoc>(
  {
    sellerId: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    shoeName: { type: String, required: true },
    brand: { type: String, required: true },
    description: { type: String, required: true },
    imageUrls: { type: [String], default: [] },
    status: { type: String, enum: ['pending', 'approved', 'rejected'], default: 'pending', index: true }
  },
  { timestamps: true }
);

export const ShoeUploadRequestModel = model<ShoeUploadRequestDoc>('ShoeUploadRequest', shoeUploadRequestSchema);
