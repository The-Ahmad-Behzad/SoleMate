import { S3Client, PutObjectCommand, GetObjectCommand, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';

dotenv.config();

const REGION = process.env.AWS_REGION || 'us-east-1';
const BUCKET_NAME = process.env.AWS_BUCKET_NAME || 'solemate-assets-placeholder';
const AWS_ACCESS_KEY_ID = process.env.AWS_ACCESS_KEY_ID || 'PLACEHOLDER_KEY';
const AWS_SECRET_ACCESS_KEY = process.env.AWS_SECRET_ACCESS_KEY || 'PLACEHOLDER_SECRET';

const isMockS3 = AWS_ACCESS_KEY_ID === 'PLACEHOLDER_KEY';

// Ensure uploads directory exists if mocking
const UPLOADS_DIR = path.join(process.cwd(), 'uploads');
if (isMockS3 && !fs.existsSync(UPLOADS_DIR)) {
    fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

const s3Client = isMockS3 ? null : new S3Client({
    region: REGION,
    credentials: {
        accessKeyId: AWS_ACCESS_KEY_ID,
        secretAccessKey: AWS_SECRET_ACCESS_KEY,
    },
});

export class S3Service {
    /**
     * Uploads a file to S3 or Local FS if S3 is mocked.
     */
    async uploadFile(key: string, body: Buffer, contentType: string): Promise<string> {
        if (isMockS3) {
            const filename = key.replace(/\//g, '_'); // Flatten path
            const filePath = path.join(UPLOADS_DIR, filename);
            fs.writeFileSync(filePath, body);
            // Returns a relative path that we'll serve statically
            // e.g., /uploads/skins_user123_texture.png
            return `/uploads/${filename}`;
        }

        const command = new PutObjectCommand({
            Bucket: BUCKET_NAME,
            Key: key,
            Body: body,
            ContentType: contentType,
        });

        try {
            await s3Client!.send(command);
            return `https://${BUCKET_NAME}.s3.${REGION}.amazonaws.com/${key}`;
        } catch (err) {
            console.error('S3 Upload Error:', err);
            throw new Error('Failed to upload file to S3');
        }
    }

    /**
     * Generates a signed URL for reading a private file, or returns local path if mocked.
     */
    async getSignedDownloadUrl(key: string, expiresInSeconds = 3600): Promise<string> {
        if (isMockS3) {
            return `/uploads/${key.replace(/\//g, '_')}`;
        }

        const command = new GetObjectCommand({
            Bucket: BUCKET_NAME,
            Key: key,
        });

        try {
            const url = await getSignedUrl(s3Client!, command, { expiresIn: expiresInSeconds });
            return url;
        } catch (err) {
            console.error('S3 Signed URL Error:', err);
            throw new Error('Failed to generate signed URL');
        }
    }

    /**
     * Deletes a file from S3 or local FS.
     */
    async deleteFile(key: string): Promise<void> {
        if (isMockS3) {
            const filePath = path.join(UPLOADS_DIR, key.replace(/\//g, '_'));
            if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
            }
            return;
        }

        const command = new DeleteObjectCommand({
            Bucket: BUCKET_NAME,
            Key: key,
        });

        try {
            await s3Client!.send(command);
        } catch (err) {
            console.error('S3 Delete Error:', err);
            throw new Error('Failed to delete file from S3');
        }
    }
}

export const s3Service = new S3Service();
