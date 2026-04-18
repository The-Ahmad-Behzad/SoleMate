interface FilterBarProps {
  search: string;
  onSearchChange: (v: string) => void;
  status: string;
  onStatusChange: (v: string) => void;
  statusOptions: { value: string; label: string }[];
  sortBy?: string;
  onSortChange?: (v: string) => void;
  sortOptions?: { value: string; label: string }[];
  placeholder?: string;
  rightSlot?: React.ReactNode;
}

export default function FilterBar({
  search, onSearchChange,
  status, onStatusChange, statusOptions,
  sortBy, onSortChange, sortOptions,
  placeholder = 'Search…',
  rightSlot,
}: FilterBarProps) {
  return (
    <div className="filter-bar">
      <div className="search-input-wrap">
        <span className="search-icon">🔍</span>
        <input
          className="input"
          placeholder={placeholder}
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <select
        className="input"
        style={{ width: 'auto', minWidth: 140 }}
        value={status}
        onChange={(e) => onStatusChange(e.target.value)}
      >
        {statusOptions.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>

      {onSortChange && sortOptions && (
        <select
          className="input"
          style={{ width: 'auto', minWidth: 160 }}
          value={sortBy}
          onChange={(e) => onSortChange(e.target.value)}
        >
          {sortOptions.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      )}

      {rightSlot}
    </div>
  );
}
