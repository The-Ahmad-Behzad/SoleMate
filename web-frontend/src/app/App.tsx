import { BrowserRouter, Routes, Route, Navigate } from 'react-router';
import { Toaster } from './components/ui/sonner';
import { MainLayout } from './components/layout/MainLayout';
import { LandingPage } from './pages/Landing';
import { AuthPage } from './pages/Auth';
import { CatalogPage } from './pages/Catalog';
import { ARTryOnPage } from './pages/ARTryOn';
import { ClosetPage } from './pages/Closet';
import { AIMatchPage } from './pages/AIMatch';
import { CustomizePage } from './pages/Customize';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Auth Routes */}
        <Route path="/auth" element={<AuthPage />} />

        {/* Main App Routes */}
        <Route element={<MainLayout />}>
          <Route path="/" element={<LandingPage />} />
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="/try-on" element={<ARTryOnPage />} />
          <Route path="/closet" element={<ClosetPage />} />
          <Route path="/ai-match" element={<AIMatchPage />} />
          <Route path="/customize" element={<CustomizePage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toaster />
    </BrowserRouter>
  );
}