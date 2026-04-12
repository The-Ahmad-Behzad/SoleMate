import { useEffect, useState } from 'react';
import api from '../config/api';
import type { SkinRequestListResponse, SkinRequestStatus } from '../types';
import SkinRequestCard from '../components/requests/SkinRequestCard';
import FilterBar from '../components/requests/FilterBar';
import StatusBadge from '../components/ui/StatusBadge';
import Spinner from '../components/ui/Spinner';

const STATUS_OPTIONS = [
  { value: '', label: 'All Statuses' },
  { value: 'new', label: '🔵 New' },
  { value: 'viewed', label: '⚪ Viewed' },
  { value: 'in_progress', label: '🟡 In Progress' },
  { value: 'completed', label: '🟢 Completed' },
  { value: 'rejected', label: '🔴 Rejected' },
];

const SORT_OPTIONS = [
  { value: 'createdAt-desc', label: 'Newest first' },
  { value: 'createdAt-asc',  label: 'Oldest first' },
  { value: 'status-asc',     label: 'Status A→Z' },
];

const STATUS_COUNT_ORDER: SkinRequestStatus[] = ['new', 'viewed', 'in_progress', 'completed', 'rejected'];

export default function SkinRequestsPage() {
  const [data, setData]       = useState<SkinRequestListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch]   = useState('');
  const [status, setStatus]   = useState('');
  const [sort, setSort]       = useState('createdAt-desc');

  const [searchDebounced, setSearchDebounced] = useState('');

  // Debounce search 300ms
  useEffect(() => {
    const timer = setTimeout(() => setSearchDebounced(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Auto-refresh every 30s
  useEffect(() => {
    load();
    const interval = setInterval(load, 30_000);
    return () => clearInterval(interval);
  }, [status, sort, searchDebounced]);

  async function load() {
    try {
      const [sortBy, sortOrder] = sort.split('-');
      const params = new URLSearchParams({ limit: '50', sortBy, sortOrder });
      if (status)          params.set('status', status);
      if (searchDebounced) params.set('search', searchDebounced);
      const res = await api.get<SkinRequestListResponse>(`/skin-requests?${params}`);
      setData(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const counts = (data?.statusCounts || {}) as Record<string, number>;

  return (
    <div>
      <div className="page-header">
        <div className="page-header-left">
          <h1>Skin Design Requests</h1>
          <p>Custom shoe skin requests submitted by SoleMate users. Auto-refreshes every 30s.</p>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>⟳ Refresh</button>
      </div>

      {/* Status count pills */}
      <div className="flex gap-2" style={{ marginBottom: 20, flexWrap: 'wrap' }}>
        {STATUS_COUNT_ORDER.map((s) => (
          <button
            key={s}
            className="btn btn-ghost btn-sm"
            style={{ gap: 6 }}
            onClick={() => setStatus(status === s ? '' : s)}
          >
            <StatusBadge status={s} />
            <span style={{ fontWeight: 700 }}>{counts[s] || 0}</span>
          </button>
        ))}
      </div>

      {/* Filter bar */}
      <div style={{ marginBottom: 20 }}>
        <FilterBar
          search={search}
          onSearchChange={setSearch}
          status={status}
          onStatusChange={setStatus}
          statusOptions={STATUS_OPTIONS}
          sortBy={sort}
          onSortChange={setSort}
          sortOptions={SORT_OPTIONS}
          placeholder="Search by email, description, or shoe…"
        />
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
          <Spinner size="lg" />
        </div>
      ) : !data || data.requests.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <span className="empty-state-icon">🎨</span>
            <h3 className="empty-state-title">No requests found</h3>
            <p className="empty-state-sub">
              {search || status
                ? 'Try adjusting your search or filter.'
                : 'Custom skin design requests from the mobile app will appear here.'}
            </p>
          </div>
        </div>
      ) : (
        <>
          <p className="text-sm text-muted" style={{ marginBottom: 14 }}>
            Showing {data.requests.length} of {data.total} requests
          </p>
          <div className="grid-requests">
            {data.requests.map((req) => (
              <SkinRequestCard key={req._id} request={req} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
