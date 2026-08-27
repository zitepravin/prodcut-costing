import { httpClient } from './httpClient';
import type { BomTreeNode, BomNodeResponse } from '../types/api';

export async function getBomTree(programId: string): Promise<BomTreeNode[]> {
  const { data } = await httpClient.get<BomTreeNode[]>(`/api/v1/bom/${encodeURIComponent(programId)}/tree`);
  return data;
}

export async function getBomNode(nodeId: string): Promise<BomNodeResponse> {
  const { data } = await httpClient.get<BomNodeResponse>(`/api/v1/bom/nodes/${encodeURIComponent(nodeId)}`);
  return data;
}

export async function triggerProgramRollup(programId: string): Promise<unknown> {
  const { data } = await httpClient.post(`/api/v1/bom/${encodeURIComponent(programId)}/rollup-program`);
  return data;
}

export async function flattenBom(nodeId: string): Promise<unknown> {
  const { data } = await httpClient.get(`/api/v1/bom/${encodeURIComponent(nodeId)}/flatten`);
  return data;
}
