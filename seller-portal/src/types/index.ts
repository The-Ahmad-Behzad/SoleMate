// Seller Portal — shared TypeScript types

export type GlbUploadStatus = 'pending' | 'approved' | 'rejected';
export type IntegrationStatus = 'pending' | 'in_review' | 'scheduled' | 'deployed' | 'rejected';
export type SkinRequestStatus = 'new' | 'viewed' | 'in_progress' | 'completed' | 'rejected';

export interface SellerProfile {
  _id: string;
  uid: string;
  email: string;
  displayName: string;
  companyName: string;
  phone?: string;
  website?: string;
  bio?: string;
  logoUrl?: string;
  isApproved: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GlbUpload {
  _id: string;
  sellerId: string;
  sellerEmail: string;
  shoeName: string;
  brand: string;
  description?: string;
  s3Key: string;
  s3Url: string;
  thumbnailUrl?: string;
  scale?: [number, number, number];
  positionOffset?: [number, number, number];
  rotationOffset?: [number, number, number];
  status: GlbUploadStatus;
  adminNotes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface IntegrationRequest {
  _id: string;
  glbUploadId: string | GlbUpload;
  sellerId: string;
  sellerEmail: string;
  shoeName: string;
  requestNote?: string;
  status: IntegrationStatus;
  adminResponse?: string;
  catalogueEntry?: {
    id: string;
    name: string;
    thumbnailUrl?: string;
    modelUrl: string;
    scale?: [number, number, number];
    positionOffset?: [number, number, number];
    rotationOffset?: [number, number, number];
  };
  adminEmailSent: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SkinDesignRequest {
  _id: string;
  userFirebaseUid?: string;
  userEmail: string;
  shoeId: string;
  shoeName?: string;
  description: string;
  referenceImageUrls: string[];
  status: SkinRequestStatus;
  sellerNotes?: string;
  viewedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SkinRequestListResponse {
  requests: SkinDesignRequest[];
  total: number;
  page: number;
  limit: number;
  statusCounts: Record<SkinRequestStatus, number>;
}

export interface UploadMetadata {
  shoeName: string;
  brand: string;
  description?: string;
  scale?: [number, number, number];
  positionOffset?: [number, number, number];
  rotationOffset?: [number, number, number];
}
