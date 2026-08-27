import { httpClient } from './httpClient';

export interface MasterDataHealthReport {
  materialTotal: number;
  materialActive: number;
  machineTotal: number;
  machineActive: number;
  laborTotal: number;
  laborActive: number;
  plantTotal: number;
  programTotal: number;
}

export interface ErpStagingReport {
  totalBatches: number;
  byStatus: Record<string, number>;
}

export interface ShouldCostElementReport {
  buildUpCount: number;
  materialCostTotal: number;
  processCostTotal: number;
  toolingCostTotal: number;
  packagingFreightCostTotal: number;
  overheadCostTotal: number;
  sgaCostTotal: number;
  profitMarginTotal: number;
  grandTotal: number;
}

export interface TargetVarianceReport {
  varianceSnapshots: number;
  exceedingThreshold: number;
  withinThreshold: number;
  sumAbsoluteVariance: number;
  avgPercentageVariance: number;
}

export interface OperationalReport {
  programId: string | null;
  masterData: MasterDataHealthReport;
  erpStaging: ErpStagingReport;
  shouldCostElements: ShouldCostElementReport;
  targetVariance: TargetVarianceReport;
  bomNodeCount: number;
  packagingProfileCount: number;
  activeTargetCosts: number;
  pendingApprovalSteps: number;
  notificationCount: number;
}

export async function fetchOperationalReport(programId?: string): Promise<OperationalReport> {
  const { data } = await httpClient.get<OperationalReport>('/api/v1/reports/operational', {
    params: programId ? { programId } : undefined,
  });
  return data;
}

export async function downloadOperationalReportCsv(programId?: string): Promise<void> {
  const response = await httpClient.get<Blob>('/api/v1/reports/operational/export', {
    params: programId ? { programId } : undefined,
    responseType: 'blob',
  });
  const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = programId
    ? `operational-report-${programId}.csv`
    : 'operational-report-all.csv';
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
