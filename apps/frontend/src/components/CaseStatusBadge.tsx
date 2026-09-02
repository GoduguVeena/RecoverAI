import type { RecoveryCaseStatus } from '../types/api';
import { statusLabel } from '../utils/format';

interface Props {
  status: RecoveryCaseStatus;
}

const STATUS_STYLES: Record<RecoveryCaseStatus, { bg: string; color: string }> = {
  OPEN:              { bg: 'rgba(250,204,21,0.12)',  color: 'var(--status-open)' },
  ANALYZING:         { bg: 'rgba(56,189,248,0.12)',  color: 'var(--status-analyzing)' },
  ACTION_PENDING:    { bg: 'rgba(251,146,60,0.12)',  color: 'var(--status-pending)' },
  AWAITING_APPROVAL: { bg: 'rgba(251,146,60,0.12)',  color: 'var(--status-awaiting)' },
  RECOVERED:         { bg: 'rgba(74,222,128,0.12)',  color: 'var(--status-recovered)' },
  ESCALATED:         { bg: 'rgba(192,132,252,0.12)', color: 'var(--status-escalated)' },
  STOPPED:           { bg: 'rgba(148,163,184,0.12)', color: 'var(--status-stopped)' },
  FAILED:            { bg: 'rgba(248,113,113,0.12)', color: 'var(--status-failed)' },
};

export default function CaseStatusBadge({ status }: Props) {
  const style = STATUS_STYLES[status] ?? { bg: 'rgba(148,163,184,0.12)', color: 'var(--text-muted)' };
  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: '5px',
      padding: '2px 8px',
      borderRadius: '4px',
      fontSize: '11px',
      fontWeight: 600,
      letterSpacing: '0.04em',
      textTransform: 'uppercase',
      backgroundColor: style.bg,
      color: style.color,
      whiteSpace: 'nowrap',
    }}>
      <span style={{
        width: 6, height: 6, borderRadius: '50%',
        backgroundColor: style.color, flexShrink: 0,
      }} />
      {statusLabel(status)}
    </span>
  );
}
