import { httpClient } from './httpClient';
import type {
  GenerateShouldCostRequest,
  ParametricRequest,
  ParametricResponse,
  ShouldCostBuildUp,
  VarianceResponse,
  WaterfallResponse,
} from '../types/api';

export async function generateShouldCost(request: GenerateShouldCostRequest): Promise<ShouldCostBuildUp> {
  const { data } = await httpClient.post<ShouldCostBuildUp>('/api/v1/should-cost/generate', request);
  return data;
}

export async function listScenarios(bomNodeId: string): Promise<ShouldCostBuildUp[]> {
  const { data } = await httpClient.get<ShouldCostBuildUp[]>(
    `/api/v1/should-cost/by-bom-node/${encodeURIComponent(bomNodeId)}/scenarios`,
  );
  return data;
}

export async function getWaterfall(buildUpId: string): Promise<WaterfallResponse> {
  const { data } = await httpClient.get<WaterfallResponse>(
    `/api/v1/should-cost/${encodeURIComponent(buildUpId)}/waterfall`,
  );
  return data;
}

export async function parametricEstimate(request: ParametricRequest): Promise<ParametricResponse> {
  const { data } = await httpClient.post<ParametricResponse>('/api/v1/should-cost/parametric', request);
  return data;
}

export async function varianceAgainstQuote(buildUpId: string, quotedPrice: string): Promise<VarianceResponse> {
  const { data } = await httpClient.post<VarianceResponse>('/api/v1/should-cost/variance', {
    buildUpId,
    quotedPrice,
  });
  return data;
}

export async function lockBuildUp(buildUpId: string): Promise<ShouldCostBuildUp> {
  const { data } = await httpClient.put<ShouldCostBuildUp>(
    `/api/v1/should-cost/${encodeURIComponent(buildUpId)}/lock`,
  );
  return data;
}

export interface DeepVarianceResponse {
  buildUpId: string;
  shouldCostTotal: string | number;
  quotedTotal: string | number;
  absoluteVarianceTotal: string | number;
  percentageVarianceTotal: string | number | null;
  components: { componentCode: string; displayName: string; sequenceNo: number; shouldCostAmount: string | number;
    quotedAmount: string | number | null; absoluteVariance: string | number | null; percentageVariance: string | number | null; dataStatus: string }[];
  topContributors: string[];
  explanation: string;
}
export async function deepVariance(request: { buildUpId: string; quotedTotal: string | number; componentSplits?: { componentCode: string; quotedAmount: string | number }[] }): Promise<DeepVarianceResponse> {
  const { data } = await httpClient.post<DeepVarianceResponse>('/api/v1/should-cost/deep-variance', request);
  return data;
}
