import mongoose from 'mongoose';

const DEFAULT_DB_NAME = process.env.MONGODB_DB_NAME || 'solemate_db';

export async function connectToDatabase(): Promise<typeof mongoose> {
  const mongoUri = process.env.MONGODB_URI;

  if (!mongoUri) {
    throw new Error('MONGODB_URI is not set');
  }

  const connection = await mongoose.connect(mongoUri, {
    dbName: DEFAULT_DB_NAME,
    appName: 'solemate-bff',
    autoIndex: true,
    maxPoolSize: 10
  } as any);

  console.log(`[Database] Successfully connected to MongoDB: ${DEFAULT_DB_NAME}`);
  return connection;
}


export async function disconnectFromDatabase(): Promise<void> {
  await mongoose.disconnect();
}



