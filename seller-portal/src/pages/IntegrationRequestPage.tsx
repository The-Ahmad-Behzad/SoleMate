import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../config/api';
import type { GlbUpload, IntegrationRequest } from '../types';
import StatusBadge from '../components/ui/StatusBadge';
import Spinner from '../components/ui/Spinner';

export default function IntegrationRequestPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [upload, setUpload]   = useState<GlbUpload | null>(null);
  const [existing, setExisting] = useState<IntegrationRequest | null>(null);
  const [note, setNote]       = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError]     = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const [uploadRes, requestsRes] = await Promise.all([
          api.get(`/seller/uploads/${id}`),
          api.get('/seller/integration-requests'),
        ]);
        setUpload(uploadRes.data);
        const match = requestsRes.data.find((r: IntegrationRequest) => {
          const glbId = typeof r.glbUploadId === 'string' ? r.glbUploadId : (r.glbUploadId as GlbUpload)._id;
          return glbId === id;
        });
        setExisting(match || null);
      } catch (err: any) {
        setError(err.message || 'Failed to load');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await api.post('/seller/integration-requests', {
        glbUploadId: id,
        requestNote: note,
      });
      setSuccess(true);
    } catch (err: any) {
      setError(err.message || 'Submission failed');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}><Spinner size="lg" /></div>;

  if (!upload) return (
    <div className="card">
      <div className="empty-state">
        <span className="empty-state-icon">❌</span>
        <p className="empty-state-title">Model not found</p>
        <button className="btn btn-secondary" onClick={() => navigate('/models')}>Back to Models</button>
      </div>
    </div>
  );

  if (success) return (
    <div>
      <div className="page-header">
        <div className="page-header-left"><h1>Request Submitted! 🚀</h1></div>
      </div>
      <div className="card" style={{ maxWidth: 560, textAlign: 'center', padding: 48 }}>
        <div style={{ fontSize: 64, marginBottom: 16 }}>📨</div>
        <h2 style={{ fontSize: 20, fontWeight: 700, marginBottom: 8 }}>Integration Request Sent</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: 28 }}>
          The admin has been notified via email and Firestore. They'll review your model and schedule it for the next AR lens update.
        </p>
        <div className="flex gap-3" style={{ justifyContent: 'center' }}>
          <button className="btn btn-primary" onClick={() => navigate('/models')}>Back to Models</button>
          <button className="btn btn-secondary" onClick={() => navigate('/dashboard')}>Dashboard</button>
        </div>
      </div>
    </div>
  );

  const cataloguePreview = {
    id:   `${upload.brand.toLowerCase().replace(/\s+/g, '-')}_${upload.shoeName.toLowerCase().replace(/\s+/g, '-')}`,
    name: upload.shoeName,
    thumbnailUrl: upload.thumbnailUrl || '',
    modelUrl: upload.s3Url,
    scale:          upload.scale          || [-1, -1, -1],
    positionOffset: upload.positionOffset || [0,  0,  0],
    rotationOffset: upload.rotationOffset || [0,  0,  0],
  };

  return (
    <div>
      <div className="page-header">
        <div className="page-header-left">
          <h1>Request AR Lens Integration</h1>
          <p>Ask the admin to include your model in the next Snap AR Lens update.</p>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/models')}>← Back</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 400px', gap: 24, alignItems: 'start' }}>
        {/* Left */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* Upload summary */}
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>Model Details</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                ['Shoe Name', upload.shoeName],
                ['Brand', upload.brand],
                ['Status', <StatusBadge status={upload.status} />],
                ['S3 URL', <a href={upload.s3Url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent-light)', fontSize: 12 }} className="font-mono">{upload.s3Key}</a>],
              ].map(([label, val]) => (
                <div key={String(label)} className="flex justify-between items-center" style={{ borderBottom: '1px solid var(--border)', paddingBottom: 10, gap: 8 }}>
                  <span className="text-muted text-sm" style={{ flexShrink: 0 }}>{label}</span>
                  <span style={{ fontWeight: 500, textAlign: 'right' }}>{val}</span>
                </div>
              ))}
            </div>
          </div>

          {existing ? (
            <div className="card" style={{ borderColor: 'var(--border-active)' }}>
              <h3 style={{ fontWeight: 700, marginBottom: 12, fontSize: 15 }}>Active Integration Request</h3>
              <div className="flex items-center gap-3" style={{ marginBottom: 8 }}>
                <StatusBadge status={existing.status} />
                <span className="text-sm text-muted">
                  Submitted {new Date(existing.createdAt).toLocaleDateString()}
                </span>
              </div>
              {existing.adminResponse && (
                <p style={{ fontSize: 13, color: 'var(--text-secondary)', padding: '10px 14px', background: 'var(--bg-surface)', borderRadius: 'var(--radius-md)', marginTop: 8 }}>
                  Admin: {existing.adminResponse}
                </p>
              )}
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="card">
                <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>Submit Integration Request</h3>
                <div className="form-group" style={{ marginBottom: 16 }}>
                  <label className="form-label">Message to Admin (optional)</label>
                  <textarea
                    id="integration-note"
                    className="input"
                    placeholder="Any special notes about placement, sizing, or priority…"
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    rows={4}
                  />
                </div>
                {error && (
                  <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-md)', padding: '10px 14px', color: 'var(--red)', fontSize: 13, marginBottom: 16 }}>
                    {error}
                  </div>
                )}
                <button
                  id="integration-submit-btn"
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  {submitting ? <><Spinner /> Sending…</> : '🚀 Send Integration Request'}
                </button>
                <p className="text-sm text-muted" style={{ marginTop: 10, textAlign: 'center' }}>
                  The admin will receive an email + Firestore notification with your catalogue.json entry.
                </p>
              </div>
            </form>
          )}
        </div>

        {/* Right — Catalogue preview */}
        <div className="card" style={{ position: 'sticky', top: 28 }}>
          <h3 style={{ fontWeight: 700, marginBottom: 4, fontSize: 15 }}>Catalogue.json Preview</h3>
          <p className="text-sm text-muted" style={{ marginBottom: 14 }}>
            The admin will get this entry ready to paste into the Snap Lens <code style={{ color: 'var(--accent-light)' }}>catalogue.json</code>:
          </p>
          <pre className="code-block">{JSON.stringify(cataloguePreview, null, 2)}</pre>
        </div>
      </div>
    </div>
  );
}
