import { Router } from 'express';
import { authMiddleware, optionalAuthMiddleware } from '../middleware/authMiddleware.js';
import { 
  analyzeOutfit, 
  getRecommendations, 
  getOutfitHistory,
  matchShoesToOutfit,
  recommendOutfitFromImage
} from '../controllers/outfitController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.post('/analyze', authMiddleware, upload.single('outfitImage'), analyzeOutfit);
router.post('/recommend', authMiddleware, getRecommendations);
router.get('/history', optionalAuthMiddleware, getOutfitHistory);

// New AI endpoints
router.post('/recommend-shoes', authMiddleware, upload.single('outfitImage'), matchShoesToOutfit);
router.post('/recommend-outfit-for-shoe', authMiddleware, upload.single('file'), recommendOutfitFromImage);

export default router;



