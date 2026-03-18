import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { saveTryOn, getTryOnHistory, deleteTryOn } from '../controllers/tryonController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.use(authMiddleware);
router.post('/save', upload.single('snapshot'), saveTryOn);
router.get('/history', getTryOnHistory);
router.delete('/:id', deleteTryOn);

export default router;


