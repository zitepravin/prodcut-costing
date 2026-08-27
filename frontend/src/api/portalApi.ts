import { httpClient } from './httpClient';

export async function portalLogin(email: string, password: string) {
  const { data } = await httpClient.post('/api/v1/portal/login', { email, password });
  return data as Record<string, unknown>;
}

export async function portalMyRfqs(token?: string) {
  const { data } = await httpClient.get('/api/v1/portal/my-rfqs', {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  return data as Array<Record<string, unknown>>;
}

export async function portalSubmitQuote(supplierId: string, body: Record<string, unknown>) {
  const { data } = await httpClient.post(`/api/v1/portal/quotes?supplierId=${encodeURIComponent(supplierId)}`, body);
  return data as Record<string, unknown>;
}
