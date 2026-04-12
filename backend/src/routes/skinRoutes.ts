import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { createSkin, getSkins, updateSkin, deleteSkin, requestRedesign, getRedesignRequests } from '../controllers/skinController.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });
const router = Router();

router.use(authMiddleware);
router.post('/create', upload.single('textureFile'), createSkin);
router.get('/', getSkins);
router.put('/:id', updateSkin);
router.delete('/:id', deleteSkin);

// New routes for redesign requests
router.post('/request-redesign', upload.array('images', 5), requestRedesign);
router.get('/redesign-requests', getRedesignRequests);

export default router;
