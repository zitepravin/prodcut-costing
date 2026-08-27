import { httpClient } from './httpClient';

export async function evaluateProcurement(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/procurement-alternatives/evaluate', body);
  return data as Record<string, unknown>;
}

export async function listProcurementSets(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/procurement-alternatives', { params: { bomNodeId } });
  return data as Array<Record<string, unknown>>;
}

export async function decideProcurement(setId: string, body: Record<string, unknown>) {
  const { data } = await httpClient.post(`/api/v1/procurement-alternatives/${setId}/decide`, body);
  return data as Record<string, unknown>;
}
