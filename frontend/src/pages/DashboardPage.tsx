import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  Grid,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { fetchDashboardKpis, fetchProgramCostPosition, type DashboardKpis, type ProgramCostPosition } from '../api/dashboardApi';
import { formatMoney } from '../utils/money';
import { PageHeader, LoadingBlock, ErrorBlock } from '../components/ux';
import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';
import { useAuth } from '../auth/useAuth';
import { PERMISSIONS } from '../auth/permissionMatrix';
import { resolvePersona, type QuickActionDef, type WidgetId, type WorkspaceLink } from '../dashboard/dashboardPersona';

function MetricTile({
  label,
  value,
  subtext,
  to,
  emphasize,
  warn,
}: {
  label: string;
  value: string | number;
  subtext?: string;
  to?: string;
  emphasize?: boolean;
  warn?: boolean;
}) {
  const content = (
    <CardContent>
      <Typography variant="caption" color="text.secondary" fontWeight={600} display="block">
        {label}
      </Typography>
      <Typography
        variant="h4"
        fontWeight={700}
        sx={{ mt: 0.5, color: warn ? 'warning.main' : emphasize ? 'primary.main' : 'text.primary' }}
      >
        {value}
      </Typography>
      {subtext && (
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
          {subtext}
        </Typography>
      )}
    </CardContent>
  );
  return (
    <Card variant="outlined" sx={{ height: '100%', borderColor: warn ? 'warning.main' : undefined }}>
      {to ? (
        <CardActionArea component={RouterLink} to={to} sx={{ height: '100%' }}>
          {content}
        </CardActionArea>
      ) : (
        content
      )}
    </Card>
  );
}

function overallStatus(k: DashboardKpis): { label: string; color: 'success' | 'warning' | 'error' | 'default' } {
  const pressure =
    (k.pendingApprovalSteps ?? 0) +
    (k.targetVariancesExceedingThreshold ?? 0) +
    (k.openSupplierRisks ?? 0) +
    (k.openWarrantyClaims ?? 0) +
    (k.unresolvedGuaranteeEvents ?? 0) +
    (k.draftRecommendations ?? 0) +
    (k.expiringCapabilities ?? 0) +
    (k.openSupportTickets ?? 0) +
    (k.inProgressSupportTickets ?? 0) +
    (k.openRfqs ?? 0) +
    (k.rfqsClosingSoon ?? 0) +
    (k.overduePurchaseOrders ?? 0) +
    (k.incompleteMaterialMasters ?? 0) +
    (k.routingMissingAssignments ?? 0) +
    (k.cycleTimeExceptions ?? 0);
  if (pressure === 0) return { label: 'Stable', color: 'success' };
  if (pressure <= 5) return { label: 'Attention needed', color: 'warning' };
  return { label: 'Action required', color: 'error' };
}

interface ActionItem {
  id: string;
  title: string;
  detail: string;
  to: string;
  count: number;
}

