import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { SkinDesignRequest } from '../../types';
import StatusBadge from '../ui/StatusBadge';
import ImageLightbox from './ImageLightbox';

interface SkinRequestCardProps {
  request: SkinDesignRequest;
}

export default function SkinRequestCard({ request }: SkinRequestCardProps) {
  const navigate = useNavigate();
  const [lightboxIdx, setLightboxIdx] = useState<number | null>(null);

  const date = new Date(request.createdAt).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });

  function handleEmailUser(e: React.MouseEvent) {
    e.stopPropagation();
    const subject = encodeURIComponent(`Re: Custom Shoe Skin Request — ${request.shoeName || request.shoeId}`);
    const body = encodeURIComponent(
      `Hi,\n\nThank you for your custom shoe skin request. We have reviewed your design description:\n\n"${request.description}"\n\nBest regards,\nSoleMate Seller Team`
    );
    window.open(`mailto:${request.userEmail}?subject=${subject}&body=${body}`, '_blank');
  }

  return (
    <>
      <div
        className={`request-card status-${request.status}`}
        onClick={() => navigate(`/skin-requests/${request._id}`)}
      >
        {/* Main content */}
        <div>
          <div className="flex items-center gap-2" style={{ marginBottom: 8, flexWrap: 'wrap' }}>
            <StatusBadge status={request.status} />
            <span
              style={{
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-sm)',
                padding: '2px 8px',
                fontSize: 11.5,
                fontFamily: 'JetBrains Mono, monospace',
                color: 'var(--accent-light)',
              }}
            >
              Shoe: {request.shoeName || request.shoeId}
            </span>
            <span className="text-sm text-muted" style={{ marginLeft: 'auto' }}>{date}</span>
          </div>

          <p style={{ fontSize: 13.5, color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: 12 }}>
            {request.description.length > 140
              ? `${request.description.slice(0, 140)}…`
              : request.description}
          </p>

          {/* Reference images */}
          {request.referenceImageUrls.length > 0 && (
            <div className="request-images">
              {request.referenceImageUrls.slice(0, 5).map((url, i) => (
                <img
                  key={url}
                  src={url}
                  alt={`Reference ${i + 1}`}
                  className="request-thumb"
                  onClick={(e) => { e.stopPropagation(); setLightboxIdx(i); }}
                />
              ))}
              {request.referenceImageUrls.length > 5 && (
                <div
                  style={{
                    width: 64, height: 64, borderRadius: 'var(--radius-md)',
                    background: 'var(--bg-elevated)', border: '1px solid var(--border)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 12, color: 'var(--text-muted)',
                  }}
                >
                  +{request.referenceImageUrls.length - 5}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right actions */}
        <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: 8, alignItems: 'flex-end' }}>
          <a
            href={`mailto:${request.userEmail}`}
            style={{ fontSize: 12, color: 'var(--accent-light)', textDecoration: 'underline' }}
            onClick={(e) => e.stopPropagation()}
          >
            {request.userEmail}
          </a>
          <button
            className="btn btn-secondary btn-sm"
            onClick={handleEmailUser}
            title={`Email ${request.userEmail}`}
          >
            ✉️ Email User
          </button>
        </div>
      </div>

      {lightboxIdx !== null && (
        <ImageLightbox
          images={request.referenceImageUrls}
          initialIndex={lightboxIdx}
          onClose={() => setLightboxIdx(null)}
        />
      )}
    </>
  );
}
