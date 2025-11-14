import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { analyzeOutfit, getRecommendations, getOutfitHistory } from '../controllers/outfitController.js';

const router = Router();

router.use(authMiddleware);
router.post('/analyze', analyzeOutfit);
router.post('/recommend', getRecommendations);
router.get('/history', getOutfitHistory);

export default router;


