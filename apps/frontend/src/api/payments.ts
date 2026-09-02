import { apiFetch } from './client';
import type { ApiResponse, PaymentResponse } from '../types/api';

export async function fetchPayment(id: string): Promise<PaymentResponse> {
  const res = await apiFetch<ApiResponse<PaymentResponse>>(`/api/v1/payments/${id}`);
  return res.data;
}
