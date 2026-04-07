import { Request, Response } from 'express';
import { ProductModel } from '../models/Product.js';
import { cacheService } from '../services/cacheService.js';
import { ShoeUploadRequestModel } from '../models/ShoeUploadRequest.js';
import { AuthRequest } from '../middleware/authMiddleware.js';
import { UserModel } from '../models/User.js';
import { Types } from 'mongoose';
import { s3Service } from '../services/s3Service.js';

export async function getAllProducts(_req: Request, res: Response): Promise<void> {
  try {
    const cached = await cacheService.getCatalog();
    if (cached) {
      res.json(cached);
      return;
    }

    const products = await ProductModel.find().lean();
    await cacheService.setCatalog(products);
    res.json(products);
  } catch (err) {
    console.error('Get products error:', err);
    res.status(500).json({ error: 'Failed to fetch products' });
  }
}

export async function getProductById(req: Request, res: Response): Promise<void> {
  try {
    const { id } = req.params;
    const product = await ProductModel.findById(id).lean();
    
    if (!product) {
      res.status(404).json({ error: 'Product not found' });
      return;
    }
    
    res.json(product);
  } catch (err) {
    console.error('Get product error:', err);
    res.status(500).json({ error: 'Failed to fetch product' });
  }
}

export async function searchProducts(req: Request, res: Response): Promise<void> {
  try {
    const { q } = req.query;
    if (!q) {
      res.status(400).json({ error: 'Query parameter required' });
      return;
    }

    const products = await ProductModel.find({
      $text: { $search: q as string }
    }).lean();

    res.json(products);
  } catch (err) {
    console.error('Search error:', err);
    res.status(500).json({ error: 'Search failed' });
  }
}

export async function requestShoe(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const user = await UserModel.findOne({ uid: req.user.uid });
    if (!user || user.role !== 'seller') {
      res.status(403).json({ error: 'Forbidden: Sellers only' });
      return;
    }

    const { shoeName, brand, description } = req.body;

    if (!shoeName || !brand || !description) {
      res.status(400).json({ error: 'shoeName, brand, and description are required' });
      return;
    }

    const imageUrls: string[] = [];
    const files = req.files as Express.Multer.File[];
    
    if (files && files.length > 0) {
      for (const file of files) {
        const key = `shoe_requests/${req.user.uid}/${Date.now()}_${file.originalname.replace(/[^a-zA-Z0-9.]/g, '')}`;
        const url = await s3Service.uploadFile(key, file.buffer, file.mimetype);
        imageUrls.push(url);
      }
    }

    const request = await ShoeUploadRequestModel.create({
      sellerId: user._id,
      shoeName,
      brand,
      description,
      imageUrls,
    });

    res.status(201).json(request);
  } catch (err: any) {
    console.error('Request shoe error details:', err.message, err.stack);
    res.status(500).json({ error: 'Failed to request shoe upload', details: err.message });
  }
}

