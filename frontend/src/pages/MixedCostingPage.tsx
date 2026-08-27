import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  PageHeader,
  SectionCard,
  StatusChip,
  LoadingBlock,
  ErrorBlock,
} from '../components/ux';
import { listPrograms } from '../api/programApi';
import { getBomTree } from '../api/bomApi';
import { listScenarios } from '../api/shouldCostApi';
import { acceptedQuotesForBomNode } from '../api/rfqApi';
import type { ShouldCostBuildUp } from '../types/api';
import type { SupplierQuote } from '../api/rfqApi';
import {
  calculateMixedCost,
  createMixedCost,
  freezeMixedCost,
  listMixedCosts,
  type MixedCostLegRequest,
  type MixedCostVersion,
} from '../api/mixedCostApi';
import { formatMoney } from '../utils/money';
import type { BomTreeNode, VehicleProgram } from '../types/api';
import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';

type DraftLeg = {
  alternativeType: 'INTERNAL_SHOULD_COST' | 'SUPPLIER_QUOTE';
  sourceRefId: string;
  mixRatioPct: string;
};

function flattenBom(nodes: BomTreeNode[], acc: BomTreeNode[] = []): BomTreeNode[] {
  for (const n of nodes) {
    acc.push(n);
    if (n.children?.length) flattenBom(n.children, acc);
  }
  return acc;
}

function nodeId(n: BomTreeNode): string {
  return n.bomNodeId;
}

function ratioSumPct(legs: DraftLeg[]): number {
  return legs.reduce((s, l) => s + (Number(l.mixRatioPct) || 0), 0);
}

