import { Request, Response } from 'express';
import { ProductModel } from '../models/Product.js';
import { cacheService } from '../services/cacheService.js';

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


