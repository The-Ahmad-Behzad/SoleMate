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
    const gender = (req.body.gender || 'unisex').toLowerCase();
    
    // Normalize colors for searching, while keeping original for display if needed
    const normalize = (c: any) => (typeof c === 'object' && c.name) ? c.name.toLowerCase() : String(c).toLowerCase();
    const originalColors = Array.isArray(detectedColors) ? detectedColors.map(normalize) : [];

    // Mapping for broader search fallbacks
    const colorGroupMap: Record<string, string> = {
      'maroon': 'red', 'burgundy': 'red', 'crimson': 'red',
      'navy': 'blue', 'teal': 'blue', 'cyan': 'blue',
      'forest': 'green', 'olive': 'green', 'lime': 'green',
      'tan': 'brown', 'beige': 'brown', 'khaki': 'brown',
      'charcoal': 'grey', 'silver': 'grey',
      'gold': 'yellow', 'amber': 'yellow'
    };

    const getFallbacks = (colors: string[]) => {
      const fallbacks = new Set<string>();
      colors.forEach(c => {
        if (colorGroupMap[c]) fallbacks.add(colorGroupMap[c]);
      });
      return Array.from(fallbacks);
    };

    console.log(`[outfitController] AI detected style: ${style}, colors: ${originalColors.join(', ')}, gender: ${gender}`);

    // Tiered Match: 1. Exact Name/Color + Gender
    let matchedProducts = await ProductModel.find({
      $and: [
        { style: style.toLowerCase() },
        { gender: { $in: [gender, 'unisex'] } },
        { 
          $or: [
            { primaryColor: { $in: originalColors } },
            { secondaryColor: { $in: originalColors } }
          ]
        }
      ]
    }).limit(10).lean();

    // Tiered Match: 2. Fallback to Color Groups + Gender
    if (matchedProducts.length === 0) {
      const fallbackColors = getFallbacks(originalColors);
      if (fallbackColors.length > 0) {
        console.log(`[outfitController] No exact matches for ${originalColors.join(', ')}. Trying fallbacks: ${fallbackColors.join(', ')}`);
        matchedProducts = await ProductModel.find({
          $and: [
            { style: style.toLowerCase() },
            { gender: { $in: [gender, 'unisex'] } },
            { 
              $or: [
                { primaryColor: { $in: fallbackColors } },
                { secondaryColor: { $in: fallbackColors } }
              ]
            }
          ]
        }).limit(10).lean();
      }
    }

    // Determine Suggested Shoe Color (Z)
    // If we have matches, use the first one's primary color. 
    // Otherwise, suggest a neutral (black/white/grey)
    let suggestedShoeColor = 'neutral';
    if (matchedProducts.length > 0) {
      suggestedShoeColor = (matchedProducts[0] as any).primaryColor || 'neutral';
    } else {
      // Logic for neutral suggestion if no matches
      suggestedShoeColor = originalColors.includes('black') || originalColors.includes('dark') ? 'white' : 'black';
    }

    res.json({
      detectedStyle: style,
      detectedColors: originalColors,
      suggestedShoeColor: suggestedShoeColor,
      recommendations: matchedProducts
    });
  } catch (err: any) {
    console.error('Match shoes error:', err);
    res.status(500).json({ error: 'Failed to match shoes', details: err.message });
  }
}

/**
 * NEW: Recommends an outfit for an uploaded shoe image.
 */
export async function recommendOutfitFromImage(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.file) {
      res.status(400).json({ error: 'Shoe image is required' });
      return;
    }

    const aiService = AIService.getInstance();
    const result = await aiService.getOutfitRecommendationForShoe(req.file.buffer, req.file.originalname);

    res.json(result);
  } catch (err: any) {
    console.error('Recommend outfit from image error:', err);
    res.status(500).json({ error: 'Failed to generate outfit recommendation', details: err.message });
  }
}


