import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { CustomSkinModel } from '../models/CustomSkin.js';
import { RedesignRequestModel } from '../models/RedesignRequest.js';
import { UserModel } from '../models/User.js';
import { Types } from 'mongoose';
import { s3Service } from '../services/s3Service.js';

export async function createSkin(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { shoeId, skinName } = req.body;
    let textureUrl = req.body.textureUrl;

    if (!shoeId || !skinName) {
      res.status(400).json({ error: 'shoeId and skinName are required' });
      return;
    }

    if (req.file) {
      // Upload file to S3
      const key = `skins/${req.user.uid}/${Date.now()}_${req.file.originalname}`;
      textureUrl = await s3Service.uploadFile(key, req.file.buffer, req.file.mimetype);
    } else if (!textureUrl) {
      res.status(400).json({ error: 'Texture image or URL is required' });
      return;
    }

    const skin = await CustomSkinModel.create({
      userId: new Types.ObjectId(req.user.uid),
      shoeId: new Types.ObjectId(shoeId),
      skinName,
      textureUrl,
    });

    res.status(201).json(skin);
  } catch (err) {
    console.error('Create skin error:', err);
    res.status(500).json({ error: 'Failed to create skin' });
  }
}

export async function getSkins(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const skins = await CustomSkinModel
      .find({ userId: req.user.uid })
      .populate('shoeId')
      .lean();

    res.json(skins);
  } catch (err) {
    console.error('Get skins error:', err);
    res.status(500).json({ error: 'Failed to fetch skins' });
  }
}

export async function updateSkin(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { id } = req.params;
    const { skinName, textureUrl } = req.body;

    const skin = await CustomSkinModel.findOneAndUpdate(
      { _id: id, userId: req.user.uid },
      { skinName, textureUrl },
      { new: true }
    );

    if (!skin) {
      res.status(404).json({ error: 'Skin not found' });
      return;
    }

    res.json(skin);
  } catch (err) {
    console.error('Update skin error:', err);
    res.status(500).json({ error: 'Failed to update skin' });
  }
}

export async function deleteSkin(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { id } = req.params;
    await CustomSkinModel.deleteOne({ _id: id, userId: req.user.uid });

    res.status(204).send();
  } catch (err) {
    console.error('Delete skin error:', err);
    res.status(500).json({ error: 'Failed to delete skin' });
  }
}

export async function requestRedesign(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { shoeId, description } = req.body;

    if (!shoeId || !description) {
      res.status(400).json({ error: 'shoeId and description are required' });
      return;
    }

    const imageUrls: string[] = [];
    const files = req.files as Express.Multer.File[];
    
    if (files && files.length > 0) {
      for (const file of files) {
        const key = `redesigns/${req.user.uid}/${Date.now()}_${file.originalname.replace(/[^a-zA-Z0-9.]/g, '')}`;
        const url = await s3Service.uploadFile(key, file.buffer, file.mimetype);
        imageUrls.push(url);
      }
    }

    const request = await RedesignRequestModel.create({
      userId: new Types.ObjectId(req.user.uid),
      shoeId: new Types.ObjectId(shoeId),
      description,
      imageUrls,
    });

    res.status(201).json(request);
  } catch (err) {
    console.error('Request redesign error:', err);
    res.status(500).json({ error: 'Failed to explicitly request redesign' });
  }
}

export async function getRedesignRequests(req: AuthRequest, res: Response): Promise<void> {
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

    const requests = await RedesignRequestModel
      .find()
      .populate('userId', 'name email')
      .populate('shoeId', 'name brand')
      .sort({ createdAt: -1 })
      .lean();

    res.json(requests);
  } catch (err) {
    console.error('Get redesign requests error:', err);
    res.status(500).json({ error: 'Failed to fetch redesign requests' });
  }
}
