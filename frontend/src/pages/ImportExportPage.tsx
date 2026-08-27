import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import {
  bulkImportLabor,
  bulkImportMachines,
  bulkImportMaterials,
  exportLaborCsv,
  exportMachinesCsv,
  exportMaterialsCsv,
  type ImportResult,
} from '../api/importExportApi';
import { getErrorMessage } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';

type Entity = 'materials' | 'machines' | 'labor';

export function ImportExportPage() {
  const { hasRole } = useAuth();
  const canImport = hasRole('MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [lastResult, setLastResult] = useState<ImportResult | null>(null);
  const [busy, setBusy] = useState(false);

  async function onImport(entity: Entity, file: File | null) {
    if (!file) return;
    if (!canImport) {
      setError(
        'Bulk import requires the MASTER_DATA_ADMIN role. Sign in as demo.master (or an admin) to import CSV/Excel.',
      );
      return;
    }
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      const fn =
        entity === 'materials'
          ? bulkImportMaterials
          : entity === 'machines'
            ? bulkImportMachines
            : bulkImportLabor;
      const result = await fn(file);
      setLastResult(result);
      setInfo(
        `${entity}: ${result.succeeded} succeeded, ${result.failed} failed of ${result.totalRows} rows`,
      );
    } catch (e: unknown) {
      setError(formatImportExportError(e, 'import'));
    } finally {
      setBusy(false);
    }
  }

  async function onExport(entity: Entity) {
    setError(null);
    try {
      if (entity === 'materials') await exportMaterialsCsv();
      else if (entity === 'machines') await exportMachinesCsv();
      else await exportLaborCsv();
      setInfo(`Downloaded ${entity}.csv`);
    } catch (e: unknown) {
      setError(formatImportExportError(e, 'export'));
    }
  }

  function filePicker(entity: Entity) {
    if (!canImport) {
      setError(
        'Bulk import requires the MASTER_DATA_ADMIN role. Sign in as demo.master (or an admin) to import CSV/Excel.',
      );
      return;
    }
    const input = document.createElement('input');
    input.type = 'file';
    input.accept =
      '.csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    input.onchange = () => {
      const f = input.files?.[0] ?? null;
      void onImport(entity, f);
    };
    input.click();
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <ImportExportOutlinedIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          Import / Export
        </Typography>
      </Box>
      <Typography color="text.secondary">
        CSV / Excel bulk import (partial success per row) and export for materials, machines, and labor
        masters. Export headers match import templates.
      </Typography>

      {!canImport && (
        <Alert severity="info">
          You can <strong>export</strong> master data with your current role. <strong>Import</strong>{' '}
          requires <code>MASTER_DATA_ADMIN</code> (e.g. user <code>demo.master</code>). Costing
          engineers can view this page but cannot upload bulk files.
        </Alert>
      )}

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

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        {(
          [
            ['materials', 'Materials'],
            ['machines', 'Machines'],
            ['labor', 'Labor'],
          ] as const
        ).map(([key, label]) => (
          <Card key={key} sx={{ flex: 1 }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                {label}
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button
                  variant="contained"
                  size="small"
                  disabled={busy || !canImport}
                  onClick={() => filePicker(key)}
                  title={
                    canImport
                      ? 'Upload CSV or Excel'
                      : 'Requires MASTER_DATA_ADMIN role'
                  }
                >
                  Import CSV
                </Button>
                <Button variant="outlined" size="small" onClick={() => void onExport(key)}>
                  Export CSV
                </Button>
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {lastResult && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Last import result
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Total {lastResult.totalRows} · Succeeded {lastResult.succeeded} · Failed{' '}
              {lastResult.failed}
            </Typography>
            {lastResult.errors.length > 0 && (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Row</TableCell>
                    <TableCell>Message</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {lastResult.errors.map((e, i) => (
                    <TableRow key={`${e.rowNumber}-${i}`}>
                      <TableCell>{e.rowNumber}</TableCell>
                      <TableCell>{e.reason}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}

function formatImportExportError(e: unknown, action: 'import' | 'export'): string {
  const base = getErrorMessage(e);
  if (base.includes('403') || /forbidden|access is denied/i.test(base)) {
    if (action === 'import') {
      return (
        'Import forbidden (403): bulk import requires MASTER_DATA_ADMIN. ' +
        'Sign in as demo.master or ask an administrator to grant that role. ' +
        `(${base})`
      );
    }
    return (
      'Export forbidden (403): check PRODUCT_COSTING module entitlement and tenant (X-Customer-Id). ' +
      `(${base})`
    );
  }
  return base;
}
