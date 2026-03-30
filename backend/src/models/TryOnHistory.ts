import { Schema, model, Types } from 'mongoose';

export interface TryOnHistoryDoc {
  userId: string;
  shoeId: Types.ObjectId;
  snapshotUrl?: string;
  customSkinApplied?: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const tryOnHistorySchema = new Schema<TryOnHistoryDoc>(
  {
    userId: { type: Schema.Types.Mixed, required: true, index: true },
    shoeId: { type: Schema.Types.ObjectId, ref: 'Product', required: true },
    snapshotUrl: { type: String },
    customSkinApplied: { type: Boolean, default: false }
  },
  { timestamps: true }
);

tryOnHistorySchema.index({ userId: 1, createdAt: -1 });

export const TryOnHistoryModel = model<TryOnHistoryDoc>('TryOnHistory', tryOnHistorySchema);



