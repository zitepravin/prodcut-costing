import { MySupportTicketsPage } from './pages/support/MySupportTicketsPage';
import { SupportTicketDetailPage } from './pages/support/SupportTicketDetailPage';
import { PlatformSupportTicketsPage } from './pages/platform/PlatformSupportTicketsPage';
import { PlatformSupportTicketDetailPage } from './pages/platform/PlatformSupportTicketDetailPage';
import { CustomerEntitlementsPage } from './pages/platform/CustomerEntitlementsPage';
import { CustomerRolePermissionsPage } from './pages/customer-admin/CustomerRolePermissionsPage';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { AppLayout } from './layout/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { LifecyclePage } from './pages/LifecyclePage';
import { MixedCostingPage } from './pages/MixedCostingPage';
import { MakeVsBuyPage } from './pages/MakeVsBuyPage';
import { PurchaseRequestsPage } from './pages/PurchaseRequestsPage';
import { PurchaseOrdersPage } from './pages/PurchaseOrdersPage';
import { MaterialsPage } from './pages/MaterialsPage';
import { MachinesPage } from './pages/MachinesPage';
import { BomPage } from './pages/BomPage';
import { ShouldCostPage } from './pages/ShouldCostPage';
import { UniversalCostingPage } from './pages/UniversalCostingPage';
import { ImportCostingPage } from './pages/ImportCostingPage';
import { FtaPage } from './pages/FtaPage';
import { ApprovalsPage } from './pages/ApprovalsPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { VersionControlPage } from './pages/VersionControlPage';
import { CurrencyPage } from './pages/CurrencyPage';
import { ProcessCostingPage } from './pages/ProcessCostingPage';
import { ToolingPage } from './pages/ToolingPage';
import { PackagingPage } from './pages/PackagingPage';
import { TargetCostingPage } from './pages/TargetCostingPage';
import { RfqPage } from './pages/RfqPage';
import { CostComparisonPage } from './pages/CostComparisonPage';
import { ImportExportPage } from './pages/ImportExportPage';
import { AuditLogPage } from './pages/AuditLogPage';
import { ErpStagingPage } from './pages/ErpStagingPage';
import { SapCostEstimatePage } from './pages/SapCostEstimatePage';
import { ReportsPage } from './pages/ReportsPage';
import { VendorSelectionPage } from './pages/VendorSelectionPage';
import { PlatformDashboardPage } from './pages/platform/PlatformDashboardPage';
import { PlatformCustomersPage } from './pages/platform/PlatformCustomersPage';
import { PlatformModulesPage } from './pages/platform/PlatformModulesPage';
import { PlatformConfigurationPage } from './pages/platform/PlatformConfigurationPage';
import { CustomerUsersPage } from './pages/customer-admin/CustomerUsersPage';
import { CustomerDepartmentsPage } from './pages/customer-admin/CustomerDepartmentsPage';
import LicenseUsagePage from './pages/customer-admin/LicenseUsagePage';
import CustomerBillingPage from './pages/customer-admin/CustomerBillingPage';
import { PlatformUsersPage } from './pages/platform/PlatformUsersPage';
import { PlatformRolesPage } from './pages/platform/PlatformRolesPage';
import { PlatformBillingPage } from './pages/platform/PlatformBillingPage';
import { PlatformAccessAuditPage } from './pages/platform/PlatformAccessAuditPage';
import { PlatformPermissionsPage } from './pages/platform/PlatformPermissionsPage';
import { ModuleLandingPage } from './pages/ModuleLandingPage';
import { BenchmarkLibraryPage } from './pages/BenchmarkLibraryPage';
import { BusinessCasePage } from './pages/BusinessCasePage';
import { CostingVariantPage } from './pages/CostingVariantPage';
import { QuantityStructurePage } from './pages/QuantityStructurePage';
import { CostStructurePage } from './pages/CostStructurePage';
import { CostingSheetPage } from './pages/CostingSheetPage';
import { AdvancedCostingPage } from './pages/AdvancedCostingPage';
import { ScenarioCostingPage } from './pages/ScenarioCostingPage';
import { ProcurementAlternativesPage } from './pages/ProcurementAlternativesPage';
import { StandardCostPage } from './pages/StandardCostPage';
import { FinancialIntegrationPage } from './pages/FinancialIntegrationPage';
import { MfgIntelligencePage } from './pages/MfgIntelligencePage';
import { AutomotiveIntelPage } from './pages/AutomotiveIntelPage';
import { SupplierPortalPage } from './pages/SupplierPortalPage';
import { appTheme } from './theme/appTheme';

