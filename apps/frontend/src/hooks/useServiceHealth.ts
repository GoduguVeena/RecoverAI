import { useState, useEffect, useCallback } from 'react';
import { fetchBackendHealth, fetchMlHealth } from '../api/health';

export type ServiceStatus = 'checking' | 'online' | 'offline';

export interface ServiceHealth {
  backend: ServiceStatus;
  ml: ServiceStatus;
  refresh: () => void;
}

export function useServiceHealth(): ServiceHealth {
  const [backend, setBackend] = useState<ServiceStatus>('checking');
  const [ml, setMl] = useState<ServiceStatus>('checking');

  const check = useCallback(() => {
    setBackend('checking');
    setMl('checking');

    fetchBackendHealth()
      .then((h) => setBackend(h.status === 'UP' ? 'online' : 'offline'))
      .catch(() => setBackend('offline'));

    fetchMlHealth()
      .then((h) => setMl(h.status === 'UP' ? 'online' : 'offline'))
      .catch(() => setMl('offline'));
  }, []);

  useEffect(() => { check(); }, [check]);

  return { backend, ml, refresh: check };
}
