import type { CaseMetrics } from '../hooks/useRecoveryCases';

interface Props {
  metrics: CaseMetrics;
  totalFromApi: number;
}

interface CardProps {
  label: string;
  value: number;
  accentColor: string;
  note?: string;
}

function MetricCard({ label, value, accentColor, note }: CardProps) {
  return (
    <div style={{
      backgroundColor: 'var(--bg-surface)',
      border: '1px solid var(--border-subtle)',
      borderRadius: 'var(--radius-md)',
      padding: 'var(--space-4) var(--space-6)',
      flex: '1 1 0',
      minWidth: '130px',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Left accent bar */}
      <div style={{
        position: 'absolute',
        left: 0, top: 0, bottom: 0, width: 3,
        backgroundColor: accentColor,
        borderRadius: '4px 0 0 4px',
      }} />
      <div style={{
        fontSize: '11px',
        fontWeight: 600,
        letterSpacing: '0.06em',
        textTransform: 'uppercase',
        color: 'var(--text-muted)',
        marginBottom: 'var(--space-2)',
      }}>
        {label}
      </div>
      <div style={{
        fontSize: '28px',
        fontWeight: 700,
        fontFamily: 'var(--font-mono)',
        color: accentColor,
        lineHeight: 1,
      }}>
        {value}
      </div>
      {note && (
        <div style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: 4 }}>{note}</div>
      )}
    </div>
  );
}

export default function MetricsBar({ metrics, totalFromApi }: Props) {
  const metricsNote = metrics.total < totalFromApi
    ? `of ${totalFromApi} total`
    : undefined;

  return (
    <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap' }}>
      <MetricCard
        label="Open"
        value={metrics.open}
        accentColor="var(--status-open)"
        note={metricsNote}
      />
      <MetricCard
        label="Analyzing / Pending"
        value={metrics.analyzingOrPending}
        accentColor="var(--status-analyzing)"
      />
      <MetricCard
        label="Resolved"
        value={metrics.resolved}
        accentColor="var(--status-recovered)"
      />
      <MetricCard
        label="Blocked / Stopped"
        value={metrics.blocked}
        accentColor="var(--status-blocked)"
      />
    </div>
  );
}
