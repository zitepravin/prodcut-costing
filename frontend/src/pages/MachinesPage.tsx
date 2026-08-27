import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
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
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useEffect, useState } from 'react';
import { getRateBuildUp, searchMachines } from '../api/machineApi';
import { getErrorMessage } from '../api/httpClient';
import type { MachineMaster, MasterDataStatus, RateBuildUpResponse } from '../types/api';
import { formatMoney, formatRate } from '../utils/money';

const STATUS_OPTIONS: Array<MasterDataStatus | ''> = ['', 'ACTIVE', 'INACTIVE', 'PENDING_APPROVAL'];

export function MachinesPage() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<MasterDataStatus | ''>('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [rows, setRows] = useState<MachineMaster[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [buildUp, setBuildUp] = useState<RateBuildUpResponse | null>(null);
  const [buildUpCode, setBuildUpCode] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await searchMachines({ q: q.trim() || undefined, status, page, size: rowsPerPage });
      setRows(result.content);
      setTotal(result.totalElements);
    } catch (err) {
      setRows([]);
      setTotal(0);
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [q, status, page, rowsPerPage]);

  useEffect(() => {
    void load();
  }, [load]);

  async function openBuildUp(row: MachineMaster) {
    setError(null);
    try {
      const data = await getRateBuildUp(row.recordId);
      setBuildUp(data);
      setBuildUpCode(row.code);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom>
          Machine masters
        </Typography>
        <Typography variant="body2" color="text.secondary">
          M01 / M05 — hourly rates and OHD-01 rate build-up. Source:{' '}
          <code>GET /api/v1/master/machines</code>
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
          <TextField
            label="Code / description"
            size="small"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && void load()}
          />
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel id="mst">Status</InputLabel>
            <Select
              labelId="mst"
              label="Status"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as MasterDataStatus | '');
                setPage(0);
              }}
            >
              {STATUS_OPTIONS.map((s) => (
                <MenuItem key={s || 'all'} value={s}>
                  {s || 'All'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button variant="contained" startIcon={<SearchIcon />} onClick={() => void load()}>
            Search
          </Button>
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Rate</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Shift</TableCell>
                <TableCell>Version</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Build-up</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading && (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              )}
              {!loading &&
                rows.map((row) => (
                  <TableRow key={row.recordId} hover>
                    <TableCell sx={{ fontWeight: 600 }}>{row.code}</TableCell>
                    <TableCell>{row.description ?? '—'}</TableCell>
                    <TableCell sx={{ fontFamily: 'ui-monospace, monospace' }}>
                      {formatRate(row.rate, row.uom)}
                    </TableCell>
                    <TableCell>{row.machineType ?? '—'}</TableCell>
                    <TableCell>{row.shiftPattern ?? '—'}</TableCell>
                    <TableCell>v{row.versionNumber}</TableCell>
                    <TableCell>
                      <Chip size="small" label={row.status} variant="outlined" />
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => void openBuildUp(row)}>
                        OHD-01
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              {!loading && !rows.length && (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    <Typography color="text.secondary">No machines found.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={total}
          page={page}
          onPageChange={(_, p) => setPage(p)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Paper>

      <Dialog open={Boolean(buildUp)} onClose={() => setBuildUp(null)} fullWidth maxWidth="sm">
        <DialogTitle>Rate build-up — {buildUpCode}</DialogTitle>
        <DialogContent>
          {buildUp && (
            <Table size="small">
              <TableBody>
                <TableRow>
                  <TableCell>Depreciation / hr</TableCell>
                  <TableCell align="right">{formatMoney(buildUp.depreciationComponentPerHour)}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Power / hr</TableCell>
                  <TableCell align="right">{formatMoney(buildUp.powerCostPerHour)}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Maintenance / hr</TableCell>
                  <TableCell align="right">{formatMoney(buildUp.maintenanceCostPerHour)}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Floor space / hr</TableCell>
                  <TableCell align="right">{formatMoney(buildUp.floorSpaceCostPerHour)}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>Total hourly rate</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    {formatMoney(buildUp.totalHourlyRate)}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          )}
        </DialogContent>
      </Dialog>
    </Stack>
  );
}
