import { Request, Response, NextFunction } from 'express';
import { ParamsDictionary } from 'express-serve-static-core';
import { ParsedQs } from 'qs';
import { getFirebaseAdmin } from '../config/firebase.js';
import { SellerProfileModel } from '../models/SellerProfile.js';

export interface SellerAuthRequest extends Request<ParamsDictionary, any, any, ParsedQs> {
  user?: { uid: string; email?: string };
  seller?: { uid: string; email: string; companyName: string };
}

/**
 * Middleware: verifies Firebase ID token AND confirms the user has a
 * SellerProfile document in MongoDB (i.e. they completed self-registration).
 *
 * All seller-only routes are protected by this middleware.
 */
export async function sellerAuthMiddleware(
  req: SellerAuthRequest,
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

    // Dev-only bypass
    if (token === 'test_seller_token' && process.env.NODE_ENV === 'development') {
      req.user = { uid: '000000000000000000000456', email: 'sellerdev@example.com' };
      req.seller = { uid: '000000000000000000000456', email: 'sellerdev@example.com', companyName: 'Dev Seller Co.' };
      return next();
    }

    const admin = getFirebaseAdmin();
    const decodedToken = await admin.auth().verifyIdToken(token);

    const sellerProfile = await SellerProfileModel.findOne({ uid: decodedToken.uid }).lean();

    if (!sellerProfile) {
      res.status(403).json({
        error: 'Access denied. You do not have a seller account.',
        code: 'NOT_A_SELLER'
      });
      return;
    }

    req.user = { uid: decodedToken.uid, email: decodedToken.email };
    req.seller = {
      uid: sellerProfile.uid,
      email: sellerProfile.email,
      companyName: sellerProfile.companyName,
    };
    next();
  } catch (err) {
    console.error('[sellerAuthMiddleware] Error:', err);
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}