export function DashboardPage() {
  const navigate = useNavigate();
  const { hasPermission, hasModule, hasRole, authorities } = useAuth();
  const persona = useMemo(() => resolvePersona(authorities), [authorities]);

  const can = (permission?: string, module?: string, roles?: readonly string[]) => {
    if (roles && roles.length && !hasRole(...roles)) return false;
    if (module && !hasModule(module)) return false;
    if (permission && !hasPermission(permission)) return false;
    return true;
  };

  const canCosting = hasModule('PRODUCT_COSTING') && (hasPermission(PERMISSIONS.COSTING_VIEW) || hasPermission(PERMISSIONS.SHOULD_COST) || hasPermission(PERMISSIONS.BOM_VIEW));
  const canBom = hasModule('PRODUCT_COSTING') && hasPermission(PERMISSIONS.BOM_VIEW);
  const canVendor = hasModule('VENDOR_SELECTION');
  const canSupport = hasPermission(PERMISSIONS.SUPPORT_TICKET_READ);
  const canRfq = hasPermission(PERMISSIONS.RFQ_VIEW);
  const canPurchase = hasModule('PURCHASE');
  const isPlatform = hasRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'PLATFORM_OWNER');
  const isCustomerAdmin = hasRole('CUSTOMER_ADMIN');

  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [position, setPosition] = useState<ProgramCostPosition | null>(null);

  useEffect(() => {
    setHelpingAgentPageContext({ currentPage: 'dashboard' });
  }, []);

  const load = async () => {
    setLoading(true);
    setError(null);
    setPosition(null);
    try {
      const data = await fetchDashboardKpis();
      setKpis(data);
      const programId =
        typeof localStorage !== 'undefined' ? localStorage.getItem('pcs.lastProgramId') : null;
      if (programId && canCosting) {
        try {
          const pos = await fetchProgramCostPosition(programId);
          setPosition(pos);
        } catch {
          setPosition(null);
        }
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load KPIs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const status = kpis ? overallStatus(kpis) : null;
  const show = (id: WidgetId) => persona.widgets.includes(id);

  const actions: ActionItem[] = useMemo(() => {
    if (!kpis) return [];
    const items: ActionItem[] = [];
    if (show('approvals') && kpis.pendingApprovalSteps > 0) {
      items.push({
        id: 'approvals',
        title: 'Pending approvals',
        detail: `${kpis.pendingApprovalSteps} workflow step(s) waiting`,
        to: '/approvals',
        count: kpis.pendingApprovalSteps,
      });
    }
    if (show('variance') && kpis.targetVariancesExceedingThreshold > 0 && canCosting) {
      items.push({
        id: 'variance',
        title: 'Target variances over threshold',
        detail: `${kpis.targetVariancesExceedingThreshold} target(s) need review`,
        to: '/target-costing',
        count: kpis.targetVariancesExceedingThreshold,
      });
    }
    if (show('notifications') && (kpis.unreadNotifications ?? 0) > 0) {
      items.push({
        id: 'notif',
        title: 'Unread notifications',
        detail: `${kpis.unreadNotifications} for you`,
        to: '/notifications',
        count: kpis.unreadNotifications,
      });
    }
    if (show('vendorRisks') && canVendor && (kpis.openSupplierRisks ?? 0) > 0) {
      items.push({
        id: 'risk',
        title: 'Open supplier risks',
        detail: `${kpis.openSupplierRisks} open risk record(s)`,
        to: '/vendor-selection',
        count: kpis.openSupplierRisks!,
      });
    }
    if (show('vendorCaps') && canVendor && (kpis.expiringCapabilities ?? 0) > 0) {
      items.push({
        id: 'cap',
        title: 'Expiring capabilities',
        detail: `${kpis.expiringCapabilities} capability record(s) near expiry`,
        to: '/vendor-selection',
        count: kpis.expiringCapabilities!,
      });
    }
    if (show('vendorRecs') && canVendor && (kpis.draftRecommendations ?? 0) > 0) {
      items.push({
        id: 'rec',
        title: 'Draft recommendations',
        detail: `${kpis.draftRecommendations} awaiting completion / approval`,
        to: '/vendor-selection',
        count: kpis.draftRecommendations!,
      });
    }
    if (show('vendorWarranty') && canVendor && (kpis.openWarrantyClaims ?? 0) > 0) {
      items.push({
        id: 'war',
        title: 'Open warranty claims',
        detail: `${kpis.openWarrantyClaims} claim(s)`,
        to: '/vendor-selection',
        count: kpis.openWarrantyClaims!,
      });
    }
    if (show('support') && canSupport && ((kpis.openSupportTickets ?? 0) + (kpis.inProgressSupportTickets ?? 0) + (kpis.waitingCustomerSupportTickets ?? 0)) > 0) {
      items.push({
        id: 'support',
        title: 'Open support tickets',
        detail: `${(kpis.openSupportTickets ?? 0) + (kpis.inProgressSupportTickets ?? 0)} needing attention`,
        to: '/support/my-tickets',
        count: (kpis.openSupportTickets ?? 0) + (kpis.inProgressSupportTickets ?? 0),
      });
    }
    if (show('openRfqs') && canRfq && (kpis.openRfqs ?? 0) > 0) {
      items.push({
        id: 'rfq',
        title: 'Open RFQs',
        detail: `${kpis.openRfqs} RFQ(s) in OPEN status`,
        to: '/rfq',
        count: kpis.openRfqs!,
      });
    }
    if (show('rfqsClosingSoon') && canRfq && (kpis.rfqsClosingSoon ?? 0) > 0) {
      items.push({
        id: 'rfqSoon',
        title: 'RFQs closing soon',
        detail: `${kpis.rfqsClosingSoon} due within 7 days`,
        to: '/rfq',
        count: kpis.rfqsClosingSoon!,
      });
    }
    if (show('overdueOrders') && canRfq && canPurchase && (kpis.overduePurchaseOrders ?? 0) > 0) {
      items.push({
        id: 'overduePo',
        title: 'Overdue purchase orders',
        detail: `${kpis.overduePurchaseOrders} issued/partial past required date`,
        to: '/purchase-orders',
        count: kpis.overduePurchaseOrders!,
      });
    }
    if (show('incompleteMasterData') && canCosting && (kpis.incompleteMaterialMasters ?? 0) > 0) {
      items.push({
        id: 'incompleteMat',
        title: 'Incomplete material masters',
        detail: `${kpis.incompleteMaterialMasters} missing grade or base price`,
        to: '/materials',
        count: kpis.incompleteMaterialMasters!,
      });
    }
    if (show('routingExceptions') && canCosting && (kpis.routingMissingAssignments ?? 0) > 0) {
      items.push({
        id: 'routingEx',
        title: 'Routing assignment gaps',
        detail: `${kpis.routingMissingAssignments} operation(s) missing machine or labor`,
        to: '/process',
        count: kpis.routingMissingAssignments!,
      });
    }
    return items;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [kpis, canSupport, canVendor, canCosting, canRfq, canPurchase, persona]);

  const visibleWorkspaces = persona.workspaces.filter((w: WorkspaceLink) => can(w.permission, w.module));
  const visibleQuick = persona.quickActions.filter((a: QuickActionDef) => can(a.permission, a.module, a.roles));

  return (
    <Stack spacing={2.5}>
      <PageHeader
        title={persona.title}
        description={`${persona.purpose} Data is tenant-scoped. Missing permission or disabled module hides the widget.`}
      />
      <Chip size="small" label={`Persona · ${persona.role}`} variant="outlined" sx={{ alignSelf: 'flex-start' }} />

      {loading && <LoadingBlock label="Loading operational KPIs…" />}
      {error && !loading && <ErrorBlock message={error} onRetry={() => void load()} />}

      {kpis && !loading && (
        <>
          {(show('programCost') || show('targetCost') || show('variance') || show('shouldCostHighlight') || show('overallStatus')) && canCosting && (
            <Grid container spacing={2}>
              {show('programCost') && (
                <Grid item xs={12} sm={6} md={4} lg={2}>
                  <MetricTile
                    label="Total cost (program)"
                    value={
                      position?.shouldCostStatus === 'AVAILABLE' || position?.shouldCostStatus === 'PARTIAL'
                        ? formatMoney(position.shouldCostTotal as string | number)
                        : 'NOT AVAILABLE'
                    }
                    subtext={
                      position?.shouldCostStatus === 'AVAILABLE' || position?.shouldCostStatus === 'PARTIAL'
                        ? `${position.programCode} · ${position.reportingCurrency} · coverage ${position.coverage?.coverageRatio ?? '—'}`
                        : 'Select a program (BOM) or open Should-Cost — no invented total'
                    }
                    to="/should-cost"
                  />
                </Grid>
              )}
              {show('targetCost') && (
                <Grid item xs={12} sm={6} md={4} lg={2}>
                  <MetricTile
                    label="Target cost (program)"
                    value={
                      position?.targetStatus === 'AVAILABLE'
                        ? formatMoney(position.targetTotal as string | number)
                        : 'NOT AVAILABLE'
                    }
                    subtext={
                      position?.targetStatus === 'AVAILABLE'
                        ? `Source: ${position.targetSource ?? '—'}`
                        : 'Set VehicleProgram.targetCost or open Target Costing'
                    }
                    to="/target-costing"
                  />
                </Grid>
              )}
              {show('variance') && (
                <Grid item xs={12} sm={6} md={4} lg={2}>
                  <MetricTile
                    label="Variance (over threshold)"
                    value={kpis.targetVariancesExceedingThreshold}
                    subtext={
                      position?.varianceStatus === 'AVAILABLE'
                        ? `Program variance ${formatMoney(position.absoluteVariance as string | number)} (${position.reportingCurrency})`
                        : 'Count of targets exceeding threshold'
                    }
                    to="/target-costing"
                    warn={kpis.targetVariancesExceedingThreshold > 0}
                  />
                </Grid>
              )}
              {show('shouldCostHighlight') && (
                <Grid item xs={12} sm={6} md={4} lg={2}>
                  <MetricTile
                    label="Should-cost records"
                    value={kpis.shouldCostBuildUps}
                    subtext="Build-ups available to review"
                    to="/should-cost"
                    emphasize
                  />
                </Grid>
              )}
              {show('overallStatus') && (
                <Grid item xs={12} sm={6} md={4} lg={2}>
                  <Card variant="outlined" sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="caption" color="text.secondary" fontWeight={600} display="block">
                        Overall status
                      </Typography>
                      <Stack direction="row" alignItems="center" spacing={1} sx={{ mt: 1 }}>
                        {status?.color === 'success' ? (
                          <CheckCircleOutlineIcon color="success" />
                        ) : (
                          <WarningAmberOutlinedIcon color={status?.color === 'error' ? 'error' : 'warning'} />
                        )}
                        <Typography variant="h6" fontWeight={700}>
                          {status?.label}
                        </Typography>
                      </Stack>
                      <Chip
                        size="small"
                        label={status?.label}
                        color={status?.color === 'default' ? 'default' : status?.color}
                        sx={{ mt: 1 }}
                        variant="outlined"
                      />
                    </CardContent>
                  </Card>
                </Grid>
              )}
            </Grid>
          )}

          {show('overallStatus') && !canCosting && (
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>Overall status</Typography>
                <Typography variant="h6">{status?.label ?? 'Stable'}</Typography>
              </CardContent>
            </Card>
          )}

          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                Pending actions
              </Typography>
              {actions.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No data available for pending actions, or nothing requires attention.
                </Typography>
              ) : (
                <List dense disablePadding>
                  {actions.map((a) => (
                    <ListItem key={a.id} disablePadding secondaryAction={<Chip size="small" label={a.count} />}>
                      <ListItemButton onClick={() => navigate(a.to)}>
                        <ListItemText primary={a.title} secondary={a.detail} />
                        <ArrowForwardIcon fontSize="small" color="action" sx={{ ml: 1 }} />
                      </ListItemButton>
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>

          {visibleWorkspaces.length > 0 && (
            <Box>
              <Typography variant="subtitle1" gutterBottom>
                Workspaces
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {visibleWorkspaces.map((l) => (
                  <Button key={l.to + l.label} component={RouterLink} to={l.to} variant="outlined" size="small">
                    {l.label}
                  </Button>
                ))}
              </Stack>
            </Box>
          )}

          <Box>
            <Typography variant="subtitle1" gutterBottom>
              Operational snapshot
            </Typography>
            <Grid container spacing={2}>
              {show('programs') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Vehicle programs" value={kpis.activePrograms} />
                </Grid>
              )}
              {show('materials') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Material masters" value={kpis.materialMasters} to="/materials" />
                </Grid>
              )}
              {show('bom') && canBom && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="BOM nodes" value={kpis.bomNodes} to="/bom" />
                </Grid>
              )}
              {show('shouldCost') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Should-Cost records" value={kpis.shouldCostBuildUps} to="/should-cost" />
                </Grid>
              )}
              {show('targets') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Active targets" value={kpis.activeTargetCosts} to="/target-costing" />
                </Grid>
              )}
              {show('packaging') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Packaging profiles" value={kpis.packagingProfiles} to="/packaging" />
                </Grid>
              )}
              {show('approvals') && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile
                    label="Pending approvals"
                    value={kpis.pendingApprovalSteps}
                    to="/approvals"
                    warn={kpis.pendingApprovalSteps > 0}
                  />
                </Grid>
              )}
              {show('notifications') && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile
                    label="Unread notifications"
                    value={kpis.unreadNotifications}
                    to="/notifications"
                    warn={kpis.unreadNotifications > 0}
                  />
                </Grid>
              )}
              {show('vendorRecs') && canVendor && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile
                    label="Submitted recommendations"
                    value={kpis.submittedRecommendations ?? 0}
                    to="/vendor-selection"
                  />
                </Grid>
              )}
              {show('vendorRisks') && canVendor && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile
                    label="Open supplier risks"
                    value={kpis.openSupplierRisks ?? 0}
                    to="/vendor-selection"
                    warn={(kpis.openSupplierRisks ?? 0) > 0}
                  />
                </Grid>
              )}
              {show('platformAdmin') && isPlatform && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Platform admin" value="Open" subtext="Customers, users, modules" to="/platform" />
                </Grid>
              )}
              {show('customerAdmin') && isCustomerAdmin && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Customer users" value="Open" subtext="This tenant only" to="/customer-admin/users" />
                </Grid>
              )}
              {show('openRfqs') && canRfq && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Open RFQs" value={kpis.openRfqs ?? 0} to="/rfq" warn={(kpis.openRfqs ?? 0) > 0} />
                </Grid>
              )}
              {show('rfqsClosingSoon') && canRfq && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="RFQs closing soon" value={kpis.rfqsClosingSoon ?? 0} to="/rfq" warn={(kpis.rfqsClosingSoon ?? 0) > 0} />
                </Grid>
              )}
              {show('awaitingQuotations') && canRfq && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Awaiting quotations" value={kpis.awaitingQuotations ?? 0} to="/rfq" />
                </Grid>
              )}
              {show('openPurchaseOrders') && canRfq && canPurchase && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Open purchase orders" value={kpis.openPurchaseOrders ?? 0} to="/purchase-orders" />
                </Grid>
              )}
              {show('overdueOrders') && canRfq && canPurchase && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Overdue orders" value={kpis.overduePurchaseOrders ?? 0} to="/purchase-orders" warn={(kpis.overduePurchaseOrders ?? 0) > 0} />
                </Grid>
              )}
              {show('openPurchaseRequests') && canRfq && canPurchase && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Open purchase requests" value={kpis.openPurchaseRequests ?? 0} to="/purchase-requests" />
                </Grid>
              )}
              {show('buyerWorkload') && canRfq && canPurchase && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="My open orders" value={kpis.buyerOpenOrders ?? 0} to="/purchase-orders" />
                </Grid>
              )}
              {show('purchasingSpend') && canRfq && canPurchase && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile
                    label="Purchasing spend (issued+)"
                    value={formatMoney(kpis.purchasingSpend)}
                    to="/purchase-orders"
                  />
                </Grid>
              )}
              {show('incompleteMasterData') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Incomplete materials" value={kpis.incompleteMaterialMasters ?? 0} to="/materials" warn={(kpis.incompleteMaterialMasters ?? 0) > 0} />
                </Grid>
              )}
              {show('draftRoutings') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Draft routings" value={kpis.draftRoutings ?? 0} to="/process" />
                </Grid>
              )}
              {show('routingExceptions') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Routing exceptions" value={kpis.routingMissingAssignments ?? 0} to="/process" warn={(kpis.routingMissingAssignments ?? 0) > 0} />
                </Grid>
              )}
              {show('cycleTimeIssues') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile label="Cycle-time issues" value={kpis.cycleTimeExceptions ?? 0} to="/process" warn={(kpis.cycleTimeExceptions ?? 0) > 0} />
                </Grid>
              )}
              {show('toolingInvestment') && canCosting && (
                <Grid item xs={6} sm={4} md={3}>
                  <MetricTile
                    label="Active tooling investment"
                    value={formatMoney(kpis.toolingInvestment)}
                    subtext={`${kpis.activeToolingRecords ?? 0} active record(s)`}
                    to="/tooling"
                  />
                </Grid>
              )}
            </Grid>
          </Box>

          {show('support') && canSupport && (
            <Box>
              <Typography variant="subtitle1" gutterBottom>
                Support tickets
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={6} sm={3}>
                  <MetricTile
                    label="Open"
                    value={kpis.openSupportTickets ?? 0}
                    to="/support/my-tickets"
                    warn={(kpis.openSupportTickets ?? 0) > 0}
                  />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <MetricTile label="In progress" value={kpis.inProgressSupportTickets ?? 0} to="/support/my-tickets" />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <MetricTile label="Waiting for customer" value={kpis.waitingCustomerSupportTickets ?? 0} to="/support/my-tickets" />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <MetricTile label="Resolved" value={kpis.resolvedSupportTickets ?? 0} to="/support/my-tickets" />
                </Grid>
              </Grid>
            </Box>
          )}

          {visibleQuick.length > 0 && (
            <Box>
              <Typography variant="subtitle1" gutterBottom>
                Quick actions
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {visibleQuick.map((a) => (
                  <Button
                    key={a.id}
                    variant={a.variant === 'contained' ? 'contained' : 'outlined'}
                    color={a.variant === 'secondary' ? 'secondary' : 'primary'}
                    component={RouterLink}
                    to={a.to}
                  >
                    {a.label}
                  </Button>
                ))}
              </Stack>
            </Box>
          )}
        </>
      )}
    </Stack>
  );
}
