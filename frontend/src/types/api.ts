/** Mirrors backend ApiErrorResponse (common.dto.ApiErrorResponse). */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Array<{ field: string; message: string }> | null;
}

/** Mirrors AuthDtos.TokenResponse. */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
}

export interface LoginRequest {
  username: string;
  password: string;
  mfaCode?: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

/** MasterDataStatus enum on the backend. */
export type MasterDataStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING_APPROVAL';

/** Mirrors MaterialMasterDtos.Response — money fields are decimal strings from JSON. */
export interface MaterialMaster {
  recordId: string;
  code: string;
  description: string | null;
  rate: string;
  uom: string;
  plantId: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  versionNumber: number;
  status: MasterDataStatus;
  grade: string | null;
  specificationStandard: string | null;
  density: string;
  scrapRecoveryRate: string | null;
  materialForm: string | null;
  indexLinkedFlag: boolean;
  indexCode: string | null;
  basePrice: string | null;
  alloySurchargePerUnit: string | null;
  createdBy: string | null;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string | null;
}

/** Spring Data Page JSON shape. */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface MaterialSearchParams {
  q?: string;
  status?: MasterDataStatus;
  plantId?: string;
  page?: number;
  size?: number;
  sort?: string;
}


/** Mirrors BomNodeDtos.TreeNode */
export type MakeBuyFlag = 'MAKE' | 'BUY';
export type BomNodeStatus = 'DRAFT' | 'RELEASED';

export interface BomTreeNode {
  bomNodeId: string;
  partNumber: string;
  revision: string | null;
  quantityPer: string;
  uom: string;
  makeBuyFlag: MakeBuyFlag | null;
  materialCode: string | null;
  status: BomNodeStatus;
  rolledUpCost: string | null;
  overrideFlag: boolean;
  children: BomTreeNode[];
}

export interface BomNodeResponse {
  bomNodeId: string;
  parentNodeId: string | null;
  programId: string;
  partNumber: string;
  revision: string | null;
  quantityPer: string;
  uom: string;
  makeBuyFlag: MakeBuyFlag | null;
  materialCode: string | null;
  status: BomNodeStatus;
  rolledUpCost: string | null;
  overrideFlag: boolean;
  overrideJustification: string | null;
}


export interface ShouldCostBuildUp {
  buildUpId: string;
  bomNodeId: string;
  scenarioLabel: string;
  materialCost: string;
  processCost: string;
  toolingCost: string;
  packagingFreightCost: string;
  overheadCost: string;
  sgaCost: string;
  profitMargin: string;
  totalCost: string;
  staleInputsFlag: boolean;
  locked: boolean;
  approvalStatus: string;
  calculationBasis: string | null;
  calculatedAt: string;
}

export interface GenerateShouldCostRequest {
  bomNodeId: string;
  scenarioLabel?: string;
  commodityFamily: string;
  region?: string;
  allocationBaseQuantity?: string | null;
  overheadPoolCode?: string | null;
}

export interface WaterfallPoint {
  element: string;
  amount: string;
}

export interface WaterfallResponse {
  buildUpId: string;
  elements: WaterfallPoint[];
}

export interface ParametricRequest {
  materialFamily: string;
  processFamily: string;
  region: string;
  netWeightKg: string;
}

export interface ParametricResponse {
  netWeightKg: string;
  costPerKg: string;
  estimatedCost: string;
}

export interface VarianceResponse {
  shouldCost: string;
  quotedPrice: string;
  absoluteVariance: string;
  percentageVariance: string;
}

export interface ApprovalStep {
  stepInstanceId: string;
  sequenceNumber: number;
  approverRole: string;
  approverUserId: string | null;
  decision: string | null;
  decidedAt: string | null;
  comment: string | null;
  dueAt: string | null;
  escalated: boolean;
  overrideFlag: boolean;
  overrideJustification: string | null;
  workflowInstanceId?: string | null;
  entityType?: string | null;
  entityId?: string | null;
}

export interface ApprovalInstance {
  workflowInstanceId: string;
  entityType: string;
  entityId: string;
  status: string;
  submittedBy: string;
  submittedAt: string;
  completedAt: string | null;
  steps: ApprovalStep[];
}

export interface SubmitWorkflowRequest {
  entityType: string;
  entityId: string;
  value: string;
  commodityFamily?: string;
  plantCode?: string;
}

export interface DecisionRequest {
  decision: 'APPROVED' | 'REJECTED' | 'RETURNED';
  comment?: string;
  overrideFlag?: boolean;
  overrideJustification?: string;
}


export interface MachineMaster {
  recordId: string;
  code: string;
  description: string | null;
  rate: string;
  uom: string;
  plantId: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  versionNumber: number;
  status: MasterDataStatus;
  machineType: string | null;
  capacity: string | null;
  depreciationLifeYears: string | null;
  capitalCost: string | null;
  annualOperatingHours: string | null;
  powerCostPerHour: string | null;
  maintenanceCostPerHour: string | null;
  floorSpaceCostPerHour: string | null;
  shiftPattern: string | null;
}

export interface RateBuildUpResponse {
  code: string;
  depreciationComponentPerHour: string;
  powerCostPerHour: string;
  maintenanceCostPerHour: string;
  floorSpaceCostPerHour: string;
  totalHourlyRate: string;
}


export interface VehicleProgram {
  programId: string;
  programCode: string;
  modelName: string | null;
  platformCode: string | null;
  sourcingRegion: string | null;
  lifetimeVolume: string | null;
  targetCost: string | null;
  targetCurrency: string | null;
  status: string;
}


export type NotificationType =
  | 'WORKFLOW_SUBMITTED'
  | 'WORKFLOW_APPROVED'
  | 'WORKFLOW_REJECTED'
  | 'APPROVAL_ESCALATED'
  | 'APPROVAL_ASSIGNED'
  | 'SYSTEM';

export type NotificationSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface AppNotification {
  notificationId: string;
  notificationType: NotificationType;
  title: string;
  body: string;
  entityType: string | null;
  entityId: string | null;
  severity: NotificationSeverity;
  readFlag: boolean;
  readAt: string | null;
  createdAt: string;
}
