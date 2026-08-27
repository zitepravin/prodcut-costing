import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
  Button,
  DialogActions,
  DialogContent,
  DialogTitle,
  Dialog,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useCallback, useEffect, useState } from 'react';
import { bulkImportMaterials, createMaterial, listMaterialVersions, searchMaterials } from '../api/materialApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';
import { useAuth } from '../auth/useAuth';
import type { MasterDataStatus, MaterialMaster } from '../types/api';
import { formatRate } from '../utils/money';

const STATUS_OPTIONS: Array<MasterDataStatus | ''> = ['', 'ACTIVE', 'INACTIVE', 'PENDING_APPROVAL'];

export function MaterialsPage() {
  const { hasRole } = useAuth();
  const canCreate = hasRole('MASTER_DATA_ADMIN');
  const [code, setCode] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [versionOpen, setVersionOpen] = useState(false);
  const [versionCode, setVersionCode] = useState('');
  const [versionRows, setVersionRows] = useState<MaterialMaster[]>([]);
  const [versionLoading, setVersionLoading] = useState(false);
  const [importBusy, setImportBusy] = useState(false);
  const [importInfo, setImportInfo] = useState<string | null>(null);
  const [cCode, setCCode] = useState('');
  const [cDesc, setCDesc] = useState('');
  const [cRate, setCRate] = useState('');
  const [cUom, setCUom] = useState('KG');
  const [cDensity, setCDensity] = useState('');
  const [cFrom, setCFrom] = useState(new Date().toISOString().slice(0, 10));
  const [creating, setCreating] = useState(false);
  const [status, setStatus] = useState<MasterDataStatus | ''>('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [rows, setRows] = useState<MaterialMaster[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await searchMaterials({
        q: code.trim() || undefined,
        status: status || undefined,
        page,
        size: rowsPerPage,
        sort: 'code,asc',
      });
      setRows(result.content);
      setTotal(result.totalElements);
    } catch (err) {
      setRows([]);
      setTotal(0);
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [code, status, page, rowsPerPage]);

  useEffect(() => {
    void load();
  }, [load]);

  function handleSearch() {
    setPage(0);
    void load();
  }


  async function handleShowVersions(code: string) {
    setVersionCode(code);
    setVersionOpen(true);
    setVersionLoading(true);
    setError(null);
    try {
      setVersionRows(await listMaterialVersions(code));
    } catch (err) {
      setError(getErrorMessage(err));
      setVersionRows([]);
    } finally {
      setVersionLoading(false);
    }
  }

  async function handleBulkImport(file: File | null) {
    if (!file) return;
    setImportBusy(true);
    setError(null);
    setImportInfo(null);
    try {
      const result = await bulkImportMaterials(file);
      setImportInfo(
        `Bulk import: ${result.succeeded}/${result.totalRows} succeeded, ${result.failed} failed` +
          (result.errors?.length
            ? ` — first error row ${result.errors[0].rowNumber}: ${result.errors[0].reason}`
            : ''),
      );
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setImportBusy(false);
    }
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom>
          Material masters
        </Typography>
        <Typography variant="body2" color="text.secondary">
          MDM catalog — rates, density, material form, and index linkage. Source:{' '}
          <code>GET /api/v1/master/materials</code>
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
          <TextField
            label="Code contains"
            size="small"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch();
            }}
            sx={{ minWidth: 200 }}
          />
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id="status-label">Status</InputLabel>
            <Select
              labelId="status-label"
              label="Status"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as MasterDataStatus | '');
                setPage(0);
              }}
            >
              {STATUS_OPTIONS.map((s) => (
                <MenuItem key={s || 'all'} value={s}>
                  {s || 'All statuses'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button variant="contained" startIcon={<SearchIcon />} onClick={handleSearch}>
            Search
          </Button>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => void load()}>
            Refresh
          </Button>
          {canCreate && (
            <>
              <Button variant="contained" color="secondary" onClick={() => setCreateOpen(true)}>
                New material
              </Button>
              <Button variant="outlined" component="label" disabled={importBusy}>
                {importBusy ? 'Importing…' : 'Bulk CSV import'}
                <input
                  type="file"
                  accept=".csv,text/csv"
                  hidden
                  onChange={(e) => {
                    const f = e.target.files?.[0] ?? null;
                    e.target.value = '';
                    void handleBulkImport(f);
                  }}
                />
              </Button>
            </>
          )}
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {importInfo && (
        <Alert severity="info" onClose={() => setImportInfo(null)}>
          {importInfo}
        </Alert>
      )}

      <Paper>
        <TableContainer>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Rate</TableCell>
                <TableCell>UOM</TableCell>
                <TableCell>Density</TableCell>
                <TableCell>Form</TableCell>
                <TableCell>Version</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Effective from</TableCell>
                <TableCell>Index</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading && (
                <TableRow>
                  <TableCell colSpan={11} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={32} />
                  </TableCell>
                </TableRow>
              )}
              {!loading && rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={11} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">
                      {error ? 'Unable to load materials.' : 'No materials match the current filters.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {!loading &&
                rows.map((row) => (
                  <TableRow key={row.recordId} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {row.code}
                      </Typography>
                    </TableCell>
                    <TableCell>{row.description ?? '—'}</TableCell>
                    <TableCell sx={{ fontFamily: 'ui-monospace, monospace' }}>
                      {formatRate(row.rate, row.uom)}
                    </TableCell>
                    <TableCell>{row.uom}</TableCell>
                    <TableCell sx={{ fontFamily: 'ui-monospace, monospace' }}>
                      {row.density}
                    </TableCell>
                    <TableCell>{row.materialForm ?? '—'}</TableCell>
                    <TableCell>v{row.versionNumber}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={row.status}
                        color={
                          row.status === 'ACTIVE'
                            ? 'success'
                            : row.status === 'INACTIVE'
                              ? 'default'
                              : 'warning'
                        }
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>{row.effectiveFrom}</TableCell>
                    <TableCell>
                      {row.indexLinkedFlag ? row.indexCode ?? 'Linked' : '—'}
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => void handleShowVersions(row.code)}>
                        Versions
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={total}
          page={page}
          onPageChange={(_, next) => setPage(next)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Paper>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create material master</DialogTitle>
        <DialogContent>
          <RequiredFieldLegend dense />
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Code" size="small" value={cCode} onChange={(e) => setCCode(e.target.value)} required />
            <TextField label="Description" size="small" value={cDesc} onChange={(e) => setCDesc(e.target.value)} />
            <TextField label="Rate" size="small" value={cRate} onChange={(e) => setCRate(e.target.value)} required />
            <TextField label="UOM" size="small" value={cUom} onChange={(e) => setCUom(e.target.value)} required />
            <TextField label="Density" size="small" value={cDensity} onChange={(e) => setCDensity(e.target.value)} required />
            <TextField label="Effective from" size="small" type="date" value={cFrom} onChange={(e) => setCFrom(e.target.value)} InputLabelProps={{ shrink: true }} required />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={creating || !cCode || !cRate || !cUom || !cDensity || !cFrom}
            onClick={() => {
              void (async () => {
                setCreating(true);
                setError(null);
                try {
                  await createMaterial({
                    code: cCode.trim(),
                    description: cDesc.trim() || undefined,
                    rate: cRate.trim(),
                    uom: cUom.trim(),
                    density: cDensity.trim(),
                    effectiveFrom: cFrom,
                    indexLinkedFlag: false,
                  });
                  setCreateOpen(false);
                  setCCode('');
                  setCDesc('');
                  setCRate('');
                  await load();
                } catch (err) {
                  setError(getErrorMessage(err));
                } finally {
                  setCreating(false);
                }
              })();
            }}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={versionOpen} onClose={() => setVersionOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>Version history — {versionCode}</DialogTitle>
        <DialogContent>
          {versionLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Version</TableCell>
                  <TableCell>Rate</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Effective from</TableCell>
                  <TableCell>Effective to</TableCell>
                  <TableCell>Record ID</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {versionRows.map((v) => (
                  <TableRow key={v.recordId}>
                    <TableCell>v{v.versionNumber}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace' }}>{formatRate(v.rate, v.uom)}</TableCell>
                    <TableCell>{v.status}</TableCell>
                    <TableCell>{v.effectiveFrom}</TableCell>
                    <TableCell>{v.effectiveTo ?? '—'}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: 11 }}>{v.recordId}</TableCell>
                  </TableRow>
                ))}
                {!versionRows.length && (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Typography color="text.secondary">No versions found.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVersionOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

    </Stack>
  );
}
