import { jest } from '@jest/globals';

// Mock Firebase Admin
jest.mock('firebase-admin/app', () => ({
  initializeApp: jest.fn(),
  cert: jest.fn(),
}));

jest.mock('firebase-admin/auth', () => ({
  getAuth: () => ({
    verifyIdToken: jest.fn().mockResolvedValue({ uid: 'testuid123', email: 'test@example.com' }),
  }),
}));

// Mock Database Connection
jest.mock('../src/config/database.js', () => ({
  connectToDatabase: jest.fn().mockResolvedValue(true),
}));
