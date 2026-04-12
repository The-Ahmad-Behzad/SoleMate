import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { TryOnHistoryModel } from '../models/TryOnHistory.js';
import { UserModel } from '../models/User.js';
import { Types } from 'mongoose';

import { s3Service } from '../services/s3Service.js';

export async function saveTryOn(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { shoeId, customSkinApplied } = req.body;
    let snapshotUrl = req.body.snapshotUrl;

    if (!shoeId) {
      res.status(400).json({ error: 'shoeId is required' });
      return;
    }

    if (req.file) {
      const key = `snapshots/${req.user.uid}/${Date.now()}_${req.file.originalname}`;
      snapshotUrl = await s3Service.uploadFile(key, req.file.buffer, req.file.mimetype);
    }

    const user = await UserModel.findOne({ uid: req.user.uid });
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    if (!Types.ObjectId.isValid(shoeId)) {
      res.status(400).json({ error: 'Invalid shoeId format' });
      return;
    }

    const tryOn = await TryOnHistoryModel.create({
      userId: user._id,
      shoeId: new Types.ObjectId(shoeId),
      snapshotUrl,
      customSkinApplied,
    });

    res.status(201).json(tryOn);
  } catch (err: any) {
    console.error('Save try-on error details:', err.message, err.stack);
    res.status(500).json({ error: 'Failed to save try-on history', details: err.message });
  }
}

export async function getTryOnHistory(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const user = await UserModel.findOne({ uid: req.user.uid });
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    const history = await TryOnHistoryModel
      .find({ userId: user._id })
      .populate('shoeId')
      .sort({ createdAt: -1 })
      .limit(5)
      .lean();

    res.json(history);
  } catch (err: any) {
    console.error('Get history error details:', err.message, err.stack);
    res.status(500).json({ error: 'Failed to fetch history', details: err.message });
  }
}

export async function deleteTryOn(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const user = await UserModel.findOne({ uid: req.user.uid });
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    const { id } = req.params;
    await TryOnHistoryModel.deleteOne({ _id: id, userId: user._id });

    res.status(204).send();
  } catch (err) {
    console.error('Delete try-on error:', err);
    res.status(500).json({ error: 'Failed to delete' });
  }
}

