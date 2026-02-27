import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { TryOnHistoryModel } from '../models/TryOnHistory.js';
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

    if (req.file) {
      const key = `snapshots/${req.user.uid}/${Date.now()}_${req.file.originalname}`;
      snapshotUrl = await s3Service.uploadFile(key, req.file.buffer, req.file.mimetype);
    }

    const tryOn = await TryOnHistoryModel.create({
      userId: new Types.ObjectId(req.user.uid),
      shoeId: new Types.ObjectId(shoeId),
      snapshotUrl,
      customSkinApplied,
    });

    res.status(201).json(tryOn);
  } catch (err) {
    console.error('Save try-on error:', err);
    res.status(500).json({ error: 'Failed to save try-on' });
  }
}

export async function getTryOnHistory(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const history = await TryOnHistoryModel
      .find({ userId: req.user.uid })
      .populate('shoeId')
      .sort({ createdAt: -1 })
      .limit(5)
      .lean();

    res.json(history);
  } catch (err) {
    console.error('Get history error:', err);
    res.status(500).json({ error: 'Failed to fetch history' });
  }
}

export async function deleteTryOn(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { id } = req.params;
    await TryOnHistoryModel.deleteOne({ _id: id, userId: req.user.uid });

    res.status(204).send();
  } catch (err) {
    console.error('Delete try-on error:', err);
    res.status(500).json({ error: 'Failed to delete' });
  }
}

