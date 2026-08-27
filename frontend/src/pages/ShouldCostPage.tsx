import { PageHeader, CostSummaryStrip } from '../components/ux';
import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Paper,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { useState, useEffect } from 'react';
import {
  generateShouldCost,
  getWaterfall,
  listScenarios,
  lockBuildUp,
  parametricEstimate,
  varianceAgainstQuote,
  deepVariance,
  type DeepVarianceResponse,
} from '../api/shouldCostApi';
import { submitWorkflow } from '../api/workflowApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';
import type {
  ParametricResponse,
  ShouldCostBuildUp,
  VarianceResponse,
  WaterfallResponse,
} from '../types/api';
import { WaterfallChart } from '../components/shouldcost/WaterfallChart';
import { formatMoney } from '../utils/money';

type TabKey = 'full' | 'parametric' | 'variance';

export function ShouldCostPage() {
  const [tab, setTab] = useState<TabKey>('full');
  const [bomNodeId, setBomNodeId] = useState(localStorage.getItem('pcs.lastBomNodeId') ?? '');
  const [scenarioLabel, setScenarioLabel] = useState('Current Design');
  const [commodityFamily, setCommodityFamily] = useState('');
  const [region, setRegion] = useState('');
  const [overheadPoolCode, setOverheadPoolCode] = useState('');
  const [allocationBase, setAllocationBase] = useState('');

  const [scenarios, setScenarios] = useState<ShouldCostBuildUp[]>([]);
  const [selected, setSelected] = useState<ShouldCostBuildUp | null>(null);
  const [waterfall, setWaterfall] = useState<WaterfallResponse | null>(null);

  const [materialFamily, setMaterialFamily] = useState('');
  const [processFamily, setProcessFamily] = useState('');
  const [paramRegion, setParamRegion] = useState('');
  const [netWeightKg, setNetWeightKg] = useState('');
  const [parametric, setParametric] = useState<ParametricResponse | null>(null);

  const [quotedPrice, setQuotedPrice] = useState('');
  const [variance, setVariance] = useState<VarianceResponse | null>(null);
  const [deepVarianceResult, setDeepVarianceResult] = useState<DeepVarianceResponse | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  async function loadScenarios(nodeId: string) {
    const list = await listScenarios(nodeId);
    setScenarios(list);
    return list;
  }

  async function handleLoadScenarios() {
    if (!bomNodeId.trim()) {
      setError('BOM node id is required.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      localStorage.setItem('pcs.lastBomNodeId', bomNodeId.trim());
      await loadScenarios(bomNodeId.trim());
      setInfo(`Loaded scenarios for node ${bomNodeId.trim()}.`);
    } catch (err) {
      setError(getErrorMessage(err));
      setScenarios([]);
    } finally {
      setLoading(false);
    }
  }


  useEffect(() => {
    setHelpingAgentPageContext({
      currentPage: 'should-cost',
      bomNodeId: selected?.bomNodeId,
      buildUpId: selected?.buildUpId,
    });
  }, [selected]);

  async function handleGenerate() {
    if (!bomNodeId.trim() || !commodityFamily.trim()) {
      setError('BOM node id and commodity family are required.');
      return;
    }
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const created = await generateShouldCost({
        bomNodeId: bomNodeId.trim(),
        scenarioLabel: scenarioLabel.trim() || undefined,
        commodityFamily: commodityFamily.trim(),
        region: region.trim() || undefined,
        allocationBaseQuantity: allocationBase.trim() || null,
        overheadPoolCode: overheadPoolCode.trim() || null,
      });
      localStorage.setItem('pcs.lastBomNodeId', bomNodeId.trim());
      setSelected(created);
      const wf = await getWaterfall(created.buildUpId);
      setWaterfall(wf);
      await loadScenarios(bomNodeId.trim());
      setInfo(`Generated scenario "${created.scenarioLabel}" · total ${formatMoney(created.totalCost)}.`);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleSelect(row: ShouldCostBuildUp) {
    setSelected(row);
    setLoading(true);
    setError(null);
    try {
      const wf = await getWaterfall(row.buildUpId);
      setWaterfall(wf);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleParametric() {
    setLoading(true);
    setError(null);
    try {
      const result = await parametricEstimate({
        materialFamily: materialFamily.trim(),
        processFamily: processFamily.trim(),
        region: paramRegion.trim(),
        netWeightKg: netWeightKg.trim(),
      });
      setParametric(result);
    } catch (err) {
      setError(getErrorMessage(err));
      setParametric(null);
    } finally {
      setLoading(false);
    }
  }

  async function handleVariance() {
    if (!selected) {
      setError('Select a build-up first.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = await varianceAgainstQuote(selected.buildUpId, quotedPrice.trim());
      const deep = await deepVariance({
        buildUpId: selected.buildUpId,
        quotedTotal: quotedPrice.trim(),
      });
      setDeepVarianceResult(deep);
      setVariance(result);
    } catch (err) {
      setError(getErrorMessage(err));
      setVariance(null);
    } finally {
      setLoading(false);
    }
  }


  async function handleSubmitApproval() {
    if (!selected) {
      setError('Select a should-cost scenario first.');
      return;
    }
    if (selected.locked) {
      setError('Locked build-ups cannot be re-submitted.');
      return;
    }
    if (selected.approvalStatus === 'SUBMITTED' || selected.approvalStatus === 'APPROVED') {
      setError(`Build-up is already ${selected.approvalStatus}.`);
      return;
    }
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const instance = await submitWorkflow({
        entityType: 'ShouldCostBuildUp',
        entityId: selected.buildUpId,
        value: selected.totalCost,
        commodityFamily: commodityFamily.trim() || undefined,
      });
      setInfo(
        `Submitted for approval · workflow ${instance.workflowInstanceId} · status ${instance.status}`,
      );
      const list = await loadScenarios(selected.bomNodeId);
      const refreshed = list.find((s) => s.buildUpId === selected.buildUpId) ?? null;
      setSelected(refreshed);
      if (refreshed) {
        setWaterfall(await getWaterfall(refreshed.buildUpId));
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleLock() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    try {
      const locked = await lockBuildUp(selected.buildUpId);
      setSelected(locked);
      if (bomNodeId.trim()) await loadScenarios(bomNodeId.trim());
      setInfo('Build-up locked.');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Stack spacing={2}>
      <Box>
      <PageHeader
        title="Should-Cost"
        description="Bottom-up should-cost, parametric estimate, waterfall, and quote variance."
      />
      {selected && (
        <CostSummaryStrip
          shouldCost={selected.totalCost}
          drivers={[
            { label: 'Material', amount: selected.materialCost },
            { label: 'Process', amount: selected.processCost },
            { label: 'Tooling', amount: selected.toolingCost },
          ]}
        />
      )}
      </Box>

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

      <Paper sx={{ px: 2 }}>
        <Tabs value={tab} onChange={(_, v: TabKey) => setTab(v)}>
          <Tab value="full" label="Full build-up" />
          <Tab value="parametric" label="Parametric" />
          <Tab value="variance" label="Variance vs quote" />
        </Tabs>
      </Paper>

      {tab === 'full' && (
        <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
          <Paper sx={{ p: 2, flex: 1 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
              Generate / load
            </Typography>
            <RequiredFieldLegend dense />
            <Stack spacing={2}>
              <TextField
                label="BOM node ID"
                size="small"
                value={bomNodeId}
                onChange={(e) => setBomNodeId(e.target.value)}
                required
                fullWidth
              />
              <TextField
                label="Scenario label"
                size="small"
                value={scenarioLabel}
                onChange={(e) => setScenarioLabel(e.target.value)}
                fullWidth
              />
              <TextField
                label="Commodity family"
                size="small"
                value={commodityFamily}
                onChange={(e) => setCommodityFamily(e.target.value)}
                required
                fullWidth
                helperText="Required for SG&A / margin benchmark lookup"
              />
              <TextField
                label="Region (optional)"
                size="small"
                value={region}
                onChange={(e) => setRegion(e.target.value)}
                fullWidth
              />
              <TextField
                label="Overhead pool code (optional)"
                size="small"
                value={overheadPoolCode}
                onChange={(e) => setOverheadPoolCode(e.target.value)}
                fullWidth
              />
              <TextField
                label="Allocation base quantity (optional)"
                size="small"
                value={allocationBase}
                onChange={(e) => setAllocationBase(e.target.value)}
                fullWidth
                helperText="e.g. total routing labor hours"
              />
              <Stack direction="row" spacing={1}>
                <Button
                  variant="contained"
                  startIcon={<CalculateOutlinedIcon />}
                  onClick={() => void handleGenerate()}
                  disabled={loading}
                >
                  Generate
                </Button>
                <Button variant="outlined" onClick={() => void handleLoadScenarios()} disabled={loading}>
                  Load scenarios
                </Button>
              </Stack>
            </Stack>
          </Paper>

          <Paper sx={{ p: 2, flex: 1.2, overflow: 'auto' }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
              Scenarios
            </Typography>
            {loading && !scenarios.length ? (
              <CircularProgress size={28} />
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Label</TableCell>
                    <TableCell align="right">Total</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Flags</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {scenarios.map((s) => (
                    <TableRow
                      key={s.buildUpId}
                      hover
                      selected={selected?.buildUpId === s.buildUpId}
                      onClick={() => void handleSelect(s)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{s.scenarioLabel}</TableCell>
                      <TableCell align="right" sx={{ fontFamily: 'ui-monospace, monospace' }}>
                        {formatMoney(s.totalCost)}
                      </TableCell>
                      <TableCell>
                        <Chip size="small" label={s.approvalStatus ?? '—'} variant="outlined" />
                      </TableCell>
                      <TableCell>
                        {s.locked && <Chip size="small" label="Locked" color="default" sx={{ mr: 0.5 }} />}
                        {s.staleInputsFlag && <Chip size="small" label="Stale" color="warning" />}
                      </TableCell>
                    </TableRow>
                  ))}
                  {!scenarios.length && (
                    <TableRow>
                      <TableCell colSpan={4}>
                        <Typography variant="body2" color="text.secondary">
                          No scenarios loaded.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}
          </Paper>
        </Stack>
      )}

      {tab === 'full' && selected && (
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
          <Paper sx={{ p: 2, flex: 1 }}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }} flexWrap="wrap" useFlexGap spacing={1}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                Selected: {selected.scenarioLabel}
              </Typography>
              <Stack direction="row" spacing={1} alignItems="center">
                <Chip
                  size="small"
                  label={selected.approvalStatus ?? 'DRAFT'}
                  color={
                    selected.approvalStatus === 'APPROVED'
                      ? 'success'
                      : selected.approvalStatus === 'SUBMITTED'
                        ? 'info'
                        : 'default'
                  }
                />
                <Button
                  size="small"
                  variant="contained"
                  color="secondary"
                  onClick={() => void handleSubmitApproval()}
                  disabled={
                    loading ||
                    selected.locked ||
                    selected.approvalStatus === 'SUBMITTED' ||
                    selected.approvalStatus === 'APPROVED'
                  }
                >
                  Submit for approval
                </Button>
                <Button
                  size="small"
                  startIcon={<LockOutlinedIcon />}
                  onClick={() => void handleLock()}
                  disabled={selected.locked || loading}
                >
                  Lock
                </Button>
              </Stack>
            </Stack>
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
              {selected.buildUpId} · total {selected.totalCost}
            </Typography>
            <Table size="small">
              <TableBody>
                {(
                  [
                    ['Material', selected.materialCost],
                    ['Process', selected.processCost],
                    ['Tooling', selected.toolingCost],
                    ['Packaging / freight', selected.packagingFreightCost],
                    ['Overhead', selected.overheadCost],
                    ['SG&A', selected.sgaCost],
                    ['Margin', selected.profitMargin],
                    ['Total', selected.totalCost],
                  ] as const
                ).map(([label, val]) => (
                  <TableRow key={label}>
                    <TableCell>{label}</TableCell>
                    <TableCell align="right" sx={{ fontFamily: 'ui-monospace, monospace', fontWeight: label === 'Total' ? 700 : 400 }}>
                      {formatMoney(val)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
          <Paper sx={{ p: 2, flex: 1 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
              Waterfall (SC-03)
            </Typography>
            {waterfall ? (
              <WaterfallChart elements={waterfall.elements} />
            ) : (
              <Typography variant="body2" color="text.secondary">
                Select or generate a build-up to load the waterfall.
              </Typography>
            )}
          </Paper>
        </Stack>
      )}

      {tab === 'parametric' && (
        <Paper sx={{ p: 2, maxWidth: 520 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
            Rapid parametric (SC-02)
          </Typography>
          <Stack spacing={2}>
            <TextField label="Material family" size="small" value={materialFamily} onChange={(e) => setMaterialFamily(e.target.value)} required />
            <TextField label="Process family" size="small" value={processFamily} onChange={(e) => setProcessFamily(e.target.value)} required />
            <TextField label="Region" size="small" value={paramRegion} onChange={(e) => setParamRegion(e.target.value)} required />
            <TextField label="Net weight (kg)" size="small" value={netWeightKg} onChange={(e) => setNetWeightKg(e.target.value)} required />
            <Button variant="contained" onClick={() => void handleParametric()} disabled={loading}>
              Estimate
            </Button>
            {parametric && (
              <Alert severity="success">
                Est. cost <strong>{formatMoney(parametric.estimatedCost)}</strong> ·{' '}
                {formatMoney(parametric.costPerKg)} / kg · weight {parametric.netWeightKg} kg
              </Alert>
            )}
          </Stack>
        </Paper>
      )}

      {tab === 'variance' && (
        <Paper sx={{ p: 2, maxWidth: 520 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
            Variance vs quoted price (SC-04)
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Uses the selected full build-up{selected ? ` (${selected.scenarioLabel})` : ' — select one on the Full tab first'}.
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="Quoted price"
              size="small"
              value={quotedPrice}
              onChange={(e) => setQuotedPrice(e.target.value)}
              required
            />
            <Button variant="contained" onClick={() => void handleVariance()} disabled={loading || !selected}>
              Compare
            </Button>
            {variance && (
              <Box>
                <Divider sx={{ mb: 1 }} />
                <Typography variant="body2">Should-cost (CALCULATED): {formatMoney(variance.shouldCost)}</Typography>
                <Typography variant="body2">Quoted (FACT / input): {formatMoney(variance.quotedPrice)}</Typography>
                <Typography variant="body2">Absolute variance: {formatMoney(variance.absoluteVariance)}</Typography>
                <Typography variant="body2">
                  Percentage: {variance.percentageVariance}%
                </Typography>
                {Number(variance.absoluteVariance) > 0 && (
                  <Alert severity="warning" sx={{ mt: 1 }}>
                    Quote is above should-cost by {formatMoney(variance.absoluteVariance)} (
                    {variance.percentageVariance}%). Investigate material/process assumptions — do not treat gap as negotiated award.
                  </Alert>
                )}
                {Number(variance.absoluteVariance) < 0 && (
                  <Alert severity="info" sx={{ mt: 1 }}>
                    Quote is below should-cost by {formatMoney(String(Math.abs(Number(variance.absoluteVariance))))}. Verify scope completeness and DATA NOT AVAILABLE gaps before accepting.
                  </Alert>
                )}
              </Box>
            )}

            {deepVarianceResult && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Component variance (P2-01)
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  {deepVarianceResult.explanation}
                </Typography>
                {deepVarianceResult.topContributors?.length > 0 && (
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    Top contributors: {deepVarianceResult.topContributors.join(', ')}
                  </Typography>
                )}
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Component</TableCell>
                      <TableCell align="right">Should-cost</TableCell>
                      <TableCell align="right">Quoted</TableCell>
                      <TableCell align="right">Variance</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {deepVarianceResult.components
                      .filter((c) => c.componentCode !== 'TOTAL')
                      .map((c) => (
                        <TableRow key={c.componentCode}>
                          <TableCell>{c.displayName}</TableCell>
                          <TableCell align="right">{formatMoney(c.shouldCostAmount)}</TableCell>
                          <TableCell align="right">
                            {c.quotedAmount != null ? formatMoney(c.quotedAmount) : '—'}
                          </TableCell>
                          <TableCell align="right">
                            {c.absoluteVariance != null ? formatMoney(c.absoluteVariance) : '—'}
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              label={c.dataStatus === 'AVAILABLE' ? 'AVAILABLE' : 'DATA NOT AVAILABLE'}
                              color={c.dataStatus === 'AVAILABLE' ? 'success' : 'default'}
                              variant="outlined"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </Box>
            )}

          </Stack>
        </Paper>
      )}
    </Stack>
  );
}