import { useNavigate } from 'react-router-dom';
import type { RecoveryCaseResponse, RecoveryCaseStatus } from '../types/api';
import CaseStatusBadge from './CaseStatusBadge';
import ProbabilityBar from './ProbabilityBar';
import { shortId, formatDateTime, actionLabel, formatCurrency } from '../utils/format';

const ALL_STATUSES: Array<RecoveryCaseStatus | ''> = [
  '', 'OPEN', 'ANALYZING', 'ACTION_PENDING', 'AWAITING_APPROVAL',
  'RECOVERED', 'ESCALATED', 'STOPPED', 'FAILED',
];

const STATUS_FILTER_LABELS: Record<string, string> = {
  '': 'All Statuses',
  OPEN: 'Open',
  ANALYZING: 'Analyzing',
  ACTION_PENDING: 'Action Pending',
  AWAITING_APPROVAL: 'Awaiting Approval',
  RECOVERED: 'Recovered',
  ESCALATED: 'Escalated',
  STOPPED: 'Stopped',
  FAILED: 'Failed',
};

interface Props {
  cases: RecoveryCaseResponse[];
  loading: boolean;
  error: string | null;
  statusFilter: RecoveryCaseStatus | '';
  onFilterChange: (s: RecoveryCaseStatus | '') => void;
  onRefresh: () => void;
  currentPage: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (p: number) => void;
  lastRefreshed: Date | null;
}

