import { Schema, model, Types } from 'mongoose';

export interface RedesignRequestDoc {
  userId: Types.ObjectId;
  shoeId: Types.ObjectId;
  description: string;
  primaryColor: string;
  secondaryColor?: string;
  imageUrls: string[];
  status: 'pending' | 'reviewed' | 'completed';
  createdAt: Date;
  updatedAt: Date;
}

const redesignRequestSchema = new Schema<RedesignRequestDoc>(
  {
    userId: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    shoeId: { type: Schema.Types.ObjectId, ref: 'Product', required: true },
    description: { type: String, required: true },
    primaryColor: { type: String, required: true },
    secondaryColor: { type: String, required: false },
    imageUrls: { type: [String], default: [] },
    status: { type: String, enum: ['pending', 'reviewed', 'completed'], default: 'pending', index: true }
  },
  { timestamps: true }
);

export const RedesignRequestModel = model<RedesignRequestDoc>('RedesignRequest', redesignRequestSchema);
