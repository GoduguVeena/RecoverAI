import type { ServiceStatus } from '../hooks/useServiceHealth';

interface Props {
  backend: ServiceStatus;
  ml: ServiceStatus;
}

function StatusDot({ status, label }: { status: ServiceStatus; label: string }) {
  const colors: Record<ServiceStatus, string> = {
    checking: '#facc15',
    online:   '#4ade80',
    offline:  '#f87171',
  };
  const texts: Record<ServiceStatus, string> = {
    checking: 'Checking',
    online:   'Online',
    offline:  'Offline',
  };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
      <span style={{
        width: 7, height: 7, borderRadius: '50%',
        backgroundColor: colors[status],
        boxShadow: status === 'online' ? `0 0 6px ${colors[status]}` : 'none',
        flexShrink: 0,
      }} />
      <span style={{ color: 'var(--text-secondary)', fontSize: '12px' }}>{label}</span>
      <span style={{ color: colors[status], fontSize: '12px', fontWeight: 600 }}>
        {texts[status]}
      </span>
    </div>
  );
}

export default function Nav({ backend, ml }: Props) {
  return (
    <header style={{
      position: 'sticky',
      top: 0,
      zIndex: 100,
      backgroundColor: 'var(--bg-surface)',
      borderBottom: '1px solid var(--border-subtle)',
      padding: '0 var(--space-6)',
      height: '56px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 'var(--space-4)',
    }}>
      {/* Brand */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexShrink: 0 }}>
        <div>
          <div style={{
            fontSize: '16px',
            fontWeight: 700,
            color: 'var(--brand)',
            letterSpacing: '-0.02em',
            lineHeight: 1.1,
          }}>
            RecoverAI
          </div>
          <div style={{ fontSize: '10px', color: 'var(--text-muted)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
            Recovery Operations
          </div>
        </div>
        {/* Divider */}
        <div style={{ width: 1, height: 28, backgroundColor: 'var(--border-subtle)', marginLeft: 4 }} />
        {/* Environment badge */}
        <span style={{
          fontSize: '10px',
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          backgroundColor: 'rgba(251,146,60,0.15)',
          color: '#fb923c',
          border: '1px solid rgba(251,146,60,0.3)',
          padding: '2px 7px',
          borderRadius: '4px',
        }}>
          DRY RUN · DEMO
        </span>
      </div>

      {/* Service status */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 'var(--space-6)',
        flexShrink: 0,
      }}>
        <StatusDot status={backend} label="Backend" />
        <StatusDot status={ml} label="ML Service" />
      </div>
    </header>
  );
}
