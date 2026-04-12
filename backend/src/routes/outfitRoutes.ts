import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { analyzeOutfit, getRecommendations, getOutfitHistory } from '../controllers/outfitController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.use(authMiddleware);
router.post('/analyze', upload.single('outfitImage'), analyzeOutfit);
router.post('/recommend', getRecommendations);
router.get('/history', getOutfitHistory);

export default router;


