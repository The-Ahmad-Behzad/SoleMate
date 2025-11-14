import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { OutfitMatchModel } from '../models/OutfitMatch.js';
import { ProductModel } from '../models/Product.js';
import { Types } from 'mongoose';

export async function analyzeOutfit(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { outfitImageUrl, dominantColors } = req.body;

    const match = await OutfitMatchModel.create({
      userId: new Types.ObjectId(req.user.uid),
      outfitImageUrl,
      dominantColors: dominantColors || [],
      recommendedShoeIds: [],
    });

    res.json(match);
  } catch (err) {
    console.error('Analyze outfit error:', err);
    res.status(500).json({ error: 'Failed to analyze outfit' });
  }
}

export async function getRecommendations(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { colors } = req.body;

    const shoes = await ProductModel.find({
      colors: { $in: colors }
    }).limit(5).lean();

    res.json(shoes);
  } catch (err) {
    console.error('Get recommendations error:', err);
    res.status(500).json({ error: 'Failed to get recommendations' });
  }
}

export async function getOutfitHistory(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const history = await OutfitMatchModel
      .find({ userId: req.user.uid })
      .populate('recommendedShoeIds')
      .sort({ createdAt: -1 })
      .limit(10)
      .lean();

    res.json(history);
  } catch (err) {
    console.error('Get outfit history error:', err);
    res.status(500).json({ error: 'Failed to fetch history' });
  }
}

