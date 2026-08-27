import { httpClient } from './httpClient';

export interface DashboardKpis {
  activePrograms: number;
  materialMasters: number;
  bomNodes: number;
  shouldCostBuildUps: number;
  packagingProfiles: number;
  activeTargetCosts: number;
  pendingApprovalSteps: number;
  targetVariancesExceedingThreshold: number;
  unreadNotifications: number;
  openSupplierRisks?: number;
  pendingPolicyEvidence?: number;
  openWarrantyClaims?: number;
  unresolvedGuaranteeEvents?: number;
  pendingPerformanceFeedback?: number;
  draftRecommendations?: number;
  submittedRecommendations?: number;
  expiringCapabilities?: number;
  openSupportTickets?: number;
  inProgressSupportTickets?: number;
  waitingCustomerSupportTickets?: number;
  resolvedSupportTickets?: number;
  openRfqs?: number;
  rfqsClosingSoon?: number;
  awaitingQuotations?: number;
  openPurchaseOrders?: number;
  overduePurchaseOrders?: number;
  openPurchaseRequests?: number;
  buyerOpenOrders?: number;
  purchasingSpend?: string | number;
  incompleteMaterialMasters?: number;
  draftRoutings?: number;
  routingMissingAssignments?: number;
  cycleTimeExceptions?: number;
  activeToolingRecords?: number;
  toolingInvestment?: string | number;
}

export async function fetchDashboardKpis(): Promise<DashboardKpis> {
  const { data } = await httpClient.get<DashboardKpis>('/api/v1/dashboard/kpis');
  return data;
}

export interface ProgramCostPosition {
  programId: string;
  programCode: string;
  modelName?: string;
  reportingCurrency: string;
  asOf: string;
  selectionPolicy: string;
  shouldCostTotal: string | number | null;
  shouldCostStatus: string;
  targetTotal: string | number | null;
  targetStatus: string;
  targetSource: string | null;
  absoluteVariance: string | number | null;
  percentageVariance: string | number | null;
  varianceStatus: string;
  coverage: {
    bomNodesInProgram: number;
    nodesWithSelectedShouldCost: number;
    nodesMissingShouldCost: number;
    nodesWithCurrencyGap: number;
    buildUpsConsideredBeforeSelection: number;
    coverageRatio: string | number;
  };
  drivers: { componentCode: string; displayName: string; amount: string | number; status: string }[];
  dataQualityNotes: string[];
}

export async function fetchProgramCostPosition(programId: string): Promise<ProgramCostPosition> {
  const { data } = await httpClient.get<ProgramCostPosition>(
    `/api/v1/programs/${encodeURIComponent(programId)}/cost-position`,
  );
  return data;
}
