import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { OutfitMatchModel } from '../models/OutfitMatch.js';
import { ProductModel } from '../models/Product.js';
import { Types } from 'mongoose';
import { AIService } from '../services/aiService.js';

export async function analyzeOutfit(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { outfitImageUrl, dominantColors } = req.body;

    if (!outfitImageUrl && (!dominantColors || !dominantColors.length)) {
      res.status(400).json({ error: 'outfitImageUrl or dominantColors are required' });
      return;
    }

    const match = await OutfitMatchModel.create({
      userId: req.user.uid,
      outfitImageUrl,
      dominantColors: dominantColors || [],
      recommendedShoeIds: [],
    });

    res.status(201).json(match);
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

    if (!colors || !Array.isArray(colors)) {
      res.status(400).json({ error: 'A valid colors array is required' });
      return;
    }

    // Use the AI Service to get recommendations
    const aiService = AIService.getInstance();
    const recommendations = await aiService.getRecommendations(colors || []);

    res.json(recommendations);
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
  } catch (err: any) {
    console.error('Get outfit history error details:', {
      message: err.message,
      name: err.name,
      stack: err.stack,
      userId: req.user?.uid
    });
    res.status(500).json({
      error: 'Failed to fetch history',
      details: err.message,
      code: err.name
    });
  }
}

