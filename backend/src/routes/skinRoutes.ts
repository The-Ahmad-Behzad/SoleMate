import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { createSkin, getSkins, updateSkin, deleteSkin } from '../controllers/skinController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.use(authMiddleware);
router.post('/create', upload.single('textureFile'), createSkin);
router.get('/', getSkins);
router.put('/:id', updateSkin);
router.delete('/:id', deleteSkin);

export default router;


