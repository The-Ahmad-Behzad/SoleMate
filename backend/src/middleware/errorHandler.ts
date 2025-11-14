import { Request, Response, NextFunction } from 'express';
import { AppError } from '../utils/errors.js';
import pino from 'pino';

const logger = pino();

export function errorHandler(
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  if (err instanceof AppError) {
    logger.error({ err, statusCode: err.statusCode }, 'Application error');
    res.status(err.statusCode).json({
      error: err.message,
      statusCode: err.statusCode,
    });
    return;
  }

  logger.error({ err }, 'Unhandled error');
  
  res.status(500).json({
    error: 'Internal server error',
    statusCode: 500,
  });
}

export function notFoundHandler(_req: Request, res: Response): void {
  res.status(404).json({
    error: 'Route not found',
    statusCode: 404,
  });
}

