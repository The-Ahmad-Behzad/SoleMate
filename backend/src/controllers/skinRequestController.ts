import { Response, Request } from 'express';
import { SellerAuthRequest } from '../middleware/sellerAuthMiddleware.js';
import { SkinDesignRequestModel } from '../models/SkinDesignRequest.js';

// ─── Mobile App: Submit a Skin Design Request ────────────────────────────────
export async function createSkinDesignRequest(req: Request, res: Response): Promise<void> {
  try {
    const { userEmail, shoeId, shoeName, description, referenceImageUrls, userFirebaseUid } = req.body;

    if (!userEmail || !shoeId || !description) {
      res.status(400).json({ error: 'userEmail, shoeId, and description are required' });
      return;
    }

    if (!Array.isArray(referenceImageUrls) || referenceImageUrls.length === 0) {
      res.status(400).json({ error: 'At least one reference image URL is required' });
      return;
    }

    if (referenceImageUrls.length > 5) {
      res.status(400).json({ error: 'Maximum 5 reference images allowed' });
      return;
    }

    const skinRequest = await SkinDesignRequestModel.create({
      userFirebaseUid,
      userEmail,
      shoeId,
      shoeName,
      description,
      referenceImageUrls,
      status: 'new',
    });

    res.status(201).json(skinRequest);
  } catch (err) {
    console.error('[skinRequestController] createSkinDesignRequest:', err);
    res.status(500).json({ error: 'Failed to submit design request' });
  }
}

// ─── Seller: List All Skin Design Requests ───────────────────────────────────
export async function getSkinDesignRequests(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const {
      status, shoeId, search,
      page = '1', limit = '20',
      sortBy = 'createdAt', sortOrder = 'desc',
    } = req.query as Record<string, string>;

    const filter: Record<string, unknown> = {};
    if (status) filter.status = status;
    if (shoeId) filter.shoeId = shoeId;
    if (search) {
      filter.$or = [
        { userEmail:   { $regex: search, $options: 'i' } },
        { description: { $regex: search, $options: 'i' } },
        { shoeName:    { $regex: search, $options: 'i' } },
      ];
    }

    const pageNum  = Math.max(1, parseInt(page));
    const limitNum = Math.min(100, parseInt(limit));
    const sort: Record<string, 1 | -1> = { [sortBy]: sortOrder === 'asc' ? 1 : -1 };

    const [requests, total] = await Promise.all([
      SkinDesignRequestModel.find(filter)
        .sort(sort)
        .skip((pageNum - 1) * limitNum)
        .limit(limitNum)
        .lean(),
      SkinDesignRequestModel.countDocuments(filter),
    ]);

    // Count by status for dashboard stats
    const statusCounts = await SkinDesignRequestModel.aggregate([
      { $group: { _id: '$status', count: { $sum: 1 } } },
    ]);

    res.json({
      requests,
      total,
      page: pageNum,
      limit: limitNum,
      statusCounts: statusCounts.reduce((acc: Record<string, number>, s) => {
        acc[s._id] = s.count;
        return acc;
      }, {}),
    });
  } catch (err) {
    console.error('[skinRequestController] getSkinDesignRequests:', err);
    res.status(500).json({ error: 'Failed to fetch design requests' });
  }
}

// ─── Seller: Get Single Request ───────────────────────────────────────────────
export async function getSkinDesignRequestById(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const request = await SkinDesignRequestModel.findById(req.params.id).lean();
    if (!request) { res.status(404).json({ error: 'Request not found' }); return; }

    // Auto-mark as viewed if still 'new'
    if (request.status === 'new') {
      await SkinDesignRequestModel.findByIdAndUpdate(req.params.id, {
        status: 'viewed',
        viewedAt: new Date(),
      });
    }

    res.json(request);
  } catch (err) {
    console.error('[skinRequestController] getSkinDesignRequestById:', err);
    res.status(500).json({ error: 'Failed to fetch request' });
  }
}

// ─── Seller: Update Request Status / Notes ───────────────────────────────────
export async function updateSkinDesignRequest(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const { status, sellerNotes } = req.body;
    const validStatuses = ['viewed', 'in_progress', 'completed', 'rejected'];

    if (status && !validStatuses.includes(status)) {
      res.status(400).json({ error: `Status must be one of: ${validStatuses.join(', ')}` });
      return;
    }

    const update: Record<string, unknown> = {};
    if (status) update.status = status;
    if (sellerNotes !== undefined) update.sellerNotes = sellerNotes;
    if (status === 'completed') update.completedAt = new Date();

    const updated = await SkinDesignRequestModel.findByIdAndUpdate(
      req.params.id,
      update,
      { new: true }
    ).lean();

    if (!updated) { res.status(404).json({ error: 'Request not found' }); return; }
    res.json(updated);
  } catch (err) {
    console.error('[skinRequestController] updateSkinDesignRequest:', err);
    res.status(500).json({ error: 'Failed to update request' });
  }
}
