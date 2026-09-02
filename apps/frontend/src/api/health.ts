import { apiFetch, mlFetch } from './client';
import type { ApiResponse, HealthResponse } from '../types/api';

export async function fetchBackendHealth(): Promise<HealthResponse> {
  const res = await apiFetch<ApiResponse<HealthResponse>>('/api/v1/health');
  return res.data;
}

export async function fetchMlHealth(): Promise<HealthResponse> {
  return mlFetch<HealthResponse>('/health');
}
