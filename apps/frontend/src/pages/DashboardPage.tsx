import { useServiceHealth } from '../hooks/useServiceHealth';
import { useRecoveryCases } from '../hooks/useRecoveryCases';
import Nav from '../components/Nav';
import MetricsBar from '../components/MetricsBar';
import CasesTable from '../components/CasesTable';

export default function DashboardPage() {
  const health = useServiceHealth();
  const {
    cases, metrics, totalElements, totalPages, currentPage,
    loading, error, statusFilter, setStatusFilter,
    setPage, refresh, lastRefreshed,
  } = useRecoveryCases();

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-base)' }}>
      <Nav backend={health.backend} ml={health.ml} />

      <div style={{
        maxWidth: '1400px',
        margin: '0 auto',
        padding: 'var(--space-6)',
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-6)',
      }}>
        {/* Page header */}
        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 'var(--space-4)',
          flexWrap: 'wrap',
        }}>
          <div>
            <h1 style={{
              fontSize: '20px',
              fontWeight: 700,
              color: 'var(--text-primary)',
              margin: 0,
              letterSpacing: '-0.02em',
            }}>
              Recovery Operations
            </h1>
            <p style={{
              fontSize: '13px',
              color: 'var(--text-muted)',
              marginTop: '4px',
              margin: '4px 0 0',
            }}>
              Operator console for AI-assisted payment recovery &middot; Policy-gated &middot; Dry-run simulation
            </p>
          </div>
          <button
            onClick={() => { health.refresh(); refresh(); }}
            disabled={loading}
            style={{
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-subtle)',
              color: 'var(--text-secondary)',
              borderRadius: 'var(--radius-md)',
              padding: '8px 16px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: loading ? 'default' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              flexShrink: 0,
            }}
          >
            <span style={{ fontSize: '14px' }}>↻</span>
            Refresh All
          </button>
        </div>

        {/* Metrics */}
        <MetricsBar metrics={metrics} totalFromApi={totalElements} />

        {/* Cases table */}
        <CasesTable
          cases={cases}
          loading={loading}
          error={error}
          statusFilter={statusFilter}
          onFilterChange={setStatusFilter}
          onRefresh={refresh}
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
          lastRefreshed={lastRefreshed}
        />
      </div>
    </div>
  );
}
