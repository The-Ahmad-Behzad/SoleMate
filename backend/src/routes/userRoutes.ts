import { Router } from 'express';
import { authMiddleware } from '../middleware/authMiddleware.js';
import { getUserProfile, updateUserProfile, getUserStats } from '../controllers/userController.js';

const router = Router();

router.use(authMiddleware);
router.get('/profile', getUserProfile);
router.put('/profile', updateUserProfile);
router.get('/stats', getUserStats);

export default router;


