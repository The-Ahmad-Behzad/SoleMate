import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { createSkin, getSkins, updateSkin, deleteSkin } from '../controllers/skinController.js';

const router = Router();

router.use(authMiddleware);
router.post('/create', createSkin);
router.get('/', getSkins);
router.put('/:id', updateSkin);
router.delete('/:id', deleteSkin);

export default router;


