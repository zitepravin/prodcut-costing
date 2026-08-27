import { httpClient } from './httpClient';

export async function analyzeEcn(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/auto-intel/ecn-impacts/analyze', body);
  return data as Record<string, unknown>;
}

export async function listEcn(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/auto-intel/ecn-impacts', { params: { bomNodeId } });
  return data as Array<Record<string, unknown>>;
}

export async function runWhatIf(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/auto-intel/what-if/run', body);
  return data as Record<string, unknown>;
}

export async function listWhatIf(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/auto-intel/what-if', { params: { bomNodeId } });
  return data as Array<Record<string, unknown>>;
}