export default function CasesTable({
  cases, loading, error, statusFilter, onFilterChange,
  onRefresh, currentPage, totalPages, totalElements, onPageChange, lastRefreshed,
}: Props) {
  const navigate = useNavigate();

  return (
    <div style={{
      backgroundColor: 'var(--bg-surface)',
      border: '1px solid var(--border-subtle)',
      borderRadius: 'var(--radius-lg)',
      overflow: 'hidden',
    }}>
      {/* Table header / controls */}
      <div style={{
        padding: 'var(--space-4) var(--space-6)',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 'var(--space-4)',
        flexWrap: 'wrap',
      }}>
        <div>
          <h3 style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
            Recovery Cases
          </h3>
          {lastRefreshed && (
            <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: 2 }}>
              {totalElements} case{totalElements !== 1 ? 's' : ''} &middot; refreshed {lastRefreshed.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })}
            </div>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          {/* Status filter */}
          <select
            value={statusFilter}
            onChange={(e) => onFilterChange(e.target.value as RecoveryCaseStatus | '')}
            style={{
              backgroundColor: 'var(--bg-input)',
              border: '1px solid var(--border-subtle)',
              color: 'var(--text-primary)',
              borderRadius: 'var(--radius-sm)',
              padding: '5px 10px',
              fontSize: '12px',
              cursor: 'pointer',
              outline: 'none',
            }}
          >
            {ALL_STATUSES.map((s) => (
              <option key={s} value={s}>{STATUS_FILTER_LABELS[s]}</option>
            ))}
          </select>
          {/* Refresh */}
          <button
            onClick={onRefresh}
            disabled={loading}
            title="Refresh"
            style={{
              backgroundColor: 'var(--bg-input)',
              border: '1px solid var(--border-subtle)',
              color: loading ? 'var(--text-muted)' : 'var(--text-secondary)',
              borderRadius: 'var(--radius-sm)',
              padding: '5px 10px',
              fontSize: '12px',
              cursor: loading ? 'default' : 'pointer',
              display: 'flex', alignItems: 'center', gap: '5px',
            }}
          >
            <span style={{
              display: 'inline-block',
              animation: loading ? 'spin 1s linear infinite' : 'none',
            }}>↻</span>
            {loading ? 'Loading…' : 'Refresh'}
          </button>
        </div>
      </div>

      {/* Scrollable table */}
      <div style={{ overflowX: 'auto' }}>
        <table style={{
          width: '100%',
          borderCollapse: 'collapse',
          fontSize: '13px',
        }}>
          <thead>
            <tr style={{ backgroundColor: 'var(--bg-elevated)' }}>
              {['CASE', 'PAYMENT', 'STATUS', 'PROBABILITY', 'RECOMMENDED ACTION', 'CREATED', ''].map((h) => (
                <th key={h} style={{
                  padding: '8px 16px',
                  textAlign: 'left',
                  fontSize: '10px',
                  fontWeight: 700,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  whiteSpace: 'nowrap',
                  borderBottom: '1px solid var(--border-subtle)',
                }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {!loading && !error && cases.length === 0 && (
              <tr>
                <td colSpan={7} style={{
                  padding: 'var(--space-8)',
                  textAlign: 'center',
                  color: 'var(--text-muted)',
                  fontSize: '13px',
                }}>
                  {statusFilter
                    ? `No cases with status "${STATUS_FILTER_LABELS[statusFilter]}"`
                    : 'No recovery cases found. Ingest a webhook event to create your first case.'}
                </td>
              </tr>
            )}
            {loading && (
              <tr>
                <td colSpan={7} style={{
                  padding: 'var(--space-8)',
                  textAlign: 'center',
                  color: 'var(--text-muted)',
                  fontSize: '13px',
                }}>
                  Loading recovery cases…
                </td>
              </tr>
            )}
            {error && (
              <tr>
                <td colSpan={7} style={{
                  padding: 'var(--space-8)',
                  textAlign: 'center',
                  color: 'var(--status-failed)',
                  fontSize: '13px',
                }}>
                  ⚠ Error loading cases: {error}
                </td>
              </tr>
            )}
            {!loading && !error && cases.map((c, idx) => (
              <tr
                key={c.id}
                style={{
                  backgroundColor: idx % 2 === 0 ? 'transparent' : 'rgba(30,41,59,0.4)',
                  borderBottom: '1px solid var(--border)',
                  transition: 'background-color 0.1s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'rgba(56,189,248,0.04)')}
                onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = idx % 2 === 0 ? 'transparent' : 'rgba(30,41,59,0.4)')}
              >
                {/* CASE */}
                <td style={{ padding: '10px 16px', whiteSpace: 'nowrap' }}>
                  <div style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: '12px',
                    color: 'var(--brand)',
                    fontWeight: 600,
                  }}>
                    {shortId(c.id)}
                  </div>
                  <div style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: 1 }}>
                    {c.id.slice(0, 18)}…
                  </div>
                </td>

                {/* PAYMENT */}
                <td style={{ padding: '10px 16px', whiteSpace: 'nowrap' }}>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--text-secondary)' }}>
                    {shortId(c.paymentId)}
                  </div>
                  {c.expectedRecoveryValue && (
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: 1 }}>
                      {formatCurrency(c.expectedRecoveryValue)}
                    </div>
                  )}
                </td>

                {/* STATUS */}
                <td style={{ padding: '10px 16px', whiteSpace: 'nowrap' }}>
                  <CaseStatusBadge status={c.status} />
                </td>

                {/* PROBABILITY */}
                <td style={{ padding: '10px 16px', minWidth: '160px' }}>
                  <ProbabilityBar value={c.recoveryProbability} />
                </td>

                {/* RECOMMENDED ACTION */}
                <td style={{ padding: '10px 16px', whiteSpace: 'nowrap' }}>
                  {c.recommendedAction ? (
                    <span style={{
                      fontSize: '11px',
                      fontWeight: 600,
                      backgroundColor: 'rgba(56,189,248,0.1)',
                      color: 'var(--brand)',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      textTransform: 'uppercase',
                      letterSpacing: '0.04em',
                    }}>
                      {actionLabel(c.recommendedAction)}
                    </span>
                  ) : (
                    <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>—</span>
                  )}
                </td>

                {/* CREATED */}
                <td style={{ padding: '10px 16px', whiteSpace: 'nowrap' }}>
                  <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                    {formatDateTime(c.createdAt)}
                  </div>
                </td>

                {/* ACTION */}
                <td style={{ padding: '10px 16px', textAlign: 'right', whiteSpace: 'nowrap' }}>
                  <button
                    onClick={() => navigate(`/cases/${c.id}`)}
                    style={{
                      backgroundColor: 'transparent',
                      border: '1px solid var(--border-subtle)',
                      color: 'var(--brand)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '4px 12px',
                      fontSize: '12px',
                      fontWeight: 600,
                      cursor: 'pointer',
                      transition: 'border-color 0.15s, background-color 0.15s',
                    }}
                    onMouseEnter={(e) => {
                      (e.currentTarget as HTMLButtonElement).style.backgroundColor = 'rgba(56,189,248,0.1)';
                      (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--brand)';
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLButtonElement).style.backgroundColor = 'transparent';
                      (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--border-subtle)';
                    }}
                  >
                    View Case →
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && !loading && !error && (
        <div style={{
          padding: 'var(--space-3) var(--space-6)',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 'var(--space-4)',
        }}>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
            Page {currentPage + 1} of {totalPages} &middot; {totalElements} total cases
          </span>
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            <button
              disabled={currentPage === 0}
              onClick={() => onPageChange(currentPage - 1)}
              style={paginationBtnStyle(currentPage === 0)}
            >
              ← Prev
            </button>
            <button
              disabled={currentPage >= totalPages - 1}
              onClick={() => onPageChange(currentPage + 1)}
              style={paginationBtnStyle(currentPage >= totalPages - 1)}
            >
              Next →
            </button>
          </div>
        </div>
      )}

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}

function paginationBtnStyle(disabled: boolean): React.CSSProperties {
  return {
    backgroundColor: disabled ? 'transparent' : 'var(--bg-input)',
    border: '1px solid var(--border-subtle)',
    color: disabled ? 'var(--text-muted)' : 'var(--text-secondary)',
    borderRadius: 'var(--radius-sm)',
    padding: '4px 12px',
    fontSize: '12px',
    cursor: disabled ? 'default' : 'pointer',
    opacity: disabled ? 0.4 : 1,
  };
}