export default function App() {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<DashboardPage />} />
              <Route path="master-data" element={<ModuleLandingPage />} />
              <Route path="product-costing" element={<ModuleLandingPage />} />
              <Route path="sourcing" element={<ModuleLandingPage />} />
              <Route path="operations" element={<ModuleLandingPage />} />
              <Route path="materials" element={<MaterialsPage />} />
              <Route path="bom" element={<BomPage />} />
              <Route path="should-cost" element={<ShouldCostPage />} />
              <Route path="universal-costing" element={<UniversalCostingPage />} />
              <Route path="import-costing" element={<ImportCostingPage />} />
              <Route path="fta" element={<FtaPage />} />
              <Route path="costing-variants" element={<CostingVariantPage />} />
              <Route path="quantity-structure" element={<QuantityStructurePage />} />
              <Route path="cost-structure" element={<CostStructurePage />} />
              <Route path="costing-sheets" element={<CostingSheetPage />} />
              <Route path="advanced-costing" element={<AdvancedCostingPage />} />
              <Route path="scenarios" element={<ScenarioCostingPage />} />
              <Route path="procurement-alternatives" element={<ProcurementAlternativesPage />} />
              <Route path="standard-costs" element={<StandardCostPage />} />
              <Route path="financial" element={<FinancialIntegrationPage />} />
              <Route path="mfg-intel" element={<MfgIntelligencePage />} />
              <Route path="auto-intel" element={<AutomotiveIntelPage />} />
              <Route path="supplier-portal" element={<SupplierPortalPage />} />
              <Route path="lifecycle" element={<LifecyclePage />} />
              <Route path="mixed-costing" element={<MixedCostingPage />} />
              <Route path="make-vs-buy" element={<MakeVsBuyPage />} />
              <Route path="business-case" element={<BusinessCasePage />} />
              <Route path="purchase-requests" element={<PurchaseRequestsPage />} />
              <Route path="purchase-orders" element={<PurchaseOrdersPage />} />
              <Route path="approvals" element={<ApprovalsPage />} />
              <Route path="support/my-tickets" element={<MySupportTicketsPage />} />
              <Route path="support/my-tickets/:ticketId" element={<SupportTicketDetailPage />} />
              <Route path="platform/support/tickets" element={<PlatformSupportTicketsPage />} />
              <Route path="platform/support/tickets/:ticketId" element={<PlatformSupportTicketDetailPage />} />
              <Route path="platform/entitlements" element={<CustomerEntitlementsPage />} />
              <Route path="customer-admin/role-permissions" element={<CustomerRolePermissionsPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="versions" element={<VersionControlPage />} />
              <Route path="machines" element={<MachinesPage />} />
              <Route path="currency" element={<CurrencyPage />} />
              <Route path="benchmarks" element={<BenchmarkLibraryPage />} />
              <Route path="process" element={<ProcessCostingPage />} />
              <Route path="tooling" element={<ToolingPage />} />
              <Route path="packaging" element={<PackagingPage />} />
              <Route path="target-costing" element={<TargetCostingPage />} />
              <Route path="rfq" element={<RfqPage />} />
              <Route path="vendor-selection" element={<VendorSelectionPage />} />
              <Route path="cost-comparison" element={<CostComparisonPage />} />
              <Route path="import-export" element={<ImportExportPage />} />
              <Route path="audit" element={<AuditLogPage />} />
              <Route path="erp-staging" element={<ErpStagingPage />} />
              <Route path="sap-cost-estimates" element={<SapCostEstimatePage />} />
              <Route path="reports" element={<ReportsPage />} />
              <Route path="customer-admin" element={<Navigate to="/customer-admin/users" replace />} />
              <Route path="customer-admin/users" element={<CustomerUsersPage />} />
              <Route path="customer-admin/departments" element={<CustomerDepartmentsPage />} />
              <Route path="customer-admin/license-usage" element={<LicenseUsagePage />} />
              <Route path="customer-admin/billing" element={<CustomerBillingPage />} />
              <Route path="platform" element={<PlatformDashboardPage />} />
              <Route path="platform/customers" element={<PlatformCustomersPage />} />
              <Route path="platform/modules" element={<PlatformModulesPage />} />
              <Route path="platform/configuration" element={<PlatformConfigurationPage />} />
              <Route path="platform/users" element={<PlatformUsersPage />} />
              <Route path="platform/roles" element={<PlatformRolesPage />} />
              <Route path="platform/permissions" element={<PlatformPermissionsPage />} />
              <Route path="platform/access-audit" element={<PlatformAccessAuditPage />} />
              <Route path="platform/billing" element={<PlatformBillingPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}
