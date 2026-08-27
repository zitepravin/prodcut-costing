import { httpClient } from './httpClient';

export async function requestSupportAccess(body: {
  customerId: string;
  durationMinutes?: number;
  reason?: string;
}) {
  const { data } = await httpClient.post('/api/v1/platform/support-access/request', body);
  return data as Record<string, unknown>;
}

export async function listSupportAccess(customerId: string) {
  const { data } = await httpClient.get(`/api/v1/platform/customers/${customerId}/support-access`);
  return data as Array<Record<string, unknown>>;
}

export async function approveSupportAccess(sessionId: string) {
  const { data } = await httpClient.post(`/api/v1/platform/support-access/${sessionId}/approve`);
  return data as Record<string, unknown>;
}

export async function platformDashboardSummary() {
  const { data } = await httpClient.get('/api/v1/platform/dashboard-summary');
  return data as Record<string, number>;
}
