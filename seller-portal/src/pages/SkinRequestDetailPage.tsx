import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../config/api';
import type { SkinDesignRequest, SkinRequestStatus } from '../types';
import StatusBadge from '../components/ui/StatusBadge';
import ImageLightbox from '../components/requests/ImageLightbox';
import Spinner from '../components/ui/Spinner';

const TRANSITION_STATUSES: { value: SkinRequestStatus; label: string }[] = [
  { value: 'viewed',      label: 'Viewed' },
  { value: 'in_progress', label: 'In Progress' },
  { value: 'completed',   label: 'Completed' },
  { value: 'rejected',    label: 'Rejected' },
];

export default function SkinRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [request, setRequest] = useState<SkinDesignRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [newStatus, setNewStatus] = useState<SkinRequestStatus | ''>('');
  const [notes, setNotes]     = useState('');
  const [lightboxIdx, setLightboxIdx] = useState<number | null>(null);
  const [saveMsg, setSaveMsg] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const res = await api.get<SkinDesignRequest>(`/skin-requests/${id}`);
        setRequest(res.data);
        setNewStatus(res.data.status);
        setNotes(res.data.sellerNotes || '');
      } catch (err: any) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  async function handleSave() {
    if (!request) return;
    setUpdating(true);
    setSaveMsg('');
    try {
      const res = await api.patch<SkinDesignRequest>(`/skin-requests/${id}`, {
        status: newStatus,
        sellerNotes: notes,
      });
      setRequest(res.data);
      setSaveMsg('✓ Changes saved');
    } catch (err: any) {
      setSaveMsg('⚠ ' + (err.message || 'Failed to save'));
    } finally {
      setUpdating(false);
      setTimeout(() => setSaveMsg(''), 3000);
    }
  }

  function handleEmailUser() {
    if (!request) return;
    const subject = encodeURIComponent(`Re: Custom Shoe Skin Request — ${request.shoeName || request.shoeId}`);
    const body = encodeURIComponent(
      `Hi,\n\nThank you for submitting a custom shoe skin request. Here's an update on your request:\n\n` +
      `Shoe: ${request.shoeName || request.shoeId}\n` +
      `Description: "${request.description}"\n\n` +
      `Status: ${newStatus || request.status}\n\n` +
      `Best regards,\nSoleMate Seller Team`
    );
    window.open(`mailto:${request.userEmail}?subject=${subject}&body=${body}`, '_blank');
  }

  if (loading) return <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}><Spinner size="lg" /></div>;
  if (!request) return <div className="card"><div className="empty-state"><p>Request not found.</p><button className="btn btn-secondary" onClick={() => navigate('/skin-requests')}>← Back</button></div></div>;

  return (
    <div>
      {/* Header */}
      <div className="page-header">
        <div className="page-header-left">
          <button className="btn btn-ghost btn-sm" onClick={() => navigate('/skin-requests')} style={{ marginBottom: 8 }}>
            ← All Requests
          </button>
          <h1 style={{ fontSize: 20, marginTop: 4 }}>Skin Request Detail</h1>
          <div className="flex items-center gap-3" style={{ marginTop: 8 }}>
            <StatusBadge status={request.status} />
            <span className="text-muted text-sm">
              Submitted {new Date(request.createdAt).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
            </span>
          </div>
        </div>
        <button className="btn btn-primary" onClick={handleEmailUser} id="email-user-btn">
          ✉️ Email User
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 24, alignItems: 'start' }}>
        {/* Main */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* User info */}
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>User Information</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                ['User Email', <a href={`mailto:${request.userEmail}`} style={{ color: 'var(--accent-light)' }}>{request.userEmail}</a>],
                ['Shoe ID / Name', <span className="font-mono" style={{ color: 'var(--text-primary)' }}>{request.shoeName || request.shoeId}</span>],
                ['Shoe DB ID', <span className="font-mono text-sm text-muted">{request.shoeId}</span>],
                ['Request ID', <span className="font-mono text-sm text-muted">{request._id}</span>],
              ].map(([label, val]) => (
                <div key={String(label)} className="flex justify-between items-center" style={{ borderBottom: '1px solid var(--border)', paddingBottom: 10, gap: 8 }}>
                  <span className="text-muted text-sm" style={{ flexShrink: 0 }}>{label}</span>
                  <span style={{ fontWeight: 500, textAlign: 'right' }}>{val}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Description */}
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 12, fontSize: 15 }}>Design Description</h3>
            <p style={{ fontSize: 14, lineHeight: 1.7, color: 'var(--text-secondary)', background: 'var(--bg-surface)', padding: '14px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
              {request.description}
            </p>
          </div>

          {/* Reference images */}
          {request.referenceImageUrls.length > 0 && (
            <div className="card">
              <h3 style={{ fontWeight: 700, marginBottom: 12, fontSize: 15 }}>
                Reference Images ({request.referenceImageUrls.length})
              </h3>
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                {request.referenceImageUrls.map((url, i) => (
                  <div key={url} style={{ position: 'relative', cursor: 'zoom-in' }} onClick={() => setLightboxIdx(i)}>
                    <img
                      src={url}
                      alt={`Reference ${i + 1}`}
                      style={{
                        width: 120, height: 120, objectFit: 'cover',
                        borderRadius: 'var(--radius-md)',
                        border: '1px solid var(--border)',
                        transition: 'transform 0.15s',
                      }}
                      onMouseOver={(e) => (e.currentTarget.style.transform = 'scale(1.04)')}
                      onMouseOut={(e) => (e.currentTarget.style.transform = 'scale(1)')}
                    />
                    <span style={{ position: 'absolute', bottom: 4, right: 6, background: 'rgba(0,0,0,0.6)', color: 'white', fontSize: 10, padding: '1px 5px', borderRadius: 4 }}>
                      🔍
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar — manage */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, position: 'sticky', top: 28 }}>
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>Manage Request</h3>

            <div className="form-group" style={{ marginBottom: 16 }}>
              <label className="form-label">Status</label>
              <select
                id="request-status-select"
                className="input"
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value as SkinRequestStatus)}
              >
                {TRANSITION_STATUSES.map((s) => (
                  <option key={s.value} value={s.value}>{s.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: 16 }}>
              <label className="form-label">Seller Notes</label>
              <textarea
                id="seller-notes-input"
                className="input"
                placeholder="Internal notes about this request…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
              />
            </div>

            {saveMsg && (
              <div style={{
                padding: '8px 12px', marginBottom: 12, borderRadius: 'var(--radius-md)', fontSize: 13,
                background: saveMsg.startsWith('✓') ? 'rgba(34,197,94,0.1)' : 'rgba(239,68,68,0.1)',
                border: `1px solid ${saveMsg.startsWith('✓') ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`,
                color: saveMsg.startsWith('✓') ? 'var(--green)' : 'var(--red)',
              }}>
                {saveMsg}
              </div>
            )}

            <button
              id="save-request-btn"
              className="btn btn-primary"
              style={{ width: '100%', justifyContent: 'center' }}
              onClick={handleSave}
              disabled={updating}
            >
              {updating ? <Spinner /> : '💾 Save Changes'}
            </button>

            <button
              className="btn btn-secondary"
              style={{ width: '100%', justifyContent: 'center', marginTop: 10 }}
              onClick={handleEmailUser}
            >
              ✉️ Email User
            </button>
          </div>

          {/* Audit info */}
          <div className="card card-sm">
            <h4 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>Audit</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {[
                ['Created', new Date(request.createdAt).toLocaleString()],
                ['Viewed', request.viewedAt ? new Date(request.viewedAt).toLocaleString() : '—'],
                ['Completed', request.completedAt ? new Date(request.completedAt).toLocaleString() : '—'],
              ].map(([label, val]) => (
                <div key={label} className="flex justify-between">
                  <span className="text-muted text-sm">{label}</span>
                  <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{val}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {lightboxIdx !== null && (
        <ImageLightbox
          images={request.referenceImageUrls}
          initialIndex={lightboxIdx}
          onClose={() => setLightboxIdx(null)}
        />
      )}
    </div>
  );
}
