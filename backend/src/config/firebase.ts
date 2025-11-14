import admin from 'firebase-admin';

let firebaseInitialized = false;

function initializeFirebase() {
  if (firebaseInitialized) {
    return;
  }

  const projectId = process.env.FIREBASE_PROJECT_ID;
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n');

  if (!projectId || !clientEmail || !privateKey || privateKey === '-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n') {
    throw new Error(
      'Missing Firebase Admin credentials. ' +
      'Please set FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, and FIREBASE_PRIVATE_KEY in .env file. ' +
      'Get credentials from Firebase Console > Project Settings > Service Accounts.'
    );
  }

  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId,
        clientEmail,
        privateKey,
      }),
      projectId,
    });
  }
  
  firebaseInitialized = true;
}

// Lazy initialization - only initialize when auth is actually needed
export function getFirebaseAdmin() {
  initializeFirebase();
  return admin;
}

export default {
  get auth() {
    initializeFirebase();
    return admin.auth();
  },
  firestore() {
    initializeFirebase();
    return admin.firestore();
  },
  storage() {
    initializeFirebase();
    return admin.storage();
  }
};

