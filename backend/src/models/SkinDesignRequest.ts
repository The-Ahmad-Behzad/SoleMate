import { Schema, model } from 'mongoose';

export type SkinRequestStatus = 'new' | 'viewed' | 'in_progress' | 'completed' | 'rejected';

export interface SkinDesignRequestDoc {
  // Submitted by mobile user
  userFirebaseUid?: string;       // Optional: if user is signed in
  userEmail: string;
  shoeId: string;                 // Product MongoDB _id or catalogue shoe id
  shoeName?: string;              // Denormalised for display
  description: string;
  referenceImageUrls: string[];   // S3 or Firebase Storage URLs
  // Seller management
  status: SkinRequestStatus;
  sellerNotes?: string;
  assignedSellerId?: string;      // For future multi-seller routing
  // Audit
  viewedAt?: Date;
  completedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
}

const skinDesignRequestSchema = new Schema<SkinDesignRequestDoc>(
  {
    userFirebaseUid: { type: String },
    userEmail:       { type: String, required: true, index: true },
    shoeId:          { type: String, required: true, index: true },
    shoeName:        { type: String },
    description:     { type: String, required: true },
    referenceImageUrls: { type: [String], default: [] },
    status:          { type: String, enum: ['new', 'viewed', 'in_progress', 'completed', 'rejected'], default: 'new', index: true },
    sellerNotes:     { type: String },
    assignedSellerId: { type: String },
    viewedAt:        { type: Date },
    completedAt:     { type: Date },
  },
  { timestamps: true }
);

skinDesignRequestSchema.index({ status: 1, createdAt: -1 });
skinDesignRequestSchema.index({ shoeId: 1, status: 1 });

export const SkinDesignRequestModel = model<SkinDesignRequestDoc>('SkinDesignRequest', skinDesignRequestSchema);
