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
    console.log('[catalogController] Fetching all products...');
    const cached = await cacheService.getCatalog();
    if (cached) {
      console.log(`[catalogController] Returning ${cached.length} cached products`);
      res.json(cached);
      return;
    }

    console.log('[catalogController] Cache miss, querying database...');
    const products = await ProductModel.find().lean();
    console.log(`[catalogController] Database returned ${products.length} products`);
    
    await cacheService.setCatalog(products);
    res.json(products);
  } catch (err) {
    console.error('[catalogController] Get products error:', err);
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

export async function uploadShoeModel(req: Request, res: Response): Promise<void> {
  try {
    const { name, brand, price, category, colors, sizes, modelUrl, arLensId, arLensGroupId } = req.body;

    if (!name || !brand || !price || !category) {
      res.status(400).json({ error: 'name, brand, price, and category are required' });
      return;
    }

    const files = req.files as { [fieldname: string]: Express.Multer.File[] };
    let finalModelUrl = modelUrl;
    let finalThumbnailUrl = undefined;

    if (files && files['model'] && files['model'].length > 0) {
      const modelFile = files['model'][0];
      const key = `3d_models/${brand.replace(/\\s+/g, '_')}/${Date.now()}_${modelFile.originalname.replace(/[^a-zA-Z0-9.]/g, '')}`;
      finalModelUrl = await s3Service.uploadFile(key, modelFile.buffer, modelFile.mimetype);
    }

    if (files && files['thumbnail'] && files['thumbnail'].length > 0) {
      const thumbFile = files['thumbnail'][0];
      const thumbKey = `thumbnails/${brand.replace(/\\s+/g, '_')}/${Date.now()}_${thumbFile.originalname.replace(/[^a-zA-Z0-9.]/g, '')}`;
      finalThumbnailUrl = await s3Service.uploadFile(thumbKey, thumbFile.buffer, thumbFile.mimetype);
    }

    let parsedColors = [];
    try { parsedColors = typeof colors === 'string' ? JSON.parse(colors) : (colors || []); } catch (e) {}

    let parsedSizes = [];
    try { parsedSizes = typeof sizes === 'string' ? JSON.parse(sizes) : (sizes || []); } catch (e) {}

    const updateData = {
      name,
      brand,
      price: Number(price),
      category,
      colors: parsedColors,
      sizes: parsedSizes,
      ...(finalModelUrl && { modelUrl: finalModelUrl }),
      ...(finalThumbnailUrl && { thumbnailUrl: finalThumbnailUrl }),
      ...(arLensId && { arLensId }),
      ...(arLensGroupId && { arLensGroupId })
    };

    const existingProduct = await ProductModel.findOne({ name, brand });

    if (existingProduct) {
      Object.assign(existingProduct, updateData);
      const updatedProduct = await existingProduct.save();
      await cacheService.clearCatalog();
      res.status(200).json(updatedProduct);
      return;
    }

    const product = await ProductModel.create(updateData);
    await cacheService.clearCatalog();
    res.status(201).json(product);
  } catch (err: any) {
    console.error('Upload 3D model error details:', err.message, err.stack);
    res.status(500).json({ error: 'Failed to upload 3D model', details: err.message });
  }
}

