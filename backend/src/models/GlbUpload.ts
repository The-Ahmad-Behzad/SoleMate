import { Schema, model, Types } from 'mongoose';

export type GlbUploadStatus = 'pending' | 'approved' | 'rejected';

export interface GlbUploadDoc {
  sellerId: string;              // Firebase UID of uploader
  sellerEmail: string;
  shoeName: string;
  brand: string;
  description?: string;
  // S3 storage
  s3Key: string;                 // e.g. "shoes/nike/air-max.glb"
  s3Url: string;                 // Public S3 URL
  thumbnailS3Key?: string;       // Optional thumbnail
  thumbnailUrl?: string;
  // AR Lens placement metadata (matches catalogue.json format)
  scale?: [number, number, number];
  positionOffset?: [number, number, number];
  rotationOffset?: [number, number, number];
  // Admin workflow
  status: GlbUploadStatus;
  adminNotes?: string;
  // Integration
  integrationRequestId?: Types.ObjectId;
  createdAt: Date;
  updatedAt: Date;
}

const glbUploadSchema = new Schema<GlbUploadDoc>(
  {
    sellerId:    { type: String, required: true, index: true },
    sellerEmail: { type: String, required: true },
    shoeName:    { type: String, required: true },
    brand:       { type: String, required: true, index: true },
    description: { type: String },
    s3Key:       { type: String, required: true },
    s3Url:       { type: String, required: true },
    thumbnailS3Key: { type: String },
    thumbnailUrl:   { type: String },
    scale:           { type: [Number] },
    positionOffset:  { type: [Number] },
    rotationOffset:  { type: [Number] },
    status:      { type: String, enum: ['pending', 'approved', 'rejected'], default: 'pending', index: true },
    adminNotes:  { type: String },
    integrationRequestId: { type: Schema.Types.ObjectId, ref: 'IntegrationRequest' },
  },
  { timestamps: true }
);

glbUploadSchema.index({ sellerId: 1, createdAt: -1 });

export const GlbUploadModel = model<GlbUploadDoc>('GlbUpload', glbUploadSchema);
