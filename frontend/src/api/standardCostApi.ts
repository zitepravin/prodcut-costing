import { httpClient } from './httpClient';

export async function listStandardCosts(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/standard-costs', { params: { bomNodeId } });
  return data as Array<Record<string, unknown>>;
}

export async function standardCostHistory(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/standard-costs/history', { params: { bomNodeId } });
  return data as Record<string, unknown>;
}

export async function createFromShouldCost(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/standard-costs/from-should-cost', body);
  return data as Record<string, unknown>;
}

export async function markForRelease(id: string) {
  const { data } = await httpClient.post(`/api/v1/standard-costs/${id}/mark-for-release`);
  return data as Record<string, unknown>;
}

export async function releaseStandardCost(id: string) {
  const { data } = await httpClient.post(`/api/v1/standard-costs/${id}/release`);
  return data as Record<string, unknown>;
}

export async function freezeStandardCost(id: string) {
  const { data } = await httpClient.post(`/api/v1/standard-costs/${id}/freeze`);
  return data as Record<string, unknown>;
}
