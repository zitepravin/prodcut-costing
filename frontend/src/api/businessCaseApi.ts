import { httpClient } from './httpClient';

export interface BusinessCaseScenario {
  scenarioId: string;
  programId: string;
  scenarioCode: string;
  scenarioName: string;
  status: string;
  currencyCode: string;
  unitSellingPrice: number | string;
  unitShouldCost: number | string;
  unitVariableCost: number | string;
  annualVolume: number | string;
  horizonYears: number;
  discountRatePct: number | string;
  initialInvestment: number | string;
  fixedAnnualCost: number | string;
  contributionMarginUnit?: number | string;
  annualContribution?: number | string;
  npvAmount?: number | string;
  paybackYears?: number | string;
  calculationBasis?: string;
}

export async function listBusinessCases(programId: string): Promise<BusinessCaseScenario[]> {
  const { data } = await httpClient.get<BusinessCaseScenario[]>('/api/v1/business-cases', {
    params: { programId },
  });
  return data;
}

export async function createBusinessCase(body: Record<string, unknown>): Promise<BusinessCaseScenario> {
  const { data } = await httpClient.post<BusinessCaseScenario>('/api/v1/business-cases', body);
  return data;
}

export async function calculateBusinessCase(scenarioId: string): Promise<BusinessCaseScenario> {
  const { data } = await httpClient.post<BusinessCaseScenario>(`/api/v1/business-cases/${scenarioId}/calculate`);
  return data;
}
