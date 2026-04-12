import { useAuth } from '../../contexts/AuthContext';
import { Navigate, useLocation } from 'react-router-dom';

interface AuthGuardProps {
  children: React.ReactNode;
  requireSeller?: boolean;
}

export default function AuthGuard({ children, requireSeller = true }: AuthGuardProps) {
  const { user, isSeller, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', background: 'var(--bg-base)' }}>
        <div className="spinner spinner-lg" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requireSeller && !isSeller) {
    return <Navigate to="/register-seller" replace />;
  }

  return <>{children}</>;
}
