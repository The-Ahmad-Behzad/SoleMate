import { Schema, model, Types } from 'mongoose';

export interface CustomSkinDoc {
  userId: Types.ObjectId;
  shoeId: Types.ObjectId;
  skinName: string;
  textureUrl?: string;
  createdAt: Date;
  updatedAt: Date;
}

const customSkinSchema = new Schema<CustomSkinDoc>(
  {
    userId: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    shoeId: { type: Schema.Types.ObjectId, ref: 'Product', required: true },
    skinName: { type: String, required: true },
    textureUrl: { type: String }
  },
  { timestamps: true }
);

customSkinSchema.index({ userId: 1, shoeId: 1 });

export const CustomSkinModel = model<CustomSkinDoc>('CustomSkin', customSkinSchema);



