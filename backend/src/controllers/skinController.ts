import { AuthRequest } from '../middleware/authMiddleware.js';
import { Response } from 'express';
import { CustomSkinModel } from '../models/CustomSkin.js';
import { Types } from 'mongoose';

export async function createSkin(req: AuthRequest, res: Response): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const { shoeId, skinName, textureUrl } = req.body;

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


