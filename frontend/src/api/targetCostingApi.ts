import { httpClient } from './httpClient';

export type TargetCostStatus = 'DRAFT' | 'ACTIVE' | 'SUPERSEDED';
export type TargetApprovalStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
export type ActualCostSource = 'SHOULD_COST' | 'BOM_ROLLUP';

export interface TargetCostRecord {
  targetCostId: string;
  bomNodeId: string;
  programId: string;
  targetAmount: string;
  currencyCode: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  status: TargetCostStatus;
  approvalStatus: TargetApprovalStatus;
  notes: string | null;
}

export interface TargetVariance {
  varianceId: string;
  targetCostId: string;
  bomNodeId: string;
  programId: string;
  targetAmount: string;
  actualAmount: string;
  actualSource: ActualCostSource;
  actualSourceId: string | null;
  absoluteVariance: string;
  percentageVariance: string;
  exceedsThreshold: boolean;
  thresholdPct: string | null;
  calculationBasis: string | null;
  calculatedAt: string | null;
}

export async function listTargetsByBomNode(bomNodeId: string): Promise<TargetCostRecord[]> {
  const { data } = await httpClient.get<TargetCostRecord[]>('/api/v1/target-costing/targets', {
    params: { bomNodeId },
  });
  return data;
}

export async function createTarget(req: {
  bomNodeId: string;
  targetAmount: string;
  currencyCode?: string;
  effectiveFrom?: string;
  notes?: string;
}): Promise<TargetCostRecord> {
  const { data } = await httpClient.post<TargetCostRecord>('/api/v1/target-costing/targets', req);
  return data;
}

export async function activateTarget(targetCostId: string): Promise<TargetCostRecord> {
  const { data } = await httpClient.post<TargetCostRecord>(
    `/api/v1/target-costing/targets/${encodeURIComponent(targetCostId)}/activate`,
  );
  return data;
}

export async function computeVariance(req: {
  targetCostId: string;
  actualSource: ActualCostSource;
  shouldCostBuildUpId?: string;
  thresholdPct?: string;
}): Promise<TargetVariance> {
  const { data } = await httpClient.post<TargetVariance>('/api/v1/target-costing/variance', req);
  return data;
}

export async function varianceHistory(bomNodeId: string): Promise<TargetVariance[]> {
  const { data } = await httpClient.get<TargetVariance[]>('/api/v1/target-costing/variance', {
    params: { bomNodeId },
  });
  return data;
}

export async function exceedingVariances(): Promise<{ items: TargetVariance[]; totalExceeding: number }> {
  const { data } = await httpClient.get<{ items: TargetVariance[]; totalExceeding: number }>(
    '/api/v1/target-costing/variance/exceeding',
  );
  return data;
}

export async function markTargetSubmitted(targetCostId: string): Promise<TargetCostRecord> {
  const { data } = await httpClient.post<TargetCostRecord>(
    `/api/v1/target-costing/targets/${encodeURIComponent(targetCostId)}/mark-submitted`,
  );
  return data;
}

