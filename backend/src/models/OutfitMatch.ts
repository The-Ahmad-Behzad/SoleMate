import { Schema, model, Types } from 'mongoose';

export interface OutfitMatchDoc {
  userId: string;
  outfitImageUrl?: string;
  dominantColors: string[];
  recommendedShoeIds: Types.ObjectId[];
  createdAt: Date;
  updatedAt: Date;
}

const outfitMatchSchema = new Schema<OutfitMatchDoc>(
  {
    userId: { type: String, required: true, index: true },
    outfitImageUrl: { type: String },
    dominantColors: { type: [String], default: [] },
    recommendedShoeIds: [{ type: Schema.Types.ObjectId, ref: 'Product' }]
  },
  { timestamps: true }
);

outfitMatchSchema.index({ userId: 1, createdAt: -1 });

export const OutfitMatchModel = model<OutfitMatchDoc>('OutfitMatch', outfitMatchSchema);



