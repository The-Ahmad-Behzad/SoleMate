import { Router } from 'express';
import { getAllProducts, getProductById, searchProducts, requestShoe } from '../controllers/catalogController.js';
import { authMiddleware } from '../middleware/authMiddleware.js';
import multer from 'multer';

const upload = multer({ storage: multer.memoryStorage() });

const router = Router();

router.get('/', getAllProducts);
router.get('/search', searchProducts);
router.get('/:id', getProductById);

router.post('/request-shoe', authMiddleware, upload.array('images', 5), requestShoe);

export default router;
