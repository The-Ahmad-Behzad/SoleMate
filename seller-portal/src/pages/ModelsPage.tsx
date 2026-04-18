import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../config/api';
import type { GlbUpload } from '../types';
import ModelCard from '../components/models/ModelCard';
import FilterBar from '../components/requests/FilterBar';
import Spinner from '../components/ui/Spinner';

const STATUS_OPTIONS = [
  { value: '', label: 'All Statuses' },
  { value: 'pending', label: 'Pending' },
  { value: 'approved', label: 'Approved' },
  { value: 'rejected', label: 'Rejected' },
];

export default function ModelsPage() {
  const navigate = useNavigate();
  const [models, setModels]   = useState<GlbUpload[]>([]);
  const [total, setTotal]     = useState(0);
  const [loading, setLoading] = useState(true);
  const [status, setStatus]   = useState('');
  const [search, setSearch]   = useState('');

  useEffect(() => {
    load();
  }, [status]);

  async function load() {
    setLoading(true);
    try {
      const params = new URLSearchParams({ limit: '50' });
      if (status) params.set('status', status);
      const res = await api.get(`/seller/uploads?${params}`);
      setModels(res.data.uploads);
      setTotal(res.data.total);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const filtered = search
    ? models.filter(
        (m) =>
          m.shoeName.toLowerCase().includes(search.toLowerCase()) ||
          m.brand.toLowerCase().includes(search.toLowerCase())
      )
    : models;

  return (
    <div>
      <div className="page-header">
        <div className="page-header-left">
          <h1>My Models</h1>
          <p>{total} uploaded GLB file{total !== 1 ? 's' : ''}</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/models/upload')}>
          ⬆️ Upload New Model
        </button>
      </div>

      <div style={{ marginBottom: 20 }}>
        <FilterBar
          search={search}
          onSearchChange={setSearch}
          status={status}
          onStatusChange={setStatus}
          statusOptions={STATUS_OPTIONS}
          placeholder="Search by shoe name or brand…"
        />
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
          <Spinner size="lg" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <span className="empty-state-icon">📦</span>
            <h3 className="empty-state-title">No models found</h3>
            <p className="empty-state-sub">Upload your first .glb shoe model to get started.</p>
            <button className="btn btn-primary" onClick={() => navigate('/models/upload')}>
              Upload Model
            </button>
          </div>
        </div>
      ) : (
        <div className="grid-models">
          {filtered.map((model) => (
            <ModelCard key={model._id} model={model} />
          ))}
        </div>
      )}
    </div>
  );
}
