import { Router } from 'express';
import catalogRoutes from './catalogRoutes.js';
import tryonRoutes from './tryonRoutes.js';
import skinRoutes from './skinRoutes.js';
import outfitRoutes from './outfitRoutes.js';
import userRoutes from './userRoutes.js';
import sellerRoutes from './sellerRoutes.js';
import skinRequestRoutes from './skinRequestRoutes.js';

const router = Router();

router.use('/catalog', catalogRoutes);
router.use('/tryon', tryonRoutes);
router.use('/skins', skinRoutes);
router.use('/outfit', outfitRoutes);
router.use('/user', userRoutes);
router.use('/seller', sellerRoutes);
router.use('/skin-requests', skinRequestRoutes);

export default router;

