import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import HubOutlinedIcon from '@mui/icons-material/HubOutlined';
import { useCallback, useEffect, useState } from 'react';
import {
  cancelBatch,
  getBatch,
  importBomCsv,
  importMaterialPricesCsv,
  importPartsCsv,
  ingestBatch,
  listBatches,
  promoteBatch,
  reopenBatch,
  validateBatch,
  type BatchDetailResponse,
  type BatchResponse,
  type ErpBatchStatus,
  type ErpBatchType,
  type PromoteResult,
  type ValidateResult,
} from '../api/erpStagingApi';
import { getErrorMessage } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { RequiredFieldLegend } from '../components/ux';

function statusColor(
  s: ErpBatchStatus,
): 'default' | 'info' | 'success' | 'warning' | 'error' {
  switch (s) {
    case 'READY':
      return 'info';
    case 'PROMOTED':
      return 'success';
    case 'FAILED':
    case 'CANCELLED':
      return 'error';
    case 'VALIDATING':
      return 'warning';
    default:
      return 'default';
  }
}

export function ErpStagingPage() {
  const { hasRole } = useAuth();
  const canMutate = hasRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR');

  const [batches, setBatches] = useState<BatchResponse[]>([]);
  const [selected, setSelected] = useState<BatchDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [lastValidate, setLastValidate] = useState<ValidateResult | null>(null);
  const [lastPromote, setLastPromote] = useState<PromoteResult | null>(null);

  const [ingestOpen, setIngestOpen] = useState(false);
  const [sourceSystem, setSourceSystem] = useState('SAP');
  const [batchType, setBatchType] = useState<ErpBatchType>('MATERIAL_PRICE');
  const [sourceFileName, setSourceFileName] = useState('');
  const [materialCode, setMaterialCode] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [currencyCode, setCurrencyCode] = useState('USD');
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().slice(0, 10),
  );

  const loadBatches = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await listBatches();
      setBatches(list);
    } catch (err) {
      setError(getErrorMessage(err));
      setBatches([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadBatches();
  }, [loadBatches]);

  const selectBatch = async (batchId: string) => {
    setActionLoading(true);
    setError(null);
    setLastValidate(null);
    setLastPromote(null);
    try {
      const detail = await getBatch(batchId);
      setSelected(detail);
    } catch (err) {
      setError(getErrorMessage(err));
      setSelected(null);
    } finally {
      setActionLoading(false);
    }
  };

  const onValidate = async () => {
    if (!selected) return;
    setActionLoading(true);
    setError(null);
    setInfo(null);
    try {
      const result = await validateBatch(selected.batch.batchId);
      setLastValidate(result);
      setInfo(
        `Validate complete: ${result.validRows} valid, ${result.invalidRows} invalid → ${result.status}`,
      );
      await loadBatches();
      await selectBatch(selected.batch.batchId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const onPromote = async () => {
    if (!selected) return;
    setActionLoading(true);
    setError(null);
    setInfo(null);
    try {
      const result = await promoteBatch(selected.batch.batchId);
      setLastPromote(result);
      setInfo(
        `Promoted: prices ${result.materialPricesPromoted}/${result.materialPricesSkipped} skip, ` +
          `parts ${result.partsAcknowledged}, BOM ${result.bomLinesPromoted}/${result.bomLinesSkipped} skip`,
      );
      await loadBatches();
      await selectBatch(selected.batch.batchId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const onCancel = async () => {
    if (!selected) return;
    setActionLoading(true);
    setError(null);
    setInfo(null);
    try {
      await cancelBatch(selected.batch.batchId);
      setInfo(`Batch ${selected.batch.batchId} cancelled`);
      await loadBatches();
      await selectBatch(selected.batch.batchId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const onReopen = async () => {
    if (!selected) return;
    setActionLoading(true);
    setError(null);
    setInfo(null);
    try {
      await reopenBatch(selected.batch.batchId);
      setInfo(`Batch ${selected.batch.batchId} re-opened to RECEIVED`);
      await loadBatches();
      await selectBatch(selected.batch.batchId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const onCsvUpload = async (
    kind: 'material-prices' | 'parts' | 'bom',
    file: File | null,
  ) => {
    if (!file) return;
    setActionLoading(true);
    setError(null);
    setInfo(null);
    try {
      const result =
        kind === 'material-prices'
          ? await importMaterialPricesCsv(file, 'CSV')
          : kind === 'parts'
            ? await importPartsCsv(file, 'CSV')
            : await importBomCsv(file, 'CSV');
      setInfo(
        `CSV ${kind}: accepted ${result.acceptedRows}/${result.totalRows}` +
          (result.rejectedRows ? `, rejected ${result.rejectedRows}` : '') +
          (result.batch ? ` → batch ${result.batch.batchId}` : ''),
      );
      await loadBatches();
      if (result.batch) {
        await selectBatch(result.batch.batchId);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const onIngest = async () => {
    setActionLoading(true);
    setError(null);
    setInfo(null);
    try {
      const request =
        batchType === 'MATERIAL_PRICE'
          ? {
              sourceSystem,
              batchType,
              sourceFileName: sourceFileName || undefined,
              materialPrices: [
                {
                  materialCode,
                  unitPrice: Number(unitPrice),
                  currencyCode,
                  effectiveFrom,
                },
              ],
            }
          : {
              sourceSystem,
              batchType,
              sourceFileName: sourceFileName || undefined,
              parts: [
                {
                  externalPartId: 'EXT-' + materialCode,
                  partNumber: materialCode || 'PN-DEMO',
                  uom: 'EA',
                  makeBuyCode: 'MAKE',
                },
              ],
            };

      const created = await ingestBatch(request);
      setIngestOpen(false);
      setInfo(`Ingested batch ${created.batchId} (${created.rowCount} rows)`);
      await loadBatches();
      await selectBatch(created.batchId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Box>
        <Typography
          variant="h5"
          gutterBottom
          sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
        >
          <HubOutlinedIcon /> ERP Staging
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Ingest → validate → promote ERP/PLM batches into live masters (material
          prices, parts, program-scoped BOM).
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {info && <Alert severity="success">{info}</Alert>}

      <Paper sx={{ p: 2 }}>
        <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => void loadBatches()}
            disabled={loading}
          >
            Refresh
          </Button>
          {canMutate && (
            <>
              <Button
                size="small"
                variant="contained"
                startIcon={<CloudUploadOutlinedIcon />}
                onClick={() => setIngestOpen(true)}
              >
                Ingest batch
              </Button>
              <Button size="small" component="label" variant="outlined">
                CSV/XLSX prices
                <input
                  type="file"
                  accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                  hidden
                  onChange={(e) => {
                    const f = e.target.files?.[0] ?? null;
                    e.target.value = '';
                    void onCsvUpload('material-prices', f);
                  }}
                />
              </Button>
              <Button size="small" component="label" variant="outlined">
                CSV/XLSX parts
                <input
                  type="file"
                  accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                  hidden
                  onChange={(e) => {
                    const f = e.target.files?.[0] ?? null;
                    e.target.value = '';
                    void onCsvUpload('parts', f);
                  }}
                />
              </Button>
              <Button size="small" component="label" variant="outlined">
                CSV/XLSX BOM
                <input
                  type="file"
                  accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                  hidden
                  onChange={(e) => {
                    const f = e.target.files?.[0] ?? null;
                    e.target.value = '';
                    void onCsvUpload('bom', f);
                  }}
                />
              </Button>
            </>
          )}
        </Stack>

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress size={28} />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Received</TableCell>
                <TableCell>Source</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Rows</TableCell>
                <TableCell align="right">Errors</TableCell>
                <TableCell>File</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {batches.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                      No staging batches yet.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                batches.map((b) => (
                  <TableRow
                    key={b.batchId}
                    hover
                    selected={selected?.batch.batchId === b.batchId}
                    onClick={() => void selectBatch(b.batchId)}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>
                      {new Date(b.receivedAt).toLocaleString()}
                    </TableCell>
                    <TableCell>{b.sourceSystem}</TableCell>
                    <TableCell>{b.batchType}</TableCell>
                    <TableCell>
                      <Chip size="small" label={b.status} color={statusColor(b.status)} />
                    </TableCell>
                    <TableCell align="right">{b.rowCount}</TableCell>
                    <TableCell align="right">{b.errorCount}</TableCell>
                    <TableCell>{b.sourceFileName ?? '—'}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </Paper>

      {selected && (
        <Paper sx={{ p: 2 }}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            sx={{ justifyContent: 'space-between', mb: 2 }}
          >
            <Box>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                Batch {selected.batch.batchId}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {selected.batch.sourceSystem} · {selected.batch.batchType} ·{' '}
                {selected.parts.length} parts · {selected.bomLineCount} BOM ·{' '}
                {selected.materialPriceCount} prices
              </Typography>
            </Box>
            {canMutate && (
              <Stack direction="row" spacing={1}>
                <Button
                  variant="outlined"
                  startIcon={<FactCheckOutlinedIcon />}
                  disabled={
                    actionLoading ||
                    selected.batch.status === 'PROMOTED' ||
                    selected.batch.status === 'CANCELLED'
                  }
                  onClick={() => void onValidate()}
                >
                  Validate
                </Button>
                <Button
                  variant="contained"
                  startIcon={<PublishOutlinedIcon />}
                  disabled={actionLoading || selected.batch.status !== 'READY'}
                  onClick={() => void onPromote()}
                >
                  Promote
                </Button>
                <Button
                  variant="outlined"
                  color="warning"
                  disabled={
                    actionLoading ||
                    selected.batch.status === 'PROMOTED' ||
                    selected.batch.status === 'CANCELLED'
                  }
                  onClick={() => void onCancel()}
                >
                  Cancel
                </Button>
                <Button
                  variant="outlined"
                  disabled={
                    actionLoading ||
                    (selected.batch.status !== 'CANCELLED' &&
                      selected.batch.status !== 'FAILED')
                  }
                  onClick={() => void onReopen()}
                >
                  Re-open
                </Button>
              </Stack>
            )}
          </Stack>

          {actionLoading && (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={24} />
            </Box>
          )}

          {selected.parts.length > 0 && (
            <Table size="small" sx={{ mb: 2 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Part</TableCell>
                  <TableCell>Rev</TableCell>
                  <TableCell>Net wt (kg)</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Error</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {selected.parts.map((p) => (
                  <TableRow key={p.stagingId}>
                    <TableCell>{p.partNumber}</TableCell>
                    <TableCell>{p.revision ?? '—'}</TableCell>
                    <TableCell>{p.netWeightKg ?? '—'}</TableCell>
                    <TableCell>
                      <Chip size="small" label={p.status} />
                    </TableCell>
                    <TableCell>{p.errorMessage ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          {lastValidate && (
            <Alert severity="info" sx={{ mb: 1 }}>
              Validate: {lastValidate.validRows} valid / {lastValidate.invalidRows}{' '}
              invalid → {lastValidate.status}
              {lastValidate.messages.length > 0 && (
                <Box component="ul" sx={{ m: 0, pl: 2 }}>
                  {lastValidate.messages.slice(0, 5).map((m) => (
                    <li key={m}>{m}</li>
                  ))}
                </Box>
              )}
            </Alert>
          )}

          {lastPromote && (
            <Alert severity="success">
              Promote: prices {lastPromote.materialPricesPromoted} (
              {lastPromote.materialPricesSkipped} skipped), parts{' '}
              {lastPromote.partsAcknowledged}, BOM {lastPromote.bomLinesPromoted} (
              {lastPromote.bomLinesSkipped} skipped)
              {lastPromote.messages.length > 0 && (
                <Box component="ul" sx={{ m: 0, pl: 2 }}>
                  {lastPromote.messages.slice(0, 8).map((m) => (
                    <li key={m}>{m}</li>
                  ))}
                </Box>
              )}
            </Alert>
          )}
        </Paper>
      )}

      <Dialog open={ingestOpen} onClose={() => setIngestOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Ingest staging batch</DialogTitle>
        <DialogContent>
          <RequiredFieldLegend dense />
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Source system"
              size="small"
              value={sourceSystem}
              onChange={(e) => setSourceSystem(e.target.value)}
              required
            />
            <TextField
              select
              label="Batch type"
              size="small"
              value={batchType}
              onChange={(e) => setBatchType(e.target.value as ErpBatchType)}
            >
              <MenuItem value="MATERIAL_PRICE">MATERIAL_PRICE</MenuItem>
              <MenuItem value="PART">PART</MenuItem>
              <MenuItem value="BOM">BOM</MenuItem>
              <MenuItem value="MIXED">MIXED</MenuItem>
            </TextField>
            <TextField
              label="Source file name"
              size="small"
              value={sourceFileName}
              onChange={(e) => setSourceFileName(e.target.value)}
            />
            {batchType === 'MATERIAL_PRICE' ? (
              <>
                <TextField
                  label="Material code"
                  size="small"
                  value={materialCode}
                  onChange={(e) => setMaterialCode(e.target.value)}
                  required
                  helperText="Must exist in live material master to promote"
                />
                <TextField
                  label="Unit price"
                  size="small"
                  type="number"
                  value={unitPrice}
                  onChange={(e) => setUnitPrice(e.target.value)}
                  required
                />
                <TextField
                  label="Currency"
                  size="small"
                  value={currencyCode}
                  onChange={(e) => setCurrencyCode(e.target.value.toUpperCase())}
                  inputProps={{ maxLength: 3 }}
                />
                <TextField
                  label="Effective from"
                  size="small"
                  type="date"
                  value={effectiveFrom}
                  onChange={(e) => setEffectiveFrom(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </>
            ) : (
              <TextField
                label="Part number"
                size="small"
                value={materialCode}
                onChange={(e) => setMaterialCode(e.target.value)}
                helperText="Simple single-part ingest for PART type"
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIngestOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={
              actionLoading ||
              !sourceSystem.trim() ||
              (batchType === 'MATERIAL_PRICE' &&
                (!materialCode.trim() || unitPrice === '' || Number.isNaN(Number(unitPrice))))
            }
            onClick={() => void onIngest()}
          >
            Ingest
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
