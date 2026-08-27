import { httpClient } from './httpClient';

export type MixedCostLegRequest = {
  alternativeType: 'INTERNAL_SHOULD_COST' | 'SUPPLIER_QUOTE';
  sourceRefId: string;
  mixRatio: string;
};

export type MixedCostLeg = {
  legSeq: number;
  alternativeType: string;
  sourceRefId: string;
  supplierId?: string | null;
  mixRatio: string | number;
  unitCostSnapshot?: string | number | null;
  currencySnapshot?: string | null;
  eligibilityStatus: string;
  eligibilityNotes?: string | null;
  dataStatus: string;
};

export type MixedCostVersion = {
  mixedCostId: string;
  programId: string;
  bomNodeId: string;
  versionLabel: string;
  status: string;
  reportingCurrency?: string | null;
  blendedUnitTotal?: string | number | null;
  blendedStatus?: string | null;
  ratioSumCheck?: string | number | null;
  notes?: string | null;
  calculatedAt?: string | null;
  frozenAt?: string | null;
  legs: MixedCostLeg[];
  dataQualityNotes?: string[];
};

export async function listMixedCosts(bomNodeId: string): Promise<MixedCostVersion[]> {
  const { data } = await httpClient.get<MixedCostVersion[]>(
    `/api/v1/bom-nodes/${encodeURIComponent(bomNodeId)}/mixed-costs`,
  );
  return data;
}

export async function getMixedCost(mixedCostId: string): Promise<MixedCostVersion> {
  const { data } = await httpClient.get<MixedCostVersion>(
    `/api/v1/mixed-costs/${encodeURIComponent(mixedCostId)}`,
  );
  return data;
}

export async function createMixedCost(
  bomNodeId: string,
  body: { versionLabel: string; notes?: string; legs: MixedCostLegRequest[] },
): Promise<MixedCostVersion> {
  const { data } = await httpClient.post<MixedCostVersion>(
    `/api/v1/bom-nodes/${encodeURIComponent(bomNodeId)}/mixed-costs`,
    body,
  );
  return data;
}

export async function replaceMixedCostLegs(
  mixedCostId: string,
  legs: MixedCostLegRequest[],
): Promise<MixedCostVersion> {
  const { data } = await httpClient.put<MixedCostVersion>(
    `/api/v1/mixed-costs/${encodeURIComponent(mixedCostId)}/legs`,
    { legs },
  );
  return data;
}

export async function calculateMixedCost(mixedCostId: string): Promise<MixedCostVersion> {
  const { data } = await httpClient.post<MixedCostVersion>(
    `/api/v1/mixed-costs/${encodeURIComponent(mixedCostId)}/calculate`,
  );
  return data;
}

export async function freezeMixedCost(mixedCostId: string): Promise<MixedCostVersion> {
  const { data } = await httpClient.post<MixedCostVersion>(
    `/api/v1/mixed-costs/${encodeURIComponent(mixedCostId)}/freeze`,
  );
  return data;
}
