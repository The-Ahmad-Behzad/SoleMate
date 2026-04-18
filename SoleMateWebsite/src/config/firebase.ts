import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";

const firebaseConfig = {
  apiKey: "AIzaSyCGMJekPZuyMzXMsn8mSpl1jozjQ-2NSX0",
  authDomain: "solemate-app-d4560.firebaseapp.com",
  projectId: "solemate-app-d4560",
  storageBucket: "solemate-app-d4560.firebasestorage.app",
  messagingSenderId: "145878439915",
  appId: "1:145878439915:web:30b1c641cd280b7152ebae"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const googleProvider = new GoogleAuthProvider();

export default app;
