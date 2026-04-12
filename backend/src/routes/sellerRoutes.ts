import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { sellerAuthMiddleware } from '../middleware/sellerAuthMiddleware.js';
import {
  registerSeller,
  getSellerProfile,
  updateSellerProfile,
  getUploadPresignedUrl,
  confirmUpload,
  getMyUploads,
  getUploadById,
  submitIntegrationRequest,
  getMyIntegrationRequests,
} from '../controllers/sellerController.js';

const router = Router();

// Public seller registration (requires only a valid Firebase token, not yet seller)
router.post('/register', authMiddleware as any, registerSeller as any);

// All other seller routes require full seller auth
router.use(sellerAuthMiddleware as any);

// Profile
router.get('/profile', getSellerProfile as any);
router.put('/profile', updateSellerProfile as any);

// GLB Uploads
router.post('/upload-url', getUploadPresignedUrl as any);
router.post('/uploads/confirm', confirmUpload as any);
router.get('/uploads', getMyUploads as any);
router.get('/uploads/:id', getUploadById as any);

// Integration Requests
router.post('/integration-requests', submitIntegrationRequest as any);
router.get('/integration-requests', getMyIntegrationRequests as any);

export default router;
