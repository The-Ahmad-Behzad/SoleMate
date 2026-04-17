import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { OutfitMatchModel } from '../models/OutfitMatch.js';
import { ProductModel } from '../models/Product.js';
import { UserModel } from '../models/User.js';
import { Types } from 'mongoose';
import { AIService } from '../services/aiService.js';
import { s3Service } from '../services/s3Service.js';

export async function analyzeOutfit(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    console.log('Analyze Outfit Request Body:', req.body);
    console.log('Analyze Outfit Request File:', req.file ? 'Present' : 'Missing');

    let { outfitImageUrl, dominantColors, category, description, recommendedShoeIds } = req.body;

    // Handle stringified arrays from multipart/form-data
    if (typeof dominantColors === 'string') {
      try { dominantColors = JSON.parse(dominantColors); } catch (e) {}
    }
    if (typeof recommendedShoeIds === 'string') {
      try { recommendedShoeIds = JSON.parse(recommendedShoeIds); } catch (e) {}
    }

    if (req.file) {
      const key = `outfits/${req.user.uid}/${Date.now()}_${req.file.originalname.replace(/[^a-zA-Z0-9.]/g, '')}`;
      outfitImageUrl = await s3Service.uploadFile(key, req.file.buffer, req.file.mimetype);
    }

    // Sanitize and validate recommendedShoeIds to prevent 500 CastErrors
    let validShoeIds: Types.ObjectId[] = [];
    if (Array.isArray(recommendedShoeIds)) {
      validShoeIds = recommendedShoeIds
        .filter(id => id && Types.ObjectId.isValid(id))
        .map(id => new Types.ObjectId(id));
      
      if (validShoeIds.length < recommendedShoeIds.length) {
        console.warn(`[analyzeOutfit] Filtered out ${recommendedShoeIds.length - validShoeIds.length} invalid shoe IDs.`);
      }
    }

    if (!outfitImageUrl && (!dominantColors || !dominantColors.length)) {
      res.status(400).json({ error: 'outfitImageUrl or dominantColors are required' });
      return;
    }

    const user = await UserModel.findOne({ uid: req.user.uid });
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    const match = await OutfitMatchModel.create({
      userId: user._id,
      outfitImageUrl,
      category,
      description,
      dominantColors: Array.isArray(dominantColors) ? dominantColors : [],
      recommendedShoeIds: validShoeIds,
    });

    // Populate shoes before returning to ensure frontend has full data
    await match.populate('recommendedShoeIds');

    res.status(201).json(match);
  } catch (err: any) {
    console.error('Analyze outfit error details:', err.message, err.stack);
    res.status(500).json({ error: 'Failed to analyze outfit', details: err.message });
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
    const recommendations = await aiService.getRecommendationsByColors(colors || []);

    res.json(recommendations);
  } catch (err) {
    console.error('Get recommendations error:', err);
    res.status(500).json({ error: 'Failed to get recommendations' });
  }
}

export async function getOutfitHistory(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.json([]); // Return empty history for guests/diagnostics
      return;
    }

    const user = await UserModel.findOne({ uid: req.user.uid });
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    const history = await OutfitMatchModel
      .find({ userId: user._id })
      .populate('recommendedShoeIds')
      .sort({ createdAt: -1 })
      .limit(10)
      .lean();

    res.json(history);
  } catch (err: any) {
    console.error('Get outfit history error details:', err.message, err.stack);
    res.status(500).json({ error: 'Failed to fetch history', details: err.message });
  }
}

/**
 * NEW: Matches shoes from DB to an uploaded outfit image.
 */
export async function matchShoesToOutfit(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.file) {
      res.status(400).json({ error: 'Outfit image is required' });
      return;
    }

    const aiService = AIService.getInstance();
    const aiResult = await aiService.getRecommendationsFromImage(req.file.buffer, req.file.originalname);

    const { colors: detectedColors, style } = aiResult;
    
    // Normalize colors: extract 'name' if it's an object from the AI service
    const colors = Array.isArray(detectedColors) 
      ? detectedColors.map((c: any) => (typeof c === 'object' && c.name) ? c.name.toLowerCase() : String(c).toLowerCase())
      : [];

    console.log(`[outfitController] AI detected style: ${style}, normalized colors: ${colors.join(', ')}`);

    // Match products in DB
    // Match logic: Style must match, and primary or secondary color must be in detected colors.
    const matchedProducts = await ProductModel.find({
      $and: [
        { style: style.toLowerCase() },
        { 
          $or: [
            { primaryColor: { $in: colors } },
            { secondaryColor: { $in: colors } }
          ]
        }
      ]
    }).limit(10).lean();

    res.json({
      detectedStyle: style,
      detectedColors: colors,
      recommendations: matchedProducts
    });
  } catch (err: any) {
    console.error('Match shoes error:', err);
    res.status(500).json({ error: 'Failed to match shoes', details: err.message });
  }
}

/**
 * NEW: Recommends an outfit for a specific shoe ID.
 */
export async function recommendOutfitForShoe(req: AuthRequest, res: Response): Promise<void> {
  try {
    const { shoeId } = req.params;
    
    const product = await ProductModel.findById(shoeId).lean();
    if (!product) {
      res.status(404).json({ error: 'Shoe not found' });
      return;
    }

    if (!product.thumbnailUrl) {
      res.status(400).json({ error: 'Shoe has no thumbnail for AI analysis' });
      return;
    }

    // Fetch the thumbnail image buffer
    const imgResponse = await fetch(product.thumbnailUrl);
    if (!imgResponse.ok) throw new Error('Failed to fetch shoe thumbnail');
    const arrayBuffer = await imgResponse.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    const aiService = AIService.getInstance();
    const result = await aiService.getOutfitRecommendationForShoe(buffer, 'shoe_thumbnail.jpg');

    res.json(result);
  } catch (err: any) {
    console.error('Recommend outfit error:', err);
    res.status(500).json({ error: 'Failed to generate outfit recommendation', details: err.message });
  }
}


