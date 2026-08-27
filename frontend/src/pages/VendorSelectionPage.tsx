import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import HubOutlinedIcon from '@mui/icons-material/HubOutlined';
import {
  calculateTco,
  createCapability,
  listQuestionnaireQuestions,
  createQuestionnaireQuestion,
  addPolicy,
  listPolicies,
  type QuestionnaireQuestion,
  type PolicyType,
  type PolicyEvidence,
  type QuestionnaireAnswer,
  type CapabilityType,
  createComparison,
  createRecommendation,
  createRisk,
  evaluateEligibility,
  evaluateScorecard,
  cloneScorecardTemplate,
  listScorecardLineage,
  fetchSupplier360,
  getQualification,
  getRecommendation,
  listRecommendationsByStatus,
  getRiskSummary,
  initializeQualification,
  listCapabilities,
  listScorecardTemplates,
  listTco,
  approveRecommendation,
  rejectRecommendation,
  overrideRecommendation,
  submitRecommendation,
  transitionQualification,
  listQualificationApprovalChecks,
  type QualificationApprovalCheck,
  listQualificationLifecycleEvents,
  type QualificationLifecycleEvent,
  listLifecycleOperationBlocks,
  type LifecycleOperationBlock,
  updateRiskStatus,
  evaluateQuestionnaire,
  submitQuestionnaireResponse,
  listQuestionnaireResponses,
  verifyQuestionnaireEvidence,
  verifyPolicy,
  deactivateScorecardTemplate,
  getEvidenceExpiryJobStatus,
  runCapabilityExpiry,
  listCapabilityExpiryRuns,
  getCapabilityExpiryJobStatus,
  setCapabilityExpiryJobEnabled,
  type CapabilityExpiryRun,
  type CapabilityExpiryJobStatus,
  setEvidenceExpiryJobEnabled,
  runEvidenceExpiry,
  listEvidenceExpiryRuns,
  type EvidenceExpiryJobStatus,
  type EvidenceExpiryRun,
  type QuestionnaireEvaluation,
  getRecommendationExplanation,
  type RecommendationExplanation,
  addWarrantyClaim,
  verifyWarrantyClaim,
  addGuaranteeEvent,
  verifyGuaranteeEvent,
  addAfterSalesMetric,
  verifyAfterSalesMetric,
  listAfterSalesMetrics,
  listPerformanceFeedback,
  type PerformanceFeedback,
  listWarrantyClaimItems,
  updateWarrantyClaimStatus,
  listGuaranteeEventItems,
  listQualityMetrics,
  listDeliveryMetrics,
  verifyQualityMetric,
  verifyDeliveryMetric,
  type QualityMetric,
  type DeliveryMetric,
  type AfterSalesMetric,
  createPerformanceFeedback,
  verifyPerformanceFeedback,
  applyPerformanceFeedbackToHistory,
  fetchAfterSalesSummary,
  fetchGuaranteeEventSummary,
  fetchEvidencePendingSummary,
  listComparisons,
  listScorecardResults,
  listExpiringCapabilities,
  type ComparisonSummary,
  type EvidencePendingSummary,
  fetchWarrantyClaimSummary,
  type AfterSalesSummary,
  type GuaranteeEventSummary,
  type WarrantyClaimSummary,
  type WarrantyClaim,
  type WarrantyClaimStatus,
  type GuaranteeEvent,
  type GuaranteeEventType,
  type PerformanceFeedbackType,
  listRecommendationDecisions,
  type RecommendationDecision,
  listRecommendationDecisionEvents,
  type RecommendationDecisionEvent,
  dispatchRecommendationDecisionEvents,
  type Capability,
  type ComparisonResult,
  type EligibilityResult,
  type LifecycleStatus,
  type Qualification,
  type Recommendation,
  type RiskItem,
  type RiskSeverity,
  type RiskStatus,
  type RiskCategory,
  type RiskLikelihood,
  type ScorecardResult,
  type ScorecardTemplate,
  type Supplier360,
  type TcoResult,
} from '../api/vendorSelectionApi';
import { listSuppliers, type Supplier } from '../api/rfqApi';
import { evidenceChip, yesNoChip } from '../components/vendorselection/evidenceChips';
import { WarrantyClaimsPanel } from '../components/vendorselection/WarrantyClaimsPanel';
import { GuaranteeEventsPanel } from '../components/vendorselection/GuaranteeEventsPanel';
import { AfterSalesMetricsPanel } from '../components/vendorselection/AfterSalesMetricsPanel';
import { SupplierListPanel } from '../components/vendorselection/SupplierListPanel';
import { ExplanationClassificationPanel } from '../components/vendorselection/ExplanationClassificationPanel';
import { PerformanceFeedbackPanel } from '../components/vendorselection/PerformanceFeedbackPanel';
import { QualityMetricsPanel } from '../components/vendorselection/QualityMetricsPanel';
import { DeliveryMetricsPanel } from '../components/vendorselection/DeliveryMetricsPanel';
import { RequiredFieldLegend } from '../components/ux';

type TabKey =
  | '360'
  | 'eligibility'
  | 'tco'
  | 'scorecard'
  | 'comparison'
  | 'recommendation'
  | 'qualification'
  | 'risk'
  | 'config';


