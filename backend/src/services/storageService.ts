import { getFirebaseAdmin } from '../config/firebase.js';

const getBucket = () => getFirebaseAdmin().storage().bucket();

export interface FileMetadata {
  contentType?: string;
  contentDisposition?: string;
  metadata?: Record<string, string>;
}

export class StorageService {
  async uploadFile(filePath: string, buffer: Buffer, metadata?: FileMetadata): Promise<string> {
    try {
      const bucket = getBucket();
      const file = bucket.file(filePath);
      
      await file.save(buffer, {
        metadata: {
          contentType: metadata?.contentType || 'application/octet-stream',
          metadata: metadata?.metadata || {},
        },
      });

      return file.publicUrl();
    } catch (err) {
      console.error('Upload error:', err);
      throw new Error('Failed to upload file');
    }
  }

  async generateDownloadURL(filePath: string, expiresIn = 3600): Promise<string> {
    try {
      const bucket = getBucket();
      const file = bucket.file(filePath);
      const [url] = await file.getSignedUrl({
        action: 'read',
        expires: Date.now() + expiresIn * 1000,
      });

      return url;
    } catch (err) {
      console.error('Generate URL error:', err);
      throw new Error('Failed to generate download URL');
    }
  }

  async deleteFile(filePath: string): Promise<void> {
    try {
      const bucket = getBucket();
      await bucket.file(filePath).delete();
    } catch (err) {
      console.error('Delete error:', err);
      throw new Error('Failed to delete file');
    }
  }

  async fileExists(filePath: string): Promise<boolean> {
    try {
      const bucket = getBucket();
      const [exists] = await bucket.file(filePath).exists();
      return exists;
    } catch (err) {
      return false;
    }
  }
}

export const storageService = new StorageService();
