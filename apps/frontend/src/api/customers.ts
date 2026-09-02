import { apiFetch } from './client';
import type { ApiResponse, CustomerResponse } from '../types/api';

export async function fetchCustomer(id: string): Promise<CustomerResponse> {
  const res = await apiFetch<ApiResponse<CustomerResponse>>(`/api/v1/customers/${id}`);
  return res.data;
}
