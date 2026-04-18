import { jest } from '@jest/globals';
import request from 'supertest';
import express from 'express';
import outfitRoutes from '../src/routes/outfitRoutes.js';
import { OutfitMatchModel } from '../src/models/OutfitMatch.js';

const app = express();
app.use(express.json());
// Stub auth
app.use((req, res, next) => {
  (req as any).user = { uid: 'testuser123' };
  next();
});
app.use('/api/outfit', outfitRoutes);

jest.mock('../src/models/OutfitMatch.js', () => ({
  OutfitMatchModel: {
    create: jest.fn().mockResolvedValue({ _id: 'match1' }),
    find: jest.fn().mockReturnThis(),
    populate: jest.fn().mockReturnThis(),
    sort: jest.fn().mockReturnThis(),
    limit: jest.fn().mockReturnThis(),
    lean: jest.fn().mockResolvedValue([{ _id: 'match1' }]),
  }
}));

jest.mock('../src/services/aiService.js', () => ({
  AIService: {
    getInstance: jest.fn().mockReturnValue({
      getRecommendations: jest.fn().mockResolvedValue([{ _id: 'shoe1' }])
    })
  }
}));

describe('Outfit API', () => {
  it('POST /api/outfit/analyze accepts valid body', async () => {
    const res = await request(app)
      .post('/api/outfit/analyze')
      .send({ outfitImageUrl: 'test.jpg' });
    expect(res.status).toBe(200);
  });

  it('POST /api/outfit/analyze rejects missing fields', async () => {
    const res = await request(app)
      .post('/api/outfit/analyze')
      .send({});
    expect(res.status).toBe(400);
  });

  it('POST /api/outfit/recommend returns shoes', async () => {
    const res = await request(app)
      .post('/api/outfit/recommend')
      .send({ colors: ['red', 'blue'] });
    expect(res.status).toBe(200);
    expect(res.body).toEqual([{ _id: 'shoe1' }]);
  });

  it('POST /api/outfit/recommend rejects omitting colors array', async () => {
    const res = await request(app)
      .post('/api/outfit/recommend')
      .send({});
    expect(res.status).toBe(400);
  });
});
