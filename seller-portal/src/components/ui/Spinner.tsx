export default function Spinner({ size = 'sm' }: { size?: 'sm' | 'lg' }) {
  return <div className={`spinner${size === 'lg' ? ' spinner-lg' : ''}`} />;
}
