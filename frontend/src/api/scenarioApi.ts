import { httpClient } from './httpClient';

export async function listScenarios(bomNodeId: string) {
  const { data } = await httpClient.get('/api/v1/costing-scenarios', { params: { bomNodeId } });
  return data as Array<Record<string, unknown>>;
}

export async function createScenario(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/costing-scenarios', body);
  return data as Record<string, unknown>;
}

export async function branchScenario(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/costing-scenarios/branch', body);
  return data as Record<string, unknown>;
}

export async function approveScenario(scenarioId: string) {
  const { data } = await httpClient.post(`/api/v1/costing-scenarios/${scenarioId}/approve`);
  return data as Record<string, unknown>;
}

export async function addAdditiveLine(scenarioId: string, body: Record<string, unknown>) {
  const { data } = await httpClient.post(`/api/v1/costing-scenarios/${scenarioId}/additive-lines`, body);
  return data as Record<string, unknown>;
}

export async function getScenario(scenarioId: string) {
  const { data } = await httpClient.get(`/api/v1/costing-scenarios/${scenarioId}`);
  return data as Record<string, unknown>;
}
