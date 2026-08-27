import { useCallback, useEffect, useState } from 'react';
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
  applyMakeVsBuyToBom,
  compareMakeVsBuy,
  createMakeVsBuy,
  decideMakeVsBuy,
  exportMakeVsBuyPackage,
  listMakeVsBuy,
  type MakeVsBuySimulation,
} from '../api/makeVsBuyApi';
import { formatMoney } from '../utils/money';
import type { BomTreeNode, VehicleProgram } from '../types/api';
import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';

function flattenBom(nodes: BomTreeNode[], acc: BomTreeNode[] = []): BomTreeNode[] {
  for (const n of nodes) {
    acc.push(n);
    if (n.children?.length) flattenBom(n.children, acc);
  }
  return acc;
}

export function MakeVsBuyPage() {
  const [programs, setPrograms] = useState<VehicleProgram[]>([]);
  const [programId, setProgramId] = useState('');
  const [bomNodes, setBomNodes] = useState<BomTreeNode[]>([]);
  const [bomNodeId, setBomNodeId] = useState('');
  const [sims, setSims] = useState<MakeVsBuySimulation[]>([]);
  const [selected, setSelected] = useState<MakeVsBuySimulation | null>(null);
  const [label, setLabel] = useState('Make vs Buy decision');
  const [buildUpId, setBuildUpId] = useState('');
  const [mixedCostId, setMixedCostId] = useState('');
  const [scenarios, setScenarios] = useState<ShouldCostBuildUp[]>([]);
  const [quotes, setQuotes] = useState<SupplierQuote[]>([]);
  const [selectedQuoteIds, setSelectedQuoteIds] = useState<string[]>([]);
  const [decisionCode, setDecisionCode] = useState('APPROVE_MAKE');
  const [decisionReason, setDecisionReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    setHelpingAgentPageContext({ currentPage: 'make-vs-buy' });
    const stored = localStorage.getItem('pcs.lastProgramId');
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
    void getBomTree(programId)
      .then((tree) => {
        const flat = flattenBom(tree);
        setBomNodes(flat);
        const sn = localStorage.getItem('pcs.lastBomNodeId');
        if (sn && flat.some((n) => n.bomNodeId === sn)) setBomNodeId(sn);
        else if (flat[0]) setBomNodeId(flat[0].bomNodeId);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'BOM load failed'));
  }, [programId]);

  const refresh = useCallback(async () => {
    if (!bomNodeId) return;
    setLoading(true);
    setError(null);
    try {
      const list = await listMakeVsBuy(bomNodeId);
      setSims(list);
      if (list[0]) setSelected(list[0]);
      else setSelected(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Load failed');
    } finally {
      setLoading(false);
    }
  }, [bomNodeId]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    if (!bomNodeId) {
      setScenarios([]);
      setQuotes([]);
      setBuildUpId('');
      setSelectedQuoteIds([]);
      return;
    }
    void listScenarios(bomNodeId)
      .then((list) => {
        setScenarios(list);
        if (list[0] && !buildUpId) setBuildUpId(list[0].buildUpId);
      })
      .catch(() => setScenarios([]));
    void acceptedQuotesForBomNode(bomNodeId)
      .then((list) => setQuotes(list))
      .catch(() => setQuotes([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bomNodeId]);


  const onCreate = async () => {
    if (!bomNodeId) return;
    setError(null);
    try {
      const created = await createMakeVsBuy(bomNodeId, {
        label,
        selectedBuildUpId: buildUpId || undefined,
        buyQuoteIds: selectedQuoteIds.length ? selectedQuoteIds : undefined,
        mixedCostId: mixedCostId || undefined,
      });
      setSelected(created);
      setInfo('Simulation created. Run Compare for draft recommendation.');
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create failed');
    }
  };

  const onCompare = async () => {
    if (!selected) return;
    try {
      const r = await compareMakeVsBuy(selected.simulationId);
      setSelected(r);
      setInfo(r.recommendationSummary || 'Compared');
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Compare failed');
    }
  };

  const onDecide = async () => {
    if (!selected || !decisionReason.trim()) return;
    try {
      const r = await decideMakeVsBuy(selected.simulationId, decisionCode, decisionReason.trim());
      setSelected(r);
      setInfo('Decision recorded (human). Optional: Apply to BOM.');
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Decide failed');
    }
  };

  const onApply = async () => {
    if (!selected) return;
    try {
      const r = await applyMakeVsBuyToBom(selected.simulationId);
      setSelected(r);
      setInfo('MakeBuyFlag applied on BOM node.');
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Apply failed');
    }
  };

  const onExportPackage = async () => {
    if (!selected) return;
    setError(null);
    try {
      const pkg = await exportMakeVsBuyPackage(selected.simulationId);
      const blob = new Blob([JSON.stringify(pkg, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `make-vs-buy-export-${selected.simulationId.slice(0, 8)}.json`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
      setInfo(
        'Export package downloaded (read-only JSON for CPI/SAP process teams). System does not post to SAP CO.',
      );
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Export package failed');
    }
  };

  return (
    <Box>
      <PageHeader
        title="Make vs Buy"
        description="Compare MAKE (should-cost), BUY (quotes), and optional MIXED. Draft recommendation only — human decides. NOT_ELIGIBLE suppliers cannot win."
        primaryAction={
          selected?.status === 'COMPARED'
            ? { label: 'Record decision', onClick: () => void onDecide(), disabled: !decisionReason.trim() }
            : selected
              ? { label: 'Compare', onClick: () => void onCompare() }
              : undefined
        }
      />

      {error && <ErrorBlock message={error} onRetry={() => void refresh()} />}
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
              <MenuItem key={n.bomNodeId} value={n.bomNodeId}>
                {n.partNumber || n.bomNodeId}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      {loading && <LoadingBlock />}

      <Stack spacing={2}>
        <SectionCard title="Create simulation">
          <Stack spacing={1.5}>
            <TextField size="small" label="Label" value={label} onChange={(e) => setLabel(e.target.value)} fullWidth />
            <FormControl size="small" fullWidth>
              <InputLabel>Should-cost scenario (MAKE)</InputLabel>
              <Select
                label="Should-cost scenario (MAKE)"
                value={buildUpId}
                onChange={(e) => setBuildUpId(e.target.value)}
              >
                {scenarios.map((sc) => (
                  <MenuItem key={sc.buildUpId} value={sc.buildUpId}>
                    {(sc.scenarioLabel || 'Scenario') + ' · ' + (sc.totalCost != null ? String(sc.totalCost) : 'n/a') + ' · ' + sc.buildUpId.slice(0, 8)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {!scenarios.length && (
              <Typography variant="caption" color="text.secondary">No should-cost scenarios for this node — generate one under Should-Cost first.</Typography>
            )}
            <FormControl size="small" fullWidth>
              <InputLabel>Accepted quotes (BUY)</InputLabel>
              <Select
                multiple
                label="Accepted quotes (BUY)"
                value={selectedQuoteIds}
                onChange={(e) => {
                  const v = e.target.value;
                  setSelectedQuoteIds(typeof v === 'string' ? v.split(',') : v);
                }}
                renderValue={(selected) =>
                  (selected as string[])
                    .map((id) => {
                      const q = quotes.find((x) => x.quoteId === id);
                      return q ? `${q.supplierId?.slice(0, 6) || 'sup'} @ ${q.unitPrice}` : id.slice(0, 8);
                    })
                    .join(', ')
                }
              >
                {quotes.map((q) => (
                  <MenuItem key={q.quoteId} value={q.quoteId}>
                    {(q.supplierId || 'Supplier') + ' · unit ' + String(q.unitPrice) + ' · ' + (q.currencyCode || '')}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {!quotes.length && (
              <Typography variant="caption" color="text.secondary">No accepted quotes for this node — accept quotes under RFQ first.</Typography>
            )}
            <TextField size="small" label="Mixed-cost version ID (optional)" value={mixedCostId} onChange={(e) => setMixedCostId(e.target.value)} fullWidth helperText="Optional — paste mixed-cost version UUID if used" />
            <Box>
              <Button variant="contained" disabled={!bomNodeId} onClick={() => void onCreate()}>
                Create
              </Button>
            </Box>
          </Stack>
        </SectionCard>

        <SectionCard title="Simulations">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Label</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Draft recommendation</TableCell>
                <TableCell>Decision</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {sims.map((s) => (
                <TableRow key={s.simulationId} hover selected={selected?.simulationId === s.simulationId} onClick={() => setSelected(s)} sx={{ cursor: 'pointer' }}>
                  <TableCell>{s.label}</TableCell>
                  <TableCell>
                    <StatusChip status={s.status} />
                  </TableCell>
                  <TableCell>{s.recommendationCode || '—'}</TableCell>
                  <TableCell>{s.decisionCode || '—'}</TableCell>
                  <TableCell align="right">
                    <Button size="small">Open</Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </SectionCard>

        {selected && (
          <SectionCard title={`Detail — ${selected.label}`}>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
              <StatusChip status={selected.status} />
              {selected.recommendationCode && <StatusChip status={selected.recommendationCode} />}
              {selected.decisionCode && <StatusChip status={selected.decisionCode} />}
            </Stack>
            {selected.recommendationSummary && (
              <Alert severity="info" sx={{ mb: 2 }}>
                <strong>Draft recommendation (not approval):</strong> {selected.recommendationSummary}
              </Alert>
            )}
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Strategy</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell align="right">Unit</TableCell>
                  <TableCell>TCO</TableCell>
                  <TableCell>Eligibility</TableCell>
                  <TableCell>Data</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(selected.legs || []).map((l) => (
                  <TableRow key={l.legSeq}>
                    <TableCell>{l.strategyType}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>{l.sourceRefId}</TableCell>
                    <TableCell align="right">
                      {l.unitCostSnapshot != null ? formatMoney(l.unitCostSnapshot) : 'DATA NOT AVAILABLE'}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={l.tcoStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusChip status={l.eligibilityStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusChip status={l.dataStatus} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {(selected.dataQualityNotes?.length ?? 0) > 0 && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {selected.dataQualityNotes!.map((n, i) => (
                    <li key={i}>{n}</li>
                  ))}
                </ul>
              </Alert>
            )}
            <Stack direction="row" spacing={1} sx={{ mt: 2 }} flexWrap="wrap" useFlexGap>
              {selected.status !== 'DECIDED' && (
                <Button variant="contained" onClick={() => void onCompare()}>
                  Compare
                </Button>
              )}
              <Button variant="outlined" onClick={() => void onExportPackage()}>
                Export package
              </Button>
            </Stack>
            {selected.status === 'COMPARED' || selected.status === 'DECIDED' ? (
              <Stack spacing={1} sx={{ mt: 2 }}>
                <Typography variant="subtitle2">Human decision</Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                  <FormControl size="small" sx={{ minWidth: 180 }}>
                    <InputLabel>Decision</InputLabel>
                    <Select label="Decision" value={decisionCode} onChange={(e) => setDecisionCode(e.target.value)}>
                      <MenuItem value="APPROVE_MAKE">Approve MAKE</MenuItem>
                      <MenuItem value="APPROVE_BUY">Approve BUY</MenuItem>
                      <MenuItem value="APPROVE_MIXED">Approve MIXED</MenuItem>
                      <MenuItem value="DEFER">Defer</MenuItem>
                      <MenuItem value="REJECT">Reject</MenuItem>
                    </Select>
                  </FormControl>
                  <TextField
                    size="small"
                    fullWidth
                    required
                    label="Reason"
                    value={decisionReason}
                    onChange={(e) => setDecisionReason(e.target.value)}
                  />
                  <Button variant="contained" disabled={!decisionReason.trim() || selected.status === 'DECIDED'} onClick={() => void onDecide()}>
                    Record decision
                  </Button>
                  {selected.status === 'DECIDED' && !selected.bomFlagApplied && (
                    <Button variant="outlined" onClick={() => void onApply()}>
                      Apply to BOM
                    </Button>
                  )}
                </Stack>
                <Typography variant="caption" color="text.secondary">
                  Export package is read-only JSON for CPI/SAP process teams. It does not post into SAP CO.
                </Typography>
              </Stack>
            ) : null}
          </SectionCard>
        )}
      </Stack>
    </Box>
  );
}
