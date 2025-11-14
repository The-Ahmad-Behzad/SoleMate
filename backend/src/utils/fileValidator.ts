export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const ALLOWED_TEXTURE_TYPES = ['image/png', 'image/jpeg'];

export const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
export const MAX_TEXTURE_SIZE = 10 * 1024 * 1024; // 10MB

export interface FileValidation {
  isValid: boolean;
  error?: string;
}

export function validateImage(file: { buffer?: Buffer; mimetype?: string; size?: number }): FileValidation {
  if (!file.buffer || file.size === undefined) {
    return { isValid: false, error: 'File is missing or empty' };
  }

  if (!file.mimetype || !ALLOWED_IMAGE_TYPES.includes(file.mimetype)) {
    return { isValid: false, error: `Invalid file type. Allowed: ${ALLOWED_IMAGE_TYPES.join(', ')}` };
  }

  if (file.size > MAX_FILE_SIZE) {
    return { isValid: false, error: `File too large. Max size: ${MAX_FILE_SIZE / 1024 / 1024}MB` };
  }

  return { isValid: true };
}

export function validateTexture(file: { buffer?: Buffer; mimetype?: string; size?: number }): FileValidation {
  if (!file.buffer || file.size === undefined) {
    return { isValid: false, error: 'File is missing or empty' };
  }

  if (!file.mimetype || !ALLOWED_TEXTURE_TYPES.includes(file.mimetype)) {
    return { isValid: false, error: `Invalid texture type. Allowed: ${ALLOWED_TEXTURE_TYPES.join(', ')}` };
  }

  if (file.size > MAX_TEXTURE_SIZE) {
    return { isValid: false, error: `Texture too large. Max size: ${MAX_TEXTURE_SIZE / 1024 / 1024}MB` };
  }

  return { isValid: true };
}

