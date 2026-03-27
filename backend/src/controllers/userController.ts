import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { UserModel } from '../models/User.js';
import { TryOnHistoryModel } from '../models/TryOnHistory.js';

export async function getUserProfile(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const user = await UserModel.findOne({ uid: req.user.uid }).lean();

    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    res.json(user);
  } catch (err) {
    console.error('[userController] getUserProfile error:', err);
    res.status(500).json({ 
      error: 'Failed to fetch profile',
      message: process.env.NODE_ENV === 'development' ? (err as Error).message : undefined
    });
  }
}

export async function updateUserProfile(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { name, preferences } = req.body;

    if (name && typeof name !== 'string') {
      res.status(400).json({ error: 'Name must be a string' });
      return;
    }
    
    if (preferences && typeof preferences !== 'object') {
       res.status(400).json({ error: 'Preferences must be an object' });
       return;
    }

    const user = await UserModel.findOneAndUpdate(
      { uid: req.user.uid },
      { name, preferences, email: req.user.email },
      { new: true, upsert: true }
    ).lean();

    res.json(user);
  } catch (err) {
    console.error('[userController] updateUserProfile error:', err);
    res.status(500).json({ error: 'Failed to update profile' });
  }
}

export async function getUserStats(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const tryOnCount = await TryOnHistoryModel.countDocuments({ userId: req.user.uid });

    res.json({
      tryOnCount,
      userId: req.user.uid,
    });
  } catch (err) {
    console.error('[userController] getUserStats error:', err);
    res.status(500).json({ 
      error: 'Failed to fetch stats',
      message: process.env.NODE_ENV === 'development' ? (err as Error).message : undefined
    });
  }
}

