import { Response } from 'express';
import { SellerAuthRequest } from '../middleware/sellerAuthMiddleware.js';
import { SellerProfileModel } from '../models/SellerProfile.js';
import { GlbUploadModel } from '../models/GlbUpload.js';
import { IntegrationRequestModel } from '../models/IntegrationRequest.js';
import { s3Service } from '../services/s3Service.js';
import { sendAdminIntegrationNotification } from '../services/emailService.js';
import { getFirebaseAdmin } from '../config/firebase.js';
import { AuthRequest } from '../middleware/authMiddleware.js';

// ─── Slugify helper ───────────────────────────────────────────────────────────
function slugify(str: string): string {
  return str.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '');
}

// ─── Auth: Register Seller ────────────────────────────────────────────────────
export async function registerSeller(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) { res.status(401).json({ error: 'Unauthorized' }); return; }

    const { displayName, companyName, phone, website, bio } = req.body;
    if (!displayName || !companyName) {
      res.status(400).json({ error: 'displayName and companyName are required' });
      return;
    }

    const existing = await SellerProfileModel.findOne({ uid: req.user.uid });
    if (existing) {
      res.status(409).json({ error: 'Seller profile already exists', profile: existing });
      return;
    }

    const profile = await SellerProfileModel.create({
      uid: req.user.uid,
      email: req.user.email || '',
      displayName,
      companyName,
      phone,
      website,
      bio,
      isApproved: true,
    });

    res.status(201).json(profile);
  } catch (err) {
    console.error('[sellerController] registerSeller:', err);
    res.status(500).json({ error: 'Failed to register seller' });
  }
}

// ─── Auth: Get Seller Profile ─────────────────────────────────────────────────
export async function getSellerProfile(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const profile = await SellerProfileModel.findOne({ uid: req.seller!.uid }).lean();
    if (!profile) { res.status(404).json({ error: 'Profile not found' }); return; }
    res.json(profile);
  } catch (err) {
    console.error('[sellerController] getSellerProfile:', err);
    res.status(500).json({ error: 'Failed to fetch profile' });
  }
}

// ─── Auth: Update Seller Profile ──────────────────────────────────────────────
export async function updateSellerProfile(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const { displayName, companyName, phone, website, bio } = req.body;
    const updated = await SellerProfileModel.findOneAndUpdate(
      { uid: req.seller!.uid },
      { displayName, companyName, phone, website, bio },
      { new: true }
    ).lean();
    res.json(updated);
  } catch (err) {
    console.error('[sellerController] updateSellerProfile:', err);
    res.status(500).json({ error: 'Failed to update profile' });
  }
}

// ─── GLB Upload: Get Presigned URL ────────────────────────────────────────────
export async function getUploadPresignedUrl(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const { fileName, contentType, shoeName, brand } = req.body;

    if (!fileName || !contentType || !shoeName || !brand) {
      res.status(400).json({ error: 'fileName, contentType, shoeName, and brand are required' });
      return;
    }

    // Validate file type
    if (contentType !== 'model/gltf-binary' && !fileName.toLowerCase().endsWith('.glb')) {
      res.status(400).json({ error: 'Only .glb files are supported' });
      return;
    }

    const brandSlug  = slugify(brand);
    const fileSlug   = slugify(fileName.replace(/\.glb$/i, ''));
    const s3Key      = `shoes/${brandSlug}/${fileSlug}-${Date.now()}.glb`;

    const { PutObjectCommand } = await import('@aws-sdk/client-s3');
    const { getSignedUrl }     = await import('@aws-sdk/s3-request-presigner');
    const { S3Client }         = await import('@aws-sdk/client-s3');

    const region    = process.env.AWS_REGION || 'ap-southeast-2';
    const bucket    = process.env.AWS_BUCKET_NAME!;
    const accessKey = process.env.AWS_ACCESS_KEY_ID!;
    const secretKey = process.env.AWS_SECRET_ACCESS_KEY!;

    const client = new S3Client({
      region,
      credentials: { accessKeyId: accessKey, secretAccessKey: secretKey },
    });

    const command = new PutObjectCommand({
      Bucket:      bucket,
      Key:         s3Key,
      ContentType: 'model/gltf-binary',
    });

    const presignedUrl = await getSignedUrl(client, command, { expiresIn: 900 }); // 15 minutes
    const s3Url = `https://${bucket}.s3.${region}.amazonaws.com/${s3Key}`;

    res.json({ presignedUrl, s3Key, s3Url, expiresIn: 900 });
  } catch (err) {
    console.error('[sellerController] getUploadPresignedUrl:', err);
    res.status(500).json({ error: 'Failed to generate upload URL' });
  }
}

// ─── GLB Upload: Confirm Upload ───────────────────────────────────────────────
export async function confirmUpload(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const {
      s3Key, s3Url, shoeName, brand, description,
      scale, positionOffset, rotationOffset,
    } = req.body;

    if (!s3Key || !s3Url || !shoeName || !brand) {
      res.status(400).json({ error: 's3Key, s3Url, shoeName, and brand are required' });
      return;
    }

    const upload = await GlbUploadModel.create({
      sellerId:    req.seller!.uid,
      sellerEmail: req.seller!.email,
      shoeName,
      brand,
      description,
      s3Key,
      s3Url,
      scale,
      positionOffset,
      rotationOffset,
      status: 'pending',
    });

    res.status(201).json(upload);
  } catch (err) {
    console.error('[sellerController] confirmUpload:', err);
    res.status(500).json({ error: 'Failed to record upload' });
  }
}

