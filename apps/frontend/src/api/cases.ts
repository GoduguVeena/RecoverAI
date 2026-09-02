import { apiFetch } from './client';
import type {
  ApiResponse,
  PageResponse,
  RecoveryCaseResponse,
  RecoveryCaseStatus,
  RecoveryAnalysisResponse,
  RecoveryExecutionResponse,
} from '../types/api';

export async function fetchRecoveryCases(params?: {
  merchantId?: string;
  status?: RecoveryCaseStatus;
  page?: number;
  size?: number;
}): Promise<PageResponse<RecoveryCaseResponse>> {
  const query = new URLSearchParams();
  if (params?.merchantId) query.set('merchantId', params.merchantId);
  if (params?.status) query.set('status', params.status);
  query.set('page', String(params?.page ?? 0));
  query.set('size', String(params?.size ?? 20));

  const res = await apiFetch<ApiResponse<PageResponse<RecoveryCaseResponse>>>(
    `/api/v1/recovery/cases?${query.toString()}`,
  );
  return res.data;
}

export async function fetchRecoveryCase(id: string): Promise<RecoveryCaseResponse> {
  const res = await apiFetch<ApiResponse<RecoveryCaseResponse>>(
    `/api/v1/recovery/cases/${id}`,
  );
  return res.data;
}

export async function analyzeRecoveryCase(id: string): Promise<RecoveryAnalysisResponse> {
  const res = await apiFetch<ApiResponse<RecoveryAnalysisResponse>>(
    `/api/v1/recovery/cases/${id}/analyze`,
    { method: 'POST' },
  );
  return res.data;
}

export async function executeRecoveryCase(id: string): Promise<RecoveryExecutionResponse> {
  const res = await apiFetch<ApiResponse<RecoveryExecutionResponse>>(
    `/api/v1/recovery/cases/${id}/execute`,
    { method: 'POST' },
  );
  return res.data;
}
