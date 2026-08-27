import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import PrecisionManufacturingOutlinedIcon from '@mui/icons-material/PrecisionManufacturingOutlined';
import {
  approveRouting,
  calculateProcessCost,
  compareProcessRoutings,
  createRouting,
  listRoutingsByBomNode,
  type ProcessCostResponse,
  type RoutingResponse,
} from '../api/processApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';
import { formatMoney } from '../utils/money';

export function ProcessCostingPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [routings, setRoutings] = useState<RoutingResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [costResult, setCostResult] = useState<ProcessCostResponse | null>(null);
  const [compareNote, setCompareNote] = useState<string | null>(null);

  // Single-operation draft for create routing
  const [processName, setProcessName] = useState('CNC Mill');
  const [machineCode, setMachineCode] = useState('');
  const [laborCode, setLaborCode] = useState('');
  const [cycleTimeSeconds, setCycleTimeSeconds] = useState('60');
  const [operatorCount, setOperatorCount] = useState('1');
  const [setupTimeSeconds, setSetupTimeSeconds] = useState('300');
  const [batchSize, setBatchSize] = useState('100');
  const [busy, setBusy] = useState(false);

  const loadRoutings = async () => {
    if (!bomNodeId.trim()) {
      setError('BOM node ID is required');
      return;
    }
    setLoading(true);
    setError(null);
    setCostResult(null);
    setCompareNote(null);
    try {
      const list = await listRoutingsByBomNode(bomNodeId.trim());
      setRoutings(list);
    } catch (e: unknown) {
      setError(getErrorMessage(e));
      setRoutings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRouting = async () => {
    if (!bomNodeId.trim()) {
      setError('BOM node ID is required');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await createRouting({
        bomNodeId: bomNodeId.trim(),
        operations: [
          {
            sequenceNumber: 10,
            processName: processName || null,
            machineCode: machineCode || null,
            laborCode: laborCode || null,
            cycleTimeSeconds,
            operatorCount: operatorCount || null,
            setupTimeSeconds: setupTimeSeconds || null,
            batchSize: batchSize || null,
          },
        ],
      });
      await loadRoutings();
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleApprove = async (routingId: string) => {
    setBusy(true);
    setError(null);
    try {
      await approveRouting(routingId);
      await loadRoutings();
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleCalculate = async (routingId: string) => {
    setBusy(true);
    setError(null);
    setCostResult(null);
    try {
      const result = await calculateProcessCost(routingId);
      setCostResult(result);
      await loadRoutings();
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleCompare = async () => {
    if (routings.length < 2) {
      setError('At least two routings are required to compare');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const res = await compareProcessRoutings(routings.map((r) => r.routingId));
      const lowest = res.lowestCostRoutingId ?? '—';
      const lines = res.lines
        .map((l) => `${l.routingId}: ${formatMoney(l.totalProcessCost)} (${l.operationCount} ops)`)
        .join(' · ');
      setCompareNote(`Lowest: ${lowest}. ${lines}`);
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
        <PrecisionManufacturingOutlinedIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          Process &amp; Labor Costing
        </Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Manage process routings for a BOM node, approve them, calculate process cost (PRC-02/04/06),
        and compare alternate routings (PRC-07). Calculation triggers program roll-up on the backend.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
            <TextField
              label="BOM Node ID"
              required
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              size="small"
              fullWidth
              sx={{ maxWidth: 420 }}
            />
            <Button variant="contained" onClick={() => void loadRoutings()} disabled={loading || busy}>
              {loading ? <CircularProgress size={22} /> : 'Load routings'}
            </Button>
            <Button
              variant="outlined"
              onClick={() => void handleCompare()}
              disabled={busy || routings.length < 2}
            >
              Compare routings
            </Button>
          </Stack>
          {compareNote && (
            <Alert severity="info" sx={{ mt: 2 }}>
              {compareNote}
            </Alert>
          )}
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Create routing (single operation starter)
          </Typography>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap" useFlexGap>
            <TextField
              label="Process name"
              size="small"
              value={processName}
              onChange={(e) => setProcessName(e.target.value)}
            />
            <TextField
              label="Machine code"
              size="small"
              value={machineCode}
              onChange={(e) => setMachineCode(e.target.value)}
            />
            <TextField
              label="Labor code"
              size="small"
              value={laborCode}
              onChange={(e) => setLaborCode(e.target.value)}
            />
            <TextField
              label="Cycle time (s)"
              size="small"
              value={cycleTimeSeconds}
              onChange={(e) => setCycleTimeSeconds(e.target.value)}
            />
            <TextField
              label="Operators"
              size="small"
              value={operatorCount}
              onChange={(e) => setOperatorCount(e.target.value)}
            />
            <TextField
              label="Setup time (s)"
              size="small"
              value={setupTimeSeconds}
              onChange={(e) => setSetupTimeSeconds(e.target.value)}
            />
            <TextField
              label="Batch size"
              size="small"
              value={batchSize}
              onChange={(e) => setBatchSize(e.target.value)}
            />
            <Button variant="contained" onClick={() => void handleCreateRouting()} disabled={busy}>
              Create routing
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Routings for BOM node
          </Typography>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : routings.length === 0 ? (
            <Typography color="text.secondary">No routings loaded.</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Routing ID</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Operations</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {routings.map((r) => (
                  <TableRow key={r.routingId}>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>{r.routingId}</TableCell>
                    <TableCell>
                      <Chip size="small" label={r.status} color={r.status === 'APPROVED' ? 'success' : 'default'} />
                    </TableCell>
                    <TableCell>{r.operations?.length ?? 0}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        {r.status !== 'APPROVED' && (
                          <Button size="small" onClick={() => void handleApprove(r.routingId)} disabled={busy}>
                            Approve
                          </Button>
                        )}
                        <Button
                          size="small"
                          variant="contained"
                          onClick={() => void handleCalculate(r.routingId)}
                          disabled={busy}
                        >
                          Calculate
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {costResult && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Process cost result
            </Typography>
            <Typography variant="body2" sx={{ mb: 1 }}>
              Total process cost:{' '}
              <strong>{formatMoney(costResult.totalProcessCost)}</strong> · Routing{' '}
              <code>{costResult.routingId}</code>
            </Typography>
            {costResult.calculationBasis && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 2 }}>
                {costResult.calculationBasis}
              </Typography>
            )}
            <Divider sx={{ mb: 2 }} />
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Operation</TableCell>
                  <TableCell align="right">Run cost</TableCell>
                  <TableCell align="right">Setup / piece</TableCell>
                  <TableCell align="right">Total</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(costResult.operationCosts ?? []).map((line) => (
                  <TableRow key={line.operationId}>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>{line.operationId}</TableCell>
                    <TableCell align="right">{formatMoney(line.runCost)}</TableCell>
                    <TableCell align="right">{formatMoney(line.setupCostPerPiece)}</TableCell>
                    <TableCell align="right">{formatMoney(line.totalOperationCost)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
