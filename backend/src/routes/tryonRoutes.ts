import { Router } from 'express';
import { authMiddleware, optionalAuthMiddleware } from '../middleware/authMiddleware.js';
import { saveTryOn, getTryOnHistory, deleteTryOn } from '../controllers/tryonController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.post('/save', authMiddleware, upload.single('snapshot'), saveTryOn);
router.get('/history', optionalAuthMiddleware, getTryOnHistory);
router.delete('/:id', authMiddleware, deleteTryOn);

export default router;