export function VendorSelectionPage() {
  const [tab, setTab] = useState<TabKey>('360');
  const [supplierId, setSupplierId] = useState('');
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  // 360
  const [view360, setView360] = useState<Supplier360 | null>(null);
  const [asSummary, setAsSummary] = useState<AfterSalesSummary | null>(null);
  const [asMetrics, setAsMetrics] = useState<AfterSalesMetric[]>([]);
  const [asEvidenceFilter, setAsEvidenceFilter] = useState<string>('');
  const [pfItems, setPfItems] = useState<PerformanceFeedback[]>([]);
  const [pfEvidenceFilter, setPfEvidenceFilter] = useState<string>('');
  const [pfAppliedFilter, setPfAppliedFilter] = useState<string>('');
  const [wcItems, setWcItems] = useState<WarrantyClaim[]>([]);
  const [wcClaimFilter, setWcClaimFilter] = useState<string>('');
  const [wcEvidenceFilter, setWcEvidenceFilter] = useState<string>('');
  const [geItems, setGeItems] = useState<GuaranteeEvent[]>([]);
  const [geUnresolvedOnly, setGeUnresolvedOnly] = useState<string>('');
  const [geEvidenceFilter, setGeEvidenceFilter] = useState<string>('');
  const [qmMetrics, setQmMetrics] = useState<QualityMetric[]>([]);
  const [qmEvidenceFilter, setQmEvidenceFilter] = useState<string>('');
  const [dmMetrics, setDmMetrics] = useState<DeliveryMetric[]>([]);
  const [dmEvidenceFilter, setDmEvidenceFilter] = useState<string>('');
  const [geSummary, setGeSummary] = useState<GuaranteeEventSummary | null>(null);
  const [wcSummary, setWcSummary] = useState<WarrantyClaimSummary | null>(null);
  const [pendingSummary, setPendingSummary] = useState<EvidencePendingSummary | null>(null);
  const [claimDate, setClaimDate] = useState('');
  const [claimNumber, setClaimNumber] = useState('');
  const [claimCost, setClaimCost] = useState('');
  const [claimStatus, setClaimStatus] = useState<WarrantyClaimStatus>('OPEN');
  const [geDate, setGeDate] = useState('');
  const [geType, setGeType] = useState<GuaranteeEventType>('FAILURE');
  const [geDesc, setGeDesc] = useState('');
  const [gePenalty, setGePenalty] = useState('');
  const [asStart, setAsStart] = useState('');
  const [asEnd, setAsEnd] = useState('');
  const [asResponseHrs, setAsResponseHrs] = useState('');
  const [asSlaPct, setAsSlaPct] = useState('');
  const [asUnresolved, setAsUnresolved] = useState('');
  const [pfDate, setPfDate] = useState('');
  const [pfType, setPfType] = useState<PerformanceFeedbackType>('QUALITY');
  const [pfMetricCode, setPfMetricCode] = useState('PPM');
  const [pfMetricValue, setPfMetricValue] = useState('');
  const [pfNarrative, setPfNarrative] = useState('');

  // Eligibility
  const [reqCaps, setReqCaps] = useState('');
  const [reqCapacity, setReqCapacity] = useState('');
  const [requireCert, setRequireCert] = useState(false);
  const [reqPolicies, setReqPolicies] = useState('');
  const [requireQuestionnaire, setRequireQuestionnaire] = useState(false);
  const [requireNoCriticalOpenRisks, setRequireNoCriticalOpenRisks] = useState(false);
  const [requireNoHighOpenRisks, setRequireNoHighOpenRisks] = useState(false);
  const [qEval, setQEval] = useState<QuestionnaireEvaluation | null>(null);
  const [expiryJob, setExpiryJob] = useState<EvidenceExpiryJobStatus | null>(null);
  const [expiryRuns, setExpiryRuns] = useState<EvidenceExpiryRun[]>([]);
  const [capExpiryJob, setCapExpiryJob] = useState<CapabilityExpiryJobStatus | null>(null);
  const [capExpiryRuns, setCapExpiryRuns] = useState<CapabilityExpiryRun[]>([]);
  const [lineageRows, setLineageRows] = useState<ScorecardTemplate[]>([]);
  const [eligibility, setEligibility] = useState<EligibilityResult | null>(null);
  const [capabilities, setCapabilities] = useState<Capability[]>([]);

  // TCO
  const [tcoCurrency, setTcoCurrency] = useState('INR');
  const [tcoVolume, setTcoVolume] = useState('');
  const [tcoPurchase, setTcoPurchase] = useState('');
  const [tcoFreight, setTcoFreight] = useState('');
  const [tcoLogistics, setTcoLogistics] = useState('');
  const [tcoPackaging, setTcoPackaging] = useState('');
  const [tcoQuality, setTcoQuality] = useState('');
  const [tcoWarranty, setTcoWarranty] = useState('');
  const [tcoTooling, setTcoTooling] = useState('');
  const [tcoResult, setTcoResult] = useState<TcoResult | null>(null);
  const [tcoHistory, setTcoHistory] = useState<TcoResult[]>([]);

  // Scorecard
  const [templates, setTemplates] = useState<ScorecardTemplate[]>([]);
  const [templateId, setTemplateId] = useState('');
  const [scoreInputs, setScoreInputs] = useState<Record<string, string>>({});
  const [scoreResult, setScoreResult] = useState<ScorecardResult | null>(null);
  const [cloneCode, setCloneCode] = useState('');
  const [cloneName, setCloneName] = useState('');
  const [cloneWeights, setCloneWeights] = useState<Record<string, string>>({});
  // Configuration tab
  const [configQuestions, setConfigQuestions] = useState<QuestionnaireQuestion[]>([]);
  const [qFilterCategory, setQFilterCategory] = useState<string>('');
  const [qCode, setQCode] = useState('');
  const [qText, setQText] = useState('');
  const [qCategory, setQCategory] = useState('QUALITY');
  const [qAnswerType, setQAnswerType] = useState('YES_NO');
  const [qRequired, setQRequired] = useState(true);
  const [qEvidence, setQEvidence] = useState(false);
  const [qGate, setQGate] = useState(false);
  const [qWeight, setQWeight] = useState('5');
  const [capCode, setCapCode] = useState('');
  const [capName, setCapName] = useState('');
  const [capType, setCapType] = useState<CapabilityType>('PROCESS');
  const [capCapacity, setCapCapacity] = useState('');
  const [capCertified, setCapCertified] = useState(false);
  const [policyType, setPolicyType] = useState<PolicyType>('QUALITY_POLICY');
  const [policyTitle, setPolicyTitle] = useState('');
  const [policyItems, setPolicyItems] = useState<PolicyEvidence[]>([]);
  const [policyEvidenceFilter, setPolicyEvidenceFilter] = useState<string>('');
  const [policyTypeFilter, setPolicyTypeFilter] = useState<string>('');
  const [configCaps, setConfigCaps] = useState<Capability[]>([]);
  const [answerDrafts, setAnswerDrafts] = useState<Record<string, string>>({});
  const [answerEvidence, setAnswerEvidence] = useState<Record<string, string>>({});
  const [configAnswers, setConfigAnswers] = useState<QuestionnaireAnswer[]>([]);
  
  // Comparison
  const [compareIds, setCompareIds] = useState('');
  const [comparison, setComparison] = useState<ComparisonResult | null>(null);
  const [comparisonHistory, setComparisonHistory] = useState<ComparisonSummary[]>([]);
  const [scoreHistory, setScoreHistory] = useState<ScorecardResult[]>([]);
  const [expiringCaps, setExpiringCaps] = useState<Capability[]>([]);

  // Recommendation
  const [recEventCode, setRecEventCode] = useState('');
  const [recCandidatesJson, setRecCandidatesJson] = useState(
    '[{"supplierId":"","eligible":true,"overallScore":"80","tcoTotal":"100","tcoCurrency":"INR","tcoDataComplete":true}]',
  );
  const [recommendation, setRecommendation] = useState<Recommendation | null>(null);
  const [recQueue, setRecQueue] = useState<Recommendation[]>([]);
  const [recQueueStatus, setRecQueueStatus] = useState<string>('SUBMITTED');
  const [recIdLoad, setRecIdLoad] = useState('');
  const [decisionComment, setDecisionComment] = useState('');
  const [overrideSupplierId, setOverrideSupplierId] = useState('');
  const [overrideReason, setOverrideReason] = useState('');
  const [explanation, setExplanation] = useState<RecommendationExplanation | null>(null);
  const [decisions, setDecisions] = useState<RecommendationDecision[]>([]);
  const [decisionEvents, setDecisionEvents] = useState<RecommendationDecisionEvent[]>([]);

  // Qualification
  const [qualification, setQualification] = useState<Qualification | null>(null);
  const [approvalChecks, setApprovalChecks] = useState<QualificationApprovalCheck[]>([]);
  const [lifecycleEvents, setLifecycleEvents] = useState<QualificationLifecycleEvent[]>([]);
  const [operationBlocks, setOperationBlocks] = useState<LifecycleOperationBlock[]>([]);
  const [targetStatus, setTargetStatus] = useState<LifecycleStatus>('UNDER_VERIFICATION');
  const [qualReason, setQualReason] = useState('');
  const [qualRecommendationId, setQualRecommendationId] = useState('');

  // Risk
  const [risks, setRisks] = useState<RiskItem[]>([]);
  const [riskSummary, setRiskSummary] = useState<{
    openCount: number;
    criticalOpenCount: number;
    highOpenCount: number;
  } | null>(null);
  const [riskTitle, setRiskTitle] = useState('');
  const [riskCategory, setRiskCategory] = useState<RiskCategory>('QUALITY');
  const [riskSeverity, setRiskSeverity] = useState<RiskSeverity>('MEDIUM');
  const [riskLikelihood, setRiskLikelihood] = useState<RiskLikelihood>('MEDIUM');
  const [riskDesc, setRiskDesc] = useState('');
  const [riskMitigation, setRiskMitigation] = useState('');


  const [searchParams] = useSearchParams();
  useEffect(() => {
    const tabParam = searchParams.get('tab');
    if (tabParam && ['360','eligibility','tco','scorecard','comparison','recommendation','qualification','risk','config'].includes(tabParam)) {
      setTab(tabParam as TabKey);
    }
    const statusParam = searchParams.get('status');
    if (statusParam) {
      const st = statusParam.toUpperCase();
      setRecQueueStatus(st);
      if (tabParam === 'recommendation' || (!tabParam && st)) {
        void listRecommendationsByStatus(st)
          .then((list) => {
            setRecQueue(list);
            setInfo(`${list.length} recommendation(s) with status ${st}`);
          })
          .catch((e: unknown) => {
            setError(e instanceof Error ? e.message : 'Failed to load recommendation queue');
          });
      }
    }
  }, [searchParams]);

  const clearMsgs = () => {
    setError(null);
    setInfo(null);
  };

  const loadSuppliers = useCallback(async () => {
    clearMsgs();
    try {
      const list = await listSuppliers();
      setSuppliers(list);
      setInfo(`Loaded ${list.length} suppliers`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load suppliers');
    }
  }, []);

  // Auto-load suppliers for Vendor Selection landing (same source as RFQ suppliers tab)
  useEffect(() => {
    void loadSuppliers();
  }, [loadSuppliers]);

  
  
  async function transitionClaimStatus(claimId: string, claimStatus: WarrantyClaimStatus) {
    try {
      setError(null);
      await updateWarrantyClaimStatus(claimId, { claimStatus });
      setInfo(`Claim status → ${claimStatus}`);
      await load360();
      if (wcItems.length > 0 || wcClaimFilter || wcEvidenceFilter) {
        await loadWarrantyClaimItems();
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Claim status transition failed');
    }
  }

async function verifyClaim(claimId: string) {
    if (!supplierId) return;
    try {
      setError(null);
      await verifyWarrantyClaim(claimId, 'VERIFIED');
      setInfo('Warranty claim verified');
      await load360();
      if (wcItems.length > 0 || wcClaimFilter || wcEvidenceFilter) {
        await loadWarrantyClaimItems();
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Verify claim failed');
    }
  }

  async function submitClaim() {
    if (!supplierId || !claimDate) {
      setError('Supplier and claim date required');
      return;
    }
    try {
      setError(null);
      await addWarrantyClaim({
        supplierId,
        claimDate,
        claimNumber: claimNumber || null,
        claimCost: claimCost || null,
        currencyCode: 'INR',
        claimStatus,
      });
      setInfo('Warranty claim recorded (PENDING evidence)');
      setClaimNumber('');
      setClaimCost('');
      await load360();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Add claim failed');
    }
  }

  async function verifyGe(eventId: string) {
    try {
      setError(null);
      await verifyGuaranteeEvent(eventId, 'VERIFIED');
      setInfo('Guarantee event verified');
      await load360();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Verify event failed');
    }
  }

  async function submitGe() {
    if (!supplierId || !geDate) {
      setError('Supplier and event date required');
      return;
    }
    try {
      setError(null);
      await addGuaranteeEvent({
        supplierId,
        eventDate: geDate,
        eventType: geType,
        description: geDesc || null,
        penaltyAmount: gePenalty || null,
        currencyCode: 'INR',
        resolvedFlag: false,
      });
      setInfo('Guarantee event recorded (PENDING evidence)');
      setGeDesc('');
      setGePenalty('');
      await load360();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Add event failed');
    }
  }

  async function verifyAs(metricId: string) {
    try {
      setError(null);
      await verifyAfterSalesMetric(metricId, 'VERIFIED');
      setInfo('After-sales metric verified');
      await load360();
      if (asMetrics.length > 0 || asEvidenceFilter) {
        await loadAfterSalesMetrics();
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Verify after-sales failed');
    }
  }

  async function submitAs() {
    if (!supplierId || !asStart || !asEnd) {
      setError('Supplier, period start and end required');
      return;
    }
    try {
      setError(null);
      await addAfterSalesMetric({
        supplierId,
        periodStart: asStart,
        periodEnd: asEnd,
        responseTimeHours: asResponseHrs || null,
        serviceSlaMetPercent: asSlaPct || null,
        unresolvedComplaints: asUnresolved ? Number(asUnresolved) : null,
        emergencySupportFlag: true,
      });
      setInfo('After-sales metric recorded (PENDING evidence)');
      await load360();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Add after-sales failed');
    }
  }

  async function verifyPf(feedbackId: string) {
    try {
      setError(null);
      await verifyPerformanceFeedback(feedbackId, 'VERIFIED');
      setInfo('Performance feedback verified');
      await load360();
      if (pfItems.length > 0 || pfEvidenceFilter || pfAppliedFilter) {
        await loadPerformanceFeedback();
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Verify feedback failed');
    }
  }

  async function applyPf(feedbackId: string) {
    try {
      setError(null);
      await applyPerformanceFeedbackToHistory(feedbackId);
      setInfo('Feedback marked applied to history (scores unchanged)');
      await load360();
      if (pfItems.length > 0 || pfEvidenceFilter || pfAppliedFilter) {
        await loadPerformanceFeedback();
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Apply feedback failed');
    }
  }

  async function submitPf() {
    if (!supplierId || !pfDate) {
      setError('Supplier and feedback date required');
      return;
    }
    try {
      setError(null);
      await createPerformanceFeedback({
        supplierId,
        feedbackDate: pfDate,
        feedbackType: pfType,
        metricCode: pfMetricCode || null,
        metricValue: pfMetricValue || null,
        narrative: pfNarrative || null,
      });
      setInfo('Performance feedback recorded (PENDING evidence)');
      setPfMetricValue('');
      setPfNarrative('');
      await load360();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Add feedback failed');
    }
  }

  async function load360() {
    if (!supplierId) {
      setError('Select a supplier');
      return;
    }
    clearMsgs();
    try {
      const [v, as, ge, wc, pending] = await Promise.all([
        fetchSupplier360(supplierId),
        fetchAfterSalesSummary(supplierId).catch(() => null),
        fetchGuaranteeEventSummary(supplierId).catch(() => null),
        fetchWarrantyClaimSummary(supplierId).catch(() => null),
        fetchEvidencePendingSummary(supplierId).catch(() => null),
      ]);
      setView360(v);
      setAsSummary(as);
      setGeSummary(ge);
      setWcSummary(wc);
      setPendingSummary(pending);
      setInfo('Supplier 360 loaded');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load 360');
    }
  }


  async function runEligibility() {
    if (!supplierId.trim()) {
      setError('Enter supplier ID');
      return;
    }
    clearMsgs();
    try {
      const codes = reqCaps
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      const policies = reqPolicies
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      const result = await evaluateEligibility({
        supplierId: supplierId.trim(),
        requiredCapabilityCodes: codes.length ? codes : undefined,
        requiredMinCapacity: reqCapacity.trim() || undefined,
        requireCertification: requireCert,
        requiredPolicyTypes: policies.length ? policies : undefined,
        requireQuestionnaire,
        requireNoCriticalOpenRisks,
        requireNoHighOpenRisks,
      });
      setEligibility(result);
      const caps = await listCapabilities(supplierId.trim());
      setCapabilities(caps);
      setInfo(
        result.overallEligible
          ? 'Supplier is ELIGIBLE (passed mandatory gates)'
          : `NOT ELIGIBLE — failed: ${result.failedGates.join(', ') || 'gates'}`,
      );
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Eligibility evaluation failed');
    }
  }

  async function addCapability() {
    if (!supplierId.trim() || !capCode.trim() || !capName.trim()) {
      setError('Supplier, capability code and name are required');
      return;
    }
    clearMsgs();
    try {
      await createCapability({
        supplierId: supplierId.trim(),
        capabilityType: capType,
        capabilityCode: capCode.trim(),
        capabilityName: capName.trim(),
        certified: true,
      });
      setInfo('Capability registered');
      const caps = await listCapabilities(supplierId.trim());
      setCapabilities(caps);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to create capability');
    }
  }

  async function runTco() {
    if (!supplierId.trim()) {
      setError('Enter supplier ID');
      return;
    }
    clearMsgs();
    try {
      const result = await calculateTco({
        supplierId: supplierId.trim(),
        currencyCode: tcoCurrency || 'INR',
        annualVolume: tcoVolume.trim() || undefined,
        purchasePrice: tcoPurchase.trim() || undefined,
        freight: tcoFreight.trim() || undefined,
        logistics: tcoLogistics.trim() || undefined,
        packaging: tcoPackaging.trim() || undefined,
        qualityRejectionCost: tcoQuality.trim() || undefined,
        warrantyCost: tcoWarranty.trim() || undefined,
        toolingCost: tcoTooling.trim() || undefined,
      });
      setTcoResult(result);
      setInfo(
        result.dataComplete
          ? `TCO unit = ${result.unitTco} ${result.currencyCode}`
          : `TCO incomplete — missing: ${result.missingComponents.join(', ')}`,
      );
      const hist = await listTco(supplierId.trim());
      setTcoHistory(hist);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'TCO calculation failed');
    }
  }

  async function loadTemplates() {
    clearMsgs();
    try {
      const list = await listScorecardTemplates();
      setTemplates(list);
      if (list.length && !templateId) setTemplateId(list[0].templateId);
      setInfo(`${list.length} scorecard template(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load templates');
    }
  }

  async function runScorecard() {
    if (!supplierId.trim() || !templateId) {
      setError('Supplier ID and template required');
      return;
    }
    clearMsgs();
    try {
      const tpl = templates.find((t) => t.templateId === templateId);
      const categoryScores = (tpl?.categories ?? []).map((c) => ({
        categoryCode: c.categoryCode,
        rawScore0to100: scoreInputs[c.categoryCode] ?? '50',
      }));
      const result = await evaluateScorecard({
        templateId,
        supplierId: supplierId.trim(),
        categoryScores,
      });
      setScoreResult(result);
      setInfo(`Overall score: ${result.overallScore}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Scorecard evaluation failed');
    }
  }

  async function runComparison() {
    const ids = compareIds
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    if (ids.length < 2) {
      setError('Enter at least two supplier IDs (comma-separated)');
      return;
    }
    clearMsgs();
    try {
      const result = await createComparison({ supplierIds: ids, currencyCode: 'INR' });
      setComparison(result);
      setInfo(result.tradeOffSummary ?? 'Comparison created');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Comparison failed');
    }
  }

  async function loadComparisonHistory() {
    setError(null);
    try {
      const rows = await listComparisons();
      setComparisonHistory(rows);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load comparison history');
    }
  }

  async function loadScoreHistory() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const rows = await listScorecardResults(supplierId.trim());
      setScoreHistory(rows);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load scorecard history');
    }
  }

  async function loadExpiringCaps() {
    setError(null);
    try {
      const rows = await listExpiringCapabilities();
      setExpiringCaps(rows);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load expiring capabilities');
    }
  }


  async function loadAfterSalesMetrics() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const rows = await listAfterSalesMetrics(
        supplierId.trim(),
        asEvidenceFilter.trim() || undefined,
      );
      setAsMetrics(rows);
      setInfo(`${rows.length} after-sales metric(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load after-sales metrics');
    }
  }


  async function loadPerformanceFeedback() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const applied =
        pfAppliedFilter === 'true' ? true : pfAppliedFilter === 'false' ? false : undefined;
      const rows = await listPerformanceFeedback(supplierId.trim(), {
        appliedToHistory: applied,
        evidenceStatus: pfEvidenceFilter.trim() || undefined,
      });
      setPfItems(rows);
      setInfo(`${rows.length} performance feedback row(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load performance feedback');
    }
  }


  async function loadWarrantyClaimItems() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const rows = await listWarrantyClaimItems(supplierId.trim(), {
        claimStatus: wcClaimFilter.trim() || undefined,
        evidenceStatus: wcEvidenceFilter.trim() || undefined,
      });
      setWcItems(rows);
      setInfo(`${rows.length} warranty claim(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load warranty claims');
    }
  }

  async function loadGuaranteeEventItems() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const unresolved =
        geUnresolvedOnly === 'true' ? true : geUnresolvedOnly === 'false' ? false : undefined;
      const rows = await listGuaranteeEventItems(supplierId.trim(), {
        unresolvedOnly: unresolved,
        evidenceStatus: geEvidenceFilter.trim() || undefined,
      });
      setGeItems(rows);
      setInfo(`${rows.length} guarantee event(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load guarantee events');
    }
  }


  async function loadQualityMetrics() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const rows = await listQualityMetrics(
        supplierId.trim(),
        qmEvidenceFilter.trim() || undefined,
      );
      setQmMetrics(rows);
      setInfo(`${rows.length} quality metric(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load quality metrics');
    }
  }

  async function loadDeliveryMetrics() {
    if (!supplierId.trim()) {
      setError('Enter a supplier ID');
      return;
    }
    setError(null);
    try {
      const rows = await listDeliveryMetrics(
        supplierId.trim(),
        dmEvidenceFilter.trim() || undefined,
      );
      setDmMetrics(rows);
      setInfo(`${rows.length} delivery metric(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load delivery metrics');
    }
  }

  async function verifyQm(metricId: string) {
    try {
      await verifyQualityMetric(metricId, 'VERIFIED');
      setInfo(`Quality metric ${metricId.slice(0, 8)}… verified`);
      if (qmMetrics.length > 0 || qmEvidenceFilter) {
        await loadQualityMetrics();
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Quality verify failed');
    }
  }

  async function verifyDm(metricId: string) {
    try {
      await verifyDeliveryMetric(metricId, 'VERIFIED');
      setInfo(`Delivery metric ${metricId.slice(0, 8)}… verified`);
      if (dmMetrics.length > 0 || dmEvidenceFilter) {
        await loadDeliveryMetrics();
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Delivery verify failed');
    }
  }


  async function createRecDraft() {
    clearMsgs();
    try {
      const candidates = JSON.parse(recCandidatesJson) as {
        supplierId: string;
        eligible: boolean;
        overallScore?: string;
        tcoTotal?: string;
        tcoCurrency?: string;
        tcoDataComplete?: boolean;
        failedGates?: string[];
        missingDataHints?: string[];
      }[];
      if (!recEventCode.trim()) {
        setError('Sourcing event code required');
        return;
      }
      const rec = await createRecommendation({
        sourcingEventCode: recEventCode.trim(),
        candidates,
      });
      setRecommendation(rec);
      setInfo(`DRAFT recommendation ${rec.recommendationId} — ranking only, not an approval`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create recommendation failed');
    }
  }

  async function loadRec() {
    if (!recIdLoad.trim()) return;
    clearMsgs();
    try {
      const rec = await getRecommendation(recIdLoad.trim());
      setRecommendation(rec);
      setInfo(`Loaded recommendation ${rec.recommendationId} (${rec.status})`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Load failed');
    }
  }

  async function doSubmitRec() {
    if (!recommendation) return;
    clearMsgs();
    try {
      const rec = await submitRecommendation(recommendation.recommendationId);
      setRecommendation(rec);
      setInfo('Submitted for human decision');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Submit failed');
    }
  }

  async function doApprove() {
    if (!recommendation) return;
    clearMsgs();
    try {
      const rec = await approveRecommendation(recommendation.recommendationId, decisionComment || undefined);
      setRecommendation(rec);
      setInfo('HUMAN APPROVED — recommended supplier accepted');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Approve failed');
    }
  }

  async function doReject() {
    if (!recommendation) return;
    clearMsgs();
    try {
      const rec = await rejectRecommendation(recommendation.recommendationId, decisionComment || undefined);
      setRecommendation(rec);
      setInfo('HUMAN REJECTED');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Reject failed');
    }
  }

  async function doOverride() {
    if (!recommendation || !overrideSupplierId.trim() || !overrideReason.trim()) {
      setError('Override requires supplier ID and mandatory reason');
      return;
    }
    clearMsgs();
    try {
      const rec = await overrideRecommendation(
        recommendation.recommendationId,
        overrideSupplierId.trim(),
        overrideReason.trim(),
        decisionComment || undefined,
      );
      setRecommendation(rec);
      setInfo('HUMAN OVERRIDE applied');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Override failed');
    }
  }

  async function loadQual() {
    if (!supplierId.trim()) {
      setError('Enter supplier ID');
      return;
    }
    clearMsgs();
    try {
      const q = await getQualification(supplierId.trim());
      setQualification(q);
      setInfo(`Current lifecycle: ${q.lifecycleStatus}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'No qualification record (initialize first)');
      setQualification(null);
    }
  }

  async function initQual() {
    if (!supplierId.trim()) return;
    clearMsgs();
    try {
      const q = await initializeQualification(supplierId.trim());
      setQualification(q);
      setInfo(`Initialized at ${q.lifecycleStatus}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Initialize failed');
    }
  }

  async function doTransition() {
    if (!supplierId.trim()) return;
    clearMsgs();
    try {
      const q = await transitionQualification({
        supplierId: supplierId.trim(),
        targetStatus,
        reason: qualReason.trim() || undefined,
        recommendationId: qualRecommendationId.trim() || undefined,
      });
      setQualification(q);
      setInfo(`Transitioned to ${q.lifecycleStatus}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Transition failed');
    }
  }


  async function loadRisks() {
    if (!supplierId.trim()) {
      setError('Enter supplier ID');
      return;
    }
    clearMsgs();
    try {
      const summary = await getRiskSummary(supplierId.trim());
      setRiskSummary({
        openCount: summary.openCount,
        criticalOpenCount: summary.criticalOpenCount,
        highOpenCount: summary.highOpenCount,
      });
      setRisks(summary.openRisks);
      setInfo(
        `Open risks: ${summary.openCount} (critical ${summary.criticalOpenCount}, high ${summary.highOpenCount})`,
      );
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load risks');
    }
  }

  async function addRisk() {
    if (!supplierId.trim() || !riskTitle.trim()) {
      setError('Supplier and risk title required');
      return;
    }
    clearMsgs();
    try {
      await createRisk({
        supplierId: supplierId.trim(),
        riskCategory,
        riskTitle: riskTitle.trim(),
        description: riskDesc.trim() || undefined,
        severity: riskSeverity,
        likelihood: riskLikelihood,
        mitigationPlan: riskMitigation.trim() || undefined,
      });
      setInfo('Risk registered (status OPEN)');
      setRiskTitle('');
      await loadRisks();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create risk failed');
    }
  }

  async function setRiskStatus(riskId: string, status: RiskStatus) {
    clearMsgs();
    try {
      // ACCEPTED requires rationale via mitigationPlan
      const mitigation =
        status === 'ACCEPTED' ? riskMitigation.trim() || 'Accepted with documented residual risk' : undefined;
      await updateRiskStatus(riskId, { status, mitigationPlan: mitigation });
      setInfo(`Risk ${riskId} → ${status}`);
      await loadRisks();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Update risk status failed');
    }
  }


  async function runQuestionnaireEval() {
    if (!supplierId.trim()) {
      setError('Enter supplier ID');
      return;
    }
    clearMsgs();
    try {
      const result = await evaluateQuestionnaire(supplierId.trim());
      setQEval(result);
      setInfo(
        result.overallEligible
          ? `Questionnaire eligible — score ${result.weightedScore ?? '—'} / ${result.maxPossibleScore ?? '—'}`
          : `Questionnaire NOT eligible — failed gates: ${result.failedGateCodes.join(', ') || 'n/a'}; missing: ${result.missingQuestionCodes.join(', ') || 'n/a'}`,
      );
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Questionnaire evaluation failed');
    }
  }




  async function dispatchDecisionWebhooks() {
    clearMsgs();
    try {
      const s = await dispatchRecommendationDecisionEvents();
      setInfo(`Webhook dispatch attempted=${s.attempted} ok=${s.succeeded} fail=${s.failed} enabled=${s.enabled}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Webhook dispatch failed');
    }
  }

  async function loadDecisions() {
    if (!recommendation) return;
    clearMsgs();
    try {
      const [list, events] = await Promise.all([
        listRecommendationDecisions(recommendation.recommendationId),
        listRecommendationDecisionEvents(recommendation.recommendationId),
      ]);
      setDecisions(list);
      setDecisionEvents(events);
      setInfo(`${list.length} ledger entr(y/ies), ${events.length} outbox event(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load decisions');
    }
  }

  async function loadExplanation() {
    if (!recommendation) return;
    clearMsgs();
    try {
      const exp = await getRecommendationExplanation(recommendation.recommendationId);
      setExplanation(exp);
      setInfo('Explainability snapshot loaded');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load explanation');
    }
  }

  async function loadRecQueue() {
    clearMsgs();
    try {
      const list = await listRecommendationsByStatus(recQueueStatus);
      setRecQueue(list);
      setInfo(`${list.length} recommendation(s) with status ${recQueueStatus}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load recommendation queue');
    }
  }



  async function cloneTemplateWithWeights() {
    if (!templateId || !cloneCode.trim() || !cloneName.trim()) {
      setError('Select a source template and enter new template code and name');
      return;
    }
    const source = templates.find((t) => t.templateId === templateId);
    if (!source) {
      setError('Source template not loaded — click Refresh templates');
      return;
    }
    const categories = source.categories.map((c, i) => ({
      categoryCode: c.categoryCode,
      categoryName: c.categoryName,
      weightPercent: cloneWeights[c.categoryCode] ?? String(c.weightPercent),
      sequenceNo: c.sequenceNo ?? i + 1,
    }));
    const total = categories.reduce((s, c) => s + (parseFloat(c.weightPercent) || 0), 0);
    if (Math.abs(total - 100) > 0.0001) {
      setError(`Weights total ${total.toFixed(4)}% — must equal 100% before saving`);
      return;
    }
    clearMsgs();
    try {
      const created = await cloneScorecardTemplate(templateId, {
        newTemplateCode: cloneCode.trim(),
        newTemplateName: cloneName.trim(),
        categories,
      });
      setInfo(`Saved configuration as ${created.templateCode}. Select it to evaluate suppliers.`);
      await loadTemplates();
      setTemplateId(created.templateId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Save failed — check unique code and weights total 100%');
    }
  }


  async function loadConfigQuestions() {
    clearMsgs();
    try {
      const list = await listQuestionnaireQuestions(qFilterCategory || undefined);
      setConfigQuestions(list);
      const seedCount = list.filter((q) =>
        q.questionCode.startsWith('VS-Q-AS-')
        || q.questionCode.startsWith('VS-Q-WC-')
        || q.questionCode.startsWith('VS-Q-GE-'),
      ).length;
      setInfo(
        `${list.length} active question(s)`
        + (qFilterCategory ? ` in ${qFilterCategory}` : '')
        + (seedCount ? ` · ${seedCount} after-sales/warranty-claim/guarantee seed(s)` : ''),
      );
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load questions');
    }
  }

  function isSeedQuestion(code: string) {
    return code.startsWith('VS-Q-AS-') || code.startsWith('VS-Q-WC-') || code.startsWith('VS-Q-GE-');
  }

  async function saveNewQuestion() {
    if (!qCode.trim() || !qText.trim()) {
      setError('Question code and text are required');
      return;
    }
    clearMsgs();
    try {
      await createQuestionnaireQuestion({
        questionCode: qCode.trim(),
        category: qCategory,
        questionText: qText.trim(),
        answerType: qAnswerType,
        requiredFlag: qRequired,
        evidenceRequired: qEvidence,
        mandatoryGate: qGate,
        weight: qWeight || undefined,
      });
      setInfo(`Question ${qCode.trim().toUpperCase()} saved (new version if code existed)`);
      setQCode('');
      setQText('');
      await loadConfigQuestions();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to create question');
    }
  }

  async function saveCapability() {
    if (!supplierId || !capCode.trim() || !capName.trim()) {
      setError('Select supplier and enter capability code/name');
      return;
    }
    clearMsgs();
    try {
      await createCapability({
        supplierId,
        capabilityType: capType,
        capabilityCode: capCode.trim(),
        capabilityName: capName.trim(),
        maxAnnualCapacity: capCapacity || undefined,
        certified: capCertified,
      });
      setInfo(`Capability ${capCode.trim()} registered`);
      const caps = await listCapabilities(supplierId);
      setConfigCaps(caps);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to create capability');
    }
  }

  async function loadConfigCaps() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const caps = await listCapabilities(supplierId);
      setConfigCaps(caps);
      setInfo(`${caps.length} capability(ies)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load capabilities');
    }
  }

  async function savePolicy() {
    if (!supplierId || !policyTitle.trim()) {
      setError('Select supplier and enter policy title');
      return;
    }
    clearMsgs();
    try {
      await addPolicy({
        supplierId,
        policyType,
        policyTitle: policyTitle.trim(),
      });
      setInfo(`Policy ${policyType} recorded (evidence still PENDING until verified)`);
      setPolicyTitle('');
      const items = await listPolicies(supplierId.trim(), {
        evidenceStatus: policyEvidenceFilter.trim() || undefined,
        policyType: policyTypeFilter.trim() || undefined,
      });
      setPolicyItems(items);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to add policy');
    }
  }



  async function verifyAnswerEvidence(responseId: string, status: 'VERIFIED' | 'FAILED' | 'WAIVED') {
    clearMsgs();
    try {
      await verifyQuestionnaireEvidence(
        responseId,
        status,
        status === 'VERIFIED' ? 'config-verified' : undefined,
        status === 'WAIVED' ? 'Waived via Configuration tab' : undefined,
      );
      setInfo(`Response evidence → ${status}`);
      await loadConfigAnswers();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Evidence verify failed');
    }
  }

  async function loadConfigAnswers() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const list = await listQuestionnaireResponses(supplierId);
      setConfigAnswers(list);
      setInfo(`${list.length} questionnaire response(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load responses');
    }
  }

  async function submitAnswerForQuestion(q: QuestionnaireQuestion) {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    const raw = (answerDrafts[q.questionId] ?? '').trim();
    if (!raw && q.requiredFlag) {
      setError(`Answer required for ${q.questionCode}`);
      return;
    }
    clearMsgs();
    try {
      const body: Parameters<typeof submitQuestionnaireResponse>[0] = {
        supplierId,
        questionId: q.questionId,
        evidenceRef: answerEvidence[q.questionId] || undefined,
      };
      if (q.answerType === 'YES_NO') {
        body.answerBoolean = raw.toUpperCase() === 'YES' || raw === 'true' || raw === '1';
      } else if (q.answerType === 'NUMBER' || q.answerType === 'PERCENTAGE' || q.answerType === 'CURRENCY' || q.answerType === 'RATING') {
        body.answerNumber = raw;
      } else if (q.answerType === 'DATE') {
        body.answerDate = raw;
      } else {
        body.answerText = raw;
      }
      await submitQuestionnaireResponse(body);
      setInfo(`Answer saved for ${q.questionCode}`);
      await loadConfigAnswers();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to submit answer');
    }
  }

  async function runConfigQuestionnaireEval() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const ev = await evaluateQuestionnaire(supplierId);
      setQEval(ev);
      setInfo(
        `Questionnaire evaluation: eligible=${ev.overallEligible} gates=${ev.mandatoryGatesPassed} answered=${ev.answeredCount}/${ev.totalActiveQuestions}`,
      );
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Questionnaire evaluate failed');
    }
  }

  async function verifyPolicyRow(policyId: string, status: 'VERIFIED' | 'FAILED') {
    clearMsgs();
    try {
      await verifyPolicy(policyId, status, status === 'VERIFIED' ? 'config-verified' : undefined);
      setInfo(`Policy ${policyId} → ${status}`);
      await loadConfigPolicies();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Policy verify failed');
    }
  }

  async function deactivateSelectedTemplate() {
    if (!templateId) {
      setError('Select a template on Scorecard or load templates first');
      return;
    }
    clearMsgs();
    try {
      await deactivateScorecardTemplate(templateId);
      setInfo('Template deactivated for new evaluations (history preserved)');
      await loadTemplates();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Deactivate failed');
    }
  }



  async function loadLineage() {
    if (!templateId) {
      setError('Select a scorecard template first');
      return;
    }
    clearMsgs();
    try {
      const rows = await listScorecardLineage(templateId);
      setLineageRows(rows);
      setInfo(`${rows.length} template version(s) in lineage`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load lineage');
    }
  }




  async function loadOperationBlocks() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const list = await listLifecycleOperationBlocks(supplierId);
      setOperationBlocks(list);
      setInfo(`${list.length} blocked operation attempt(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load operation blocks');
    }
  }

  async function loadLifecycleEvents() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const list = await listQualificationLifecycleEvents(supplierId);
      setLifecycleEvents(list);
      setInfo(`${list.length} lifecycle event(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load lifecycle events');
    }
  }

  async function loadApprovalChecks() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const list = await listQualificationApprovalChecks(supplierId);
      setApprovalChecks(list);
      setInfo(`${list.length} approval gate check(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load approval checks');
    }
  }


  async function loadCapExpiryJob() {
    clearMsgs();
    try {
      const [job, runs] = await Promise.all([getCapabilityExpiryJobStatus(), listCapabilityExpiryRuns()]);
      setCapExpiryJob(job);
      setCapExpiryRuns(runs);
      setInfo(`Capability expiry job enabled=${job.enabled}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load capability expiry job');
    }
  }

  async function runCapExpiryNow() {
    clearMsgs();
    try {
      const run = await runCapabilityExpiry();
      setInfo(`Capability expiry run ${run.runId}: expired=${run.capabilitiesExpired}`);
      await loadCapExpiryJob();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Capability expiry failed');
    }
  }

  async function loadExpiryJob() {
    clearMsgs();
    try {
      const [job, runs] = await Promise.all([getEvidenceExpiryJobStatus(), listEvidenceExpiryRuns()]);
      setExpiryJob(job);
      setExpiryRuns(runs);
      setInfo(`Expiry job enabled=${job.enabled}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load expiry job');
    }
  }

  async function toggleExpiryJob(enabled: boolean) {
    clearMsgs();
    try {
      const job = await setEvidenceExpiryJobEnabled(enabled);
      setExpiryJob(job);
      setInfo(`Expiry job ${enabled ? 'enabled' : 'disabled'}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to update job');
    }
  }

  async function runExpiryNow() {
    clearMsgs();
    try {
      const run = await runEvidenceExpiry();
      setInfo(`Manual expiry run ${run.runId}: P=${run.policiesExpired} W=${run.warrantiesExpired} G=${run.guaranteesExpired}`);
      await loadExpiryJob();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Expiry run failed');
    }
  }

  async function loadConfigPolicies() {
    if (!supplierId) {
      setError('Select a supplier first');
      return;
    }
    clearMsgs();
    try {
      const items = await listPolicies(supplierId.trim(), {
        evidenceStatus: policyEvidenceFilter.trim() || undefined,
        policyType: policyTypeFilter.trim() || undefined,
      });
      setPolicyItems(items);
      setInfo(`${items.length} policy evidence record(s)`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load policies');
    }
  }

  return (
    <Stack spacing={2}>
      <RequiredFieldLegend />
      <Stack direction="row" alignItems="center" spacing={1}>
        <HubOutlinedIcon color="primary" />
        <Typography variant="h5">Vendor Selection</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Eligibility gates → evidence → TCO → scorecard → comparison → recommendation → human approval.
        Risk is maintained separately from score. Lowest price is never treated as best supplier.
      </Typography>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="flex-start">
            <TextField
              id="vs-supplier-id"
              name="supplierId"
              label="Supplier ID"
              size="small"
              value={supplierId}
              required
              onChange={(e) => setSupplierId(e.target.value)}
              sx={{ minWidth: 280 }}
              slotProps={{ htmlInput: { 'data-testid': 'vs-supplier-id', name: 'supplierId' } }}
            />
            <Button variant="outlined" onClick={() => void loadSuppliers()}>
              List suppliers
            </Button>
            {suppliers.length > 0 && (
              <FormControl size="small" sx={{ minWidth: 260 }}>
                <InputLabel>Pick supplier</InputLabel>
                <Select
                  label="Pick supplier"
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value)}
                >
                  {suppliers.map((s) => (
                    <MenuItem key={s.supplierId} value={s.supplierId}>
                      {s.supplierCode} — {s.supplierName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          </Stack>
        </CardContent>
      </Card>

      <SupplierListPanel
        suppliers={suppliers}
        selectedId={supplierId}
        onSelect={(id) => {
          setSupplierId(id);
          setInfo(`Selected supplier ${id}`);
        }}
      />

      <Tabs
        value={tab}
        onChange={(_, v: TabKey) => setTab(v)}
        variant="scrollable"
        scrollButtons="auto"
      >
        <Tab label="360 View" value="360" />
        <Tab label="Eligibility" value="eligibility" />
        <Tab label="TCO" value="tco" />
        <Tab label="Scorecard" value="scorecard" />
        <Tab label="Comparison" value="comparison" />
        <Tab label="Recommendation" value="recommendation" />
        <Tab label="Qualification" value="qualification" />
        <Tab label="Risk Register" value="risk" />
        <Tab label="Configuration" value="config" />
      </Tabs>

      {tab === '360' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Button variant="contained" onClick={() => void load360()}>
                Load Supplier 360
              </Button>
              {view360 && (
                <Stack direction="row" spacing={1} flexWrap="wrap">
                  {wcSummary && (
                    <Chip size="small" color="warning" variant="outlined"
                      label={`Warranty claims: ${wcSummary.totalClaims} (open ${wcSummary.openClaims}, accepted ${wcSummary.acceptedClaims}, rejected ${wcSummary.rejectedClaims}, closed ${wcSummary.closedClaims ?? 0}, withdrawn ${wcSummary.withdrawnClaims ?? 0})`} />
                  )}
                  {geSummary && (
                    <Chip size="small" color="warning" variant="outlined"
                      label={`Guarantee events: ${geSummary.totalEvents} (unresolved ${geSummary.unresolvedEvents})`} />
                  )}
                  {asSummary && (
                    <Chip size="small" color="info" variant="outlined"
                      label={`After-sales metrics: ${asSummary.totalMetrics} (verified ${asSummary.verifiedMetrics})`} />
                  )}
                  {pendingSummary && (pendingSummary.totalPending ?? 0) > 0 && (
                    <Chip size="small" color="error" variant="outlined"
                      label={`Pending evidence: ${pendingSummary.totalPending}`} />
                  )}
                  <Button size="small" variant="outlined" onClick={() => void loadExpiringCaps()}>
                    Load expiring capabilities
                  </Button>
                  {expiringCaps.length > 0 && (
                    <Chip size="small" color="warning" variant="outlined"
                      label={`Expiring capabilities (system-wide): ${expiringCaps.length}`} />
                  )}
                </Stack>
              )}
              {expiringCaps.length > 0 && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Supplier</TableCell>
                      <TableCell>Code</TableCell>
                      <TableCell>Name</TableCell>
                      <TableCell>Valid to</TableCell>
                      <TableCell>Type</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {expiringCaps.map((c) => (
                      <TableRow key={c.capabilityId}>
                        <TableCell>{c.supplierId}</TableCell>
                        <TableCell>{c.capabilityCode}</TableCell>
                        <TableCell>{c.capabilityName}</TableCell>
                        <TableCell>{c.validTo ?? '—'}</TableCell>
                        <TableCell>{c.capabilityType}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
              {view360 && (
                <>
                  <Typography variant="subtitle1">Capabilities ({view360.capabilities.length})</Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Code</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Certified</TableCell>
                        <TableCell>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {view360.capabilities.map((c) => (
                        <TableRow key={c.capabilityId}>
                          <TableCell>{c.capabilityCode}</TableCell>
                          <TableCell>{c.capabilityName}</TableCell>
                          <TableCell>{c.capabilityType}</TableCell>
                          <TableCell>{yesNoChip(c.certified)}</TableCell>
                          <TableCell>{c.status}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <Divider />
                  <QualityMetricsPanel
                    metrics={qmMetrics.length > 0 ? qmMetrics : view360.qualityMetrics}
                    evidenceFilter={qmEvidenceFilter}
                    onEvidenceFilter={setQmEvidenceFilter}
                    onLoad={() => void loadQualityMetrics()}
                    onVerify={(id) => void verifyQm(id)}
                  />
                  <DeliveryMetricsPanel
                    metrics={dmMetrics.length > 0 ? dmMetrics : view360.deliveryMetrics}
                    evidenceFilter={dmEvidenceFilter}
                    onEvidenceFilter={setDmEvidenceFilter}
                    onLoad={() => void loadDeliveryMetrics()}
                    onVerify={(id) => void verifyDm(id)}
                  />
                  <Typography variant="subtitle1">Warranties</Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 1 }}>
                    {view360.warranties.map((w) => (
                      <Chip
                        key={w.warrantyId}
                        label={`Warranty ${w.durationMonths ?? '?'} mo · ${w.evidenceStatus}`}
                        size="small"
                      />
                    ))}
                    {view360.warranties.length === 0 && (
                      <Typography variant="body2" color="text.secondary">No warranty terms</Typography>
                    )}
                  </Stack>
                  <WarrantyClaimsPanel
                    claims={wcItems.length > 0 ? wcItems : (view360.warrantyClaims ?? [])}
                    claimStatusFilter={wcClaimFilter}
                    evidenceFilter={wcEvidenceFilter}
                    onClaimStatusFilter={setWcClaimFilter}
                    onEvidenceFilter={setWcEvidenceFilter}
                    onLoad={() => void loadWarrantyClaimItems()}
                    onVerify={(id) => void verifyClaim(id)}
                    onTransition={(id, st) => void transitionClaimStatus(id, st)}
                  />
                  <Typography variant="subtitle1" sx={{ mt: 1 }}>Guarantees</Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 1 }}>
                    {view360.guarantees.map((g) => (
                      <Chip
                        key={g.guaranteeId}
                        label={`${g.guaranteeType} · ${g.evidenceStatus}`}
                        size="small"
                      />
                    ))}
                  </Stack>
                  <GuaranteeEventsPanel
                    events={geItems.length > 0 ? geItems : (view360.guaranteeEvents ?? [])}
                    unresolvedOnly={geUnresolvedOnly}
                    evidenceFilter={geEvidenceFilter}
                    onUnresolvedOnly={setGeUnresolvedOnly}
                    onEvidenceFilter={setGeEvidenceFilter}
                    onLoad={() => void loadGuaranteeEventItems()}
                    onVerify={(id) => void verifyGe(id)}
                  />
                  <AfterSalesMetricsPanel
                    metrics={view360.afterSalesMetrics ?? []}
                    onVerify={(id) => void verifyAs(id)}
                  />
                  <PerformanceFeedbackPanel
                    items={pfItems.length > 0 ? pfItems : (view360.performanceFeedback ?? [])}
                    evidenceFilter={pfEvidenceFilter}
                    appliedFilter={pfAppliedFilter}
                    onEvidenceFilter={setPfEvidenceFilter}
                    onAppliedFilter={setPfAppliedFilter}
                    onLoad={() => void loadPerformanceFeedback()}
                    onVerify={(id) => void verifyPf(id)}
                    onApplyToHistory={(id) => void applyPf(id)}
                  />
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2">Record warranty claim</Typography>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap">
                    <TextField size="small" type="date" label="Claim date" InputLabelProps={{ shrink: true }} value={claimDate} onChange={(e) => setClaimDate(e.target.value)} />
                    <TextField size="small" label="Claim #" value={claimNumber} onChange={(e) => setClaimNumber(e.target.value)} />
                    <TextField size="small" label="Cost" value={claimCost} onChange={(e) => setClaimCost(e.target.value)} />
                    <FormControl size="small" sx={{ minWidth: 120 }}>
                      <InputLabel>Status</InputLabel>
                      <Select label="Status" value={claimStatus} onChange={(e) => setClaimStatus(e.target.value as WarrantyClaimStatus)}>
                        {(['OPEN', 'ACCEPTED', 'REJECTED', 'CLOSED', 'WITHDRAWN'] as WarrantyClaimStatus[]).map((s) => (
                          <MenuItem key={s} value={s}>{s}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                    <Button variant="outlined" onClick={() => void submitClaim()}>Add claim</Button>
                  </Stack>
                  <Typography variant="subtitle2" sx={{ mt: 1 }}>Record guarantee event</Typography>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap">
                    <TextField size="small" type="date" label="Event date" InputLabelProps={{ shrink: true }} value={geDate} onChange={(e) => setGeDate(e.target.value)} />
                    <FormControl size="small" sx={{ minWidth: 160 }}>
                      <InputLabel>Type</InputLabel>
                      <Select label="Type" value={geType} onChange={(e) => setGeType(e.target.value as GuaranteeEventType)}>
                        {(['FAILURE', 'BREACH', 'PENALTY_APPLIED', 'REPLACEMENT_TRIGGERED', 'RESOLVED', 'OTHER'] as GuaranteeEventType[]).map((t) => (
                          <MenuItem key={t} value={t}>{t}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                    <TextField size="small" label="Description" value={geDesc} onChange={(e) => setGeDesc(e.target.value)} />
                    <TextField size="small" label="Penalty" value={gePenalty} onChange={(e) => setGePenalty(e.target.value)} />
                    <Button variant="outlined" onClick={() => void submitGe()}>Add event</Button>
                  </Stack>
                  <Typography variant="subtitle2" sx={{ mt: 1 }}>After-sales metrics (separate from warranty)</Typography>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                    <FormControl size="small" sx={{ minWidth: 160 }}>
                      <InputLabel>Evidence</InputLabel>
                      <Select
                        label="Evidence"
                        value={asEvidenceFilter}
                        onChange={(e) => setAsEvidenceFilter(e.target.value)}
                      >
                        <MenuItem value="">All</MenuItem>
                        <MenuItem value="PENDING">PENDING</MenuItem>
                        <MenuItem value="VERIFIED">VERIFIED</MenuItem>
                        <MenuItem value="FAILED">FAILED</MenuItem>
                        <MenuItem value="EXPIRED">EXPIRED</MenuItem>
                      </Select>
                    </FormControl>
                    <Button variant="outlined" onClick={() => void loadAfterSalesMetrics()}>
                      Load after-sales metrics
                    </Button>
                  </Stack>
                  {asMetrics.length > 0 && (
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Period</TableCell>
                          <TableCell>SLA met %</TableCell>
                          <TableCell>Unresolved</TableCell>
                          <TableCell>Evidence</TableCell>
                          <TableCell>Action</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {asMetrics.map((m) => (
                          <TableRow key={m.metricId}>
                            <TableCell>{m.periodStart} → {m.periodEnd}</TableCell>
                            <TableCell>{m.serviceSlaMetPercent ?? '—'}</TableCell>
                            <TableCell>{m.unresolvedComplaints ?? '—'}</TableCell>
                            <TableCell>{m.evidenceStatus}</TableCell>
                            <TableCell>
                              {m.evidenceStatus === 'PENDING' && (
                                <Button size="small" onClick={() => void verifyAs(m.metricId)}>Verify</Button>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}
                  <Typography variant="subtitle2" sx={{ mt: 1 }}>Record after-sales metric</Typography>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap">
                    <TextField size="small" type="date" label="Period start" InputLabelProps={{ shrink: true }} value={asStart} onChange={(e) => setAsStart(e.target.value)} />
                    <TextField size="small" type="date" label="Period end" InputLabelProps={{ shrink: true }} value={asEnd} onChange={(e) => setAsEnd(e.target.value)} />
                    <TextField size="small" label="Response hrs" value={asResponseHrs} onChange={(e) => setAsResponseHrs(e.target.value)} />
                    <TextField size="small" label="SLA met %" value={asSlaPct} onChange={(e) => setAsSlaPct(e.target.value)} />
                    <TextField size="small" label="Unresolved" value={asUnresolved} onChange={(e) => setAsUnresolved(e.target.value)} />
                    <Button variant="outlined" onClick={() => void submitAs()}>Add metric</Button>
                  </Stack>
                  <Typography variant="subtitle2" sx={{ mt: 1 }}>Record performance feedback</Typography>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap">
                    <TextField size="small" type="date" label="Feedback date" InputLabelProps={{ shrink: true }} value={pfDate} onChange={(e) => setPfDate(e.target.value)} />
                    <FormControl size="small" sx={{ minWidth: 140 }}>
                      <InputLabel>Type</InputLabel>
                      <Select label="Type" value={pfType} onChange={(e) => setPfType(e.target.value as PerformanceFeedbackType)}>
                        {(['QUALITY', 'DELIVERY', 'WARRANTY', 'SERVICE', 'CAPACITY', 'OTHER'] as PerformanceFeedbackType[]).map((t) => (
                          <MenuItem key={t} value={t}>{t}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                    <TextField size="small" label="Metric code" value={pfMetricCode} onChange={(e) => setPfMetricCode(e.target.value)} />
                    <TextField size="small" label="Value" value={pfMetricValue} onChange={(e) => setPfMetricValue(e.target.value)} />
                    <TextField size="small" label="Narrative" value={pfNarrative} onChange={(e) => setPfNarrative(e.target.value)} sx={{ minWidth: 200 }} />
                    <Button variant="outlined" onClick={() => void submitPf()}>Add feedback</Button>
                  </Stack>
                  <Typography variant="subtitle1" sx={{ mt: 2 }}>
                    Policy / compliance ({view360.policies?.length ?? 0})
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Declaration-only is never shown as verified. WAIVED requires rationale.
                  </Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Type</TableCell>
                        <TableCell>Title</TableCell>
                        <TableCell>Declaration only</TableCell>
                        <TableCell>Evidence</TableCell>
                        <TableCell>Expiry</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {(view360.policies ?? []).map((p) => (
                        <TableRow key={p.policyId}>
                          <TableCell>{p.policyType}</TableCell>
                          <TableCell>{p.policyTitle}</TableCell>
                          <TableCell>{p.declarationOnly ? 'YES' : 'NO'}</TableCell>
                          <TableCell>{evidenceChip(p.evidenceStatus)}</TableCell>
                          <TableCell>{p.expiryDate ?? '—'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'eligibility' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="subtitle2">Mandatory gates (score does not override)</Typography>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  label="Required capability codes (comma)"
                  size="small"
                  fullWidth
                  value={reqCaps}
                  onChange={(e) => setReqCaps(e.target.value)}
                />
                <TextField
                  label="Min annual capacity"
                  size="small"
                  value={reqCapacity}
                  onChange={(e) => setReqCapacity(e.target.value)}
                />
                <FormControl size="small" sx={{ minWidth: 160 }}>
                  <InputLabel>Require cert</InputLabel>
                  <Select
                    label="Require cert"
                    value={requireCert ? 'yes' : 'no'}
                    onChange={(e) => setRequireCert(e.target.value === 'yes')}
                  >
                    <MenuItem value="no">No</MenuItem>
                    <MenuItem value="yes">Yes</MenuItem>
                  </Select>
                </FormControl>
                <TextField
                  label="Required policy types (comma)"
                  size="small"
                  fullWidth
                  value={reqPolicies}
                  onChange={(e) => setReqPolicies(e.target.value)}
                  helperText="e.g. QUALITY_POLICY,ENVIRONMENTAL_POLICY — only VERIFIED non-expired counts"
                />
                <FormControl size="small" sx={{ minWidth: 200 }}>
                  <InputLabel>Require questionnaire</InputLabel>
                  <Select
                    label="Require questionnaire"
                    value={requireQuestionnaire ? 'yes' : 'no'}
                    onChange={(e) => setRequireQuestionnaire(e.target.value === 'yes')}
                  >
                    <MenuItem value="no">No</MenuItem>
                    <MenuItem value="yes">Yes — latest eval must pass</MenuItem>
                  </Select>
                </FormControl>
                <FormControl size="small" sx={{ minWidth: 180 }}>
                  <InputLabel>Block critical risks</InputLabel>
                  <Select
                    label="Block critical risks"
                    value={requireNoCriticalOpenRisks ? 'yes' : 'no'}
                    onChange={(e) => setRequireNoCriticalOpenRisks(e.target.value === 'yes')}
                  >
                    <MenuItem value="no">No</MenuItem>
                    <MenuItem value="yes">Yes — no critical open</MenuItem>
                  </Select>
                </FormControl>
                <FormControl size="small" sx={{ minWidth: 180 }}>
                  <InputLabel>Block high risks</InputLabel>
                  <Select
                    label="Block high risks"
                    value={requireNoHighOpenRisks ? 'yes' : 'no'}
                    onChange={(e) => setRequireNoHighOpenRisks(e.target.value === 'yes')}
                  >
                    <MenuItem value="no">No</MenuItem>
                    <MenuItem value="yes">Yes — no high open</MenuItem>
                  </Select>
                </FormControl>
              </Stack>
              <Button variant="contained" onClick={() => void runEligibility()}>
                Evaluate eligibility
              </Button>
              {eligibility && (
                <Box>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Typography>Overall eligible:</Typography>
                    {yesNoChip(eligibility.overallEligible)}
                  </Stack>
                  <Table size="small" sx={{ mt: 1 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Gate</TableCell>
                        <TableCell>Passed</TableCell>
                        <TableCell>Detail</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {eligibility.gates.map((g) => (
                        <TableRow key={g.gateCode}>
                          <TableCell>{g.gateCode}</TableCell>
                          <TableCell>{yesNoChip(g.passed)}</TableCell>
                          <TableCell>{g.detail}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              )}
              <Divider />
              <Typography variant="subtitle2">Register capability</Typography>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  label="Code"
                  size="small"
                  value={capCode}
                  onChange={(e) => setCapCode(e.target.value)}
                />
                <TextField
                  label="Name"
                  size="small"
                  value={capName}
                  onChange={(e) => setCapName(e.target.value)}
                />
                <FormControl size="small" sx={{ minWidth: 140 }}>
                  <InputLabel>Type</InputLabel>
                  <Select
                    label="Type"
                    value={capType}
                    onChange={(e) => setCapType(e.target.value as typeof capType)}
                  >
                    {(['PROCESS', 'COMMODITY', 'PLANT', 'CERTIFICATION', 'OTHER'] as const).map((t) => (
                      <MenuItem key={t} value={t}>
                        {t}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Button variant="outlined" onClick={() => void addCapability()}>
                  Add capability
                </Button>
              </Stack>
              {capabilities.length > 0 && (
                <Typography variant="body2">{capabilities.length} active capability(ies)</Typography>
              )}
              <Divider />
              <Typography variant="subtitle2">Questionnaire evaluation (active bank)</Typography>
              <Typography variant="caption" color="text.secondary">
                Mandatory gates + required answers; weighted score from answered questions only (no invented scores).
              </Typography>
              <Button variant="outlined" onClick={() => void runQuestionnaireEval()}>
                Evaluate questionnaire
              </Button>
              {qEval && (
                <Box>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    <Chip label={qEval.overallEligible ? 'ELIGIBLE' : 'NOT ELIGIBLE'} color={qEval.overallEligible ? 'success' : 'error'} size="small" />
                    <Chip label={`Answered ${qEval.answeredCount}/${qEval.totalActiveQuestions}`} size="small" />
                    <Chip label={`Gates failed ${qEval.mandatoryGateFailed}`} size="small" />
                    <Chip label={`Pending evidence ${qEval.evidencePendingCount}`} size="small" />
                    <Chip label={`Score ${qEval.weightedScore ?? '—'} / ${qEval.maxPossibleScore ?? '—'}`} size="small" />
                  </Stack>
                </Box>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'tco' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="subtitle2">
                Total Cost of Ownership — missing components marked DATA_NOT_AVAILABLE (never assumed zero)
              </Typography>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap">
                <TextField
                  label="Currency"
                  size="small"
                  value={tcoCurrency}
                  onChange={(e) => setTcoCurrency(e.target.value)}
                />
                <TextField
                  label="Annual volume"
                  size="small"
                  value={tcoVolume}
                  onChange={(e) => setTcoVolume(e.target.value)}
                />
                <TextField
                  label="Purchase price"
                  size="small"
                  value={tcoPurchase}
                  onChange={(e) => setTcoPurchase(e.target.value)}
                />
                <TextField
                  label="Freight"
                  size="small"
                  value={tcoFreight}
                  onChange={(e) => setTcoFreight(e.target.value)}
                />
                <TextField
                  label="Logistics"
                  size="small"
                  value={tcoLogistics}
                  onChange={(e) => setTcoLogistics(e.target.value)}
                />
                <TextField
                  label="Packaging"
                  size="small"
                  value={tcoPackaging}
                  onChange={(e) => setTcoPackaging(e.target.value)}
                />
                <TextField
                  label="Quality / rejection"
                  size="small"
                  value={tcoQuality}
                  onChange={(e) => setTcoQuality(e.target.value)}
                />
                <TextField
                  label="Warranty cost"
                  size="small"
                  value={tcoWarranty}
                  onChange={(e) => setTcoWarranty(e.target.value)}
                />
                <TextField
                  label="Tooling"
                  size="small"
                  value={tcoTooling}
                  onChange={(e) => setTcoTooling(e.target.value)}
                />
              </Stack>
              <Button variant="contained" onClick={() => void runTco()}>
                Calculate TCO
              </Button>
              {tcoResult && (
                <Box>
                  <Stack direction="row" spacing={2} alignItems="center">
                    <Typography>
                      Unit TCO: <strong>{tcoResult.unitTco ?? '—'}</strong> {tcoResult.currencyCode}
                    </Typography>
                    <Typography>
                      Total: <strong>{tcoResult.tcoTotal ?? '—'}</strong>
                    </Typography>
                    {yesNoChip(tcoResult.dataComplete)} data complete
                  </Stack>
                  {!tcoResult.dataComplete && (
                    <Alert severity="warning" sx={{ mt: 1 }}>
                      DATA NOT AVAILABLE for: {tcoResult.missingComponents.join(', ')} — amounts are not treated as zero
                    </Alert>
                  )}
                  <Table size="small" sx={{ mt: 1 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Component</TableCell>
                        <TableCell>Amount</TableCell>
                        <TableCell>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {tcoResult.components.map((c) => (
                        <TableRow key={c.componentCode}>
                          <TableCell>{c.componentCode}</TableCell>
                          <TableCell>
                            {c.status === 'DATA_NOT_AVAILABLE' || c.amount == null
                              ? 'DATA NOT AVAILABLE'
                              : c.amount}
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              label={c.status === 'DATA_NOT_AVAILABLE' ? 'DATA NOT AVAILABLE' : c.status}
                              color={c.status === 'PRESENT' ? 'success' : 'warning'}
                              variant="outlined"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              )}
              {tcoHistory.length > 0 && (
                <Typography variant="body2">{tcoHistory.length} historical TCO calculation(s)</Typography>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'scorecard' && (
        <Card>
          <CardContent>
            <Stack spacing={3}>
              <Box>
                <Typography variant="h6">Scorecard configuration</Typography>
                <Typography variant="body2" color="text.secondary">
                  Weights are customer-configurable and must total 100%. Seed template VS-DEFAULT-100 is a
                  starting baseline — clone it to save your own policy. Templates that already have evaluation
                  results cannot change weights (history stays on the original template).
                </Typography>
              </Box>

              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="center">
                <Button variant="outlined" onClick={() => void loadTemplates()}>
                  Refresh templates
                </Button>
                <FormControl size="small" sx={{ minWidth: 320 }}>
                  <InputLabel>Active template</InputLabel>
                  <Select
                    label="Active template"
                    value={templateId}
                    onChange={(e) => {
                      const id = e.target.value;
                      setTemplateId(id);
                      const src = templates.find((t) => t.templateId === id);
                      if (src) {
                        const w: Record<string, string> = {};
                        src.categories.forEach((c) => {
                          w[c.categoryCode] = String(c.weightPercent);
                        });
                        setCloneWeights(w);
                        if (!cloneCode) setCloneCode(`${src.templateCode}-CUSTOM`);
                        if (!cloneName) setCloneName(`${src.templateName} (custom)`);
                      }
                    }}
                  >
                    {templates.map((t) => (
                      <MenuItem key={t.templateId} value={t.templateId}>
                        {t.templateCode} — {t.templateName}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Stack>

              {templates.find((t) => t.templateId === templateId) && (
                <Box>
                  <Typography variant="subtitle1" sx={{ mb: 1 }}>
                    1. Edit category weights (%)
                  </Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Category</TableCell>
                        <TableCell>Code</TableCell>
                        <TableCell align="right">Weight %</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {templates
                        .find((t) => t.templateId === templateId)!
                        .categories.map((c) => (
                          <TableRow key={c.categoryCode}>
                            <TableCell>{c.categoryName}</TableCell>
                            <TableCell>{c.categoryCode}</TableCell>
                            <TableCell align="right">
                              <TextField
                                size="small"
                                type="number"
                                inputProps={{ min: 0, max: 100, step: 0.0001 }}
                                value={cloneWeights[c.categoryCode] ?? c.weightPercent}
                                onChange={(e) =>
                                  setCloneWeights((prev) => ({
                                    ...prev,
                                    [c.categoryCode]: e.target.value,
                                  }))
                                }
                                sx={{ width: 120 }}
                              />
                            </TableCell>
                          </TableRow>
                        ))}
                      <TableRow>
                        <TableCell colSpan={2}>
                          <strong>Total</strong>
                        </TableCell>
                        <TableCell align="right">
                          {(() => {
                            const src = templates.find((t) => t.templateId === templateId);
                            const total = (src?.categories ?? []).reduce((sum, c) => {
                              const v = parseFloat(cloneWeights[c.categoryCode] ?? String(c.weightPercent));
                              return sum + (Number.isFinite(v) ? v : 0);
                            }, 0);
                            const ok = Math.abs(total - 100) < 0.0001;
                            return (
                              <Chip
                                size="small"
                                label={`${total.toFixed(4)}%`}
                                color={ok ? 'success' : 'error'}
                                variant="outlined"
                              />
                            );
                          })()}
                        </TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                  <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                    Total must be exactly 100% to save.
                  </Typography>
                </Box>
              )}

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 1 }}>
                  2. Save as customer template (clone)
                </Typography>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                  <TextField
                    label="New template code"
                    size="small"
                    value={cloneCode}
                    onChange={(e) => setCloneCode(e.target.value)}
                    placeholder="e.g. CUSTOMER-A-100"
                    helperText="Unique code for this customer policy"
                  />
                  <TextField
                    label="New template name"
                    size="small"
                    fullWidth
                    value={cloneName}
                    onChange={(e) => setCloneName(e.target.value)}
                    helperText="Visible name in template list"
                  />
                  <Button variant="contained" onClick={() => void cloneTemplateWithWeights()}>
                    Save configuration
                  </Button>
                </Stack>
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 1 }}>
                  3. Evaluate supplier against selected template
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                  Enter raw scores 0–100 per category. Overall score uses the template weights above.
                </Typography>
                {templates
                  .find((t) => t.templateId === templateId)
                  ?.categories.map((c) => (
                    <TextField
                      key={c.categoryCode}
                      label={`${c.categoryName} (${cloneWeights[c.categoryCode] ?? c.weightPercent}%) — score 0–100`}
                      size="small"
                      fullWidth
                      sx={{ mb: 1 }}
                      value={scoreInputs[c.categoryCode] ?? ''}
                      onChange={(e) =>
                        setScoreInputs((prev) => ({ ...prev, [c.categoryCode]: e.target.value }))
                      }
                    />
                  ))}
                <Button variant="contained" onClick={() => void runScorecard()}>
                  Evaluate scorecard
                </Button>
                <Button variant="outlined" onClick={() => void loadScoreHistory()} sx={{ ml: 1 }}>
                  Load scorecard history
                </Button>
              </Box>

              {scoreHistory.length > 0 && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Result</TableCell>
                      <TableCell>Template</TableCell>
                      <TableCell>Score</TableCell>
                      <TableCell>Scored at</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {scoreHistory.map((s) => (
                      <TableRow key={s.resultId}>
                        <TableCell>{s.resultId.slice(0, 8)}…</TableCell>
                        <TableCell>{s.templateId}</TableCell>
                        <TableCell>{s.overallScore ?? '—'} / {s.maxScore ?? '—'}</TableCell>
                        <TableCell>{s.scoredAt ?? s.evaluatedAt ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}

              {scoreResult && (
                <Box>
                  <Typography>
                    Overall score: <strong>{scoreResult.overallScore}</strong>
                  </Typography>
                  <Table size="small" sx={{ mt: 1 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Category</TableCell>
                        <TableCell>Weight %</TableCell>
                        <TableCell>Raw</TableCell>
                        <TableCell>Weighted</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {(scoreResult.categoryBreakdown ?? scoreResult.categoryScores ?? []).map((b) => (
                        <TableRow key={b.categoryCode}>
                          <TableCell>{b.categoryCode}</TableCell>
                          <TableCell>{b.weightPercent}</TableCell>
                          <TableCell>{b.rawScore}</TableCell>
                          <TableCell>{b.weightedScore}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'comparison' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="subtitle2">
                Side-by-side comparison — trade-offs visible; not an approval
              </Typography>
              <TextField
                label="Supplier IDs (comma-separated, ≥2)"
                size="small"
                fullWidth
                value={compareIds}
                onChange={(e) => setCompareIds(e.target.value)}
              />
              <Button variant="contained" onClick={() => void runComparison()}>
                Compare suppliers
              </Button>
              <Button variant="outlined" onClick={() => void loadComparisonHistory()}>
                Load comparison history
              </Button>
              {comparisonHistory.length > 0 && (
                <>
                  <Typography variant="subtitle2">Recent comparison snapshots (decision support only)</Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Suppliers</TableCell>
                        <TableCell>BOM</TableCell>
                        <TableCell>Program</TableCell>
                        <TableCell>Created</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {comparisonHistory.map((h) => (
                        <TableRow key={h.comparisonId}>
                          <TableCell>{h.comparisonId.slice(0, 8)}…</TableCell>
                          <TableCell>{h.supplierIdsCsv}</TableCell>
                          <TableCell>{h.bomNodeId ?? '—'}</TableCell>
                          <TableCell>{h.programId ?? '—'}</TableCell>
                          <TableCell>{h.createdAt ?? '—'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </>
              )}
              {comparison && (
                <>
                  {comparison.tradeOffSummary && (
                    <Alert severity="info">{comparison.tradeOffSummary}</Alert>
                  )}
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Supplier</TableCell>
                        <TableCell>Eligible</TableCell>
                        <TableCell>Score</TableCell>
                        <TableCell>TCO</TableCell>
                        <TableCell>PPM</TableCell>
                        <TableCell>OTD %</TableCell>
                        <TableCell>Warranty mo</TableCell>
                        <TableCell>Gaps / risk</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {comparison.rows.map((r) => (
                        <TableRow key={r.supplierId}>
                          <TableCell>{r.supplierId}</TableCell>
                          <TableCell>{yesNoChip(r.eligible)}</TableCell>
                          <TableCell>{r.overallScore ?? '—'}</TableCell>
                          <TableCell>
                            {r.tcoTotal ?? '—'} {r.tcoCurrency ?? ''}
                            {r.tcoDataComplete === false && (
                              <Chip size="small" label="incomplete" color="warning" sx={{ ml: 0.5 }} />
                            )}
                          </TableCell>
                          <TableCell>{r.ppm ?? '—'}</TableCell>
                          <TableCell>{r.otdPercent ?? '—'}</TableCell>
                          <TableCell>{r.warrantyMonths ?? '—'}</TableCell>
                          <TableCell>
                            {[...(r.failedGates ?? []), ...(r.missingData ?? []), ...(r.riskFlags ?? [])]
                              .join('; ') || '—'}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'recommendation' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Alert severity="warning">
                AI / system produces DRAFT ranking only. Human approve / reject / override is mandatory.
                No automatic award of business.
              </Alert>
              <Typography variant="subtitle2">Decision queue</Typography>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                <FormControl size="small" sx={{ minWidth: 140 }}>
                  <InputLabel>Status</InputLabel>
                  <Select
                    label="Status"
                    value={recQueueStatus}
                    onChange={(e) => setRecQueueStatus(e.target.value)}
                  >
                    {['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'OVERRIDDEN'].map((s) => (
                      <MenuItem key={s} value={s}>{s}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Button variant="outlined" onClick={() => void loadRecQueue()}>
                  Load queue
                </Button>
              </Stack>
              {recQueue.length > 0 && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>ID</TableCell>
                      <TableCell>Event</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Recommended</TableCell>
                      <TableCell></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recQueue.map((r) => (
                      <TableRow key={r.recommendationId}>
                        <TableCell>{r.recommendationId.slice(0, 8)}…</TableCell>
                        <TableCell>{r.sourcingEventCode ?? '—'}</TableCell>
                        <TableCell>{r.status}</TableCell>
                        <TableCell>{r.recommendedSupplierId ?? '—'}</TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            onClick={() => {
                              setRecIdLoad(r.recommendationId);
                              setRecommendation(r);
                            }}
                          >
                            Open
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
              <Divider />
              <TextField
                label="Sourcing event code"
                size="small"
                value={recEventCode}
                onChange={(e) => setRecEventCode(e.target.value)}
              />
              <TextField
                label="Candidates JSON"
                size="small"
                fullWidth
                multiline
                minRows={3}
                value={recCandidatesJson}
                onChange={(e) => setRecCandidatesJson(e.target.value)}
                helperText="Array of {supplierId, eligible, overallScore, tcoTotal, tcoCurrency, tcoDataComplete}"
              />
              <Stack direction="row" spacing={1}>
                <Button variant="contained" onClick={() => void createRecDraft()}>
                  Create DRAFT
                </Button>
                <TextField
                  label="Load by ID"
                  size="small"
                  value={recIdLoad}
                  onChange={(e) => setRecIdLoad(e.target.value)}
                />
                <Button variant="outlined" onClick={() => void loadRec()}>
                  Load
                </Button>
              </Stack>
              {recommendation && (
                <Box>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <Typography>
                      {recommendation.recommendationId} · status <strong>{recommendation.status}</strong>
                    </Typography>
                    {recommendation.recommendedSupplierId && (
                      <Chip label={`Top: ${recommendation.recommendedSupplierId}`} color="primary" size="small" />
                    )}
                  </Stack>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Rank</TableCell>
                        <TableCell>Supplier</TableCell>
                        <TableCell>Eligible</TableCell>
                        <TableCell>Lifecycle</TableCell>
                        <TableCell>Score</TableCell>
                        <TableCell>TCO</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {recommendation.ranking.map((c) => (
                        <TableRow key={c.candidateId}>
                          <TableCell>{c.rankPosition}</TableCell>
                          <TableCell>{c.supplierId}</TableCell>
                          <TableCell>{yesNoChip(c.eligible)}</TableCell>
                          <TableCell>
                            {c.lifecycleBlocked ? (
                              <Chip size="small" color="error" label={c.lifecycleStatus ?? 'BLOCKED'} />
                            ) : (
                              c.lifecycleStatus ?? '—'
                            )}
                          </TableCell>
                          <TableCell>{c.overallScore ?? '—'}</TableCell>
                          <TableCell>
                            {c.tcoTotal ?? '—'} {c.tcoCurrency ?? ''}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mt: 2 }}>
                    <TextField
                      label="Decision comment"
                      size="small"
                      fullWidth
                      value={decisionComment}
                      onChange={(e) => setDecisionComment(e.target.value)}
                    />
                    <Button variant="outlined" onClick={() => void doSubmitRec()}>
                      Submit
                    </Button>
                    <Button variant="contained" color="success" onClick={() => void doApprove()}>
                      Approve
                    </Button>
                    <Button variant="contained" color="error" onClick={() => void doReject()}>
                      Reject
                    </Button>
                  </Stack>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mt: 2 }}>
                    <TextField
                      label="Override supplier ID"
                      size="small"
                      value={overrideSupplierId}
                      onChange={(e) => setOverrideSupplierId(e.target.value)}
                    />
                    <TextField
                      label="Override reason (mandatory)"
                      size="small"
                      fullWidth
                      value={overrideReason}
                      onChange={(e) => setOverrideReason(e.target.value)}
                    />
                    <Button variant="outlined" color="warning" onClick={() => void doOverride()}>
                      Override
                    </Button>
                    <Button variant="outlined" onClick={() => void loadExplanation()}>
                      Load explainability
                    </Button>
                    <Button variant="outlined" onClick={() => void loadDecisions()}>
                      Decision ledger
                    </Button>
                    <Button variant="outlined" onClick={() => void dispatchDecisionWebhooks()}>
                      Dispatch webhooks
                    </Button>
                  </Stack>
                  {explanation && (
                    <Box sx={{ mt: 2 }}>
                      <Alert severity="warning">{explanation.decisionBoundaryNote}</Alert>
                      <Typography variant="body2" sx={{ mt: 1 }}>
                        <strong>Confidence:</strong> {explanation.confidenceLevel ?? '—'}
                      </Typography>
                      <Typography variant="body2"><strong>Data used:</strong> {explanation.dataUsedSummary}</Typography>
                      <Typography variant="body2"><strong>Eligibility:</strong> {explanation.eligibilitySummary}</Typography>
                      <Typography variant="body2"><strong>Score:</strong> {explanation.scoreSummary}</Typography>
                      <Typography variant="body2"><strong>TCO:</strong> {explanation.tcoSummary}</Typography>
                      <Typography variant="body2"><strong>Strengths:</strong> {explanation.strengthsSummary}</Typography>
                      <Typography variant="body2"><strong>Weaknesses:</strong> {explanation.weaknessesSummary}</Typography>
                      <Typography variant="body2"><strong>Risk:</strong> {explanation.riskSummary}</Typography>
                      <Typography variant="body2"><strong>Missing data:</strong> {explanation.missingDataSummary}</Typography>
                      <Typography variant="body2"><strong>Cost variance:</strong> {explanation.costVarianceNotes ?? '—'}</Typography>
                      <Typography variant="body2"><strong>Negotiation opportunities:</strong> {explanation.negotiationOpportunities?.trim() ? explanation.negotiationOpportunities : 'No data'}</Typography>
                      <Typography variant="body2"><strong>Evidence notes:</strong> {explanation.evidenceNotes ?? '—'}</Typography>
                      <ExplanationClassificationPanel explanation={explanation} />
                    </Box>
                  )}
                  {decisions.length > 0 && (
                    <Box sx={{ mt: 2 }}>
                      <Typography variant="subtitle2">Decision ledger (append-only)</Typography>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>When</TableCell>
                            <TableCell>Action</TableCell>
                            <TableCell>From → To</TableCell>
                            <TableCell>By</TableCell>
                            <TableCell>Comment / override reason</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {decisions.map((d) => (
                            <TableRow key={d.decisionId}>
                              <TableCell>{d.decidedAt}</TableCell>
                              <TableCell>{d.decisionAction}</TableCell>
                              <TableCell>
                                {d.previousStatus} → {d.newStatus}
                              </TableCell>
                              <TableCell>{d.decidedBy ?? '—'}</TableCell>
                              <TableCell>
                                {d.decisionComment ?? '—'}
                                {d.overrideReason ? ` | override: ${d.overrideReason}` : ''}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </Box>
                  )}
                  {decisionEvents.length > 0 && (
                    <Box sx={{ mt: 2 }}>
                      <Typography variant="subtitle2">Decision event outbox</Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Integration feed only — does not approve suppliers automatically.
                      </Typography>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>When</TableCell>
                            <TableCell>Type</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Published</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {decisionEvents.map((e) => (
                            <TableRow key={e.eventId}>
                              <TableCell>{e.occurredAt}</TableCell>
                              <TableCell>{e.eventType}</TableCell>
                              <TableCell>
                                {e.previousStatus} → {e.newStatus}
                              </TableCell>
                              <TableCell>{e.published ? 'YES' : 'NO'}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </Box>
                  )}

                </Box>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'qualification' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="body2" color="text.secondary">
                APPROVED requires a human reason and a recommendation that is APPROVED or OVERRIDDEN for this same supplier.
                The system never auto-approves.
              </Typography>
              <Typography variant="subtitle2">
                Lifecycle: DISCOVERED → … → APPROVAL_PENDING → APPROVED (or REJECTED / BLOCKED)
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button variant="outlined" onClick={() => void loadQual()}>
                  Load current
                </Button>
                <Button variant="outlined" onClick={() => void initQual()}>
                  Initialize
                </Button>
              </Stack>
              {qualification && (
                <Alert severity="info">
                  Status: <strong>{qualification.lifecycleStatus}</strong>
                  {qualification.previousStatus && ` (from ${qualification.previousStatus})`}
                  {qualification.changeReason && ` — ${qualification.changeReason}`}
                </Alert>
              )}
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <FormControl size="small" sx={{ minWidth: 220 }}>
                  <InputLabel>Target status</InputLabel>
                  <Select
                    label="Target status"
                    value={targetStatus}
                    onChange={(e) => setTargetStatus(e.target.value as LifecycleStatus)}
                  >
                    {(
                      [
                        'DISCOVERED',
                        'IMPORTED',
                        'UNDER_VERIFICATION',
                        'QUALIFIED',
                        'EVALUATED',
                        'RECOMMENDED',
                        'APPROVAL_PENDING',
                        'APPROVED',
                        'REJECTED',
                        'BLOCKED',
                        'SUSPENDED',
                      ] as LifecycleStatus[]
                    ).map((s) => (
                      <MenuItem key={s} value={s}>
                        {s}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  label="Reason"
                  size="small"
                  fullWidth
                  value={qualReason}
                  onChange={(e) => setQualReason(e.target.value)}
                />
                <TextField
                  label="Recommendation ID"
                  size="small"
                  fullWidth
                  value={qualRecommendationId}
                  onChange={(e) => setQualRecommendationId(e.target.value)}
                  helperText="Required for RECOMMENDED / APPROVAL_PENDING / APPROVED"
                />
                <Button variant="contained" onClick={() => void doTransition()}>
                  Transition
                </Button>
              </Stack>
              <Stack direction="row" spacing={1}>
                <Button variant="outlined" onClick={() => void loadApprovalChecks()}>
                  Load approval gate checks
                </Button>
                <Button variant="outlined" onClick={() => void loadLifecycleEvents()}>
                  Load lifecycle events
                </Button>
                <Button variant="outlined" onClick={() => void loadOperationBlocks()}>
                  Load scorecard/TCO blocks
                </Button>
              </Stack>
              {approvalChecks.length > 0 && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>When</TableCell>
                      <TableCell>Recommendation</TableCell>
                      <TableCell>Rec status</TableCell>
                      <TableCell>Passed</TableCell>
                      <TableCell>Reason</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {approvalChecks.map((c) => (
                      <TableRow key={c.checkId}>
                        <TableCell>{c.checkedAt}</TableCell>
                        <TableCell>{c.recommendationId}</TableCell>
                        <TableCell>{c.recommendationStatus}</TableCell>
                        <TableCell>{c.checkPassed ? 'YES' : 'NO'}</TableCell>
                        <TableCell>{c.failureReason ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}

              {operationBlocks.length > 0 && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>When</TableCell>
                      <TableCell>Operation</TableCell>
                      <TableCell>Lifecycle</TableCell>
                      <TableCell>Reason</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {operationBlocks.map((b) => (
                      <TableRow key={b.blockId}>
                        <TableCell>{b.attemptedAt}</TableCell>
                        <TableCell>{b.operationCode}</TableCell>
                        <TableCell>{b.lifecycleStatus}</TableCell>
                        <TableCell>{b.blockReason}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
              {lifecycleEvents.length > 0 && (
                <Box>
                  <Typography variant="subtitle2">Lifecycle event outbox</Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Integration feed only — does not approve suppliers automatically.
                  </Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>When</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Published</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {lifecycleEvents.map((e) => (
                        <TableRow key={e.eventId}>
                          <TableCell>{e.occurredAt}</TableCell>
                          <TableCell>{e.eventType}</TableCell>
                          <TableCell>
                            {(e.previousStatus ?? '—') + ' → ' + e.newStatus}
                          </TableCell>
                          <TableCell>{e.published ? 'YES' : 'NO'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {tab === 'risk' && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="subtitle2">
                Risk register is independent of score. ACCEPTED status requires documented rationale.
              </Typography>
              <Button variant="outlined" onClick={() => void loadRisks()}>
                Load risk summary
              </Button>
              {riskSummary && (
                <Stack direction="row" spacing={1}>
                  <Chip label={`Open: ${riskSummary.openCount}`} />
                  <Chip
                    label={`Critical: ${riskSummary.criticalOpenCount}`}
                    color="error"
                    variant="outlined"
                  />
                  <Chip
                    label={`High: ${riskSummary.highOpenCount}`}
                    color="warning"
                    variant="outlined"
                  />
                </Stack>
              )}
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Title</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell>Severity</TableCell>
                    <TableCell>Likelihood</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {risks.map((r) => (
                    <TableRow key={r.riskId}>
                      <TableCell>{r.riskTitle}</TableCell>
                      <TableCell>{r.riskCategory}</TableCell>
                      <TableCell>{r.severity}</TableCell>
                      <TableCell>{r.likelihood}</TableCell>
                      <TableCell>{r.status}</TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={0.5}>
                          <Button size="small" onClick={() => void setRiskStatus(r.riskId, 'MITIGATING')}>
                            Mitigate
                          </Button>
                          <Button size="small" onClick={() => void setRiskStatus(r.riskId, 'ACCEPTED')}>
                            Accept
                          </Button>
                          <Button size="small" onClick={() => void setRiskStatus(r.riskId, 'CLOSED')}>
                            Close
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Divider />
              <Typography variant="subtitle2">Register new risk</Typography>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  label="Title"
                  size="small"
                  value={riskTitle}
                  onChange={(e) => setRiskTitle(e.target.value)}
                />
                <FormControl size="small" sx={{ minWidth: 160 }}>
                  <InputLabel>Category</InputLabel>
                  <Select
                    label="Category"
                    value={riskCategory}
                    onChange={(e) => setRiskCategory(e.target.value as RiskCategory)}
                  >
                    {(
                      [
                        'FINANCIAL',
                        'QUALITY',
                        'DELIVERY',
                        'CAPACITY',
                        'GEOGRAPHIC',
                        'SUPPLY_CHAIN',
                        'BUSINESS_CONTINUITY',
                        'COMPLIANCE',
                        'OTHER',
                      ] as RiskCategory[]
                    ).map((c) => (
                      <MenuItem key={c} value={c}>
                        {c}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl size="small" sx={{ minWidth: 120 }}>
                  <InputLabel>Severity</InputLabel>
                  <Select
                    label="Severity"
                    value={riskSeverity}
                    onChange={(e) => setRiskSeverity(e.target.value as RiskSeverity)}
                  >
                    {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as RiskSeverity[]).map((s) => (
                      <MenuItem key={s} value={s}>
                        {s}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl size="small" sx={{ minWidth: 120 }}>
                  <InputLabel>Likelihood</InputLabel>
                  <Select
                    label="Likelihood"
                    value={riskLikelihood}
                    onChange={(e) => setRiskLikelihood(e.target.value as RiskLikelihood)}
                  >
                    {(['HIGH', 'MEDIUM', 'LOW'] as RiskLikelihood[]).map((s) => (
                      <MenuItem key={s} value={s}>
                        {s}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Stack>
              <TextField
                label="Description"
                size="small"
                fullWidth
                value={riskDesc}
                onChange={(e) => setRiskDesc(e.target.value)}
              />
              <TextField
                label="Mitigation plan (also used as ACCEPTED rationale)"
                size="small"
                fullWidth
                value={riskMitigation}
                onChange={(e) => setRiskMitigation(e.target.value)}
              />
              <Button variant="contained" onClick={() => void addRisk()}>
                Create risk
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}
      {tab === 'config' && (
        <Card>
          <CardContent>
            <Stack spacing={3}>
              <Box>
                <Typography variant="h6">Vendor Selection configuration</Typography>
                <Typography variant="body2" color="text.secondary">
                  Configure scorecard weights, questionnaire bank, supplier capabilities, and policy evidence.
                  Score does not override mandatory gates. Unverified policy declarations are not treated as verified.
                </Typography>
              </Box>

              <Box>
                <Typography variant="subtitle1">A. Scorecard weights</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Edit category weights (must total 100%) on the Scorecard tab — clone VS-DEFAULT-100 into a customer template.
                  Used templates cannot change weights; deactivate to stop new evaluations.
                </Typography>
                <Stack direction="row" spacing={1}>
                  <Button variant="outlined" onClick={() => setTab('scorecard')}>
                    Open Scorecard configuration
                  </Button>
                  <Button variant="outlined" onClick={() => void loadTemplates()}>
                    Load templates
                  </Button>
                  <Button variant="outlined" color="warning" onClick={() => void deactivateSelectedTemplate()}>
                    Deactivate selected template
                  </Button>
                  <Button variant="outlined" onClick={() => void loadLineage()}>
                    Show lineage
                  </Button>
                </Stack>
                {lineageRows.length > 0 && (
                  <Alert severity="info" sx={{ mb: 1 }}>
                    Lineage: {lineageRows.map((r) => `${r.templateCode} v${r.versionNo ?? 1}`).join(' → ')}
                  </Alert>
                )}
                {templates.length > 0 && (
                  <Table size="small" sx={{ mt: 1 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Code</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Version</TableCell>
                        <TableCell>Categories</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {templates.map((t) => (
                        <TableRow
                          key={t.templateId}
                          hover
                          selected={t.templateId === templateId}
                          onClick={() => setTemplateId(t.templateId)}
                          sx={{ cursor: 'pointer' }}
                        >
                          <TableCell>{t.templateCode}</TableCell>
                          <TableCell>{t.templateName}</TableCell>
                          <TableCell>{t.status}</TableCell>
                          <TableCell>{t.versionNo ?? 1}{t.versionLabel ? ` (${t.versionLabel})` : ''}</TableCell>
                          <TableCell>{t.categories.map((c) => `${c.categoryCode}:${c.weightPercent}%`).join(', ')}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle1">B. Questionnaire bank</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Add or version questions. Same question code creates a new version (history preserved). Mandatory gate
                  questions can fail eligibility when questionnaire gate is required.
                </Typography>
                <Stack spacing={1} sx={{ mb: 1 }}>
                  <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} alignItems="center">
                    <FormControl size="small" sx={{ minWidth: 160 }}>
                      <InputLabel>Filter category</InputLabel>
                      <Select
                        label="Filter category"
                        value={qFilterCategory}
                        onChange={(e) => setQFilterCategory(e.target.value)}
                      >
                        <MenuItem value="">All categories</MenuItem>
                        {['QUALITY','COST','DELIVERY','TECHNICAL','CAPACITY','WARRANTY','GUARANTEE','SERVICE','HISTORY','POLICY','RISK','COMPLIANCE','OTHER'].map((c) => (
                          <MenuItem key={c} value={c}>{c}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                    <Button variant="outlined" onClick={() => void loadConfigQuestions()}>
                      Load active questions
                    </Button>
                    <Typography variant="caption" color="text.secondary">
                      Seed codes VS-Q-AS-*, VS-Q-WC-*, VS-Q-GE-* highlighted
                    </Typography>
                  </Stack>
                  <Stack direction="row" spacing={0.5} flexWrap="wrap">
                    {(['', 'SERVICE', 'WARRANTY', 'GUARANTEE', 'QUALITY', 'DELIVERY'] as const).map((c) => (
                      <Chip
                        key={c || 'ALL'}
                        size="small"
                        label={c || 'ALL'}
                        color={qFilterCategory === c ? 'primary' : 'default'}
                        variant={qFilterCategory === c ? 'filled' : 'outlined'}
                        onClick={() => {
                          setQFilterCategory(c);
                          // load after state update via dedicated path
                          void (async () => {
                            try {
                              const list = await listQuestionnaireQuestions(c || undefined);
                              setConfigQuestions(list);
                              const seedCount = list.filter((q) =>
                                q.questionCode.startsWith('VS-Q-AS-')
                                || q.questionCode.startsWith('VS-Q-WC-')
                                || q.questionCode.startsWith('VS-Q-GE-'),
                              ).length;
                              setInfo(
                                `${list.length} active question(s)`
                                + (c ? ` in ${c}` : '')
                                + (seedCount ? ` · ${seedCount} seed(s)` : ''),
                              );
                              setError(null);
                            } catch (e: unknown) {
                              setError(e instanceof Error ? e.message : 'Failed to load questions');
                            }
                          })();
                        }}
                      />
                    ))}
                  </Stack>
                </Stack>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mb: 1 }}>
                  <TextField size="small" label="Question code" value={qCode} onChange={(e) => setQCode(e.target.value)} placeholder="VS-Q-CUSTOM-1" />
                  <FormControl size="small" sx={{ minWidth: 140 }}>
                    <InputLabel>Category</InputLabel>
                    <Select label="Category" value={qCategory} onChange={(e) => setQCategory(e.target.value)}>
                      {['QUALITY','COST','DELIVERY','TECHNICAL','CAPACITY','WARRANTY','GUARANTEE','SERVICE','HISTORY','POLICY','RISK','COMPLIANCE','OTHER'].map((c) => (
                        <MenuItem key={c} value={c}>{c}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <FormControl size="small" sx={{ minWidth: 140 }}>
                    <InputLabel>Answer type</InputLabel>
                    <Select label="Answer type" value={qAnswerType} onChange={(e) => setQAnswerType(e.target.value)}>
                      {['YES_NO','NUMBER','PERCENTAGE','CURRENCY','DATE','TEXT','RATING','DOCUMENT'].map((c) => (
                        <MenuItem key={c} value={c}>{c}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField size="small" label="Weight" value={qWeight} onChange={(e) => setQWeight(e.target.value)} sx={{ width: 90 }} />
                </Stack>
                <TextField size="small" fullWidth label="Question text" value={qText} onChange={(e) => setQText(e.target.value)} sx={{ mb: 1 }} />
                <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                  <FormControl size="small" sx={{ minWidth: 120 }}>
                    <InputLabel>Required</InputLabel>
                    <Select label="Required" value={qRequired ? 'yes' : 'no'} onChange={(e) => setQRequired(e.target.value === 'yes')}>
                      <MenuItem value="yes">Yes</MenuItem>
                      <MenuItem value="no">No</MenuItem>
                    </Select>
                  </FormControl>
                  <FormControl size="small" sx={{ minWidth: 140 }}>
                    <InputLabel>Evidence required</InputLabel>
                    <Select label="Evidence required" value={qEvidence ? 'yes' : 'no'} onChange={(e) => setQEvidence(e.target.value === 'yes')}>
                      <MenuItem value="yes">Yes</MenuItem>
                      <MenuItem value="no">No</MenuItem>
                    </Select>
                  </FormControl>
                  <FormControl size="small" sx={{ minWidth: 140 }}>
                    <InputLabel>Mandatory gate</InputLabel>
                    <Select label="Mandatory gate" value={qGate ? 'yes' : 'no'} onChange={(e) => setQGate(e.target.value === 'yes')}>
                      <MenuItem value="yes">Yes</MenuItem>
                      <MenuItem value="no">No</MenuItem>
                    </Select>
                  </FormControl>
                  <Button variant="contained" onClick={() => void saveNewQuestion()}>
                    Save question
                  </Button>
                </Stack>
                {configQuestions.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Code</TableCell>
                        <TableCell>Ver</TableCell>
                        <TableCell>Category</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Req</TableCell>
                        <TableCell>Gate</TableCell>
                        <TableCell>Weight</TableCell>
                        <TableCell>Text</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {configQuestions.map((q) => (
                        <TableRow key={q.questionId} sx={isSeedQuestion(q.questionCode) ? { bgcolor: 'action.hover' } : undefined}>
                          <TableCell>
                            {q.questionCode}
                            {isSeedQuestion(q.questionCode) && (
                              <Chip size="small" label="SEED" color="info" variant="outlined" sx={{ ml: 1 }} />
                            )}
                          </TableCell>
                          <TableCell>{q.versionNo}</TableCell>
                          <TableCell>{q.category}</TableCell>
                          <TableCell>{q.answerType}</TableCell>
                          <TableCell>{q.requiredFlag ? 'Y' : 'N'}</TableCell>
                          <TableCell>{q.mandatoryGate ? 'Y' : 'N'}</TableCell>
                          <TableCell>{q.weight ?? '—'}</TableCell>
                          <TableCell>{q.questionText}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle1">C. Supplier capabilities</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Register process/part capability for the selected supplier (used by eligibility capability and capacity gates).
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                  <Button variant="outlined" onClick={() => void loadConfigCaps()}>
                    Load capabilities
                  </Button>
                </Stack>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mb: 1 }}>
                  <FormControl size="small" sx={{ minWidth: 120 }}>
                    <InputLabel>Type</InputLabel>
                    <Select label="Type" value={capType} onChange={(e) => setCapType(e.target.value as CapabilityType)}>
                      {['PROCESS','PART','COMMODITY','PLANT'].map((c) => (
                        <MenuItem key={c} value={c}>{c}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField size="small" label="Capability code" value={capCode} onChange={(e) => setCapCode(e.target.value)} placeholder="STAMPING" />
                  <TextField size="small" label="Name" value={capName} onChange={(e) => setCapName(e.target.value)} fullWidth />
                  <TextField size="small" label="Max annual capacity" value={capCapacity} onChange={(e) => setCapCapacity(e.target.value)} />
                  <FormControl size="small" sx={{ minWidth: 120 }}>
                    <InputLabel>Certified</InputLabel>
                    <Select label="Certified" value={capCertified ? 'yes' : 'no'} onChange={(e) => setCapCertified(e.target.value === 'yes')}>
                      <MenuItem value="yes">Yes</MenuItem>
                      <MenuItem value="no">No</MenuItem>
                    </Select>
                  </FormControl>
                  <Button variant="contained" onClick={() => void saveCapability()}>
                    Save capability
                  </Button>
                </Stack>
                {configCaps.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Code</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Capacity</TableCell>
                        <TableCell>Certified</TableCell>
                        <TableCell>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {configCaps.map((c) => (
                        <TableRow key={c.capabilityId}>
                          <TableCell>{c.capabilityCode}</TableCell>
                          <TableCell>{c.capabilityName}</TableCell>
                          <TableCell>{c.capabilityType}</TableCell>
                          <TableCell>{c.maxAnnualCapacity ?? '—'}</TableCell>
                          <TableCell>{c.certified ? 'Y' : 'N'}</TableCell>
                          <TableCell>{c.status}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle1">D. Policy / compliance evidence</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Record policy evidence for the selected supplier. Status starts as PENDING — verification is a separate step.
                  Only VERIFIED non-expired evidence satisfies POLICY_COMPLIANCE gate.
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mb: 1 }} flexWrap="wrap" alignItems="center">
                  <FormControl size="small" sx={{ minWidth: 160 }}>
                    <InputLabel>Evidence</InputLabel>
                    <Select
                      label="Evidence"
                      value={policyEvidenceFilter}
                      onChange={(e) => setPolicyEvidenceFilter(e.target.value)}
                    >
                      <MenuItem value="">All</MenuItem>
                      <MenuItem value="PENDING">PENDING</MenuItem>
                      <MenuItem value="VERIFIED">VERIFIED</MenuItem>
                      <MenuItem value="FAILED">FAILED</MenuItem>
                      <MenuItem value="EXPIRED">EXPIRED</MenuItem>
                      <MenuItem value="WAIVED">WAIVED</MenuItem>
                    </Select>
                  </FormControl>
                  <FormControl size="small" sx={{ minWidth: 200 }}>
                    <InputLabel>Policy type</InputLabel>
                    <Select
                      label="Policy type"
                      value={policyTypeFilter}
                      onChange={(e) => setPolicyTypeFilter(e.target.value)}
                    >
                      <MenuItem value="">All</MenuItem>
                      <MenuItem value="QUALITY_POLICY">QUALITY_POLICY</MenuItem>
                      <MenuItem value="ENVIRONMENTAL_POLICY">ENVIRONMENTAL_POLICY</MenuItem>
                      <MenuItem value="SAFETY_POLICY">SAFETY_POLICY</MenuItem>
                      <MenuItem value="BUSINESS_CONTINUITY">BUSINESS_CONTINUITY</MenuItem>
                      <MenuItem value="ETHICAL_SOURCING">ETHICAL_SOURCING</MenuItem>
                      <MenuItem value="ANTI_BRIBERY">ANTI_BRIBERY</MenuItem>
                      <MenuItem value="INFORMATION_SECURITY">INFORMATION_SECURITY</MenuItem>
                      <MenuItem value="OTHER">OTHER</MenuItem>
                    </Select>
                  </FormControl>
                  <Button variant="outlined" onClick={() => void loadConfigPolicies()}>
                    Load policies
                  </Button>
                </Stack>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mb: 1 }}>
                  <FormControl size="small" sx={{ minWidth: 200 }}>
                    <InputLabel>Policy type</InputLabel>
                    <Select label="Policy type" value={policyType} onChange={(e) => setPolicyType(e.target.value as PolicyType)}>
                      {['QUALITY_POLICY','ENVIRONMENTAL_POLICY','SAFETY_POLICY','BUSINESS_CONTINUITY','ETHICAL_SOURCING','ANTI_BRIBERY','INFORMATION_SECURITY','RESPONSIBLE_SOURCING','CONFLICT_OF_INTEREST','SUBCONTRACTOR_CONTROL','SUSTAINABILITY','REGULATORY_COMPLIANCE','MATERIAL_COMPLIANCE','OTHER'].map((c) => (
                        <MenuItem key={c} value={c}>{c}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField size="small" label="Policy title" value={policyTitle} onChange={(e) => setPolicyTitle(e.target.value)} fullWidth />
                  <Button variant="contained" onClick={() => void savePolicy()}>
                    Add policy evidence
                  </Button>
                </Stack>
                {policyItems.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Type</TableCell>
                        <TableCell>Title</TableCell>
                        <TableCell>Evidence</TableCell>
                        <TableCell>Expiry</TableCell>
                        <TableCell>Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {policyItems.map((p) => (
                        <TableRow key={p.policyId}>
                          <TableCell>{p.policyType}</TableCell>
                          <TableCell>{p.policyTitle}</TableCell>
                          <TableCell>{evidenceChip(p.evidenceStatus)}</TableCell>
                          <TableCell>{p.expiryDate ?? '—'}</TableCell>
                          <TableCell>
                            <Stack direction="row" spacing={0.5}>
                              <Button size="small" onClick={() => void verifyPolicyRow(p.policyId, 'VERIFIED')}>
                                Verify
                              </Button>
                              <Button size="small" color="error" onClick={() => void verifyPolicyRow(p.policyId, 'FAILED')}>
                                Fail
                              </Button>
                            </Stack>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle1">E. Answer questionnaire (selected supplier)</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Load the active question bank, enter answers for the selected supplier, then evaluate.
                  YES_NO: type YES or NO. Evidence ref required when the question demands evidence.
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                  <Button variant="outlined" onClick={() => void loadConfigQuestions()}>
                    Load questions
                  </Button>
                  <Button variant="outlined" onClick={() => void loadConfigAnswers()}>
                    Load answers
                  </Button>
                  <Button variant="contained" onClick={() => void runConfigQuestionnaireEval()}>
                    Evaluate questionnaire
                  </Button>
                </Stack>
                {qEval && (
                  <Alert severity={qEval.overallEligible ? 'success' : 'warning'} sx={{ mb: 1 }}>
                    Eligible={String(qEval.overallEligible)} · gates={String(qEval.mandatoryGatesPassed)} · answered={qEval.answeredCount}/{qEval.totalActiveQuestions}
                    {qEval.failedGateCodes?.length ? ` · failed: ${qEval.failedGateCodes.join(',')}` : ''}
                  </Alert>
                )}
                {configQuestions.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Code</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Answer</TableCell>
                        <TableCell>Evidence ref</TableCell>
                        <TableCell></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {configQuestions.map((q) => (
                        <TableRow key={q.questionId}>
                          <TableCell>
                            {q.questionCode}
                            {q.mandatoryGate ? ' [GATE]' : ''}
                          </TableCell>
                          <TableCell>{q.answerType}</TableCell>
                          <TableCell>
                            <TextField
                              size="small"
                              fullWidth
                              placeholder={q.answerType === 'YES_NO' ? 'YES / NO' : 'value'}
                              value={answerDrafts[q.questionId] ?? ''}
                              onChange={(e) =>
                                setAnswerDrafts((prev) => ({ ...prev, [q.questionId]: e.target.value }))
                              }
                            />
                          </TableCell>
                          <TableCell>
                            <TextField
                              size="small"
                              fullWidth
                              placeholder={q.evidenceRequired ? 'required' : 'optional'}
                              value={answerEvidence[q.questionId] ?? ''}
                              onChange={(e) =>
                                setAnswerEvidence((prev) => ({ ...prev, [q.questionId]: e.target.value }))
                              }
                            />
                          </TableCell>
                          <TableCell>
                            <Button size="small" variant="contained" onClick={() => void submitAnswerForQuestion(q)}>
                              Save
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
                {configAnswers.length > 0 && (
                  <Box sx={{ mt: 1 }}>
                    <Typography variant="caption">Recent answers</Typography>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Code</TableCell>
                          <TableCell>Ver</TableCell>
                          <TableCell>Answer</TableCell>
                          <TableCell>Gate</TableCell>
                          <TableCell>Evidence</TableCell>
                          <TableCell>Actions</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {configAnswers.map((a) => (
                          <TableRow key={a.responseId}>
                            <TableCell>{a.questionCode}</TableCell>
                            <TableCell>{a.questionVersion}</TableCell>
                            <TableCell>
                              {a.answerText ?? a.answerNumber ?? (a.answerBoolean == null ? a.answerDate : String(a.answerBoolean)) ?? '—'}
                            </TableCell>
                            <TableCell>{a.gatePassed == null ? '—' : a.gatePassed ? 'PASS' : 'FAIL'}</TableCell>
                            <TableCell>{evidenceChip(a.evidenceStatus)}</TableCell>
                            <TableCell>
                              <Stack direction="row" spacing={0.5}>
                                <Button size="small" onClick={() => void verifyAnswerEvidence(a.responseId, 'VERIFIED')}>
                                  Verify
                                </Button>
                                <Button size="small" color="error" onClick={() => void verifyAnswerEvidence(a.responseId, 'FAILED')}>
                                  Fail
                                </Button>
                                <Button size="small" color="warning" onClick={() => void verifyAnswerEvidence(a.responseId, 'WAIVED')}>
                                  Waive
                                </Button>
                              </Stack>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Box>
                )}
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle1">G. Evidence expiry scheduler</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Daily job marks past-expiry policy/warranty/guarantee evidence as EXPIRED (does not invent other statuses).
                  Can be disabled in DB without redeploy. Manual run remains available.
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                  <Button variant="outlined" onClick={() => void loadExpiryJob()}>
                    Load job status
                  </Button>
                  <Button variant="contained" onClick={() => void runExpiryNow()}>
                    Run expiry now
                  </Button>
                  <Button variant="outlined" onClick={() => void toggleExpiryJob(true)}>
                    Enable job
                  </Button>
                  <Button variant="outlined" color="warning" onClick={() => void toggleExpiryJob(false)}>
                    Disable job
                  </Button>
                </Stack>
                {expiryJob && (
                  <Alert severity={expiryJob.enabled ? 'info' : 'warning'} sx={{ mb: 1 }}>
                    Job {expiryJob.jobName}: enabled={String(expiryJob.enabled)} · lastSuccess={String(expiryJob.lastSuccess)} · lastRun={expiryJob.lastRunId ?? '—'}
                    {expiryJob.lastError ? ` · error: ${expiryJob.lastError}` : ''}
                  </Alert>
                )}
                {expiryRuns.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>When</TableCell>
                        <TableCell>Source</TableCell>
                        <TableCell>As of</TableCell>
                        <TableCell>Policies</TableCell>
                        <TableCell>Warranties</TableCell>
                        <TableCell>Guarantees</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {expiryRuns.map((r) => (
                        <TableRow key={r.runId}>
                          <TableCell>{r.ranAt}</TableCell>
                          <TableCell>{r.triggerSource ?? 'MANUAL'}</TableCell>
                          <TableCell>{r.asOfDate}</TableCell>
                          <TableCell>{r.policiesExpired}</TableCell>
                          <TableCell>{r.warrantiesExpired}</TableCell>
                          <TableCell>{r.guaranteesExpired}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>

              <Divider />

              <Box>
                              <Box>
                <Typography variant="subtitle1">H. Capability expiry scheduler</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  ACTIVE capabilities with valid_to in the past are marked EXPIRED (status only; eligibility already ignores past valid_to).
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                  <Button variant="outlined" onClick={() => void loadCapExpiryJob()}>
                    Load capability expiry job
                  </Button>
                  <Button variant="contained" onClick={() => void runCapExpiryNow()}>
                    Run capability expiry now
                  </Button>
                  <Button variant="outlined" onClick={() => void setCapabilityExpiryJobEnabled(true).then(setCapExpiryJob)}>
                    Enable
                  </Button>
                  <Button variant="outlined" color="warning" onClick={() => void setCapabilityExpiryJobEnabled(false).then(setCapExpiryJob)}>
                    Disable
                  </Button>
                </Stack>
                {capExpiryJob && (
                  <Alert severity={capExpiryJob.enabled ? 'info' : 'warning'} sx={{ mb: 1 }}>
                    Job {capExpiryJob.jobName}: enabled={String(capExpiryJob.enabled)} · lastSuccess={String(capExpiryJob.lastSuccess)}
                  </Alert>
                )}
                {capExpiryRuns.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>When</TableCell>
                        <TableCell>Source</TableCell>
                        <TableCell>Expired</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {capExpiryRuns.map((r) => (
                        <TableRow key={r.runId}>
                          <TableCell>{r.ranAt}</TableCell>
                          <TableCell>{r.triggerSource ?? 'MANUAL'}</TableCell>
                          <TableCell>{r.capabilitiesExpired}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>

              <Divider />

<Typography variant="subtitle1">F. Eligibility / risk / recommendation</Typography>
                <Typography variant="body2" color="text.secondary">
                  Eligibility gate flags (required capabilities, policies, questionnaire, open risks) are set per evaluation
                  on the Eligibility tab — not global hard-codes. Risk items are managed on Risk Register. Recommendations
                  stay human-approved on the Recommendation tab (decision ledger + explainability).
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                  <Button variant="outlined" onClick={() => setTab('eligibility')}>Eligibility</Button>
                  <Button variant="outlined" onClick={() => setTab('risk')}>Risk Register</Button>
                  <Button variant="outlined" onClick={() => setTab('recommendation')}>Recommendation</Button>
                </Stack>
              </Box>
            </Stack>
          </CardContent>
        </Card>
      )}

    </Stack>
  );
}
