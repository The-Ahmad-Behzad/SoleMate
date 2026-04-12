import { Router } from 'express';
import { sellerAuthMiddleware } from '../middleware/sellerAuthMiddleware.js';
import {
  createSkinDesignRequest,
  getSkinDesignRequests,
  getSkinDesignRequestById,
  updateSkinDesignRequest,
} from '../controllers/skinRequestController.js';

const router = Router();

// Mobile app submits a request — open endpoint (no auth required for mobile users)
router.post('/', createSkinDesignRequest as any);

// Seller-only read + update
router.use(sellerAuthMiddleware as any);
router.get('/', getSkinDesignRequests as any);
router.get('/:id', getSkinDesignRequestById as any);
router.patch('/:id', updateSkinDesignRequest as any);

export default router;
