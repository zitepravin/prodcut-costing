import { httpClient } from './httpClient';

export async function importActual(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/financial/actual-costs/import', body);
  return data as Record<string, unknown>;
}

export async function listActuals(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/financial/actual-costs', { params: { bomNodeId } });
  return data as Array<Record<string, unknown>>;
}

export async function costTriangle(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/financial/cost-triangle', { params: { bomNodeId } });
  return data as Record<string, unknown>;
}

export async function listValuationPolicies() {
  const { data } = await httpClient.get('/api/v1/financial/valuation-policies');
  return data as Array<Record<string, unknown>>;
}

export async function createValuationPolicy(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/financial/valuation-policies', body);
  return data as Record<string, unknown>;
}

export async function valuationConvert(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/financial/valuation-policies/convert', body);
  return data as Record<string, unknown>;
}
