import {
  AppBar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Toolbar,
  Typography,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import NotificationsOutlinedIcon from '@mui/icons-material/NotificationsOutlined';
import PrecisionManufacturingOutlinedIcon from '@mui/icons-material/PrecisionManufacturingOutlined';
import CurrencyExchangeOutlinedIcon from '@mui/icons-material/CurrencyExchangeOutlined';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import PublicOutlinedIcon from '@mui/icons-material/PublicOutlined';
import TrackChangesOutlinedIcon from '@mui/icons-material/TrackChangesOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import CompareArrowsOutlinedIcon from '@mui/icons-material/CompareArrowsOutlined';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import HandymanOutlinedIcon from '@mui/icons-material/HandymanOutlined';
import TimelineOutlinedIcon from '@mui/icons-material/TimelineOutlined';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import HubOutlinedIcon from '@mui/icons-material/HubOutlined';
import FactoryOutlinedIcon from '@mui/icons-material/FactoryOutlined';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import TrendingUpOutlinedIcon from '@mui/icons-material/TrendingUpOutlined';
import JoinInnerOutlinedIcon from '@mui/icons-material/JoinInnerOutlined';
import BalanceOutlinedIcon from '@mui/icons-material/BalanceOutlined';
import StorefrontOutlinedIcon from '@mui/icons-material/StorefrontOutlined';
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';
import SupportAgentIcon from '@mui/icons-material/SupportAgent';
import Fab from '@mui/material/Fab';
import Tooltip from '@mui/material/Tooltip';
import { useEffect, useState, type ReactNode } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { BackToParentBar } from '../navigation/BackToParentBar';
import { useAuth } from '../auth/useAuth';
import { canAccessPath } from '../auth/roleAccess';
import { HelpingAgentPanel } from '../components/helpingagent/HelpingAgentPanel';
import { SupportSessionBanner } from '../components/SupportSessionBanner';
import { onSupportReadOnlyChange } from '../api/httpClient';
import {
  getHelpingAgentPageContext,
  suggestedPromptsForPath,
} from '../components/helpingagent/helpingAgentContext';

const DRAWER_WIDTH = 260;

interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
}

interface NavGroup {
  id: string;
  label: string;
  items: NavItem[];
}

