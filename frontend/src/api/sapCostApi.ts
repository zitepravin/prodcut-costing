import { httpClient } from './httpClient';

export type SapComponent = {
  componentSeq: number;
  sapComponentCode: string;
  sapComponentName?: string | null;
  amount: string | number;
  mappedComponentCode?: string | null;
};

export type SapStaging = {
  stagingId: string;
  sourceSystem: string;
  sapMaterial: string;
  sapPlant: string;
  costingVariant?: string | null;
  costingVersion?: string | null;
  estimateDate?: string | null;
  currencyCode?: string | null;
  totalCost?: string | number | null;
  status: string;
  shouldCostBuildUpId?: string | null;
  bomNodeId?: string | null;
  receivedAt?: string | null;
  comparedAt?: string | null;
  components: SapComponent[];
  messages?: string[];
};

export type SapMapping = {
  mappingId: string;
  sapComponentCode: string;
  ourComponentCode: string;
  notes?: string | null;
  activeFlag: boolean;
};

export type SapCompareResult = {
  stagingId: string;
  totalStatus: string;
  sapTotal?: string | number | null;
  shouldTotal?: string | number | null;
  absoluteVariance?: string | number | null;
  percentageVariance?: string | number | null;
  shouldCostBuildUpId: string;
  components: Array<{
    code: string;
    sapAmount?: string | number | null;
    shouldAmount?: string | number | null;
    variance?: string | number | null;
    status: string;
  }>;
  dataQualityNotes: string[];
};

export async function listSapCostEstimates(): Promise<SapStaging[]> {
  const { data } = await httpClient.get<SapStaging[]>('/api/v1/erp/sap/cost-estimates');
  return data;
}

export async function getSapCostEstimate(stagingId: string): Promise<SapStaging> {
  const { data } = await httpClient.get<SapStaging>(
    `/api/v1/erp/sap/cost-estimates/${encodeURIComponent(stagingId)}`,
  );
  return data;
}

export async function remapSapComponents(stagingId: string): Promise<SapStaging> {
  const { data } = await httpClient.post<SapStaging>(
    `/api/v1/erp/sap/cost-estimates/${encodeURIComponent(stagingId)}/map-components`,
  );
  return data;
}

export async function compareSapToShouldCost(
  stagingId: string,
  shouldCostBuildUpId: string,
): Promise<SapCompareResult> {
  const { data } = await httpClient.post<SapCompareResult>(
    `/api/v1/erp/sap/cost-estimates/${encodeURIComponent(stagingId)}/compare-should-cost`,
    { shouldCostBuildUpId },
  );
  return data;
}

export async function listSapComponentMappings(): Promise<SapMapping[]> {
  const { data } = await httpClient.get<SapMapping[]>('/api/v1/erp/sap/component-mappings');
  return data;
}

export async function upsertSapComponentMapping(body: {
  sapComponentCode: string;
  ourComponentCode: string;
  notes?: string;
}): Promise<SapMapping> {
  const { data } = await httpClient.put<SapMapping>('/api/v1/erp/sap/component-mappings', body);
  return data;
}
