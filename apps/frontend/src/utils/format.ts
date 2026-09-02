import type { RecoveryCaseStatus, RecoveryActionType, PolicyDecisionOutcome } from '../types/api';

// ─── Currency ─────────────────────────────────────────────────────────────────

export function formatCurrency(value: string | number | null | undefined, currency = 'INR'): string {
  if (value === null || value === undefined) return '—';
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) return '—';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(num);
}

// ─── Probability ──────────────────────────────────────────────────────────────

export function formatProbability(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) return '—';
  return `${(num * 100).toFixed(1)}%`;
}

export function toProbabilityPct(value: string | number | null | undefined): number | null {
  if (value === null || value === undefined) return null;
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) return null;
  return Math.min(100, Math.max(0, num * 100));
}

// ─── Date/Time ────────────────────────────────────────────────────────────────

const dateFormatter = new Intl.DateTimeFormat('en-IN', {
  year: 'numeric', month: 'short', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hour12: false,
});

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  try { return dateFormatter.format(new Date(iso)); } catch { return iso; }
}

export function formatRelativeTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    const diff = Date.now() - new Date(iso).getTime();
    const secs = Math.floor(diff / 1000);
    if (secs < 60) return 'just now';
    const mins = Math.floor(secs / 60);
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  } catch { return iso; }
}

// ─── ID display ───────────────────────────────────────────────────────────────

export function shortId(id: string | null | undefined): string {
  if (!id) return '—';
  return id.slice(0, 8).toUpperCase();
}

// ─── Status labels ────────────────────────────────────────────────────────────

export function statusLabel(status: RecoveryCaseStatus): string {
  const labels: Record<RecoveryCaseStatus, string> = {
    OPEN: 'Open',
    ANALYZING: 'Analyzing',
    ACTION_PENDING: 'Action Pending',
    AWAITING_APPROVAL: 'Awaiting Approval',
    RECOVERED: 'Recovered',
    ESCALATED: 'Escalated',
    STOPPED: 'Stopped',
    FAILED: 'Failed',
  };
  return labels[status] ?? status;
}

// ─── Action labels ────────────────────────────────────────────────────────────

export function actionLabel(action: RecoveryActionType | null | undefined): string {
  if (!action) return '—';
  const labels: Record<RecoveryActionType, string> = {
    RETRY: 'Retry Payment',
    PAYMENT_LINK: 'Payment Link',
    NOTIFICATION: 'Notify Customer',
    ESCALATE: 'Escalate',
    STOP: 'Stop',
  };
  return labels[action] ?? action;
}

// ─── Policy labels ────────────────────────────────────────────────────────────

export function policyLabel(outcome: PolicyDecisionOutcome): string {
  const labels: Record<PolicyDecisionOutcome, string> = {
    ACTION_ALLOWED: 'Allowed',
    HUMAN_APPROVAL_REQUIRED: 'Approval Required',
    ACTION_BLOCKED: 'Blocked',
  };
  return labels[outcome] ?? outcome;
}