/** Grouped navigation — reduces cognitive load for business users. */
const NAV_GROUPS: NavGroup[] = [
  {
    id: 'home',
    label: 'Overview',
    items: [
      { label: 'Dashboard', path: '/', icon: <DashboardOutlinedIcon /> },
      { label: 'Notifications', path: '/notifications', icon: <NotificationsOutlinedIcon /> },
      { label: 'Approvals', path: '/approvals', icon: <AssignmentTurnedInOutlinedIcon /> },
    ],
  },
  {
    id: 'masters',
    label: 'Master data',
    items: [
      { label: 'Master Data', path: '/master-data', icon: <Inventory2OutlinedIcon /> },
      { label: 'Materials', path: '/materials', icon: <Inventory2OutlinedIcon /> },
      { label: 'Machines', path: '/machines', icon: <PrecisionManufacturingOutlinedIcon /> },
      { label: 'Currency', path: '/currency', icon: <CurrencyExchangeOutlinedIcon /> },
      { label: 'Import / Export', path: '/import-export', icon: <ImportExportOutlinedIcon /> },
      { label: 'Benchmark Library', path: '/benchmarks', icon: <Inventory2OutlinedIcon /> },
    ],
  },
  {
    id: 'costing',
    label: 'Product costing',
    items: [
      { label: 'Product Costing', path: '/product-costing', icon: <AccountTreeOutlinedIcon /> },
      { label: 'BOM', path: '/bom', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Process Costing', path: '/process', icon: <TimelineOutlinedIcon /> },
      { label: 'Tooling', path: '/tooling', icon: <HandymanOutlinedIcon /> },
      { label: 'Packaging', path: '/packaging', icon: <LocalShippingOutlinedIcon /> },
      { label: 'Should-Cost', path: '/should-cost', icon: <CalculateOutlinedIcon /> },
      { label: 'Universal Costing', path: '/universal-costing', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Import Costing', path: '/import-costing', icon: <LocalShippingOutlinedIcon /> },
      { label: 'FTA / Trade', path: '/fta', icon: <PublicOutlinedIcon /> },
      { label: 'Costing Variants', path: '/costing-variants', icon: <CalculateOutlinedIcon /> },
      { label: 'Quantity Structure', path: '/quantity-structure', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Cost Structure', path: '/cost-structure', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Costing Sheets', path: '/costing-sheets', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Advanced Costing', path: '/advanced-costing', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Scenarios', path: '/scenarios', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Procurement Alternatives', path: '/procurement-alternatives', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Standard Costs', path: '/standard-costs', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Financial Integration', path: '/financial', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Mfg Intelligence', path: '/mfg-intel', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Automotive Intel', path: '/auto-intel', icon: <AccountTreeOutlinedIcon /> },
      { label: 'Lifecycle', path: '/lifecycle', icon: <TrendingUpOutlinedIcon /> },
      { label: 'Mixed Costing', path: '/mixed-costing', icon: <JoinInnerOutlinedIcon /> },
      { label: 'Make vs Buy', path: '/make-vs-buy', icon: <BalanceOutlinedIcon /> },
      { label: 'Target Costing', path: '/target-costing', icon: <TrackChangesOutlinedIcon /> },
      { label: 'Cost Comparison', path: '/cost-comparison', icon: <CompareArrowsOutlinedIcon /> },
      { label: 'Versions', path: '/versions', icon: <HistoryOutlinedIcon /> },
      { label: 'Business Case', path: '/business-case', icon: <TrackChangesOutlinedIcon /> },
    ],
  },
  {
    id: 'purchase',
    label: 'Purchase',
    items: [
      { label: 'Purchase Requests', path: '/purchase-requests', icon: <LocalShippingOutlinedIcon /> },
      { label: 'Purchase Orders', path: '/purchase-orders', icon: <RequestQuoteOutlinedIcon /> },
    ],
  },
  {
    id: 'sourcing',

    label: 'Sourcing',
    items: [
      { label: 'Sourcing', path: '/sourcing', icon: <StorefrontOutlinedIcon /> },
      { label: 'RFQ / Quotes', path: '/rfq', icon: <RequestQuoteOutlinedIcon /> },
      { label: 'Vendor Selection', path: '/vendor-selection', icon: <StorefrontOutlinedIcon /> },
      { label: 'Supplier Portal', path: '/supplier-portal', icon: <StorefrontOutlinedIcon /> },
    ],
  },
  {
    id: 'ops',
    label: 'Operations',
    items: [
      { label: 'Operations', path: '/operations', icon: <AssessmentOutlinedIcon /> },
      { label: 'Reports', path: '/reports', icon: <AssessmentOutlinedIcon /> },
      { label: 'Audit Log', path: '/audit', icon: <HistoryOutlinedIcon /> },
      { label: 'ERP Staging', path: '/erp-staging', icon: <HubOutlinedIcon /> },
      { label: 'SAP Cost Estimates', path: '/sap-cost-estimates', icon: <FactoryOutlinedIcon /> },
    ],
  },

  {
    id: 'customer-admin',
    label: 'Customer Administration',
    items: [
      { label: 'Users', path: '/customer-admin/users', icon: <AdminPanelSettingsOutlinedIcon /> },
      { label: 'Departments', path: '/customer-admin/departments', icon: <AdminPanelSettingsOutlinedIcon /> },
      { label: 'License & Usage', path: '/customer-admin/license-usage', icon: <AdminPanelSettingsOutlinedIcon /> },
      { label: 'Billing', path: '/customer-admin/billing', icon: <AdminPanelSettingsOutlinedIcon /> },
      { label: 'Roles & Permissions', path: '/customer-admin/role-permissions', icon: <ExtensionOutlinedIcon /> },
      { label: 'Audit Log', path: '/audit', icon: <HistoryOutlinedIcon /> },
    ],
  },
];

export function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  const [supportReadOnly, setSupportReadOnly] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { username, logout, authorities, hasRole, hasModule, effectiveAccess, activeCustomerId, setActiveCustomerId } = useAuth();

  useEffect(() => onSupportReadOnlyChange(setSupportReadOnly), []);
  useEffect(() => {
    // Clear banner when leaving tenant context (no X-Customer-Id)
    if (!activeCustomerId) {
      setSupportReadOnly(false);
    }
  }, [activeCustomerId]);
  const showHelpingAgent = hasModule('PRODUCT_COSTING');
  const pageKey = location.pathname.replace(/^\//, '') || 'dashboard';
  const stored = getHelpingAgentPageContext();
  const helpContext = {
    currentPage: pageKey,
    programId: stored.programId || (typeof localStorage !== 'undefined' ? localStorage.getItem('pcs.lastProgramId') ?? undefined : undefined),
    bomNodeId: stored.bomNodeId,
    buildUpId: stored.buildUpId,
    materialCode: stored.materialCode,
    targetCostId: stored.targetCostId,
    partNumber: stored.partNumber,
  };
  const helpPrompts = suggestedPromptsForPath(location.pathname);
  const isPlatformAdmin = hasRole('SYSTEM_ADMINISTRATOR', 'ADMIN');
  const filterItem = (item: NavItem) =>
    canAccessPath(item.path, { hasRole, hasModule });

  const navGroups = NAV_GROUPS.map((g) => ({
    ...g,
    items: g.items.filter(filterItem),
  })).filter((g) => g.items.length > 0);
  const platformItems = isPlatformAdmin
    ? [
        { label: 'Platform Admin', path: '/platform', icon: <AdminPanelSettingsOutlinedIcon /> },
        { label: 'Platform Customers', path: '/platform/customers', icon: <AdminPanelSettingsOutlinedIcon /> },
        { label: 'Platform Modules', path: '/platform/modules', icon: <ExtensionOutlinedIcon /> },
        { label: 'Configuration', path: '/platform/configuration', icon: <ExtensionOutlinedIcon /> },
        { label: 'Platform Users', path: '/platform/users', icon: <AdminPanelSettingsOutlinedIcon /> },
        { label: 'Platform Roles', path: '/platform/roles', icon: <ExtensionOutlinedIcon /> },
        { label: 'Billing', path: '/platform/billing', icon: <ExtensionOutlinedIcon /> },
        { label: 'Support Tickets', path: '/platform/support/tickets', icon: <ExtensionOutlinedIcon /> },
        { label: 'Customer Entitlements', path: '/platform/entitlements', icon: <ExtensionOutlinedIcon /> },
        { label: 'Permissions', path: '/platform/permissions', icon: <ExtensionOutlinedIcon /> },
        { label: 'Access Audit', path: '/platform/access-audit', icon: <HistoryOutlinedIcon /> },
      ]
    : [];

  const drawer = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar sx={{ gap: 1 }}>
        <PrecisionManufacturingOutlinedIcon color="primary" />
        <Box>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
            Product Costing
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Enterprise · Costing & sourcing
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List sx={{ flex: 1, px: 1, overflow: 'auto' }}>
        {navGroups.map((group) => (
          <Box key={group.id} component="li" sx={{ listStyle: 'none' }}>
            <ListSubheader
              disableSticky
              sx={{ bgcolor: 'transparent', lineHeight: '32px', fontWeight: 700, color: 'text.secondary' }}
            >
              {group.label}
            </ListSubheader>
            {group.items.map((item) => {
              const selected =
                item.path === '/'
                  ? location.pathname === '/'
                  : location.pathname.startsWith(item.path);
              return (
                <ListItemButton
                  key={item.path}
                  selected={selected}
                  onClick={() => {
                    navigate(item.path);
                    setMobileOpen(false);
                  }}
                  sx={{ borderRadius: 1, mb: 0.25, py: 0.75 }}
                >
                  <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} primaryTypographyProps={{ variant: 'body2' }} />
                </ListItemButton>
              );
            })}
          </Box>
        ))}
        {platformItems.length > 0 && (
          <Box component="li" sx={{ listStyle: 'none' }}>
            <ListSubheader
              disableSticky
              sx={{ bgcolor: 'transparent', lineHeight: '32px', fontWeight: 700, color: 'text.secondary' }}
            >
              Platform
            </ListSubheader>
            {platformItems.map((item) => {
              const selected = location.pathname.startsWith(item.path);
              return (
                <ListItemButton
                  key={item.path}
                  selected={selected}
                  onClick={() => {
                    navigate(item.path);
                    setMobileOpen(false);
                  }}
                  sx={{ borderRadius: 1, mb: 0.25, py: 0.75 }}
                >
                  <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} primaryTypographyProps={{ variant: 'body2' }} />
                </ListItemButton>
              );
            })}
          </Box>
        )}
      </List>
      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>
          {username}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
          {authorities.map((a) => a.replace(/^ROLE_/, '')).join(', ') || 'Authenticated'}
        </Typography>
        {(effectiveAccess?.customers?.length ?? 0) > 0 && (
            <FormControl size="small" sx={{ minWidth: 160, mr: 1 }}>
              <InputLabel id="tenant-select-label">Tenant</InputLabel>
              <Select
                labelId="tenant-select-label"
                label="Tenant"
                value={activeCustomerId ?? ''}
                onChange={(e) => setActiveCustomerId(e.target.value || null)}
              >
                {(effectiveAccess?.customers ?? []).map((c) => (
                  <MenuItem key={c.customerId} value={c.customerId}>
                    {c.customerCode}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          <Button fullWidth variant="outlined" size="small" onClick={() => void logout().then(() => navigate('/login'))}>
          Sign out
        </Button>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
        }}
      >
        <Toolbar>
          {isMobile && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1 }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="h6" color="text.primary" sx={{ flexGrow: 1 }}>
            Automotive Product Costing Software
          </Typography>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: 8,
        }}
      >
        <SupportSessionBanner visible={supportReadOnly} />
        {/* Hierarchical back + breadcrumbs — never universal "Back to Dashboard" */}
        <BackToParentBar />
        <Outlet />
      </Box>
      {showHelpingAgent && (
        <>
          {/* Opt-in only: icon stays passive until the user clicks — no auto-open or unsolicited chat */}
          <Tooltip title="Costing help — optional. Click only if you want information. The agent does not open by itself." placement="left">
            <Fab
              color="primary"
              aria-label="Optional costing help. Opens only when activated."
              onClick={() => setHelpOpen(true)}
              sx={{ position: 'fixed', bottom: 24, right: 24, zIndex: (t) => t.zIndex.drawer + 1 }}
            >
              <SupportAgentIcon />
            </Fab>
          </Tooltip>
          <HelpingAgentPanel open={helpOpen} onClose={() => setHelpOpen(false)} context={helpContext} suggestedPrompts={helpPrompts} />
        </>
      )}
    </Box>
  );
}

