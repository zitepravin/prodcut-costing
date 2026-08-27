import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import TrackChangesOutlinedIcon from '@mui/icons-material/TrackChangesOutlined';
import {
  activateTarget,
  computeVariance,
  createTarget,
  exceedingVariances,
  listTargetsByBomNode,
  markTargetSubmitted,
  varianceHistory,
  type ActualCostSource,
  type TargetCostRecord,
  type TargetVariance,
} from '../api/targetCostingApi';
import { listScenarios } from '../api/shouldCostApi';
import type { ShouldCostBuildUp } from '../types/api';
import { RequiredFieldLegend } from '../components/ux';

function statusColor(status: string): 'default' | 'success' | 'warning' | 'error' | 'info' {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
      return 'success';
    case 'SUBMITTED':
      return 'info';
    case 'DRAFT':
      return 'default';
    case 'SUPERSEDED':
    case 'REJECTED':
      return 'warning';
    default:
      return 'default';
  }
}

function formatPct(decimalStr: string): string {
  const n = Number(decimalStr);
  if (Number.isNaN(n)) return decimalStr;
  return `${(n * 100).toFixed(2)}%`;
}

export function TargetCostingPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [targets, setTargets] = useState<TargetCostRecord[]>([]);
  const [history, setHistory] = useState<TargetVariance[]>([]);
  const [exceeding, setExceeding] = useState<TargetVariance[]>([]);
  const [scenarios, setScenarios] = useState<ShouldCostBuildUp[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const [createOpen, setCreateOpen] = useState(false);
  const [varianceOpen, setVarianceOpen] = useState(false);
  const [selectedTarget, setSelectedTarget] = useState<TargetCostRecord | null>(null);
  const [lastVariance, setLastVariance] = useState<TargetVariance | null>(null);

  const [createForm, setCreateForm] = useState({
    targetAmount: '',
    currencyCode: 'USD',
    notes: '',
  });

  const [varianceForm, setVarianceForm] = useState({
    actualSource: 'SHOULD_COST' as ActualCostSource,
    shouldCostBuildUpId: '',
    thresholdPct: '0.05',
  });

  async function loadAll() {
    if (!bomNodeId.trim()) {
      setError('Enter a BOM node id');
      return;
    }
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const [t, h, sc] = await Promise.all([
        listTargetsByBomNode(bomNodeId.trim()),
        varianceHistory(bomNodeId.trim()),
        listScenarios(bomNodeId.trim()).catch(() => [] as ShouldCostBuildUp[]),
      ]);
      setTargets(t);
      setHistory(h);
      setScenarios(sc);
      const ex = await exceedingVariances().catch(() => ({ items: [], totalExceeding: 0 }));
      setExceeding(ex.items.filter((v) => v.bomNodeId === bomNodeId.trim()));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load target costing data');
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate() {
    setError(null);
    try {
      await createTarget({
        bomNodeId: bomNodeId.trim(),
        targetAmount: createForm.targetAmount,
        currencyCode: createForm.currencyCode || undefined,
        notes: createForm.notes || undefined,
      });
      setCreateOpen(false);
      setCreateForm({ targetAmount: '', currencyCode: 'USD', notes: '' });
      setInfo('Target cost created (DRAFT)');
      await loadAll();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create failed');
    }
  }

  async function handleActivate(id: string) {
    setError(null);
    try {
      await activateTarget(id);
      setInfo('Target activated (prior ACTIVE on this node superseded)');
      await loadAll();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Activate failed');
    }
  }

  async function handleMarkSubmitted(id: string) {
    setError(null);
    try {
      await markTargetSubmitted(id);
      setInfo('Marked SUBMITTED — complete approval via Approvals inbox (entityType=TargetCostRecord)');
      await loadAll();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Mark submitted failed');
    }
  }

  function openVariance(target: TargetCostRecord) {
    setSelectedTarget(target);
    setVarianceForm({
      actualSource: 'SHOULD_COST',
      shouldCostBuildUpId: scenarios[0]?.buildUpId ?? '',
      thresholdPct: '0.05',
    });
    setVarianceOpen(true);
  }

  async function handleComputeVariance() {
    if (!selectedTarget) return;
    setError(null);
    try {
      const result = await computeVariance({
        targetCostId: selectedTarget.targetCostId,
        actualSource: varianceForm.actualSource,
        shouldCostBuildUpId:
          varianceForm.actualSource === 'SHOULD_COST'
            ? varianceForm.shouldCostBuildUpId || undefined
            : undefined,
        thresholdPct: varianceForm.thresholdPct || undefined,
      });
      setLastVariance(result);
      setVarianceOpen(false);
      setInfo(
        result.exceedsThreshold
          ? `Variance exceeds threshold (${formatPct(result.percentageVariance)})`
          : `Variance within threshold (${formatPct(result.percentageVariance)})`,
      );
      await loadAll();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Variance calculation failed');
    }
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <TrackChangesOutlinedIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          Target Costing
        </Typography>
      </Box>
      <Typography color="text.secondary">
        Part-level targets vs should-cost or BOM roll-up. Over-threshold variances can be submitted
        for approval (workflow entityType = TargetCostRecord).
      </Typography>

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

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <TextField
              label="BOM Node ID"
              required
              size="small"
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              sx={{ minWidth: 320 }}
            />
            <Button variant="contained" onClick={loadAll} disabled={loading}>
              Load
            </Button>
            <Button
              variant="outlined"
              onClick={() => setCreateOpen(true)}
              disabled={!bomNodeId.trim()}
            >
              New target
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Targets
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Amount</TableCell>
                <TableCell>Currency</TableCell>
                <TableCell>Effective from</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Approval</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {targets.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Typography color="text.secondary">No targets for this node.</Typography>
                  </TableCell>
                </TableRow>
              )}
              {targets.map((t) => (
                <TableRow key={t.targetCostId}>
                  <TableCell>
                    <Typography fontWeight={600}>{t.targetAmount}</Typography>
                  </TableCell>
                  <TableCell>{t.currencyCode}</TableCell>
                  <TableCell>{t.effectiveFrom}</TableCell>
                  <TableCell>
                    <Chip size="small" label={t.status} color={statusColor(t.status)} />
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={t.approvalStatus} color={statusColor(t.approvalStatus)} />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={0.5} justifyContent="flex-end" flexWrap="wrap">
                      {t.status !== 'ACTIVE' && t.status !== 'SUPERSEDED' && (
                        <Button size="small" onClick={() => handleActivate(t.targetCostId)}>
                          Activate
                        </Button>
                      )}
                      {t.approvalStatus === 'DRAFT' && (
                        <Button size="small" onClick={() => handleMarkSubmitted(t.targetCostId)}>
                          Mark submitted
                        </Button>
                      )}
                      <Button size="small" variant="outlined" onClick={() => openVariance(t)}>
                        Variance
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {lastVariance && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Last variance result
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Target
                </Typography>
                <Typography fontWeight={600}>{lastVariance.targetAmount}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Actual ({lastVariance.actualSource})
                </Typography>
                <Typography fontWeight={600}>{lastVariance.actualAmount}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Absolute
                </Typography>
                <Typography fontWeight={600}>{lastVariance.absoluteVariance}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  %
                </Typography>
                <Typography fontWeight={600}>{formatPct(lastVariance.percentageVariance)}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Threshold
                </Typography>
                <Chip
                  size="small"
                  label={lastVariance.exceedsThreshold ? 'EXCEEDS' : 'OK'}
                  color={lastVariance.exceedsThreshold ? 'warning' : 'success'}
                />
              </Box>
            </Stack>
            {lastVariance.calculationBasis && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                {lastVariance.calculationBasis}
              </Typography>
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Variance history
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Calculated</TableCell>
                <TableCell>Source</TableCell>
                <TableCell align="right">Target</TableCell>
                <TableCell align="right">Actual</TableCell>
                <TableCell align="right">Abs</TableCell>
                <TableCell align="right">%</TableCell>
                <TableCell>Flag</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {history.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Typography color="text.secondary">No variance history.</Typography>
                  </TableCell>
                </TableRow>
              )}
              {history.map((v) => (
                <TableRow key={v.varianceId}>
                  <TableCell>{v.calculatedAt ? new Date(v.calculatedAt).toLocaleString() : '—'}</TableCell>
                  <TableCell>{v.actualSource}</TableCell>
                  <TableCell align="right">{v.targetAmount}</TableCell>
                  <TableCell align="right">{v.actualAmount}</TableCell>
                  <TableCell align="right">{v.absoluteVariance}</TableCell>
                  <TableCell align="right">{formatPct(v.percentageVariance)}</TableCell>
                  <TableCell>
                    {v.exceedsThreshold ? (
                      <Chip size="small" label="Over" color="warning" />
                    ) : (
                      <Chip size="small" label="OK" color="success" variant="outlined" />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {exceeding.length > 0 && (
        <Card sx={{ borderColor: 'warning.main', borderWidth: 1, borderStyle: 'solid' }}>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom color="warning.main">
              Over-threshold on this node ({exceeding.length})
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Submit the related target for approval when variance requires management review.
            </Typography>
          </CardContent>
        </Card>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New target cost</DialogTitle>
        <DialogContent>
          <RequiredFieldLegend dense />
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Target amount"
              value={createForm.targetAmount}
              onChange={(e) => setCreateForm({ ...createForm, targetAmount: e.target.value })}
              fullWidth
              required
              helperText="Must be &gt; 0 (decimal string)"
            />
            <TextField
              label="Currency"
              value={createForm.currencyCode}
              onChange={(e) => setCreateForm({ ...createForm, currencyCode: e.target.value })}
              fullWidth
            />
            <TextField
              label="Notes"
              value={createForm.notes}
              onChange={(e) => setCreateForm({ ...createForm, notes: e.target.value })}
              fullWidth
              multiline
              minRows={2}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={!createForm.targetAmount.trim()}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={varianceOpen} onClose={() => setVarianceOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Compute variance</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Target: {selectedTarget?.targetAmount} {selectedTarget?.currencyCode}
            </Typography>
            <TextField
              select
              label="Actual cost source"
              value={varianceForm.actualSource}
              onChange={(e) =>
                setVarianceForm({
                  ...varianceForm,
                  actualSource: e.target.value as ActualCostSource,
                })
              }
              fullWidth
            >
              <MenuItem value="SHOULD_COST">Should-Cost build-up</MenuItem>
              <MenuItem value="BOM_ROLLUP">BOM rolled-up cost</MenuItem>
            </TextField>
            {varianceForm.actualSource === 'SHOULD_COST' && (
              <TextField
                select
                label="Should-Cost scenario"
                value={varianceForm.shouldCostBuildUpId}
                onChange={(e) =>
                  setVarianceForm({ ...varianceForm, shouldCostBuildUpId: e.target.value })
                }
                fullWidth
                helperText={
                  scenarios.length === 0
                    ? 'No scenarios for this node — generate a should-cost first'
                    : undefined
                }
              >
                {scenarios.map((s) => (
                  <MenuItem key={s.buildUpId} value={s.buildUpId}>
                    {s.scenarioLabel} — {s.totalCost}
                  </MenuItem>
                ))}
              </TextField>
            )}
            <TextField
              label="Threshold (fraction, e.g. 0.05 = 5%)"
              value={varianceForm.thresholdPct}
              onChange={(e) => setVarianceForm({ ...varianceForm, thresholdPct: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVarianceOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleComputeVariance}
            disabled={
              varianceForm.actualSource === 'SHOULD_COST' && !varianceForm.shouldCostBuildUpId
            }
          >
            Calculate
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
