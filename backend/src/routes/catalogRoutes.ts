import { Router } from 'express';
import { getAllProducts, getProductById, searchProducts, requestShoe, uploadShoeModel } from '../controllers/catalogController.js';
import { authMiddleware } from '../middleware/authMiddleware.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });

const router = Router();

router.get('/', getAllProducts);
router.get('/search', searchProducts);
router.get('/:id', getProductById);

router.post('/request-shoe', authMiddleware, upload.array('images', 5), requestShoe);

router.post('/upload-3d', upload.fields([{ name: 'model', maxCount: 1 }, { name: 'thumbnail', maxCount: 1 }]), uploadShoeModel);

export default router;
