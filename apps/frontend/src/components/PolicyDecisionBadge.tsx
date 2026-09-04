import type { PolicyDecisionOutcome } from '../types/api';

interface Props {
  decision: PolicyDecisionOutcome;
  size?: 'sm' | 'lg';
}

const CONFIG: Record<PolicyDecisionOutcome, { label: string; color: string; bg: string; border: string; icon: string }> = {
  ACTION_ALLOWED: {
    label: 'ACTION ALLOWED',
    color: 'var(--policy-allowed)',
    bg: 'rgba(74,222,128,0.08)',
    border: 'rgba(74,222,128,0.3)',
    icon: '✓',
  },
  HUMAN_APPROVAL_REQUIRED: {
    label: 'HUMAN APPROVAL REQUIRED',
    color: 'var(--policy-approval)',
    bg: 'rgba(250,204,21,0.08)',
    border: 'rgba(250,204,21,0.3)',
    icon: '⏸',
  },
  ACTION_BLOCKED: {
    label: 'ACTION BLOCKED',
    color: 'var(--policy-blocked)',
    bg: 'rgba(248,113,113,0.08)',
    border: 'rgba(248,113,113,0.3)',
    icon: '✕',
  },
};

export default function PolicyDecisionBadge({ decision, size = 'sm' }: Props) {
  const c = CONFIG[decision];
  const isLg = size === 'lg';
  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: isLg ? '8px' : '5px',
      padding: isLg ? '6px 14px' : '2px 8px',
      borderRadius: isLg ? '6px' : '4px',
      fontSize: isLg ? '13px' : '11px',
      fontWeight: 700,
      letterSpacing: '0.05em',
      backgroundColor: c.bg,
      color: c.color,
      border: `1px solid ${c.border}`,
      whiteSpace: 'nowrap',
    }}>
      <span style={{ fontSize: isLg ? '14px' : '10px' }}>{c.icon}</span>
      {c.label}
    </span>
  );
}
