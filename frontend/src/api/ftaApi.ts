import { httpClient } from './httpClient';
const BASE = '/api/v1/fta';
export async function ftaAnalyze(body: Record<string, unknown>) {
  const { data } = await httpClient.post(`${BASE}/analyze`, body);
  return data;
}
export async function listAgreements() {
  const { data } = await httpClient.get(`${BASE}/agreements`);
  return data;
}
