import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { saveTryOn, getTryOnHistory, deleteTryOn } from '../controllers/tryonController.js';

const router = Router();

router.use(authMiddleware);
router.post('/save', saveTryOn);
router.get('/history', getTryOnHistory);
router.delete('/:id', deleteTryOn);

export default router;


