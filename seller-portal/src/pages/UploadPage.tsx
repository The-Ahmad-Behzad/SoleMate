import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../config/api';
import type { UploadMetadata } from '../types';
import Spinner from '../components/ui/Spinner';

type UploadPhase = 'form' | 'uploading' | 'success' | 'error';

export default function UploadPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [phase, setPhase]     = useState<UploadPhase>('form');
  const [file, setFile]       = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError]     = useState('');
  const [uploadedId, setUploadedId] = useState('');
  const [showArSection, setShowArSection] = useState(false);

  const [meta, setMeta] = useState<UploadMetadata>({
    shoeName: '',
    brand: '',
    description: '',
    scale: [-1, -1, -1],
    positionOffset: [0, 0, 0],
    rotationOffset: [0, 0, 0],
  });

  function handleFile(f: File | null) {
    if (!f) return;
    if (!f.name.toLowerCase().endsWith('.glb')) {
      setError('Only .glb files are supported.');
      return;
    }
    if (f.size > 100 * 1024 * 1024) {
      setError('File size must be under 100 MB.');
      return;
    }
    setFile(f);
    setError('');
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragging(false);
    handleFile(e.dataTransfer.files[0] || null);
  }

  function updateVec(
    key: 'scale' | 'positionOffset' | 'rotationOffset',
    index: number,
    val: string
  ) {
    setMeta((m) => {
      const vec = [...(m[key] || [0, 0, 0])] as [number, number, number];
      vec[index] = parseFloat(val) || 0;
      return { ...m, [key]: vec };
    });
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!file) { setError('Please select a .glb file first.'); return; }
    if (!meta.shoeName || !meta.brand) { setError('Shoe name and brand are required.'); return; }

    setPhase('uploading');
    setProgress(0);
    setError('');

    try {
      // Step 1: Get presigned URL from backend
      const urlRes = await api.post('/seller/upload-url', {
        fileName:    file.name,
        contentType: 'model/gltf-binary',
        shoeName:    meta.shoeName,
        brand:       meta.brand,
      });
      const { presignedUrl, s3Key, s3Url } = urlRes.data;

      // Step 2: Upload directly to S3 with progress tracking
      await new Promise<void>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) setProgress(Math.round((e.loaded / e.total) * 100));
        };
        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) resolve();
          else reject(new Error(`S3 upload failed: ${xhr.status}`));
        };
        xhr.onerror = () => reject(new Error('Network error during upload'));
        xhr.open('PUT', presignedUrl);
        xhr.setRequestHeader('Content-Type', 'model/gltf-binary');
        xhr.send(file);
      });

      setProgress(100);

      // Step 3: Confirm with backend
      const confirmRes = await api.post('/seller/uploads/confirm', {
        s3Key,
        s3Url,
        shoeName:    meta.shoeName,
        brand:       meta.brand,
        description: meta.description,
        scale:           meta.scale,
        positionOffset:  meta.positionOffset,
        rotationOffset:  meta.rotationOffset,
      });

      setUploadedId(confirmRes.data._id);
      setPhase('success');
    } catch (err: any) {
      setError(err.message || 'Upload failed');
      setPhase('error');
    }
  }

  if (phase === 'success') {
    return (
      <div>
        <div className="page-header">
          <div className="page-header-left">
            <h1>Upload Complete! 🎉</h1>
            <p>Your model has been uploaded and is pending admin review.</p>
          </div>
        </div>
        <div className="card" style={{ maxWidth: 560, textAlign: 'center', padding: 48 }}>
          <div style={{ fontSize: 64, marginBottom: 16 }}>✅</div>
          <h2 style={{ fontSize: 20, fontWeight: 700, marginBottom: 8 }}>{meta.shoeName}</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: 28 }}>
            Your .glb model has been uploaded to S3 and registered in SoleMate. An admin will review it shortly.
          </p>
          <div className="flex gap-3" style={{ justifyContent: 'center', flexWrap: 'wrap' }}>
            <button className="btn btn-primary" onClick={() => navigate(`/models/${uploadedId}/request`)}>
              🚀 Request AR Integration
            </button>
            <button className="btn btn-secondary" onClick={() => navigate('/models')}>
              View My Models
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div className="page-header-left">
          <h1>Upload 3D Shoe Model</h1>
          <p>Upload a .glb file to the SoleMate S3 bucket. Fill in the AR placement metadata for lens integration.</p>
        </div>
      </div>

      <form onSubmit={handleUpload} style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 24, alignItems: 'start' }}>
        {/* Left: Form */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* File drop */}
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>1. Select .glb file</h3>
            <div
              className={`dropzone${dragging ? ' dragging' : ''}${file ? ' has-file' : ''}`}
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
              onDragLeave={() => setDragging(false)}
              onDrop={handleDrop}
              tabIndex={0}
              onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
              role="button"
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".glb"
                style={{ display: 'none' }}
                onChange={(e) => handleFile(e.target.files?.[0] || null)}
                id="glb-file-input"
              />
              <span className="dropzone-icon">{file ? '✅' : '📁'}</span>
              {file
                ? <span className="dropzone-file-name">{file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)</span>
                : <>
                    <span className="dropzone-title">Drop your .glb file here</span>
                    <span className="dropzone-sub">or click to browse · Max 100 MB</span>
                  </>
              }
            </div>
          </div>

          {/* Shoe info */}
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>2. Shoe Details</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Shoe Name <span className="required">*</span></label>
                  <input id="shoe-name" className="input" placeholder="Air Max 270" value={meta.shoeName} onChange={(e) => setMeta((m) => ({ ...m, shoeName: e.target.value }))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Brand <span className="required">*</span></label>
                  <input id="shoe-brand" className="input" placeholder="Nike" value={meta.brand} onChange={(e) => setMeta((m) => ({ ...m, brand: e.target.value }))} required />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea id="shoe-description" className="input" placeholder="Briefly describe the shoe style, features…" value={meta.description} onChange={(e) => setMeta((m) => ({ ...m, description: e.target.value }))} rows={3} />
              </div>
            </div>
          </div>

          {/* AR Metadata */}
          <div className="card">
            <div className="flex items-center justify-between" style={{ marginBottom: 4 }}>
              <h3 style={{ fontWeight: 700, fontSize: 15 }}>3. AR Lens Placement (optional)</h3>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowArSection((s) => !s)}>
                {showArSection ? '▲ Collapse' : '▼ Expand'}
              </button>
            </div>
            <p className="text-sm text-muted" style={{ marginBottom: 16 }}>
              These values match the <code style={{ color: 'var(--accent-light)' }}>catalogue.json</code> format used by the Snap AR lens. The admin uses these when integrating your model.
            </p>
            {showArSection && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {(['scale', 'positionOffset', 'rotationOffset'] as const).map((key) => (
                  <div key={key}>
                    <label className="form-label" style={{ textTransform: 'capitalize', marginBottom: 8 }}>
                      {key.replace(/([A-Z])/g, ' $1')}
                    </label>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
                      {['X', 'Y', 'Z'].map((axis, i) => (
                        <div key={axis} className="form-group">
                          <label className="form-label text-muted">{axis}</label>
                          <input
                            className="input font-mono"
                            type="number"
                            step="0.0001"
                            value={(meta[key] || [0,0,0])[i]}
                            onChange={(e) => updateVec(key, i, e.target.value)}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {error && (
            <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-md)', padding: '12px 16px', color: 'var(--red)', fontSize: 13 }}>
              {error}
            </div>
          )}
        </div>

        {/* Right: Summary + Submit */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, position: 'sticky', top: 28 }}>
          <div className="card">
            <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>Upload Summary</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                ['File', file?.name || '—'],
                ['Size', file ? `${(file.size / 1024 / 1024).toFixed(2)} MB` : '—'],
                ['Shoe Name', meta.shoeName || '—'],
                ['Brand', meta.brand || '—'],
              ].map(([label, val]) => (
                <div key={label} className="flex justify-between" style={{ borderBottom: '1px solid var(--border)', paddingBottom: 8 }}>
                  <span className="text-muted text-sm">{label}</span>
                  <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)' }}>{val}</span>
                </div>
              ))}
            </div>

            {phase === 'uploading' && (
              <div style={{ marginTop: 16 }}>
                <div className="flex justify-between" style={{ marginBottom: 6 }}>
                  <span className="text-muted text-sm">Uploading to S3…</span>
                  <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--accent-light)' }}>{progress}%</span>
                </div>
                <div className="progress-track">
                  <div className="progress-fill" style={{ width: `${progress}%` }} />
                </div>
              </div>
            )}

            <button
              id="upload-submit-btn"
              type="submit"
              className="btn btn-primary"
              disabled={phase === 'uploading' || !file}
              style={{ marginTop: 20, width: '100%', justifyContent: 'center' }}
            >
              {phase === 'uploading' ? <><Spinner /> Uploading…</> : '⬆️ Upload to SoleMate'}
            </button>

            <p className="text-sm text-muted" style={{ marginTop: 10, textAlign: 'center' }}>
              File goes to S3 bucket <code style={{ color: 'var(--accent-light)', fontSize: 11 }}>shoes/{'{brand}/{file}.glb'}</code>
            </p>
          </div>
        </div>
      </form>
    </div>
  );
}
