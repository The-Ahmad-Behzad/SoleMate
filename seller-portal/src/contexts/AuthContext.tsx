import React, { createContext, useContext, useEffect, useState } from 'react';
import type { User } from 'firebase/auth';
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  createUserWithEmailAndPassword,
  signOut,
} from 'firebase/auth';
import { auth, googleProvider } from '../config/firebase';
import api from '../config/api';
import type { SellerProfile } from '../types';

interface AuthContextValue {
  user: User | null;
  sellerProfile: SellerProfile | null;
  loading: boolean;
  isSeller: boolean;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  loginWithGoogle: () => Promise<void>;
  registerWithEmail: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshSellerProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser]                   = useState<User | null>(null);
  const [sellerProfile, setSellerProfile] = useState<SellerProfile | null>(null);
  const [loading, setLoading]             = useState(true);

  async function fetchSellerProfile(): Promise<SellerProfile | null> {
    try {
      const res = await api.get<SellerProfile>('/seller/profile');
      return res.data;
    } catch (err: any) {
      // 403 NOT_A_SELLER is expected for non-sellers, not an actual error
      if (err.message?.includes('Access denied') || err.response?.status === 403) {
        return null;
      }
      return null;
    }
  }

  useEffect(() => {
    if (!auth || (auth as any)._isMock) {
      setLoading(false);
      return;
    }
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        const profile = await fetchSellerProfile();
        setSellerProfile(profile);
      } else {
        setSellerProfile(null);
      }
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  async function loginWithEmail(email: string, password: string) {
    await signInWithEmailAndPassword(auth, email, password);
  }

  async function loginWithGoogle() {
    await signInWithPopup(auth, googleProvider);
  }

  async function registerWithEmail(email: string, password: string) {
    await createUserWithEmailAndPassword(auth, email, password);
  }

  async function logout() {
    await signOut(auth);
    setSellerProfile(null);
  }

  async function refreshSellerProfile() {
    if (!auth.currentUser) return;
    const profile = await fetchSellerProfile();
    setSellerProfile(profile);
  }

  return (
    <AuthContext.Provider value={{
      user,
      sellerProfile,
      loading,
      isSeller: !!sellerProfile,
      loginWithEmail,
      loginWithGoogle,
      registerWithEmail,
      logout,
      refreshSellerProfile,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}
