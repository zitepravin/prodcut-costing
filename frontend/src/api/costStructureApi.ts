import { httpClient } from './httpClient';

export async function getComponentHierarchy() {
  const { data } = await httpClient.get('/api/v1/cost-structure/components/hierarchy');
  return data as { roots: Array<Record<string, unknown>> };
}

export async function getCostedBom(rootBomNodeId: string) {
  const { data } = await httpClient.get(`/api/v1/cost-structure/costed-bom/${rootBomNodeId}`);
  return data as Record<string, unknown>;
}

export async function materializeItemization(buildUpId: string) {
  const { data } = await httpClient.post(`/api/v1/cost-structure/itemization/should-cost/${buildUpId}`);
  return data as Record<string, unknown>;
}

export async function getItemization(refType: string, refId: string) {
  const { data } = await httpClient.get(`/api/v1/cost-structure/itemization/${refType}/${refId}`);
  return data as Record<string, unknown>;
}
