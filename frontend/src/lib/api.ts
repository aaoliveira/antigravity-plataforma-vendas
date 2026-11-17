export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  components?: Record<string, { status: string; details?: Record<string, unknown> }>;
}

export interface InfoResponse {
  app?: Record<string, unknown>;
  [key: string]: unknown;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8082';

export async function fetchHealth(): Promise<HealthResponse> {
  const res = await fetch(`${API_BASE_URL}/actuator/health`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
    cache: 'no-store',
  });

  if (!res.ok) {
    throw new Error(`Health check failed with status: ${res.status}`);
  }

  return res.json();
}

export async function fetchInfo(): Promise<InfoResponse> {
  const res = await fetch(`${API_BASE_URL}/actuator/info`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
    cache: 'no-store',
  });

  if (!res.ok) {
    throw new Error(`Info check failed with status: ${res.status}`);
  }

  return res.json();
}
