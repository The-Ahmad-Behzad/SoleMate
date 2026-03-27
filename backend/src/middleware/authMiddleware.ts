import { Request, Response, NextFunction } from 'express';
import { getFirebaseAdmin } from '../config/firebase.js';
import dotenv from 'dotenv';

dotenv.config();

export interface AuthRequest extends Request {
  user?: { uid: string; email?: string };
}

export async function authMiddleware(
  req: AuthRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'No token provided' });
      return;
    }

    const token = authHeader.split('Bearer ')[1];

    // Development bypass for manual testing
    if (token === 'test_token') {
      // Use a valid 24-char hex string for the UID to avoid Mongoose CastErrors
      req.user = { uid: '000000000000000000000123', email: 'dev@example.com' };
      return next();
    }

    const admin = getFirebaseAdmin();
    const decodedToken = await admin.auth().verifyIdToken(token);
    
    req.user = { uid: decodedToken.uid, email: decodedToken.email };
    next();
  } catch (err) {
    console.error('Auth error:', err);
    res.status(401).json({ error: 'Invalid token' });
  }
}


