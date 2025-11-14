export class AppError extends Error {
  constructor(
    public statusCode: number,
    public message: string,
    public isOperational = true
  ) {
    super(message);
    Object.setPrototypeOf(this, AppError.prototype);
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super(400, message);
    this.name = 'ValidationError';
  }
}

export class AuthError extends AppError {
  constructor(message: string = 'Unauthorized') {
    super(401, message);
    this.name = 'AuthError';
  }
}

export class NotFoundError extends AppError {
  constructor(message: string = 'Resource not found') {
    super(404, message);
    this.name = 'NotFoundError';
  }
}

export class DatabaseError extends AppError {
  constructor(message: string = 'Database operation failed') {
    super(500, message);
    this.name = 'DatabaseError';
  }
}

export class StorageError extends AppError {
  constructor(message: string = 'Storage operation failed') {
    super(500, message);
    this.name = 'StorageError';
  }
}

