import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// Load .env file explicitly
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
dotenv.config({ path: join(__dirname, '../.env') });

import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import pino from 'pino';
import pinoHttp from 'pino-http';
import { connectToDatabase } from './config/database.js';
import apiRoutes from './routes/index.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';

const app = express();

// Required for express-rate-limit to work correctly behind reverse proxies (Railway, Heroku, etc.)
app.set('trust proxy', 1);

const logger = pino({ level: process.env.LOG_LEVEL || 'info' });

app.use(helmet({ crossOriginResourcePolicy: false })); // allow static files (images) to be served cross-origin
app.use(cors({ origin: '*' }));
app.use(express.json({ limit: '2mb' }));
app.use(pinoHttp({ logger: logger as any }));

app.use('/uploads', express.static(join(process.cwd(), 'uploads')));

app.get('/health', async (_req, res) => {
  try {
    await connectToDatabase();
    res.status(200).json({
      ok: true,
      service: 'solemate-bff',
      version: process.env.npm_package_version,
      time: new Date().toISOString()
    });
  } catch (err) {
    res.status(500).json({ ok: false, error: 'Database connection failed' });
  }
});

app.get('/api/health/storage', (_req, res) => {
  const isMockS3 = (process.env.AWS_ACCESS_KEY_ID || 'PLACEHOLDER_KEY') === 'PLACEHOLDER_KEY';
  res.status(200).json({
    ok: true,
    storageType: isMockS3 ? 'local_mock' : 'aws_s3'
  });
});

import rateLimit from 'express-rate-limit';

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per `window` (here, per 15 minutes)
  standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
  legacyHeaders: false, // Disable the `X-RateLimit-*` headers
  message: { error: 'Too many requests, please try again later.' }
});

app.use('/api', apiLimiter, apiRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

const port = Number(process.env.PORT || 8080);

app.listen(port, async () => {
  logger.info({ port }, 'BFF listening');
  try {
    await connectToDatabase();
    logger.info('Connected to MongoDB');
  } catch (err) {
    logger.error({ err }, 'Failed to connect to MongoDB at startup');
  }
});
