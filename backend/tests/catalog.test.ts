import { jest } from '@jest/globals';
import request from 'supertest';
import express from 'express';
import catalogRoutes from '../src/routes/catalogRoutes.js';
import { ProductModel } from '../src/models/Product.js';
import { cacheService } from '../src/services/cacheService.js';

const app = express();
app.use(express.json());
app.use('/api/catalog', catalogRoutes);

jest.mock('../src/models/Product.js', () => ({
  ProductModel: {
    find: jest.fn(),
    findById: jest.fn()
  }
}));

jest.mock('../src/services/cacheService.js', () => ({
  cacheService: {
    getCatalog: jest.fn(),
    setCatalog: jest.fn()
  }
}));

describe('Catalog API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('GET /api/catalog', () => {
    it('returns a list of products', async () => {
      (cacheService.getCatalog as jest.Mock).mockResolvedValue(null);
      
      const mockProducts = [{ _id: '1', name: 'Shoe 1' }];
      (ProductModel.find as jest.Mock).mockReturnValue({
        lean: jest.fn().mockResolvedValue(mockProducts)
      });

      const res = await request(app).get('/api/catalog');
      expect(res.status).toBe(200);
      expect(res.body).toEqual(mockProducts);
    });
  });

  describe('GET /api/catalog/:id', () => {
    it('returns product if found', async () => {
      const mockProduct = { _id: '1', name: 'Shoe 1' };
      (ProductModel.findById as jest.Mock).mockReturnValue({
        lean: jest.fn().mockResolvedValue(mockProduct)
      });

      const res = await request(app).get('/api/catalog/1');
      expect(res.status).toBe(200);
      expect(res.body).toEqual(mockProduct);
    });

    it('returns 404 if not found', async () => {
      (ProductModel.findById as jest.Mock).mockReturnValue({
        lean: jest.fn().mockResolvedValue(null)
      });

      const res = await request(app).get('/api/catalog/invalid');
      expect(res.status).toBe(404);
    });
  });
});
