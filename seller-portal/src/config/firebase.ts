import { initializeApp, getApps } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';

const appId = import.meta.env.VITE_FIREBASE_APP_ID as string | undefined;
if (!appId || appId.includes('REPLACE_WITH')) {
  console.warn(
    '[SoleMate Seller Portal] VITE_FIREBASE_APP_ID is not configured. ' +
    'Go to Firebase Console → Project Settings → General → Your Apps → Add Web App to get your App ID, ' +
    'then update seller-portal/.env'
  );
}

const firebaseConfig = {
  apiKey:            import.meta.env.VITE_FIREBASE_API_KEY as string,
  authDomain:        import.meta.env.VITE_FIREBASE_AUTH_DOMAIN as string,
  projectId:         import.meta.env.VITE_FIREBASE_PROJECT_ID as string,
  storageBucket:     import.meta.env.VITE_FIREBASE_STORAGE_BUCKET as string,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID as string,
  appId:             (appId && !appId.includes('REPLACE_WITH')) ? appId : '1:145878439915:web:placeholder',
};

const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

// If appId is the placeholder or missing, the firebase app will be initialized with dummy data,
// but getAuth() will throw a runtime error. So we intercept it.
let fbAuth: ReturnType<typeof getAuth>;
try {
  fbAuth = getAuth(app);
} catch (e) {
  console.error("Firebase auth initialization failed (likely missing config):", e);
  // Provide a dummy auth object so the app at least boots the UI
  fbAuth = { currentUser: null, _isMock: true } as any; 
}

export const auth = fbAuth;
export const googleProvider = new GoogleAuthProvider();
