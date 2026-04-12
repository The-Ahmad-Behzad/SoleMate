import { useState } from 'react';

interface ImageLightboxProps {
  images: string[];
  onClose: () => void;
  initialIndex?: number;
}

export default function ImageLightbox({ images, onClose, initialIndex = 0 }: ImageLightboxProps) {
  const [idx, setIdx] = useState(initialIndex);

  function prev(e: React.MouseEvent) {
    e.stopPropagation();
    setIdx((i) => (i - 1 + images.length) % images.length);
  }

  function next(e: React.MouseEvent) {
    e.stopPropagation();
    setIdx((i) => (i + 1) % images.length);
  }

  return (
    <div className="lightbox-overlay" onClick={onClose}>
      {/* Navigation arrows */}
      {images.length > 1 && (
        <>
          <button
            onClick={prev}
            style={{
              position: 'absolute', left: 24, top: '50%', transform: 'translateY(-50%)',
              background: 'rgba(0,0,0,0.6)', border: '1px solid var(--border)',
              color: 'white', borderRadius: 'var(--radius-md)', padding: '10px 14px',
              fontSize: 20, cursor: 'pointer', zIndex: 1,
            }}
          >←</button>
          <button
            onClick={next}
            style={{
              position: 'absolute', right: 24, top: '50%', transform: 'translateY(-50%)',
              background: 'rgba(0,0,0,0.6)', border: '1px solid var(--border)',
              color: 'white', borderRadius: 'var(--radius-md)', padding: '10px 14px',
              fontSize: 20, cursor: 'pointer', zIndex: 1,
            }}
          >→</button>
        </>
      )}

      {/* Close button */}
      <button
        onClick={onClose}
        style={{
          position: 'absolute', top: 20, right: 20,
          background: 'rgba(0,0,0,0.6)', border: '1px solid var(--border)',
          color: 'white', borderRadius: '50%', width: 36, height: 36,
          fontSize: 16, cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}
      >✕</button>

      <img
        className="lightbox-img"
        src={images[idx]}
        alt={`Reference ${idx + 1}`}
        onClick={(e) => e.stopPropagation()}
      />

      {images.length > 1 && (
        <div style={{ position: 'absolute', bottom: 20, color: 'white', fontSize: 13, opacity: 0.7 }}>
          {idx + 1} / {images.length}
        </div>
      )}
    </div>
  );
}
