import { useState, useEffect, useCallback } from 'react';
import { fetchRecoveryCases } from '../api/cases';
import type { RecoveryCaseResponse, RecoveryCaseStatus } from '../types/api';

export interface CaseMetrics {
  open: number;
  analyzingOrPending: number;
  resolved: number;
  blocked: number;
  total: number;
}

export interface UseRecoveryCasesResult {
  cases: RecoveryCaseResponse[];
  metrics: CaseMetrics;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  error: string | null;
  statusFilter: RecoveryCaseStatus | '';
  setStatusFilter: (s: RecoveryCaseStatus | '') => void;
  setPage: (p: number) => void;
  refresh: () => void;
  lastRefreshed: Date | null;
}

const PAGE_SIZE = 20;
const METRICS_SIZE = 200; // fetch up to 200 cases for metric counts

function computeMetrics(cases: RecoveryCaseResponse[]): CaseMetrics {
  let open = 0, analyzingOrPending = 0, resolved = 0, blocked = 0;
  for (const c of cases) {
    switch (c.status) {
      case 'OPEN': open++; break;
      case 'ANALYZING':
      case 'ACTION_PENDING':
      case 'AWAITING_APPROVAL': analyzingOrPending++; break;
      case 'RECOVERED': resolved++; break;
      case 'ESCALATED':
      case 'STOPPED':
      case 'FAILED': blocked++; break;
    }
  }
  return { open, analyzingOrPending, resolved, blocked, total: cases.length };
}

export function useRecoveryCases(): UseRecoveryCasesResult {
  const [cases, setCases] = useState<RecoveryCaseResponse[]>([]);
  const [allCasesForMetrics, setAllCasesForMetrics] = useState<RecoveryCaseResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilterState] = useState<RecoveryCaseStatus | ''>('');
  const [lastRefreshed, setLastRefreshed] = useState<Date | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);

  const refresh = useCallback(() => {
    setRefreshToken((t) => t + 1);
    setCurrentPage(0);
  }, []);

  const setStatusFilter = useCallback((s: RecoveryCaseStatus | '') => {
    setStatusFilterState(s);
    setCurrentPage(0);
  }, []);

  const setPage = useCallback((p: number) => setCurrentPage(p), []);

  // Fetch metrics (unfiltered, large page)
  useEffect(() => {
    fetchRecoveryCases({ page: 0, size: METRICS_SIZE })
      .then((data) => {
        // If more pages exist, fetch them all for accurate metrics
        if (data.totalPages <= 1) {
          setAllCasesForMetrics(data.content);
        } else {
          // Fetch all pages in parallel for accurate metrics
          const pagePromises = Array.from({ length: data.totalPages - 1 }, (_, i) =>
            fetchRecoveryCases({ page: i + 1, size: METRICS_SIZE }).then((d) => d.content),
          );
          Promise.all(pagePromises)
            .then((rest) => setAllCasesForMetrics([...data.content, ...rest.flat()]))
            .catch(() => setAllCasesForMetrics(data.content)); // best-effort
        }
      })
      .catch(() => {}); // metrics are best-effort; table error is authoritative
  }, [refreshToken]);

  // Fetch current page (filtered)
  useEffect(() => {
    setLoading(true);
    setError(null);

    const params: Parameters<typeof fetchRecoveryCases>[0] = {
      page: currentPage,
      size: PAGE_SIZE,
    };
    if (statusFilter) params.status = statusFilter;

    fetchRecoveryCases(params)
      .then((data) => {
        setCases(data.content);
        setTotalElements(data.totalElements);
        setTotalPages(data.totalPages);
        setLastRefreshed(new Date());
      })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : 'Failed to load recovery cases';
        setError(msg);
      })
      .finally(() => setLoading(false));
  }, [currentPage, statusFilter, refreshToken]);

  const metrics = computeMetrics(allCasesForMetrics);

  return {
    cases,
    metrics,
    totalElements,
    totalPages,
    currentPage,
    loading,
    error,
    statusFilter,
    setStatusFilter,
    setPage,
    refresh,
    lastRefreshed,
  };
}
