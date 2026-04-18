import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../config/api';
import { useAuth } from '../contexts/AuthContext';
import type { GlbUpload, SkinDesignRequest } from '../types';
import StatusBadge from '../components/ui/StatusBadge';
import Spinner from '../components/ui/Spinner';

interface DashboardStats {
  totalModels: number;
  pendingModels: number;
  totalRequests: number;
  newRequests: number;
  inProgressRequests: number;
  completedRequests: number;
}

export default function DashboardPage() {
  const { sellerProfile } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentModels, setRecentModels] = useState<GlbUpload[]>([]);
  const [recentRequests, setRecentRequests] = useState<SkinDesignRequest[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [modelsRes, requestsRes] = await Promise.all([
          api.get('/seller/uploads?limit=5'),
          api.get('/skin-requests?limit=5&sortBy=createdAt&sortOrder=desc'),
        ]);
        const models: GlbUpload[] = modelsRes.data.uploads;
        const { requests, statusCounts } = requestsRes.data;

        setRecentModels(models);
        setRecentRequests(requests);
        setStats({
          totalModels:       modelsRes.data.total,
          pendingModels:     models.filter((m) => m.status === 'pending').length,
          totalRequests:     requestsRes.data.total,
          newRequests:       statusCounts.new || 0,
          inProgressRequests: statusCounts.in_progress || 0,
          completedRequests: statusCounts.completed || 0,
        });
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const greet = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  };

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh' }}>
      <Spinner size="lg" />
    </div>
  );

  return (
    <div>
      {/* Header */}
      <div className="page-header">
        <div className="page-header-left">
          <h1>{greet()}, {sellerProfile?.displayName?.split(' ')[0] || 'Seller'} 👋</h1>
          <p>Here's what's happening with your SoleMate seller account today.</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/models/upload')}>
          ⬆️ Upload Model
        </button>
      </div>

      {/* Stats */}
      {stats && (
        <div className="stat-grid" style={{ marginBottom: 28 }}>
          <div className="stat-card">
            <span className="stat-label">Total Models</span>
            <span className="stat-value">{stats.totalModels}</span>
            <span className="stat-icon">📦</span>
            <span className="stat-delta">Uploaded GLB files</span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Pending Review</span>
            <span className="stat-value" style={{ color: 'var(--orange)' }}>{stats.pendingModels}</span>
            <span className="stat-icon">⏳</span>
            <span className="stat-delta">Awaiting admin</span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Skin Requests</span>
            <span className="stat-value">{stats.totalRequests}</span>
            <span className="stat-icon">🎨</span>
            <span className="stat-delta">All time</span>
          </div>
          <div className="stat-card">
            <span className="stat-label">New Requests</span>
            <span className="stat-value" style={{ color: 'var(--blue)' }}>{stats.newRequests}</span>
            <span className="stat-icon">🔔</span>
            <span className="stat-delta">Unread</span>
          </div>
        </div>
      )}

      {/* Recent activity */}
      <div className="grid-2" style={{ gap: 24 }}>
        {/* Recent uploads */}
        <div>
          <div className="flex items-center justify-between" style={{ marginBottom: 14 }}>
            <h2 style={{ fontSize: 16, fontWeight: 700 }}>Recent Models</h2>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/models')}>View all →</button>
          </div>
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            {recentModels.length === 0 ? (
              <div className="empty-state" style={{ padding: 32 }}>
                <span className="empty-state-icon">📦</span>
                <p className="empty-state-sub">No models uploaded yet.</p>
                <button className="btn btn-primary btn-sm" onClick={() => navigate('/models/upload')}>Upload first model</button>
              </div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Model</th>
                    <th>Brand</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentModels.map((m) => (
                    <tr key={m._id} onClick={() => navigate('/models')} style={{ cursor: 'pointer' }}>
                      <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{m.shoeName}</td>
                      <td>{m.brand}</td>
                      <td><StatusBadge status={m.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Recent skin requests */}
        <div>
          <div className="flex items-center justify-between" style={{ marginBottom: 14 }}>
            <h2 style={{ fontSize: 16, fontWeight: 700 }}>Recent Skin Requests</h2>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/skin-requests')}>View all →</button>
          </div>
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            {recentRequests.length === 0 ? (
              <div className="empty-state" style={{ padding: 32 }}>
                <span className="empty-state-icon">🎨</span>
                <p className="empty-state-sub">No design requests yet.</p>
              </div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Shoe</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentRequests.map((r) => (
                    <tr key={r._id} onClick={() => navigate(`/skin-requests/${r._id}`)} style={{ cursor: 'pointer' }}>
                      <td className="truncate" style={{ maxWidth: 140 }}>
                        <a href={`mailto:${r.userEmail}`} onClick={(e) => e.stopPropagation()} style={{ color: 'var(--accent-light)' }}>
                          {r.userEmail}
                        </a>
                      </td>
                      <td className="font-mono text-sm">{r.shoeName || r.shoeId}</td>
                      <td><StatusBadge status={r.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
