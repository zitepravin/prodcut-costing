import { httpClient } from './httpClient';

export interface OperationRequest {
  sequenceNumber: number;
  processName?: string | null;
  machineCode?: string | null;
  laborCode?: string | null;
  cycleTimeSeconds: string;
  operatorCount?: string | null;
  setupTimeSeconds?: string | null;
  batchSize?: string | null;
  outputPartsPerCycle?: string | null;
}

export interface OperationResponse {
  operationId: string;
  sequenceNumber: number;
  processName: string | null;
  machineCode: string | null;
  laborCode: string | null;
  cycleTimeSeconds: string;
  operatorCount: string | null;
  setupTimeSeconds: string | null;
  batchSize: string | null;
  outputPartsPerCycle: string | null;
}

export interface RoutingResponse {
  routingId: string;
  bomNodeId: string;
  status: string;
  operations: OperationResponse[];
}

export interface CreateRoutingRequest {
  bomNodeId: string;
  operations: OperationRequest[];
}

export interface OperationCostLine {
  operationId: string;
  runCost: string;
  setupCostPerPiece: string;
  totalOperationCost: string;
}

export interface ProcessCostResponse {
  calculationId: string;
  routingId: string;
  bomNodeId: string;
  totalProcessCost: string;
  operationCosts: OperationCostLine[];
  calculationBasis: string | null;
  calculatedAt: string | null;
}

export interface CompareProcessResponse {
  lines: Array<{
    routingId: string;
    totalProcessCost: string;
    operationCount: number;
  }>;
  lowestCostRoutingId: string | null;
}

export async function createRouting(req: CreateRoutingRequest): Promise<RoutingResponse> {
  const { data } = await httpClient.post<RoutingResponse>('/api/v1/costing/process/routing', req);
  return data;
}

export async function getRouting(routingId: string): Promise<RoutingResponse> {
  const { data } = await httpClient.get<RoutingResponse>(
    `/api/v1/costing/process/routing/${encodeURIComponent(routingId)}`,
  );
  return data;
}

export async function listRoutingsByBomNode(bomNodeId: string): Promise<RoutingResponse[]> {
  const { data } = await httpClient.get<RoutingResponse[]>(
    `/api/v1/costing/process/routing/by-bom-node/${encodeURIComponent(bomNodeId)}`,
  );
  return data;
}

export async function approveRouting(routingId: string): Promise<RoutingResponse> {
  const { data } = await httpClient.put<RoutingResponse>(
    `/api/v1/costing/process/routing/${encodeURIComponent(routingId)}/approve`,
  );
  return data;
}

export async function calculateProcessCost(routingId: string): Promise<ProcessCostResponse> {
  const { data } = await httpClient.post<ProcessCostResponse>('/api/v1/costing/process/calculate', {
    routingId,
  });
  return data;
}

export async function compareProcessRoutings(routingIds: string[]): Promise<CompareProcessResponse> {
  const { data } = await httpClient.post<CompareProcessResponse>('/api/v1/costing/process/compare', {
    routingIds,
  });
  return data;
}
