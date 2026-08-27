import { httpClient } from './httpClient';

export interface CostComparisonWorkspace {
  bomNodeId: string;
  shouldCostScenarios: Array<{
    buildUpId: string;
    scenarioLabel: string;
    totalCost: string;
    status: string | null;
  }>;
  lowestShouldCostBuildUpId: string | null;
  supplierQuotes: Array<{
    quoteId: string;
    supplierId: string;
    unitPrice: string;
    currencyCode: string;
    status: string | null;
    leadTimeDays: number | null;
  }>;
  lowestAcceptedQuoteId: string | null;
  toolingCosts: Array<{
    calculationId: string;
    toolingRecordId: string;
    amortizedCostPerPiece: string;
    includedInPiecePrice: boolean;
  }>;
  targets: Array<{
    targetCostId: string;
    targetAmount: string;
    currencyCode: string;
    status: string | null;
    approvalStatus: string | null;
  }>;
  activeTargetAmount: string | null;
  lowestShouldCost: string | null;
  lowestAcceptedQuoteUnitPrice: string | null;
}

export async function fetchCostComparison(bomNodeId: string): Promise<CostComparisonWorkspace> {
  const { data } = await httpClient.get<CostComparisonWorkspace>(
    `/api/v1/cost-comparison/by-bom-node/${encodeURIComponent(bomNodeId)}`,
  );
  return data;
}
