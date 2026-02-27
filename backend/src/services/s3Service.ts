import { S3Client, PutObjectCommand, GetObjectCommand, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import dotenv from 'dotenv';

dotenv.config();

const REGION = process.env.AWS_REGION || 'us-east-1';
const BUCKET_NAME = process.env.AWS_BUCKET_NAME || 'solemate-assets-placeholder';

// Use placeholders/environment variables for credentials
const s3Client = new S3Client({
    region: REGION,
    credentials: {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID || 'PLACEHOLDER_KEY',
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY || 'PLACEHOLDER_SECRET',
    },
});

export class S3Service {
    /**
     * Uploads a file to S3.
     * @param key The file path/key in S3 (e.g., 'skins/user123/texture.png')
     * @param body The file buffer
     * @param contentType The MIME type of the file
     * @returns The public URL of the uploaded file (if bucket is public) or the Key
     */
    async uploadFile(key: string, body: Buffer, contentType: string): Promise<string> {
        const command = new PutObjectCommand({
            Bucket: BUCKET_NAME,
            Key: key,
            Body: body,
            ContentType: contentType,
            // ACL: 'public-read', // Uncomment if bucket is configured for ACLs and you want public access
        });

        try {
            await s3Client.send(command);
            // Constructing a virtual-hosted-style URL (works for standard regions)
            return `https://${BUCKET_NAME}.s3.${REGION}.amazonaws.com/${key}`;
        } catch (err) {
            console.error('S3 Upload Error:', err);
            throw new Error('Failed to upload file to S3');
        }
    }

    /**
     * Generates a signed URL for reading a private file.
     * @param key The file path/key in S3
     * @param expiresInSeconds Duration before URL expires (default 1 hour)
     */
    async getSignedDownloadUrl(key: string, expiresInSeconds = 3600): Promise<string> {
        const command = new GetObjectCommand({
            Bucket: BUCKET_NAME,
            Key: key,
        });

        try {
            const url = await getSignedUrl(s3Client, command, { expiresIn: expiresInSeconds });
            return url;
        } catch (err) {
            console.error('S3 Signed URL Error:', err);
            throw new Error('Failed to generate signed URL');
        }
    }

    /**
     * Deletes a file from S3.
     * @param key The file path/key to delete
     */
    async deleteFile(key: string): Promise<void> {
        const command = new DeleteObjectCommand({
            Bucket: BUCKET_NAME,
            Key: key,
        });

        try {
            await s3Client.send(command);
        } catch (err) {
            console.error('S3 Delete Error:', err);
            throw new Error('Failed to delete file from S3');
        }
    }
}

export const s3Service = new S3Service();
