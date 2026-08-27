import { httpClient } from './httpClient';

/** Vendor Selection API — B1–B5 + questionnaire + comparison + qualification + risk */

export type EvidenceStatus = 'PENDING' | 'VERIFIED' | 'FAILED' | 'EXPIRED' | 'NOT_REQUIRED' | 'WAIVED';
export type CapabilityType = 'PART' | 'PROCESS' | 'COMMODITY' | 'PLANT' | 'CERTIFICATION' | 'OTHER';
export type CapabilityStatus = 'ACTIVE' | 'INACTIVE' | 'EXPIRED';
export type QualityMetricType =
  | 'PPM'
  | 'REJECTION_RATE'
  | 'NCR_COUNT'
  | 'COMPLAINT_COUNT'
  | 'CAPA_OPEN'
  | 'CAPA_OVERDUE'
  | 'AUDIT_SCORE'
  | 'FPY'
  | 'WARRANTY_CLAIMS'
  | 'CUSTOMER_COMPLAINT'
  | 'FIRST_PASS_YIELD'
  | 'OTHER';
export type GuaranteeType =
  | 'QUALITY'
  | 'DELIVERY'
  | 'CAPACITY'
  | 'PERFORMANCE'
  | 'TOOLING'
  | 'PRICE'
  | 'OTHER';
export type RiskCategory =
  | 'FINANCIAL'
  | 'QUALITY'
  | 'DELIVERY'
  | 'CAPACITY'
  | 'GEOGRAPHIC'
  | 'SUPPLY_CHAIN'
  | 'BUSINESS_CONTINUITY'
  | 'COMPLIANCE'
  | 'OTHER';
export type RiskSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type RiskLikelihood = 'HIGH' | 'MEDIUM' | 'LOW';
export type RiskStatus = 'OPEN' | 'MITIGATING' | 'ACCEPTED' | 'CLOSED';
export type LifecycleStatus =
  | 'DISCOVERED'
  | 'IMPORTED'
  | 'UNDER_VERIFICATION'
  | 'QUALIFIED'
  | 'EVALUATED'
  | 'RECOMMENDED'
  | 'APPROVAL_PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'BLOCKED'
  | 'SUSPENDED';

export interface Capability {
  capabilityId: string;
  supplierId: string;
  capabilityType: CapabilityType;
  capabilityCode: string;
  capabilityName: string;
  processCode: string | null;
  commodityCode: string | null;
  plantCode: string | null;
  maxAnnualCapacity: string | null;
  capacityUom: string | null;
  certified: boolean;
  validFrom: string | null;
  validTo: string | null;
  status: CapabilityStatus;
  notes: string | null;
}

export interface GateResult {
  gateCode: string;
  passed: boolean;
  detail: string;
}

export interface EligibilityResult {
  resultId: string;
  supplierId: string;
  bomNodeId: string | null;
  programId: string | null;
  overallEligible: boolean;
  failedGates: string[];
  gates: GateResult[];
  evaluatedAt: string;
  questionnaireEvaluationId?: string | null;
  questionnaireEligible?: boolean | null;
  openCriticalRiskCount?: number;
  openHighRiskCount?: number;
  openRiskCount?: number;
  lifecycleStatus?: string | null;
  lifecycleBlocked?: boolean;
}

