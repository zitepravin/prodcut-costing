import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import {
  downloadOperationalReportCsv,
  fetchOperationalReport,
} from '../api/reportsApi';
import type { OperationalReport } from '../api/reportsApi';
import { listPrograms } from '../api/programApi';
import type { VehicleProgram } from '../types/api';
import { getErrorMessage } from '../api/httpClient';

function money(n: number | undefined): string {
  if (n == null || Number.isNaN(n)) return '0.000000';
  return Number(n).toFixed(6);
}

export function ReportsPage() {
  const [report, setReport] = useState<OperationalReport | null>(null);
  const [programs, setPrograms] = useState<VehicleProgram[]>([]);
  const [programId, setProgramId] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exportError, setExportError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    void listPrograms()
      .then(setPrograms)
      .catch(() => setPrograms([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setReport(await fetchOperationalReport(programId || undefined));
    } catch (e) {
      setError(getErrorMessage(e));
      setReport(null);
    } finally {
      setLoading(false);
    }
  }, [programId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function onExport() {
    setExporting(true);
    setExportError(null);
    try {
      await downloadOperationalReportCsv(programId || undefined);
    } catch (e) {
      setExportError(getErrorMessage(e));
    } finally {
      setExporting(false);
    }
  }

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
        <Stack direction="row" spacing={1} alignItems="center" flex={1}>
          <AssessmentOutlinedIcon color="primary" />
          <Typography variant="h5">Reports &amp; analytics</Typography>
        </Stack>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel id="program-filter-label">Program scope</InputLabel>
          <Select
            labelId="program-filter-label"
            label="Program scope"
            value={programId}
            onChange={(e) => setProgramId(e.target.value)}
          >
            <MenuItem value="">All programs</MenuItem>
            {programs.map((p) => (
              <MenuItem key={p.programId} value={p.programId}>
                {p.programCode ?? p.programId}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button
          variant="outlined"
          startIcon={<FileDownloadOutlinedIcon />}
          onClick={() => void onExport()}
          disabled={exporting || loading}
        >
          Export CSV
        </Button>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Program scope filters BOM node counts, should-cost element totals, target variance, and
        active targets. Master-data health and ERP staging remain system-wide.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {exportError && (
        <Alert severity="warning" onClose={() => setExportError(null)}>
          Export: {exportError}
        </Alert>
      )}
      {loading && (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      )}
      {!loading && report && (
        <>
          <Typography variant="caption" color="text.secondary">
            Scope: {report.programId ?? 'ALL programs'}
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6} lg={3}>
              <StatCard title="BOM nodes" value={report.bomNodeCount} />
            </Grid>
            <Grid item xs={12} md={6} lg={3}>
              <StatCard title="Active target costs" value={report.activeTargetCosts} />
            </Grid>
            <Grid item xs={12} md={6} lg={3}>
              <StatCard title="Pending approvals" value={report.pendingApprovalSteps} />
            </Grid>
            <Grid item xs={12} md={6} lg={3}>
              <StatCard title="Notifications" value={report.notificationCount} />
            </Grid>
          </Grid>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Master data health (system-wide)
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Domain</TableCell>
                    <TableCell align="right">Total versions</TableCell>
                    <TableCell align="right">ACTIVE</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>Materials</TableCell>
                    <TableCell align="right">{report.masterData.materialTotal}</TableCell>
                    <TableCell align="right">{report.masterData.materialActive}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Machines</TableCell>
                    <TableCell align="right">{report.masterData.machineTotal}</TableCell>
                    <TableCell align="right">{report.masterData.machineActive}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Labor</TableCell>
                    <TableCell align="right">{report.masterData.laborTotal}</TableCell>
                    <TableCell align="right">{report.masterData.laborActive}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Plants / Programs</TableCell>
                    <TableCell align="right">{report.masterData.plantTotal}</TableCell>
                    <TableCell align="right">{report.masterData.programTotal}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>
                ERP staging batches ({report.erpStaging.totalBatches}) — system-wide
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Count</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {Object.entries(report.erpStaging?.byStatus ?? {}).map(([status, count]) => (
                    <TableRow key={status}>
                      <TableCell>{status}</TableCell>
                      <TableCell align="right">{count}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Should-cost element totals ({report.shouldCostElements.buildUpCount} build-ups)
              </Typography>
              <Table size="small">
                <TableBody>
                  {(
                    [
                      ['Material', report.shouldCostElements.materialCostTotal],
                      ['Process', report.shouldCostElements.processCostTotal],
                      ['Tooling', report.shouldCostElements.toolingCostTotal],
                      ['Packaging / freight', report.shouldCostElements.packagingFreightCostTotal],
                      ['Overhead', report.shouldCostElements.overheadCostTotal],
                      ['SG&A', report.shouldCostElements.sgaCostTotal],
                      ['Profit margin', report.shouldCostElements.profitMarginTotal],
                      ['Grand total', report.shouldCostElements.grandTotal],
                    ] as const
                  ).map(([label, value]) => (
                    <TableRow key={label}>
                      <TableCell>{label}</TableCell>
                      <TableCell align="right">{money(value)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Target-cost variance
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={6} md={3}>
                  <StatCard title="Snapshots" value={report.targetVariance.varianceSnapshots} />
                </Grid>
                <Grid item xs={6} md={3}>
                  <StatCard title="Over threshold" value={report.targetVariance.exceedingThreshold} />
                </Grid>
                <Grid item xs={6} md={3}>
                  <StatCard title="Within threshold" value={report.targetVariance.withinThreshold} />
                </Grid>
                <Grid item xs={6} md={3}>
                  <StatCard
                    title="Avg % variance"
                    valueLabel={money(report.targetVariance.avgPercentageVariance)}
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </>
      )}
    </Stack>
  );
}

function StatCard({
  title,
  value,
  valueLabel,
}: {
  title: string;
  value?: number;
  valueLabel?: string;
}) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="body2" color="text.secondary">
          {title}
        </Typography>
        <Typography variant="h5">{valueLabel ?? value ?? '—'}</Typography>
      </CardContent>
    </Card>
  );
}
