import { Router } from 'express';
import { authMiddleware, optionalAuthMiddleware } from '../middleware/authMiddleware.js';
import { analyzeOutfit, getRecommendations, getOutfitHistory } from '../controllers/outfitController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.post('/analyze', authMiddleware, upload.single('outfitImage'), analyzeOutfit);
router.post('/recommend', authMiddleware, getRecommendations);
router.get('/history', optionalAuthMiddleware, getOutfitHistory);

export default router;