export interface QualityMetric {
  metricId: string;
  supplierId: string;
  metricType: QualityMetricType;
  periodStart: string;
  periodEnd: string;
  metricValue: string;
  unit: string | null;
  sampleSize: number | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export interface DeliveryMetric {
  metricId: string;
  supplierId: string;
  periodStart: string;
  periodEnd: string;
  otdPercent: string | null;
  otifPercent: string | null;
  lateDeliveryCount: number | null;
  avgDelayDays: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export interface Warranty {
  warrantyId: string;
  supplierId: string;
  durationMonths: number | null;
  coverageSummary: string | null;
  exclusions: string | null;
  claimSlaDays: number | null;
  remedy: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  claimRate: string | null;
  historicalCost: string | null;
  currencyCode: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export interface Guarantee {
  guaranteeId: string;
  supplierId: string;
  guaranteeType: GuaranteeType;
  description: string | null;
  periodMonths: number | null;
  conditionsText: string | null;
  failureConsequence: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export type PolicyType =
  | 'QUALITY_POLICY'
  | 'ENVIRONMENTAL_POLICY'
  | 'SAFETY_POLICY'
  | 'BUSINESS_CONTINUITY'
  | 'ETHICAL_SOURCING'
  | 'ANTI_BRIBERY'
  | 'INFORMATION_SECURITY'
  | 'RESPONSIBLE_SOURCING'
  | 'CONFLICT_OF_INTEREST'
  | 'SUBCONTRACTOR_CONTROL'
  | 'SUSTAINABILITY'
  | 'REGULATORY_COMPLIANCE'
  | 'MATERIAL_COMPLIANCE'
  | 'OTHER';

export interface PolicyEvidence {
  policyId: string;
  supplierId: string;
  policyType: PolicyType;
  policyTitle: string;
  description: string | null;
  policyVersion: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  expiryDate: string | null;
  declarationOnly: boolean;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  waiverReason: string | null;
  notes: string | null;
}

export interface PolicySummary {
  supplierId: string;
  totalCount: number;
  verifiedCount: number;
  pendingCount: number;
  failedOrExpiredCount: number;
  waivedCount: number;
  declarationOnlyCount: number;
  items: PolicyEvidence[];
}

export type WarrantyClaimStatus = 'OPEN' | 'ACCEPTED' | 'REJECTED' | 'CLOSED' | 'WITHDRAWN';
export type GuaranteeEventType =
  | 'FAILURE'
  | 'BREACH'
  | 'PENALTY_APPLIED'
  | 'REPLACEMENT_TRIGGERED'
  | 'RESOLVED'
  | 'OTHER';
export type PerformanceFeedbackType =
  | 'QUALITY'
  | 'DELIVERY'
  | 'WARRANTY'
  | 'SERVICE'
  | 'CAPACITY'
  | 'OTHER';

export interface WarrantyClaim {
  claimId: string;
  supplierId: string;
  warrantyId: string | null;
  claimDate: string;
  claimNumber: string | null;
  partNumber: string | null;
  claimCost: string | number | null;
  currencyCode: string | null;
  claimStatus: WarrantyClaimStatus;
  acceptedFlag: boolean | null;
  resolutionDays: number | null;
  resolutionSummary: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export interface WarrantyClaimSummary {
  supplierId: string;
  totalClaims: number;
  openClaims: number;
  closedClaims?: number;
  withdrawnClaims?: number;
  acceptedClaims: number;
  rejectedClaims: number;
  totalClaimCost: string | number | null;
  currencyCode: string | null;
  claims: WarrantyClaim[];
}

export interface GuaranteeEvent {
  eventId: string;
  supplierId: string;
  guaranteeId: string | null;
  eventDate: string;
  eventType: GuaranteeEventType;
  description: string | null;
  consequenceApplied: string | null;
  penaltyAmount: string | null;
  currencyCode: string | null;
  resolvedFlag: boolean;
  resolutionSummary: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export interface AfterSalesMetric {
  metricId: string;
  supplierId: string;
  periodStart: string;
  periodEnd: string;
  responseTimeHours: string | null;
  resolutionTimeHours: string | null;
  emergencySupportFlag: boolean | null;
  fieldSupportFlag: boolean | null;
  sparePartsAvailable: boolean | null;
  localSupportFlag: boolean | null;
  unresolvedComplaints: number | null;
  avgClosureDays: string | null;
  serviceSlaMetPercent: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  notes: string | null;
}

export interface PerformanceFeedback {
  feedbackId: string;
  supplierId: string;
  bomNodeId: string | null;
  programId: string | null;
  feedbackDate: string;
  feedbackType: PerformanceFeedbackType;
  periodStart: string | null;
  periodEnd: string | null;
  metricCode: string | null;
  metricValue: string | null;
  unit: string | null;
  narrative: string | null;
  sourceReference: string | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  appliedToHistory: boolean;
  appliedAt: string | null;
  historyMetricId?: string | null;
  historyMetricKind?: string | null;
  notes: string | null;
}

export interface PerformanceFeedbackListItem {
  feedbackId: string;
  supplierId: string;
  feedbackDate: string;
  feedbackType: PerformanceFeedbackType;
  metricCode: string | null;
  metricValue: string | null;
  unit: string | null;
  evidenceStatus: EvidenceStatus;
  appliedToHistory: boolean;
  historyMetricId?: string | null;
  historyMetricKind?: string | null;
  narrative: string | null;
}

export interface Supplier360 {
  supplierId: string;
  qualityMetrics: QualityMetric[];
  deliveryMetrics: DeliveryMetric[];
  warranties: Warranty[];
  warrantyClaims?: WarrantyClaim[];
  guarantees: Guarantee[];
  guaranteeEvents?: GuaranteeEvent[];
  afterSalesMetrics?: AfterSalesMetric[];
  performanceFeedback?: PerformanceFeedbackListItem[];
  capabilities: Capability[];
  policies: PolicyEvidence[];
}

export interface TcoComponentLine {
  componentCode: string;
  amount: string | null;
  status: 'PRESENT' | 'DATA_NOT_AVAILABLE';
  notes: string | null;
}

export interface TcoResult {
  tcoId: string;
  supplierId: string;
  bomNodeId: string | null;
  programId: string | null;
  currencyCode: string;
  annualVolume: string | null;
  tcoTotal: string | null;
  unitTco: string | null;
  dataComplete: boolean;
  missingComponents: string[];
  components: TcoComponentLine[];
  calculatedAt: string;
}

export interface ScorecardCategoryDef {
  categoryCode: string;
  categoryName: string;
  weightPercent: string;
  sequenceNo: number;
}

export interface ScorecardTemplate {
  templateId: string;
  templateCode: string;
  templateName: string;
  description: string | null;
  status?: string | null;
  versionNo?: number | null;
  versionLabel?: string | null;
  parentTemplateId?: string | null;
  lineageRootId?: string | null;
  categories: ScorecardCategoryDef[];
}

export interface ScorecardResult {
  resultId: string;
  templateId: string;
  supplierId: string;
  bomNodeId?: string | null;
  overallScore: string;
  maxScore?: string | null;
  categoryBreakdown?: { categoryCode: string; weightPercent: string; rawScore: string; weightedScore: string }[];
  categoryScores?: { categoryCode: string; weightPercent: string; rawScore: string; weightedScore: string }[];
  notes?: string | null;
  scoredAt?: string | null;
  evaluatedAt?: string | null;
}

export interface RecommendationCandidate {
  candidateId: string;
  supplierId: string;
  rankPosition: number;
  eligible: boolean;
  overallScore: string | null;
  tcoTotal: string | null;
  tcoCurrency: string | null;
  tcoDataComplete: boolean | null;
  failedGates: string | null;
  strengths: string | null;
  weaknesses: string | null;
  lifecycleStatus?: string | null;
  lifecycleBlocked?: boolean;
}

export interface Recommendation {
  recommendationId: string;
  sourcingEventCode: string;
  bomNodeId: string | null;
  programId: string | null;
  templateId: string | null;
  status: string;
  recommendedSupplierId: string | null;
  decisionComment: string | null;
  overrideSupplierId: string | null;
  overrideReason: string | null;
  ranking: RecommendationCandidate[];
  createdAt: string | null;
}

export interface ComparisonRow {
  supplierId: string;
  eligible: boolean | null;
  overallScore: string | null;
  tcoTotal: string | null;
  tcoCurrency: string | null;
  tcoDataComplete: boolean | null;
  ppm: string | null;
  otdPercent: string | null;
  warrantyMonths: number | null;
  failedGates: string[] | null;
  missingData: string[] | null;
  riskFlags: string[] | null;
}

export interface ComparisonResult {
  comparisonId: string;
  supplierIds: string[];
  rows: ComparisonRow[];
  tradeOffSummary: string | null;
  notes: string | null;
}

export interface Qualification {
  qualificationId: string;
  supplierId: string;
  lifecycleStatus: string;
  previousStatus: string | null;
  changeReason: string | null;
  recommendationId: string | null;
  effectiveAt: string;
}

export interface RiskItem {
  riskId: string;
  supplierId: string;
  riskCategory: RiskCategory;
  riskTitle: string;
  description: string | null;
  severity: RiskSeverity;
  likelihood: RiskLikelihood;
  status: RiskStatus;
  mitigationPlan: string | null;
  residualSeverity: RiskSeverity | null;
  evidenceStatus: EvidenceStatus;
  evidenceRef: string | null;
  identifiedAt: string;
  closedAt: string | null;
}

export interface RiskSummary {
  supplierId: string;
  openCount: number;
  criticalOpenCount: number;
  highOpenCount: number;
  openRisks: RiskItem[];
}

// ——— Capabilities & eligibility ———

export async function createCapability(req: {
  supplierId: string;
  capabilityType: CapabilityType;
  capabilityCode: string;
  capabilityName: string;
  processCode?: string;
  commodityCode?: string;
  plantCode?: string;
  maxAnnualCapacity?: string;
  capacityUom?: string;
  certified?: boolean;
  validFrom?: string;
  validTo?: string;
  notes?: string;
}): Promise<Capability> {
  const { data } = await httpClient.post<Capability>('/api/v1/vendor-selection/capabilities', req);
  return data;
}

export async function listCapabilities(supplierId: string): Promise<Capability[]> {
  const { data } = await httpClient.get<Capability[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/capabilities`,
  );
  return data;
}

export async function evaluateEligibility(req: {
  supplierId: string;
  bomNodeId?: string;
  programId?: string;
  requiredCapabilityCodes?: string[];
  requiredMinCapacity?: string;
  requireCertification?: boolean;
  requiredPolicyTypes?: string[];
  requireQuestionnaire?: boolean;
  requireNoCriticalOpenRisks?: boolean;
  requireNoHighOpenRisks?: boolean;
}): Promise<EligibilityResult> {
  const { data } = await httpClient.post<EligibilityResult>(
    '/api/v1/vendor-selection/eligibility/evaluate',
    req,
  );
  return data;
}

// ——— Evidence ———

export async function addQualityMetric(req: {
  supplierId: string;
  metricType: QualityMetricType;
  periodStart: string;
  periodEnd: string;
  metricValue: string;
  unit?: string;
  sampleSize?: number;
  evidenceRef?: string;
  notes?: string;
}): Promise<QualityMetric> {
  const { data } = await httpClient.post<QualityMetric>('/api/v1/vendor-selection/quality-metrics', req);
  return data;
}

export async function verifyQualityMetric(
  metricId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<QualityMetric> {
  const { data } = await httpClient.post<QualityMetric>(
    `/api/v1/vendor-selection/quality-metrics/${encodeURIComponent(metricId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

export async function addDeliveryMetric(req: {
  supplierId: string;
  periodStart: string;
  periodEnd: string;
  otdPercent?: string;
  otifPercent?: string;
  lateDeliveryCount?: number;
  avgDelayDays?: string;
  evidenceRef?: string;
  notes?: string;
}): Promise<DeliveryMetric> {
  const { data } = await httpClient.post<DeliveryMetric>('/api/v1/vendor-selection/delivery-metrics', req);
  return data;
}

export async function verifyDeliveryMetric(
  metricId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<DeliveryMetric> {
  const { data } = await httpClient.post<DeliveryMetric>(
    `/api/v1/vendor-selection/delivery-metrics/${encodeURIComponent(metricId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

export async function addWarranty(req: Record<string, unknown>): Promise<Warranty> {
  const { data } = await httpClient.post<Warranty>('/api/v1/vendor-selection/warranties', req);
  return data;
}

export async function verifyWarranty(
  warrantyId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<Warranty> {
  const { data } = await httpClient.post<Warranty>(
    `/api/v1/vendor-selection/warranties/${encodeURIComponent(warrantyId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

export async function addGuarantee(req: Record<string, unknown>): Promise<Guarantee> {
  const { data } = await httpClient.post<Guarantee>('/api/v1/vendor-selection/guarantees', req);
  return data;
}

export async function verifyGuarantee(
  guaranteeId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<Guarantee> {
  const { data } = await httpClient.post<Guarantee>(
    `/api/v1/vendor-selection/guarantees/${encodeURIComponent(guaranteeId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

export async function fetchSupplier360(supplierId: string): Promise<Supplier360> {
  const { data } = await httpClient.get<Supplier360>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/360`,
  );
  return data;
}

// ——— TCO ———

export async function calculateTco(req: {
  supplierId: string;
  bomNodeId?: string;
  programId?: string;
  currencyCode?: string;
  annualVolume?: string;
  purchasePrice?: string;
  freight?: string;
  logistics?: string;
  packaging?: string;
  inventoryCost?: string;
  qualityRejectionCost?: string;
  reworkCost?: string;
  warrantyCost?: string;
  serviceCost?: string;
  toolingCost?: string;
  otherCost?: string;
}): Promise<TcoResult> {
  const { data } = await httpClient.post<TcoResult>('/api/v1/vendor-selection/tco/calculate', req);
  return data;
}

export async function listTco(supplierId: string): Promise<TcoResult[]> {
  const { data } = await httpClient.get<TcoResult[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/tco`,
  );
  return data;
}

// ——— Scorecard ———

export async function createScorecardTemplate(req: {
  templateCode: string;
  templateName: string;
  description?: string;
  categories: { categoryCode: string; categoryName: string; weightPercent: string; sequenceNo: number }[];
}): Promise<ScorecardTemplate> {
  const { data } = await httpClient.post<ScorecardTemplate>(
    '/api/v1/vendor-selection/scorecards/templates',
    req,
  );
  return data;
}

export async function listScorecardTemplates(): Promise<ScorecardTemplate[]> {
  const { data } = await httpClient.get<ScorecardTemplate[]>('/api/v1/vendor-selection/scorecards/templates');
  return data;
}

export async function evaluateScorecard(req: {
  templateId: string;
  supplierId: string;
  bomNodeId?: string;
  categoryScores: { categoryCode: string; rawScore0to100: string }[];
  notes?: string;
}): Promise<ScorecardResult> {
  const { data } = await httpClient.post<ScorecardResult>(
    '/api/v1/vendor-selection/scorecards/evaluate',
    req,
  );
  return data;
}

// ——— Recommendation ———

export async function createRecommendation(req: {
  sourcingEventCode: string;
  bomNodeId?: string;
  programId?: string;
  templateId?: string;
  candidates: {
    supplierId: string;
    eligible: boolean;
    overallScore?: string;
    tcoTotal?: string;
    tcoCurrency?: string;
    tcoDataComplete?: boolean;
    failedGates?: string[];
    missingDataHints?: string[];
  }[];
}): Promise<Recommendation> {
  const { data } = await httpClient.post<Recommendation>('/api/v1/vendor-selection/recommendations', req);
  return data;
}

export async function getRecommendation(id: string): Promise<Recommendation> {
  const { data } = await httpClient.get<Recommendation>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(id)}`,
  );
  return data;
}

export async function listRecommendationsByStatus(status: string): Promise<Recommendation[]> {
  const { data } = await httpClient.get<Recommendation[]>(
    '/api/v1/vendor-selection/recommendations',
    { params: { status } },
  );
  return data;
}


export async function submitRecommendation(id: string): Promise<Recommendation> {
  const { data } = await httpClient.post<Recommendation>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(id)}/submit`,
  );
  return data;
}

export async function approveRecommendation(
  id: string,
  comment?: string,
): Promise<Recommendation> {
  const { data } = await httpClient.post<Recommendation>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(id)}/approve`,
    { comment },
  );
  return data;
}

export async function rejectRecommendation(
  id: string,
  comment?: string,
): Promise<Recommendation> {
  const { data } = await httpClient.post<Recommendation>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(id)}/reject`,
    { comment },
  );
  return data;
}

export async function overrideRecommendation(
  id: string,
  overrideSupplierId: string,
  overrideReason: string,
  comment?: string,
): Promise<Recommendation> {
  const { data } = await httpClient.post<Recommendation>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(id)}/override`,
    { comment, overrideSupplierId, overrideReason },
  );
  return data;
}

// ——— Comparison ———

export async function createComparison(req: {
  supplierIds: string[];
  bomNodeId?: string;
  programId?: string;
  currencyCode?: string;
  notes?: string;
}): Promise<ComparisonResult> {
  const { data } = await httpClient.post<ComparisonResult>('/api/v1/vendor-selection/comparisons', req);
  return data;
}

export async function getComparison(id: string): Promise<ComparisonResult> {
  const { data } = await httpClient.get<ComparisonResult>(
    `/api/v1/vendor-selection/comparisons/${encodeURIComponent(id)}`,
  );
  return data;
}

// ——— Qualification lifecycle ———

export async function initializeQualification(supplierId: string): Promise<Qualification> {
  const { data } = await httpClient.post<Qualification>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/qualification/initialize`,
  );
  return data;
}

export async function transitionQualification(req: {
  supplierId: string;
  targetStatus: LifecycleStatus;
  reason?: string;
  recommendationId?: string;
}): Promise<Qualification> {
  const { data } = await httpClient.post<Qualification>(
    '/api/v1/vendor-selection/qualification/transitions',
    req,
  );
  return data;
}

export async function getQualification(supplierId: string): Promise<Qualification> {
  const { data } = await httpClient.get<Qualification>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/qualification`,
  );
  return data;
}

export async function listQualificationHistory(supplierId: string): Promise<Qualification[]> {
  const { data } = await httpClient.get<Qualification[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/qualification/history`,
  );
  return data;
}

// ——— Risk register ———

export async function createRisk(req: {
  supplierId: string;
  riskCategory: RiskCategory;
  riskTitle: string;
  description?: string;
  severity: RiskSeverity;
  likelihood: RiskLikelihood;
  mitigationPlan?: string;
  evidenceRef?: string;
}): Promise<RiskItem> {
  const { data } = await httpClient.post<RiskItem>('/api/v1/vendor-selection/risks', req);
  return data;
}

export async function updateRiskStatus(
  riskId: string,
  req: {
    status: RiskStatus;
    mitigationPlan?: string;
    residualSeverity?: RiskSeverity;
    evidenceRef?: string;
  },
): Promise<RiskItem> {
  const { data } = await httpClient.post<RiskItem>(
    `/api/v1/vendor-selection/risks/${encodeURIComponent(riskId)}/status`,
    req,
  );
  return data;
}

export async function listRisks(supplierId: string): Promise<RiskItem[]> {
  const { data } = await httpClient.get<RiskItem[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/risks`,
  );
  return data;
}

export async function getRiskSummary(supplierId: string): Promise<RiskSummary> {
  const { data } = await httpClient.get<RiskSummary>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/risks/summary`,
  );
  return data;
}

// ——— Policy & compliance (V43) ———

export async function addPolicy(req: {
  supplierId: string;
  policyType: PolicyType;
  policyTitle: string;
  description?: string;
  policyVersion?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  expiryDate?: string;
  declarationOnly?: boolean;
  evidenceRef?: string;
  notes?: string;
}): Promise<PolicyEvidence> {
  const { data } = await httpClient.post<PolicyEvidence>('/api/v1/vendor-selection/policies', req);
  return data;
}

export async function verifyPolicy(
  policyId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
  waiverReason?: string,
): Promise<PolicyEvidence> {
  const { data } = await httpClient.post<PolicyEvidence>(
    `/api/v1/vendor-selection/policies/${encodeURIComponent(policyId)}/verify`,
    { evidenceStatus, evidenceRef, waiverReason },
  );
  return data;
}

export async function listPolicies(
  supplierId: string,
  opts?: { evidenceStatus?: string; policyType?: string },
): Promise<PolicyEvidence[]> {
  const { data } = await httpClient.get<PolicyEvidence[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/policies`,
    { params: { evidenceStatus: opts?.evidenceStatus, policyType: opts?.policyType } },
  );
  return data;
}

export async function getPolicySummary(supplierId: string): Promise<PolicySummary> {
  const { data } = await httpClient.get<PolicySummary>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/policies/summary`,
  );
  return data;
}

// ——— Questionnaire evaluation aggregate (V45) ———

export interface QuestionnaireEvaluationLine {
  questionCode: string;
  questionVersion: number;
  category: string | null;
  required: boolean;
  mandatoryGate: boolean;
  answered: boolean;
  gatePassed: boolean | null;
  scoredValue: string | null;
  weight: string | null;
  evidenceStatus: EvidenceStatus | null;
  detail: string;
}

export interface QuestionnaireEvaluation {
  evaluationId: string;
  supplierId: string;
  overallEligible: boolean;
  mandatoryGatesPassed: boolean;
  requiredAnswered: boolean;
  totalActiveQuestions: number;
  answeredCount: number;
  mandatoryGateCount: number;
  mandatoryGateFailed: number;
  evidencePendingCount: number;
  weightedScore: string | null;
  maxPossibleScore: string | null;
  failedGateCodes: string[];
  missingQuestionCodes: string[];
  lines: QuestionnaireEvaluationLine[];
  evaluatedAt: string;
}

export async function evaluateQuestionnaire(
  supplierId: string,
  notes?: string,
): Promise<QuestionnaireEvaluation> {
  const params = notes ? `?notes=${encodeURIComponent(notes)}` : '';
  const { data } = await httpClient.post<QuestionnaireEvaluation>(
    `/api/v1/vendor-selection/questionnaire/suppliers/${encodeURIComponent(supplierId)}/evaluate${params}`,
  );
  return data;
}

export async function latestQuestionnaireEvaluation(
  supplierId: string,
): Promise<QuestionnaireEvaluation> {
  const { data } = await httpClient.get<QuestionnaireEvaluation>(
    `/api/v1/vendor-selection/questionnaire/suppliers/${encodeURIComponent(supplierId)}/evaluation`,
  );
  return data;
}

export async function questionnaireEvaluationHistory(
  supplierId: string,
): Promise<QuestionnaireEvaluation[]> {
  const { data } = await httpClient.get<QuestionnaireEvaluation[]>(
    `/api/v1/vendor-selection/questionnaire/suppliers/${encodeURIComponent(supplierId)}/evaluation/history`,
  );
  return data;
}

// ——— Recommendation explainability (V48) ———

export interface RecommendationExplanation {
  explanationId: string;
  recommendationId: string;
  recommendedSupplierId: string | null;
  confidenceLevel: string | null;
  dataUsedSummary: string | null;
  eligibilitySummary: string | null;
  scoreSummary: string | null;
  tcoSummary: string | null;
  strengthsSummary: string | null;
  weaknessesSummary: string | null;
  riskSummary: string | null;
  missingDataSummary: string | null;
  costVarianceNotes: string | null;
  evidenceNotes: string | null;
  negotiationOpportunities?: string | null;
  decisionBoundaryNote: string;
  factJson: string | null;
  calculatedJson: string | null;
  inferenceJson: string | null;
  missingJson: string | null;
}

export async function getRecommendationExplanation(
  recommendationId: string,
): Promise<RecommendationExplanation> {
  const { data } = await httpClient.get<RecommendationExplanation>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(recommendationId)}/explanation`,
  );
  return data;
}

// ——— Recommendation decision ledger (V49) ———

export interface RecommendationDecision {
  decisionId: string;
  recommendationId: string;
  explanationId: string | null;
  previousStatus: string;
  newStatus: string;
  decisionAction: string;
  recommendedSupplierId: string | null;
  overrideSupplierId: string | null;
  decisionComment: string | null;
  overrideReason: string | null;
  decidedBy: string | null;
  decidedAt: string;
}

export async function listRecommendationDecisions(
  recommendationId: string,
): Promise<RecommendationDecision[]> {
  const { data } = await httpClient.get<RecommendationDecision[]>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(recommendationId)}/decisions`,
  );
  return data;
}

// ——— Scorecard template customer customization ———

export async function cloneScorecardTemplate(
  sourceTemplateId: string,
  req: {
    newTemplateCode: string;
    newTemplateName: string;
    description?: string;
    categories?: { categoryCode: string; categoryName: string; weightPercent: string; sequenceNo: number }[];
  },
): Promise<ScorecardTemplate> {
  const { data } = await httpClient.post<ScorecardTemplate>(
    `/api/v1/vendor-selection/scorecards/templates/${encodeURIComponent(sourceTemplateId)}/clone`,
    req,
  );
  return data;
}

export async function deactivateScorecardTemplate(templateId: string): Promise<ScorecardTemplate> {
  const { data } = await httpClient.post<ScorecardTemplate>(
    `/api/v1/vendor-selection/scorecards/templates/${encodeURIComponent(templateId)}/deactivate`,
  );
  return data;
}

export async function replaceScorecardCategories(
  templateId: string,
  categories: { categoryCode: string; categoryName: string; weightPercent: string; sequenceNo: number }[],
): Promise<ScorecardTemplate> {
  const { data } = await httpClient.put<ScorecardTemplate>(
    `/api/v1/vendor-selection/scorecards/templates/${encodeURIComponent(templateId)}/categories`,
    categories,
  );
  return data;
}


// ——— Questionnaire bank configuration ———

export type QuestionCategory =
  | 'COST' | 'QUALITY' | 'SERVICE' | 'WARRANTY' | 'GUARANTEE' | 'DELIVERY'
  | 'CAPACITY' | 'TECHNICAL' | 'HISTORY' | 'POLICY' | 'RISK' | 'COMPLIANCE' | 'OTHER';

export type QuestionAnswerType =
  | 'YES_NO' | 'NUMBER' | 'PERCENTAGE' | 'CURRENCY' | 'DATE' | 'TEXT'
  | 'SINGLE_SELECT' | 'MULTI_SELECT' | 'DOCUMENT' | 'RATING';

export interface QuestionnaireQuestion {
  questionId: string;
  questionCode: string;
  versionNo: number;
  category: QuestionCategory | string;
  questionText: string;
  answerType: QuestionAnswerType | string;
  requiredFlag: boolean;
  evidenceRequired: boolean;
  mandatoryGate: boolean;
  weight: string | null;
  scoringRule: string | null;
  activeFlag: boolean;
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

export async function listQuestionnaireQuestions(category?: string): Promise<QuestionnaireQuestion[]> {
  const { data } = await httpClient.get<QuestionnaireQuestion[]>(
    '/api/v1/vendor-selection/questionnaire/questions',
    { params: category ? { category } : undefined },
  );
  return data;
}

export async function createQuestionnaireQuestion(req: {
  questionCode: string;
  category: QuestionCategory | string;
  questionText: string;
  answerType: QuestionAnswerType | string;
  requiredFlag?: boolean;
  evidenceRequired?: boolean;
  mandatoryGate?: boolean;
  weight?: string;
  scoringRule?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
}): Promise<QuestionnaireQuestion> {
  const { data } = await httpClient.post<QuestionnaireQuestion>(
    '/api/v1/vendor-selection/questionnaire/questions',
    {
      requiredFlag: true,
      evidenceRequired: false,
      mandatoryGate: false,
      ...req,
    },
  );
  return data;
}

export async function submitQuestionnaireResponse(req: {
  supplierId: string;
  questionId: string;
  answerText?: string;
  answerNumber?: string;
  answerBoolean?: boolean;
  answerDate?: string;
  evidenceRef?: string;
}): Promise<unknown> {
  const { data } = await httpClient.post(
    '/api/v1/vendor-selection/questionnaire/responses',
    req,
  );
  return data;
}

export interface QuestionnaireAnswer {
  responseId: string;
  supplierId: string;
  questionId: string;
  questionCode: string;
  questionVersion: number;
  answerText: string | null;
  answerNumber: string | null;
  answerBoolean: boolean | null;
  answerDate: string | null;
  evidenceStatus: EvidenceStatus;
  gatePassed: boolean | null;
  scoredValue: string | null;
  respondedAt: string;
}

export async function listQuestionnaireResponses(supplierId: string): Promise<QuestionnaireAnswer[]> {
  const { data } = await httpClient.get<QuestionnaireAnswer[]>(
    `/api/v1/vendor-selection/questionnaire/suppliers/${encodeURIComponent(supplierId)}/responses`,
  );
  return data;
}

export async function verifyQuestionnaireEvidence(
  responseId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
  waiverReason?: string,
): Promise<QuestionnaireAnswer> {
  const { data } = await httpClient.post<QuestionnaireAnswer>(
    `/api/v1/vendor-selection/questionnaire/responses/${encodeURIComponent(responseId)}/verify`,
    { evidenceStatus, evidenceRef, waiverReason },
  );
  return data;
}

// ——— Evidence expiry scheduler (V52) ———

export interface EvidenceExpiryRun {
  runId: string;
  asOfDate: string;
  policiesExpired: number;
  warrantiesExpired: number;
  guaranteesExpired: number;
  ranAt: string;
  triggerSource?: string;
}

export interface EvidenceExpiryJobStatus {
  jobName: string;
  enabled: boolean;
  lastScheduledAt: string | null;
  lastRunId: string | null;
  lastSuccess: boolean | null;
  lastError: string | null;
}

export async function runEvidenceExpiry(asOfDate?: string): Promise<EvidenceExpiryRun> {
  const q = asOfDate ? `?asOfDate=${encodeURIComponent(asOfDate)}` : '';
  const { data } = await httpClient.post<EvidenceExpiryRun>(
    `/api/v1/vendor-selection/evidence/expiry-run${q}`,
  );
  return data;
}

export async function listEvidenceExpiryRuns(): Promise<EvidenceExpiryRun[]> {
  const { data } = await httpClient.get<EvidenceExpiryRun[]>(
    '/api/v1/vendor-selection/evidence/expiry-runs',
  );
  return data;
}

export async function getEvidenceExpiryJobStatus(): Promise<EvidenceExpiryJobStatus> {
  const { data } = await httpClient.get<EvidenceExpiryJobStatus>(
    '/api/v1/vendor-selection/evidence/expiry-job',
  );
  return data;
}

export async function setEvidenceExpiryJobEnabled(enabled: boolean): Promise<EvidenceExpiryJobStatus> {
  const { data } = await httpClient.post<EvidenceExpiryJobStatus>(
    `/api/v1/vendor-selection/evidence/expiry-job/enabled?enabled=${enabled}`,
  );
  return data;
}

export async function listScorecardLineage(templateId: string): Promise<ScorecardTemplate[]> {
  const { data } = await httpClient.get<ScorecardTemplate[]>(
    `/api/v1/vendor-selection/scorecards/templates/${encodeURIComponent(templateId)}/lineage`,
  );
  return data;
}

// ——— Recommendation decision events (V54) ———

export interface RecommendationDecisionEvent {
  eventId: string;
  recommendationId: string;
  decisionId: string | null;
  explanationId: string | null;
  eventType: string;
  previousStatus: string;
  newStatus: string;
  recommendedSupplierId: string | null;
  overrideSupplierId: string | null;
  payloadJson: string | null;
  published: boolean;
  publishedAt: string | null;
  occurredAt: string;
  occurredBy: string | null;
}

export async function listRecommendationDecisionEvents(
  recommendationId: string,
): Promise<RecommendationDecisionEvent[]> {
  const { data } = await httpClient.get<RecommendationDecisionEvent[]>(
    `/api/v1/vendor-selection/recommendations/${encodeURIComponent(recommendationId)}/decision-events`,
  );
  return data;
}

export interface DecisionEventDispatchSummary {
  attempted: number;
  succeeded: number;
  failed: number;
  enabled: boolean;
  targetUrl: string | null;
}

export async function dispatchRecommendationDecisionEvents(): Promise<DecisionEventDispatchSummary> {
  const { data } = await httpClient.post<DecisionEventDispatchSummary>(
    '/api/v1/vendor-selection/recommendations/decision-events/dispatch',
  );
  return data;
}

// ——— Qualification approval gate audit (V56) ———

export interface QualificationApprovalCheck {
  checkId: string;
  supplierId: string;
  recommendationId: string;
  recommendationStatus: string;
  recommendedSupplierId: string | null;
  checkPassed: boolean;
  failureReason: string | null;
  checkedAt: string;
}

export async function listQualificationApprovalChecks(
  supplierId: string,
): Promise<QualificationApprovalCheck[]> {
  const { data } = await httpClient.get<QualificationApprovalCheck[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/qualification/approval-checks`,
  );
  return data;
}

// ——— Qualification lifecycle events (V57) ———

export interface QualificationLifecycleEvent {
  eventId: string;
  qualificationId: string;
  supplierId: string;
  eventType: string;
  previousStatus: string | null;
  newStatus: string;
  recommendationId: string | null;
  changeReason: string | null;
  payloadJson: string | null;
  published: boolean;
  publishedAt: string | null;
  occurredAt: string;
  occurredBy: string | null;
}

export async function listQualificationLifecycleEvents(
  supplierId: string,
): Promise<QualificationLifecycleEvent[]> {
  const { data } = await httpClient.get<QualificationLifecycleEvent[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/qualification/lifecycle-events`,
  );
  return data;
}

// ——— Lifecycle operation blocks (V61) ———

export interface LifecycleOperationBlock {
  blockId: string;
  supplierId: string;
  operationCode: string;
  lifecycleStatus: string;
  blockReason: string;
  attemptedAt: string;
}

export async function listLifecycleOperationBlocks(
  supplierId: string,
): Promise<LifecycleOperationBlock[]> {
  const { data } = await httpClient.get<LifecycleOperationBlock[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/lifecycle-operation-blocks`,
  );
  return data;
}

// ——— Capability expiry (V63) ———

export interface CapabilityExpiryRun {
  runId: string;
  asOfDate: string;
  capabilitiesExpired: number;
  ranAt: string;
  triggerSource?: string;
}

export interface CapabilityExpiryJobStatus {
  jobName: string;
  enabled: boolean;
  lastScheduledAt: string | null;
  lastRunId: string | null;
  lastSuccess: boolean | null;
  lastError: string | null;
}

export async function runCapabilityExpiry(asOfDate?: string): Promise<CapabilityExpiryRun> {
  const q = asOfDate ? `?asOfDate=${encodeURIComponent(asOfDate)}` : '';
  const { data } = await httpClient.post<CapabilityExpiryRun>(
    `/api/v1/vendor-selection/capabilities/expiry-run${q}`,
  );
  return data;
}

export async function listCapabilityExpiryRuns(): Promise<CapabilityExpiryRun[]> {
  const { data } = await httpClient.get<CapabilityExpiryRun[]>(
    '/api/v1/vendor-selection/capabilities/expiry-runs',
  );
  return data;
}

export async function getCapabilityExpiryJobStatus(): Promise<CapabilityExpiryJobStatus> {
  const { data } = await httpClient.get<CapabilityExpiryJobStatus>(
    '/api/v1/vendor-selection/capabilities/expiry-job',
  );
  return data;
}

export async function setCapabilityExpiryJobEnabled(enabled: boolean): Promise<CapabilityExpiryJobStatus> {
  const { data } = await httpClient.post<CapabilityExpiryJobStatus>(
    `/api/v1/vendor-selection/capabilities/expiry-job/enabled?enabled=${enabled}`,
  );
  return data;
}


/** ——— V64 Warranty claims ——— */

export async function addWarrantyClaim(req: Record<string, unknown>): Promise<WarrantyClaim> {
  const { data } = await httpClient.post<WarrantyClaim>('/api/v1/vendor-selection/warranty-claims', req);
  return data;
}

export async function verifyWarrantyClaim(
  claimId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<WarrantyClaim> {
  const { data } = await httpClient.post<WarrantyClaim>(
    `/api/v1/vendor-selection/warranty-claims/${encodeURIComponent(claimId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

export async function fetchWarrantyClaimSummary(supplierId: string): Promise<WarrantyClaimSummary> {
  const { data } = await httpClient.get<WarrantyClaimSummary>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/warranty-claims`,
  );
  return data;
}

/** ——— V65 Guarantee events ——— */

export async function addGuaranteeEvent(req: Record<string, unknown>): Promise<GuaranteeEvent> {
  const { data } = await httpClient.post<GuaranteeEvent>('/api/v1/vendor-selection/guarantee-events', req);
  return data;
}

export async function verifyGuaranteeEvent(
  eventId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<GuaranteeEvent> {
  const { data } = await httpClient.post<GuaranteeEvent>(
    `/api/v1/vendor-selection/guarantee-events/${encodeURIComponent(eventId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

/** ——— V66 After-sales metrics ——— */

export async function addAfterSalesMetric(req: Record<string, unknown>): Promise<AfterSalesMetric> {
  const { data } = await httpClient.post<AfterSalesMetric>('/api/v1/vendor-selection/after-sales-metrics', req);
  return data;
}

export async function verifyAfterSalesMetric(
  metricId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<AfterSalesMetric> {
  const { data } = await httpClient.post<AfterSalesMetric>(
    `/api/v1/vendor-selection/after-sales-metrics/${encodeURIComponent(metricId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

/** ——— V69 Performance feedback (C3) ——— */

export async function createPerformanceFeedback(req: Record<string, unknown>): Promise<PerformanceFeedback> {
  const { data } = await httpClient.post<PerformanceFeedback>(
    '/api/v1/vendor-selection/performance-feedback',
    req,
  );
  return data;
}

export async function verifyPerformanceFeedback(
  feedbackId: string,
  evidenceStatus: EvidenceStatus,
  evidenceRef?: string,
): Promise<PerformanceFeedback> {
  const { data } = await httpClient.post<PerformanceFeedback>(
    `/api/v1/vendor-selection/performance-feedback/${encodeURIComponent(feedbackId)}/verify`,
    { evidenceStatus, evidenceRef },
  );
  return data;
}

export async function applyPerformanceFeedbackToHistory(feedbackId: string): Promise<PerformanceFeedback> {
  const { data } = await httpClient.post<PerformanceFeedback>(
    `/api/v1/vendor-selection/performance-feedback/${encodeURIComponent(feedbackId)}/apply-to-history`,
  );
  return data;
}



export interface AfterSalesSummary {
  supplierId: string;
  totalMetrics: number;
  verifiedMetrics: number;
  pendingMetrics: number;
  latestSlaMetPercent: string | null;
  latestUnresolvedComplaints: number | null;
  metrics: AfterSalesMetric[];
}

export interface GuaranteeEventSummary {
  supplierId: string;
  totalEvents: number;
  unresolvedEvents: number;
  verifiedEvents: number;
  totalPenaltyAmount: string | null;
  currencyCode: string | null;
  events: GuaranteeEvent[];
}

export async function fetchAfterSalesSummary(supplierId: string): Promise<AfterSalesSummary> {
  const { data } = await httpClient.get<AfterSalesSummary>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/after-sales-summary`,
  );
  return data;
}

export async function fetchGuaranteeEventSummary(supplierId: string): Promise<GuaranteeEventSummary> {
  const { data } = await httpClient.get<GuaranteeEventSummary>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/guarantee-events-summary`,
  );
  return data;
}

export interface EvidencePendingSummary {
  supplierId: string;
  pendingPolicy: number;
  pendingQuality: number;
  pendingDelivery: number;
  pendingWarrantyClaims: number;
  pendingGuaranteeEvents: number;
  pendingAfterSales: number;
  pendingPerformanceFeedback: number;
  totalPending: number;
}

export async function fetchEvidencePendingSummary(supplierId: string): Promise<EvidencePendingSummary> {
  const { data } = await httpClient.get<EvidencePendingSummary>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/evidence-pending-summary`,
  );
  return data;
}

export async function listSupplierRisks(
  supplierId: string,
  opts?: { status?: string; severity?: string },
): Promise<RiskItem[]> {
  const { data } = await httpClient.get<RiskItem[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/risks`,
    { params: { status: opts?.status, severity: opts?.severity } },
  );
  return data;
}

export async function listSupplierCapabilities(
  supplierId: string,
  opts?: { type?: string; activeOnly?: boolean },
): Promise<Capability[]> {
  const { data } = await httpClient.get<Capability[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/capabilities`,
    { params: { type: opts?.type, activeOnly: opts?.activeOnly } },
  );
  return data;
}

export async function listPerformanceFeedback(
  supplierId: string,
  opts?: { appliedToHistory?: boolean; evidenceStatus?: string },
): Promise<PerformanceFeedback[]> {
  const { data } = await httpClient.get<PerformanceFeedback[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/performance-feedback`,
    {
      params: {
        appliedToHistory: opts?.appliedToHistory,
        evidenceStatus: opts?.evidenceStatus,
      },
    },
  );
  return data;
}

export async function listWarrantyClaimItems(
  supplierId: string,
  opts?: { claimStatus?: string; evidenceStatus?: string },
): Promise<WarrantyClaim[]> {
  const { data } = await httpClient.get<WarrantyClaim[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/warranty-claims/items`,
    { params: { claimStatus: opts?.claimStatus, evidenceStatus: opts?.evidenceStatus } },
  );
  return data;
}

export async function listGuaranteeEventItems(
  supplierId: string,
  opts?: { unresolvedOnly?: boolean; evidenceStatus?: string },
): Promise<GuaranteeEvent[]> {
  const { data } = await httpClient.get<GuaranteeEvent[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/guarantee-events/items`,
    {
      params: {
        unresolvedOnly: opts?.unresolvedOnly,
        evidenceStatus: opts?.evidenceStatus,
      },
    },
  );
  return data;
}

export async function listScorecardResults(supplierId: string): Promise<ScorecardResult[]> {
  const { data } = await httpClient.get<ScorecardResult[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/scorecard-results`,
  );
  return data;
}

export async function listExpiringCapabilities(asOfDate?: string): Promise<Capability[]> {
  const { data } = await httpClient.get<Capability[]>(
    '/api/v1/vendor-selection/capabilities/expiring',
    { params: asOfDate ? { asOfDate } : undefined },
  );
  return data;
}

export interface ComparisonSummary {
  comparisonId: string;
  bomNodeId?: string | null;
  programId?: string | null;
  currencyCode?: string | null;
  supplierIdsCsv: string;
  notes?: string | null;
  lifecycleSummary?: string | null;
  createdAt?: string | null;
}

export async function listComparisons(opts?: {
  bomNodeId?: string;
  programId?: string;
}): Promise<ComparisonSummary[]> {
  const { data } = await httpClient.get<ComparisonSummary[]>(
    '/api/v1/vendor-selection/comparisons',
    { params: { bomNodeId: opts?.bomNodeId, programId: opts?.programId } },
  );
  return data;
}

export async function listAfterSalesMetrics(
  supplierId: string,
  evidenceStatus?: string,
): Promise<AfterSalesMetric[]> {
  const { data } = await httpClient.get<AfterSalesMetric[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/after-sales-metrics`,
    { params: evidenceStatus ? { evidenceStatus } : undefined },
  );
  return data;
}

export async function listQualityMetrics(
  supplierId: string,
  evidenceStatus?: string,
): Promise<QualityMetric[]> {
  const { data } = await httpClient.get<QualityMetric[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/quality-metrics`,
    { params: evidenceStatus ? { evidenceStatus } : undefined },
  );
  return data;
}

export async function listDeliveryMetrics(
  supplierId: string,
  evidenceStatus?: string,
): Promise<DeliveryMetric[]> {
  const { data } = await httpClient.get<DeliveryMetric[]>(
    `/api/v1/vendor-selection/suppliers/${encodeURIComponent(supplierId)}/delivery-metrics`,
    { params: evidenceStatus ? { evidenceStatus } : undefined },
  );
  return data;
}

export async function updateWarrantyClaimStatus(
  claimId: string,
  body: {
    claimStatus: WarrantyClaimStatus;
    acceptedFlag?: boolean;
    resolutionDays?: number;
    resolutionSummary?: string;
    notes?: string;
  },
): Promise<WarrantyClaim> {
  const { data } = await httpClient.post<WarrantyClaim>(
    `/api/v1/vendor-selection/warranty-claims/${encodeURIComponent(claimId)}/status`,
    body,
  );
  return data;
}