export function MixedCostingPage() {
  const [programs, setPrograms] = useState<VehicleProgram[]>([]);
  const [programId, setProgramId] = useState('');
  const [bomNodes, setBomNodes] = useState<BomTreeNode[]>([]);
  const [bomNodeId, setBomNodeId] = useState('');
  const [versions, setVersions] = useState<MixedCostVersion[]>([]);
  const [selected, setSelected] = useState<MixedCostVersion | null>(null);
  const [versionLabel, setVersionLabel] = useState('Dual source mix');
  const [draftLegs, setDraftLegs] = useState<DraftLeg[]>([
    { alternativeType: 'INTERNAL_SHOULD_COST', sourceRefId: '', mixRatioPct: '60' },
    { alternativeType: 'SUPPLIER_QUOTE', sourceRefId: '', mixRatioPct: '40' },
  ]);
  const [scenarios, setScenarios] = useState<ShouldCostBuildUp[]>([]);
  const [quotes, setQuotes] = useState<SupplierQuote[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    setHelpingAgentPageContext({ currentPage: 'mixed-costing' });
    const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('pcs.lastProgramId') : null;
    void listPrograms()
      .then((list) => {
        setPrograms(list);
        if (stored && list.some((p) => p.programId === stored)) setProgramId(stored);
        else if (list[0]) setProgramId(list[0].programId);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Failed to load programs'));
  }, []);

  useEffect(() => {
    if (!programId) return;
    setLoading(true);
    setError(null);
    void getBomTree(programId)
      .then((tree) => {
        const flat = flattenBom(tree);
        setBomNodes(flat);
        const storedNode =
          typeof localStorage !== 'undefined' ? localStorage.getItem('pcs.lastBomNodeId') : null;
        if (storedNode && flat.some((n) => nodeId(n) === storedNode)) setBomNodeId(storedNode);
        else if (flat[0]) setBomNodeId(nodeId(flat[0]));
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Failed to load BOM'))
      .finally(() => setLoading(false));
  }, [programId]);

  const refreshVersions = useCallback(async (nodeId: string) => {
    if (!nodeId) return;
    setLoading(true);
    setError(null);
    try {
      const list = await listMixedCosts(nodeId);
      setVersions(list);
      if (list[0]) setSelected(list[0]);
      else setSelected(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load mixed costs');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (bomNodeId) void refreshVersions(bomNodeId);
  }, [bomNodeId, refreshVersions]);

  const sumPct = useMemo(() => ratioSumPct(draftLegs), [draftLegs]);
  const ratioOk = Math.abs(sumPct - 100) < 0.05;

  const onCreate = async () => {
    if (!bomNodeId || !ratioOk) return;
    setError(null);
    setInfo(null);
    try {
      const legs: MixedCostLegRequest[] = draftLegs.map((l) => ({
        alternativeType: l.alternativeType,
        sourceRefId: l.sourceRefId.trim(),
        mixRatio: (Number(l.mixRatioPct) / 100).toFixed(4),
      }));
      const created = await createMixedCost(bomNodeId, {
        versionLabel: versionLabel.trim() || 'Mixed cost',
        legs,
      });
      setInfo('Mixed-cost version created (DRAFT). Run Calculate to blend.');
      await refreshVersions(bomNodeId);
      setSelected(created);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create failed');
    }
  };

  const onCalculate = async () => {
    if (!selected) return;
    setError(null);
    setInfo(null);
    try {
      const result = await calculateMixedCost(selected.mixedCostId);
      setSelected(result);
      setVersions((prev) => prev.map((v) => (v.mixedCostId === result.mixedCostId ? result : v)));
      if (result.blendedStatus === 'AVAILABLE') {
        setInfo(`Blended unit cost: ${formatMoney(result.blendedUnitTotal)}`);
      } else {
        setInfo('Calculate completed — blended status is NOT AVAILABLE (see notes).');
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Calculate failed');
    }
  };

  const onFreeze = async () => {
    if (!selected) return;
    setError(null);
    setInfo(null);
    try {
      const result = await freezeMixedCost(selected.mixedCostId);
      setSelected(result);
      setVersions((prev) => prev.map((v) => (v.mixedCostId === result.mixedCostId ? result : v)));
      setInfo('Version frozen. Snapshots will not change if source data is edited.');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Freeze failed');
    }
  };


  useEffect(() => {
    if (!bomNodeId) {
      setScenarios([]);
      setQuotes([]);
      return;
    }
    void listScenarios(bomNodeId)
      .then((list) => {
        setScenarios(list);
        setDraftLegs((prev) =>
          prev.map((leg) => {
            if (leg.alternativeType === 'INTERNAL_SHOULD_COST' && !leg.sourceRefId && list[0]) {
              return { ...leg, sourceRefId: list[0].buildUpId };
            }
            return leg;
          }),
        );
      })
      .catch(() => setScenarios([]));
    void acceptedQuotesForBomNode(bomNodeId)
      .then((list) => {
        setQuotes(list);
        setDraftLegs((prev) =>
          prev.map((leg) => {
            if (leg.alternativeType === 'SUPPLIER_QUOTE' && !leg.sourceRefId && list[0]) {
              return { ...leg, sourceRefId: list[0].quoteId };
            }
            return leg;
          }),
        );
      })
      .catch(() => setQuotes([]));
  }, [bomNodeId]);

  const updateLeg = (idx: number, patch: Partial<DraftLeg>) => {
    setDraftLegs((prev) => prev.map((l, i) => (i === idx ? { ...l, ...patch } : l)));
  };

  return (
    <Box>
      <PageHeader
        title="Mixed Costing"
        description="Blend internal should-cost and supplier quotes with explicit mix ratios. Missing data and blocked suppliers never become zero."
        primaryAction={
          selected?.status === 'CALCULATED' && selected.blendedStatus === 'AVAILABLE'
            ? { label: 'Freeze version', onClick: () => void onFreeze() }
            : selected
              ? { label: 'Calculate blend', onClick: () => void onCalculate() }
              : undefined
        }
      />

      {error && <ErrorBlock message={error} onRetry={() => bomNodeId && void refreshVersions(bomNodeId)} />}
      {info && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Program</InputLabel>
          <Select
            label="Program"
            value={programId}
            onChange={(e) => {
              setProgramId(e.target.value);
              localStorage.setItem('pcs.lastProgramId', e.target.value);
            }}
          >
            {programs.map((p) => (
              <MenuItem key={p.programId} value={p.programId}>
                {p.modelName || p.programCode || p.programId}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 280 }}>
          <InputLabel>BOM node</InputLabel>
          <Select
            label="BOM node"
            value={bomNodeId}
            onChange={(e) => {
              setBomNodeId(e.target.value);
              localStorage.setItem('pcs.lastBomNodeId', e.target.value);
            }}
          >
            {bomNodes.map((n) => (
              <MenuItem key={nodeId(n)} value={nodeId(n)}>
                {n.partNumber || nodeId(n)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      {loading && <LoadingBlock />}

      <Stack spacing={2}>
        <SectionCard title="Create mixed-cost version">
          <Stack spacing={2}>
            <TextField
              size="small"
              label="Version label"
              value={versionLabel}
              onChange={(e) => setVersionLabel(e.target.value)}
              sx={{ maxWidth: 360 }}
            />
            <Typography variant="body2" color="text.secondary">
              Mix ratios must total 100%. Current sum:{' '}
              <strong style={{ color: ratioOk ? undefined : '#c62828' }}>{sumPct.toFixed(2)}%</strong>
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Type</TableCell>
                  <TableCell>Source (scenario or accepted quote)</TableCell>
                  <TableCell align="right">Mix %</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {draftLegs.map((leg, idx) => (
                  <TableRow key={idx}>
                    <TableCell>
                      <Select
                        size="small"
                        value={leg.alternativeType}
                        onChange={(e) =>
                          updateLeg(idx, {
                            alternativeType: e.target.value as DraftLeg['alternativeType'],
                          })
                        }
                      >
                        <MenuItem value="INTERNAL_SHOULD_COST">Internal should-cost</MenuItem>
                        <MenuItem value="SUPPLIER_QUOTE">Supplier quote</MenuItem>
                      </Select>
                    </TableCell>
                    <TableCell>
                      {leg.alternativeType === 'INTERNAL_SHOULD_COST' ? (
                        <FormControl size="small" fullWidth>
                          <Select
                            displayEmpty
                            value={leg.sourceRefId}
                            onChange={(e) => updateLeg(idx, { sourceRefId: e.target.value })}
                          >
                            <MenuItem value="">
                              <em>{scenarios.length ? 'Select scenario' : 'No scenarios'}</em>
                            </MenuItem>
                            {scenarios.map((sc) => (
                              <MenuItem key={sc.buildUpId} value={sc.buildUpId}>
                                {(sc.scenarioLabel || 'Scenario') + ' · ' + String(sc.totalCost ?? 'n/a')}
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>
                      ) : (
                        <FormControl size="small" fullWidth>
                          <Select
                            displayEmpty
                            value={leg.sourceRefId}
                            onChange={(e) => updateLeg(idx, { sourceRefId: e.target.value })}
                          >
                            <MenuItem value="">
                              <em>{quotes.length ? 'Select accepted quote' : 'No accepted quotes'}</em>
                            </MenuItem>
                            {quotes.map((q) => (
                              <MenuItem key={q.quoteId} value={q.quoteId}>
                                {(q.supplierId || 'Supplier') + ' · ' + String(q.unitPrice)}
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>
                      )}
                    </TableCell>
                    <TableCell align="right">
                      <TextField
                        size="small"
                        type="number"
                        value={leg.mixRatioPct}
                        onChange={(e) => updateLeg(idx, { mixRatioPct: e.target.value })}
                        sx={{ width: 100 }}
                        inputProps={{ min: 0, max: 100, step: 1 }}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Box>
              <Button variant="contained" disabled={!bomNodeId || !ratioOk} onClick={() => void onCreate()}>
                Create version
              </Button>
            </Box>
          </Stack>
        </SectionCard>

        <SectionCard title="Versions for this BOM node">
          {!versions.length && !loading ? (
            <Typography color="text.secondary" variant="body2">
              No mixed-cost versions yet. Create a version with at least two legs whose mix ratios sum to 100%.
            </Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Label</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Blend status</TableCell>
                  <TableCell align="right">Blended unit</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {versions.map((v) => (
                  <TableRow
                    key={v.mixedCostId}
                    hover
                    selected={selected?.mixedCostId === v.mixedCostId}
                    onClick={() => setSelected(v)}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell>{v.versionLabel}</TableCell>
                    <TableCell>
                      <StatusChip status={v.status} />
                    </TableCell>
                    <TableCell>
                      <StatusChip status={v.blendedStatus || 'NOT_AVAILABLE'} />
                    </TableCell>
                    <TableCell align="right">
                      {v.blendedUnitTotal != null ? formatMoney(v.blendedUnitTotal) : '—'}
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => setSelected(v)}>
                        Open
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </SectionCard>

        {selected && (
          <SectionCard title={`Detail — ${selected.versionLabel}`}>
            <Stack spacing={1} sx={{ mb: 2 }} direction="row" flexWrap="wrap" useFlexGap>
              <StatusChip status={selected.status} />
              <StatusChip status={selected.blendedStatus || 'NOT_AVAILABLE'} />
              {selected.blendedUnitTotal != null && (
                <Typography variant="h6" component="span">
                  Blended: {formatMoney(selected.blendedUnitTotal)}
                </Typography>
              )}
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>#</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell align="right">Ratio</TableCell>
                  <TableCell align="right">Unit snapshot</TableCell>
                  <TableCell>Eligibility</TableCell>
                  <TableCell>Data</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(selected.legs || []).map((leg) => (
                  <TableRow key={leg.legSeq}>
                    <TableCell>{leg.legSeq}</TableCell>
                    <TableCell>{leg.alternativeType}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>{leg.sourceRefId}</TableCell>
                    <TableCell align="right">
                      {(Number(leg.mixRatio) * 100).toFixed(2)}%
                    </TableCell>
                    <TableCell align="right">
                      {leg.unitCostSnapshot != null ? formatMoney(leg.unitCostSnapshot) : 'DATA NOT AVAILABLE'}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={leg.eligibilityStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusChip status={leg.dataStatus} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {(selected.dataQualityNotes?.length ?? 0) > 0 && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <Typography variant="subtitle2">Data quality</Typography>
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {selected.dataQualityNotes!.map((n, i) => (
                    <li key={i}>{n}</li>
                  ))}
                </ul>
              </Alert>
            )}
            <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
              {selected.status !== 'FROZEN' && (
                <Button variant="contained" onClick={() => void onCalculate()}>
                  Calculate blend
                </Button>
              )}
              {selected.status === 'CALCULATED' && selected.blendedStatus === 'AVAILABLE' && (
                <Button variant="outlined" onClick={() => void onFreeze()}>
                  Freeze
                </Button>
              )}
            </Stack>
          </SectionCard>
        )}
      </Stack>
    </Box>
  );
}
