import type { SkinRequestStatus, GlbUploadStatus, IntegrationStatus } from '../../types';

type AnyStatus = SkinRequestStatus | GlbUploadStatus | IntegrationStatus;

const LABELS: Record<string, string> = {
  new:         'New',
  viewed:      'Viewed',
  in_progress: 'In Progress',
  completed:   'Completed',
  rejected:    'Rejected',
  pending:     'Pending',
  approved:    'Approved',
  in_review:   'In Review',
  scheduled:   'Scheduled',
  deployed:    'Deployed',
};

export default function StatusBadge({ status }: { status: AnyStatus }) {
  return (
    <span className={`badge badge-${status}`}>
      {LABELS[status] || status}
    </span>
  );
}
