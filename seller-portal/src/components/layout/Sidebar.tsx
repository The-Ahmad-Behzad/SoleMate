import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const NAV_ITEMS = [
  { to: '/dashboard', icon: '⬛', label: 'Dashboard' },
  { to: '/models', icon: '📦', label: 'My Models' },
  { to: '/models/upload', icon: '⬆️', label: 'Upload Model' },
  { to: '/skin-requests', icon: '🎨', label: 'Skin Requests', badgeKey: 'new' },
];

export default function Sidebar({ newRequestCount = 0 }: { newRequestCount?: number }) {
  const { user, sellerProfile, logout } = useAuth();
  const navigate = useNavigate();

  const initials = sellerProfile?.displayName
    ? sellerProfile.displayName.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.email?.[0]?.toUpperCase() || '?';

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <aside className="sidebar">
      {/* Brand */}
      <div className="sidebar-brand">
        <div className="sidebar-brand-icon">👟</div>
        <div className="sidebar-brand-text">
          <span className="sidebar-brand-name">SoleMate</span>
          <span className="sidebar-brand-sub">Seller Portal</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        <span className="sidebar-section-label">Main</span>
        {NAV_ITEMS.map(({ to, icon, label, badgeKey }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <span className="nav-item-icon">{icon}</span>
            {label}
            {badgeKey === 'new' && newRequestCount > 0 && (
              <span className="nav-badge">{newRequestCount > 99 ? '99+' : newRequestCount}</span>
            )}
          </NavLink>
        ))}

        <span className="sidebar-section-label" style={{ marginTop: 8 }}>Account</span>
        <NavLink to="/settings" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
          <span className="nav-item-icon">⚙️</span>
          Settings
        </NavLink>
      </nav>

      {/* User footer */}
      <div className="sidebar-footer">
        <div className="sidebar-user">
          <div className="sidebar-avatar">{initials}</div>
          <div className="sidebar-user-info">
            <div className="sidebar-user-name truncate">{sellerProfile?.displayName || user?.email}</div>
            <div className="sidebar-user-role">Seller</div>
          </div>
          <button className="sidebar-logout-btn" onClick={handleLogout} title="Log out">
            ↩
          </button>
        </div>
      </div>
    </aside>
  );
}
