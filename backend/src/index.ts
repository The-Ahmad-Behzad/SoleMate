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

const logger = pino({ level: process.env.LOG_LEVEL || 'info' });

app.use(helmet());
app.use(cors({ origin: '*'}));
app.use(express.json({ limit: '2mb' }));
app.use(pinoHttp({ logger: logger as any }));

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

app.use('/api', apiRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

const port = Number(process.env.PORT || 8080);

// Initialize connections on startup
async function startServer() {
  // Initialize Redis connection (will connect automatically)
  const redis = (await import('./config/redis.js')).default;
  redis.instance; // Trigger initialization
  
  // Try to connect to MongoDB, but don't block server startup
  try {
    await connectToDatabase();
    logger.info('[OK] MongoDB connected');
  } catch (err: any) {
    logger.warn({ err: err.message }, '[WARN] MongoDB connection failed - server will start but database operations will fail');
    logger.warn('[TIP] To fix: Add your IP address to MongoDB Atlas IP whitelist');
    logger.warn('      See: https://www.mongodb.com/docs/atlas/security-whitelist/');
    // Continue server startup even if MongoDB fails
    // This allows Redis and other services to work
  }
  
  app.listen(port, () => {
    logger.info({ port }, 'BFF listening');
  });
}

startServer();
