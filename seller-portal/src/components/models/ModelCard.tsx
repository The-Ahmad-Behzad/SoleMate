import { useNavigate } from 'react-router-dom';
import type { GlbUpload } from '../../types';
import StatusBadge from '../ui/StatusBadge';

interface ModelCardProps {
  model: GlbUpload;
  onIntegrationRequest?: () => void;
}

export default function ModelCard({ model }: ModelCardProps) {
  const navigate = useNavigate();

  const date = new Date(model.createdAt).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });

  async function copyUrl() {
    await navigator.clipboard.writeText(model.s3Url);
  }

  function goToIntegration() {
    navigate(`/models/${model._id}/request`);
  }

  return (
    <div className="model-card card-hover">
      <div className="model-card-thumb">
        {model.thumbnailUrl
          ? <img src={model.thumbnailUrl} alt={model.shoeName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          : '📦'
        }
      </div>
      <div className="model-card-body">
        <div>
          <div className="model-card-name">{model.shoeName}</div>
          <div className="model-card-brand">{model.brand}</div>
          {model.description && (
            <p className="text-sm text-muted" style={{ marginTop: 6, lineHeight: 1.5 }}>
              {model.description.length > 80 ? `${model.description.slice(0, 80)}…` : model.description}
            </p>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
          <StatusBadge status={model.status} />
          <span className="text-sm text-muted">{date}</span>
        </div>
        <div className="model-card-footer">
          <button className="btn btn-ghost btn-sm" onClick={copyUrl} title="Copy S3 URL">
            📋 Copy URL
          </button>
          {model.status !== 'approved' ? (
            <button
              className="btn btn-secondary btn-sm"
              onClick={goToIntegration}
            >
              🚀 Request Integration
            </button>
          ) : (
            <span style={{ color: 'var(--green)', fontSize: 12, fontWeight: 600 }}>✓ Integrated</span>
          )}
        </div>
      </div>
    </div>
  );
}
