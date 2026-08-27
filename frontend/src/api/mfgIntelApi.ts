import { httpClient } from './httpClient';

export async function listActivities() {
  const { data } = await httpClient.get('/api/v1/mfg-intel/activities');
  return data as Array<Record<string, unknown>>;
}

export async function createActivity(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/mfg-intel/activities', body);
  return data as Record<string, unknown>;
}

export async function calculateActivity(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/mfg-intel/activities/calculate', body);
  return data as Record<string, unknown>;
}

export async function listCapacityCenters(periodCode?: string) {
  const { data } = await httpClient.get('/api/v1/mfg-intel/capacity-centers', {
    params: periodCode ? { periodCode } : undefined,
  });
  return data as Array<Record<string, unknown>>;
}

export async function createCapacityCenter(body: Record<string, unknown>) {
  const { data } = await httpClient.post('/api/v1/mfg-intel/capacity-centers', body);
  return data as Record<string, unknown>;
}

export async function addCapacityLoad(centerId: string, body: Record<string, unknown>) {
  const { data } = await httpClient.post(`/api/v1/mfg-intel/capacity-centers/${centerId}/loads`, body);
  return data as Record<string, unknown>;
}

export async function getUtilization(centerId: string) {
  const { data } = await httpClient.get(`/api/v1/mfg-intel/capacity-centers/${centerId}/utilization`);
  return data as Record<string, unknown>;
}