// ─── GLB Upload: List My Uploads ─────────────────────────────────────────────
export async function getMyUploads(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const { status, page = '1', limit = '20' } = req.query;
    const filter: Record<string, unknown> = { sellerId: req.seller!.uid };
    if (status) filter.status = status;

    const pageNum  = Math.max(1, parseInt(page as string));
    const limitNum = Math.min(100, parseInt(limit as string));

    const [uploads, total] = await Promise.all([
      GlbUploadModel.find(filter)
        .sort({ createdAt: -1 })
        .skip((pageNum - 1) * limitNum)
        .limit(limitNum)
        .lean(),
      GlbUploadModel.countDocuments(filter),
    ]);

    res.json({ uploads, total, page: pageNum, limit: limitNum });
  } catch (err) {
    console.error('[sellerController] getMyUploads:', err);
    res.status(500).json({ error: 'Failed to fetch uploads' });
  }
}

// ─── GLB Upload: Get Single Upload ───────────────────────────────────────────
export async function getUploadById(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const upload = await GlbUploadModel.findOne({ _id: req.params.id, sellerId: req.seller!.uid }).lean();
    if (!upload) { res.status(404).json({ error: 'Upload not found' }); return; }
    res.json(upload);
  } catch (err) {
    console.error('[sellerController] getUploadById:', err);
    res.status(500).json({ error: 'Failed to fetch upload' });
  }
}

// ─── Integration Request: Submit ─────────────────────────────────────────────
export async function submitIntegrationRequest(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const { glbUploadId, requestNote } = req.body;
    if (!glbUploadId) { res.status(400).json({ error: 'glbUploadId is required' }); return; }

    // Verify ownership
    const upload = await GlbUploadModel.findOne({ _id: glbUploadId, sellerId: req.seller!.uid }).lean();
    if (!upload) { res.status(404).json({ error: 'Upload not found or access denied' }); return; }

    // Block duplicate pending requests
    const existing = await IntegrationRequestModel.findOne({ glbUploadId, status: { $in: ['pending', 'in_review', 'scheduled'] } });
    if (existing) {
      res.status(409).json({ error: 'An active integration request already exists for this upload', request: existing });
      return;
    }

    // Build catalogue entry that admin can paste directly into catalogue.json
    const shoeName  = upload.shoeName;
    const shoeId    = `${slugify(upload.brand)}_${slugify(shoeName)}`;
    const catalogueEntry = {
      id:              shoeId,
      name:            shoeName,
      thumbnailUrl:    upload.thumbnailUrl || '',
      modelUrl:        upload.s3Url,
      scale:           upload.scale          || [-1, -1, -1],
      positionOffset:  upload.positionOffset || [0,  0,  0],
      rotationOffset:  upload.rotationOffset || [0,  0,  0],
    };

    // Save integration request in MongoDB
    const integrationRequest = await IntegrationRequestModel.create({
      glbUploadId,
      sellerId:    req.seller!.uid,
      sellerEmail: req.seller!.email,
      shoeName,
      requestNote,
      status: 'pending',
      catalogueEntry,
      adminEmailSent: false,
    });

    // Save notification in Firestore
    let firestoreDocId = '';
    try {
      const admin = getFirebaseAdmin();
      const firestore = admin.firestore();
      const docRef = await firestore.collection('glbIntegrationRequests').add({
        integrationRequestId: integrationRequest._id.toString(),
        glbUploadId:          glbUploadId.toString(),
        sellerId:             req.seller!.uid,
        sellerEmail:          req.seller!.email,
        sellerCompany:        req.seller!.companyName,
        shoeName,
        brand:                upload.brand,
        s3Url:                upload.s3Url,
        requestNote:          requestNote || '',
        status:               'pending',
        catalogueEntry,
        createdAt:            admin.firestore.FieldValue.serverTimestamp(),
      });
      firestoreDocId = docRef.id;
      await IntegrationRequestModel.findByIdAndUpdate(integrationRequest._id, { firestoreDocId });
    } catch (fsErr) {
      console.error('[sellerController] Firestore write failed (non-fatal):', fsErr);
    }

    // Send admin email notification
    try {
      await sendAdminIntegrationNotification({
        sellerEmail:          req.seller!.email,
        sellerCompany:        req.seller!.companyName,
        shoeName,
        brand:                upload.brand,
        s3Url:                upload.s3Url,
        requestNote,
        integrationRequestId: integrationRequest._id.toString(),
        catalogueEntry,
      });
      await IntegrationRequestModel.findByIdAndUpdate(integrationRequest._id, { adminEmailSent: true });
    } catch (emailErr) {
      console.error('[sellerController] Email notification failed (non-fatal):', emailErr);
    }

    res.status(201).json({ ...integrationRequest.toObject(), firestoreDocId });
  } catch (err) {
    console.error('[sellerController] submitIntegrationRequest:', err);
    res.status(500).json({ error: 'Failed to submit integration request' });
  }
}

// ─── Integration Request: List My Requests ───────────────────────────────────
export async function getMyIntegrationRequests(req: SellerAuthRequest, res: Response): Promise<void> {
  try {
    const requests = await IntegrationRequestModel.find({ sellerId: req.seller!.uid })
      .sort({ createdAt: -1 })
      .populate('glbUploadId')
      .lean();
    res.json(requests);
  } catch (err) {
    console.error('[sellerController] getMyIntegrationRequests:', err);
    res.status(500).json({ error: 'Failed to fetch integration requests' });
  }
}
