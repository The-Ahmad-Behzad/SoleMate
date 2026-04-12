import { Schema, model, Types } from 'mongoose';

export type IntegrationStatus = 'pending' | 'in_review' | 'scheduled' | 'deployed' | 'rejected';

export interface IntegrationRequestDoc {
  glbUploadId: Types.ObjectId;
  sellerId: string;
  sellerEmail: string;
  shoeName: string;
  requestNote?: string;
  status: IntegrationStatus;
  adminResponse?: string;
  // Catalogue entry data for admin to add directly to catalogue.json
  catalogueEntry?: {
    id: string;
    name: string;
    thumbnailUrl?: string;
    modelUrl: string;
    scale?: [number, number, number];
    positionOffset?: [number, number, number];
    rotationOffset?: [number, number, number];
  };
  firestoreDocId?: string;       // Firestore doc ID for cross-system tracking
  adminEmailSent: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const integrationRequestSchema = new Schema<IntegrationRequestDoc>(
  {
    glbUploadId:  { type: Schema.Types.ObjectId, ref: 'GlbUpload', required: true },
    sellerId:     { type: String, required: true, index: true },
    sellerEmail:  { type: String, required: true },
    shoeName:     { type: String, required: true },
    requestNote:  { type: String },
    status:       { type: String, enum: ['pending', 'in_review', 'scheduled', 'deployed', 'rejected'], default: 'pending', index: true },
    adminResponse: { type: String },
    catalogueEntry: { type: Schema.Types.Mixed },
    firestoreDocId: { type: String },
    adminEmailSent: { type: Boolean, default: false },
  },
  { timestamps: true }
);

integrationRequestSchema.index({ sellerId: 1, createdAt: -1 });

export const IntegrationRequestModel = model<IntegrationRequestDoc>('IntegrationRequest', integrationRequestSchema);
