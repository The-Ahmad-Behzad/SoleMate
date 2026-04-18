import { jest } from '@jest/globals';
import request from 'supertest';
import express from 'express';
import skinRoutes from '../src/routes/skinRoutes.js';
import { CustomSkinModel } from '../src/models/CustomSkin.js';

const app = express();
app.use(express.json());
// Stub auth
app.use((req, res, next) => {
  (req as any).user = { uid: 'testuser123' };
  next();
});
app.use('/api/skins', skinRoutes);

jest.mock('../src/models/CustomSkin.js', () => ({
  CustomSkinModel: {
    create: jest.fn().mockResolvedValue({ _id: 'skin1' }),
    find: jest.fn().mockReturnThis(),
    populate: jest.fn().mockReturnThis(),
    lean: jest.fn().mockResolvedValue([{ _id: 'skin1' }]),
    findOneAndUpdate: jest.fn().mockResolvedValue({ _id: 'skin1' }),
    deleteOne: jest.fn().mockResolvedValue({ deletedCount: 1 })
  }
}));

jest.mock('../src/services/s3Service.js', () => ({
  s3Service: {
    uploadFile: jest.fn().mockResolvedValue('/uploads/test.png')
  }
}));

describe('Skins API', () => {
  it('GET /api/skins returns user skins', async () => {
    const res = await request(app).get('/api/skins');
    expect(res.status).toBe(200);
  });

  it('POST /api/skins fails if shoeId missing', async () => {
    const res = await request(app).post('/api/skins').send({});
    expect(res.status).toBe(400);
  });
});
