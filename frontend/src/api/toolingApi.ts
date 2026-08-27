import { httpClient } from './httpClient';

export interface ToolingRecordResponse {
  toolingRecordId: string;
  bomNodeId: string | null;
  toolingType: string | null;
  description: string | null;
  cavityCount: number | null;
  toolCost: string;
  toolLifeHits: string | null;
  refurbishmentCostPerEvent: string | null;
  cumulativeVolumeToDate: string | null;
  ownership: string;
  supplierId: string | null;
  toolingQuoteReference: string | null;
  plantLocation: string | null;
  installationDate: string | null;
  assetStatus: string;
}

export interface CreateToolingRequest {
  bomNodeId: string;
  toolingType: string;
  description: string;
  cavityCount?: number | null;
  toolCost: string;
  toolLifeHits?: string | null;
  refurbishmentCostPerEvent?: string | null;
  cumulativeVolumeToDate?: string | null;
  ownership: string;
  supplierId?: string | null;
  toolingQuoteReference?: string | null;
  plantLocation?: string | null;
  installationDate?: string | null;
}

export interface ToolingCostResponse {
  calculationId: string;
  toolingRecordId: string;
  bomNodeId: string | null;
  programId: string | null;
  baseAmortizedCostPerPiece: string;
  refurbishmentEventsProjected: number | null;
  refurbishmentCostPerPiece: string | null;
  totalAmortizedCostPerPiece: string;
  includedInPiecePrice: boolean;
  calculationBasis: string | null;
  calculatedAt: string | null;
}

export async function listToolingAssets(plantLocation?: string): Promise<ToolingRecordResponse[]> {
  const { data } = await httpClient.get<ToolingRecordResponse[]>('/api/v1/tooling/assets', {
    params: plantLocation ? { plantLocation } : undefined,
  });
  return data;
}

export async function getToolingByBomNode(bomNodeId: string): Promise<ToolingRecordResponse[]> {
  const { data } = await httpClient.get<ToolingRecordResponse[]>(
    `/api/v1/tooling/by-bom-node/${encodeURIComponent(bomNodeId)}`,
  );
  return data;
}

export async function createTooling(req: CreateToolingRequest): Promise<ToolingRecordResponse> {
  const { data } = await httpClient.post<ToolingRecordResponse>('/api/v1/tooling', req);
  return data;
}

export async function retireTooling(id: string, reason?: string): Promise<ToolingRecordResponse> {
  const { data } = await httpClient.put<ToolingRecordResponse>(
    `/api/v1/tooling/${encodeURIComponent(id)}/retire`,
    { reason: reason ?? null },
  );
  return data;
}

export async function calculateToolingCost(toolingRecordId: string): Promise<ToolingCostResponse> {
  const { data } = await httpClient.post<ToolingCostResponse>('/api/v1/tooling/calculate', {
    toolingRecordId,
  });
  return data;
}
