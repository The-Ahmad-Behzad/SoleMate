import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import AuthGuard from './components/layout/AuthGuard';
import Sidebar from './components/layout/Sidebar';

// Pages
import LoginPage from './pages/LoginPage';
import RegisterSellerPage from './pages/RegisterSellerPage';
import DashboardPage from './pages/DashboardPage';
import ModelsPage from './pages/ModelsPage';
import UploadPage from './pages/UploadPage';
import IntegrationRequestPage from './pages/IntegrationRequestPage';
import SkinRequestsPage from './pages/SkinRequestsPage';
import SkinRequestDetailPage from './pages/SkinRequestDetailPage';

function SellerLayout({ newRequestCount }: { newRequestCount?: number }) {
  return (
    <div className="app-layout">
      <Sidebar newRequestCount={newRequestCount} />
      <div className="main-content">
        <div className="page-body">
          <Routes>
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="models" element={<ModelsPage />} />
            <Route path="models/upload" element={<UploadPage />} />
            <Route path="models/:id/request" element={<IntegrationRequestPage />} />
            <Route path="skin-requests" element={<SkinRequestsPage />} />
            <Route path="skin-requests/:id" element={<SkinRequestDetailPage />} />
            <Route path="*" element={<Navigate to="dashboard" replace />} />
          </Routes>
        </div>
      </div>
    </div>
  );
}

function AppRoutes() {
  const { user, isSeller, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
        <div className="spinner spinner-lg" />
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={
        user ? <Navigate to={isSeller ? '/dashboard' : '/register-seller'} replace /> : <LoginPage />
      } />
      <Route path="/register-seller" element={
        !user ? <Navigate to="/login" replace /> :
        isSeller ? <Navigate to="/dashboard" replace /> :
        <RegisterSellerPage />
      } />
      <Route
        path="/*"
        element={
          <AuthGuard requireSeller>
            <SellerLayout />
          </AuthGuard>
        }
      />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
