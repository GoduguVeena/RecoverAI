const BACKEND_BASE = import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080';
const ML_BASE = import.meta.env.VITE_ML_URL ?? 'http://localhost:8000';

export { BACKEND_BASE, ML_BASE };

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly requestId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function handleResponse<T>(res: Response): Promise<T> {
  const contentType = res.headers.get('content-type') ?? '';
  const isJson = contentType.includes('application/json');
  const body = isJson ? await res.json() : await res.text();

  if (!res.ok) {
    const message =
      (isJson && (body?.message || body?.error)) ||
      `HTTP ${res.status} ${res.statusText}`;
    const requestId = res.headers.get('X-Request-Id') ?? undefined;
    throw new ApiError(res.status, message, requestId);
  }

  return body as T;
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const url = `${BACKEND_BASE}${path}`;
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options?.headers ?? {}),
    },
    ...options,
  });
  return handleResponse<T>(res);
}

export async function mlFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const url = `${ML_BASE}${path}`;
  const res = await fetch(url, {
    headers: { Accept: 'application/json', ...(options?.headers ?? {}) },
    ...options,
  });
  return handleResponse<T>(res);
}
