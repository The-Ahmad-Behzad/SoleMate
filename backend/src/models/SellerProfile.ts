import { Schema, model } from 'mongoose';

export interface SellerProfileDoc {
  uid: string;            // Firebase UID
  email: string;
  displayName: string;
  companyName: string;
  phone?: string;
  website?: string;
  bio?: string;
  logoUrl?: string;
  isApproved: boolean;    // For future admin-gated flows; defaults true for self-reg
  createdAt: Date;
  updatedAt: Date;
}

const sellerProfileSchema = new Schema<SellerProfileDoc>(
  {
    uid:         { type: String, required: true, unique: true, index: true },
    email:       { type: String, required: true, unique: true, index: true },
    displayName: { type: String, required: true },
    companyName: { type: String, required: true },
    phone:       { type: String },
    website:     { type: String },
    bio:         { type: String },
    logoUrl:     { type: String },
    isApproved:  { type: Boolean, default: true },
  },
  { timestamps: true }
);

export const SellerProfileModel = model<SellerProfileDoc>('SellerProfile', sellerProfileSchema);
