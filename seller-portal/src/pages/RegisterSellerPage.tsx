import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import api from '../config/api';
import Spinner from '../components/ui/Spinner';

export default function RegisterSellerPage() {
  const { user, refreshSellerProfile } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    displayName: '',
    companyName: '',
    phone: '',
    website: '',
    bio: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/seller/register', form);
      await refreshSellerProfile();
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-panel" style={{ maxWidth: 520 }}>
        <div className="auth-logo">
          <div className="auth-logo-icon">👟</div>
          <div>
            <div className="auth-logo-text">SoleMate</div>
            <div className="auth-logo-sub">Seller Portal</div>
          </div>
        </div>

        <div className="auth-card">
          <h1 className="auth-title">Complete your seller profile</h1>
          <p className="auth-sub">
            You're signed in as <strong>{user?.email}</strong>. Fill in your seller details to continue.
          </p>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Full Name <span className="required">*</span></label>
                <input
                  className="input"
                  name="displayName"
                  placeholder="Jane Smith"
                  value={form.displayName}
                  onChange={handleChange}
                  required
                  id="seller-name"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Company Name <span className="required">*</span></label>
                <input
                  className="input"
                  name="companyName"
                  placeholder="Acme Shoes Ltd."
                  value={form.companyName}
                  onChange={handleChange}
                  required
                  id="seller-company"
                />
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Phone</label>
                <input className="input" name="phone" placeholder="+1 555 000 0000" value={form.phone} onChange={handleChange} id="seller-phone" />
              </div>
              <div className="form-group">
                <label className="form-label">Website</label>
                <input className="input" name="website" placeholder="https://mystore.com" type="url" value={form.website} onChange={handleChange} id="seller-website" />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Short Bio</label>
              <textarea
                className="input"
                name="bio"
                placeholder="Tell buyers a bit about your brand…"
                value={form.bio}
                onChange={handleChange}
                rows={3}
                id="seller-bio"
              />
            </div>

            {error && (
              <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-md)', padding: '10px 14px', color: 'var(--red)', fontSize: 13 }}>
                {error}
              </div>
            )}

            <button
              id="seller-register-btn"
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ justifyContent: 'center' }}
            >
              {loading ? <Spinner /> : '🚀 Create Seller Account'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
