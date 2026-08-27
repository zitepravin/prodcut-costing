import { httpClient } from './httpClient';

export async function listCostingRuns() {
  const { data } = await httpClient.get('/api/v1/costing-runs');
  return data as Array<Record<string, unknown>>;
}

export async function createCostingRun(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/costing-runs', body);
  return data as Record<string, unknown>;
}

export async function executeCostingRun(runId: string) {
  const { data } = await httpClient.post(`/api/v1/costing-runs/${runId}/execute`);
  return data as Record<string, unknown>;
}

export async function getCostingRun(runId: string) {
  const { data } = await httpClient.get(`/api/v1/costing-runs/${runId}`);
  return data as Record<string, unknown>;
}

export async function lotSizeCurve(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/lot-size-costing/curve', body);
  return data as Record<string, unknown>;
}

export async function varianceCompare(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/variance/compare', body);
  return data as Record<string, unknown>;
}
