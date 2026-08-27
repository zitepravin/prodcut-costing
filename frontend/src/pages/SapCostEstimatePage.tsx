import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
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
import {
  compareSapToShouldCost,
  listSapComponentMappings,
  listSapCostEstimates,
  remapSapComponents,
  upsertSapComponentMapping,
  type SapCompareResult,
  type SapMapping,
  type SapStaging,
} from '../api/sapCostApi';
import { formatMoney } from '../utils/money';
import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';

export function SapCostEstimatePage() {
  const [rows, setRows] = useState<SapStaging[]>([]);
  const [selected, setSelected] = useState<SapStaging | null>(null);
  const [mappings, setMappings] = useState<SapMapping[]>([]);
  const [buildUpId, setBuildUpId] = useState('');
  const [compare, setCompare] = useState<SapCompareResult | null>(null);
  const [sapCode, setSapCode] = useState('');
  const [ourCode, setOurCode] = useState('MATERIAL');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, maps] = await Promise.all([listSapCostEstimates(), listSapComponentMappings()]);
      setRows(list);
      setMappings(maps);
      if (selected) {
        const updated = list.find((r) => r.stagingId === selected.stagingId);
        if (updated) setSelected(updated);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load SAP estimates');
    } finally {
      setLoading(false);
    }
  }, [selected]);

  useEffect(() => {
    setHelpingAgentPageContext({ currentPage: 'sap-cost-estimates' });
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onRemap = async () => {
    if (!selected) return;
    setError(null);
    try {
      const updated = await remapSapComponents(selected.stagingId);
      setSelected(updated);
      setInfo('Component mapping refreshed');
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Remap failed');
    }
  };

  const onCompare = async () => {
    if (!selected || !buildUpId.trim()) return;
    setError(null);
    setCompare(null);
    try {
      const result = await compareSapToShouldCost(selected.stagingId, buildUpId.trim());
      setCompare(result);
      setInfo(`Compare status: ${result.totalStatus}`);
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Compare failed');
    }
  };

  const onSaveMapping = async () => {
    if (!sapCode.trim() || !ourCode.trim()) return;
    setError(null);
    try {
      await upsertSapComponentMapping({
        sapComponentCode: sapCode.trim(),
        ourComponentCode: ourCode.trim(),
      });
      setInfo(`Mapped ${sapCode} → ${ourCode}`);
      setSapCode('');
      await refresh();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Mapping save failed');
    }
  };

  return (
    <Box>
      <PageHeader
        title="SAP Cost Estimates"
        description="Landed SAP CO cost estimates (via customer CPI). Map components and compare to should-cost. No live SAP RFC from this app."
        primaryAction={{ label: 'Refresh', onClick: () => void refresh() }}
      />

      {error && <ErrorBlock message={error} onRetry={() => void refresh()} />}
      {info && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}
      {loading && <LoadingBlock />}

      <Stack spacing={2}>
        <SectionCard title="Landed estimates">
          {!rows.length && !loading ? (
            <Typography color="text.secondary" variant="body2">
              No staged SAP estimates yet. Customer CPI should POST to /api/v1/erp/sap/cost-estimates.
            </Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Material</TableCell>
                  <TableCell>Plant</TableCell>
                  <TableCell>Currency</TableCell>
                  <TableCell align="right">SAP total</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((r) => (
                  <TableRow
                    key={r.stagingId}
                    hover
                    selected={selected?.stagingId === r.stagingId}
                    onClick={() => {
                      setSelected(r);
                      setCompare(null);
                    }}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell>{r.sapMaterial}</TableCell>
                    <TableCell>{r.sapPlant}</TableCell>
                    <TableCell>{r.currencyCode || '—'}</TableCell>
                    <TableCell align="right">
                      {r.totalCost != null ? formatMoney(r.totalCost) : '—'}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={r.status} />
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => setSelected(r)}>
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
          <SectionCard title={`Detail — ${selected.sapMaterial} / ${selected.sapPlant}`}>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
              <StatusChip status={selected.status} />
              {selected.totalCost != null && (
                <Typography variant="h6">SAP total: {formatMoney(selected.totalCost)}</Typography>
              )}
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>SAP component</TableCell>
                  <TableCell>Mapped to</TableCell>
                  <TableCell align="right">Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(selected.components || []).map((c) => (
                  <TableRow key={c.componentSeq}>
                    <TableCell>
                      {c.sapComponentCode}
                      {c.sapComponentName ? ` — ${c.sapComponentName}` : ''}
                    </TableCell>
                    <TableCell>{c.mappedComponentCode || 'NOT MAPPED'}</TableCell>
                    <TableCell align="right">{formatMoney(c.amount)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 2 }} alignItems="flex-start">
              <Button variant="outlined" onClick={() => void onRemap()}>
                Refresh mapping
              </Button>
              <TextField
                size="small"
                label="Should-cost build-up ID"
                value={buildUpId}
                onChange={(e) => setBuildUpId(e.target.value)}
                sx={{ minWidth: 280 }}
              />
              <Button variant="contained" disabled={!buildUpId.trim()} onClick={() => void onCompare()}>
                Compare to should-cost
              </Button>
            </Stack>
          </SectionCard>
        )}

        {compare && (
          <SectionCard title="Compare result">
            <Stack direction="row" spacing={2} sx={{ mb: 1 }} flexWrap="wrap" useFlexGap>
              <StatusChip status={compare.totalStatus} />
              <Typography>
                SAP: {compare.sapTotal != null ? formatMoney(compare.sapTotal) : 'N/A'}
              </Typography>
              <Typography>
                Should-cost: {compare.shouldTotal != null ? formatMoney(compare.shouldTotal) : 'N/A'}
              </Typography>
              <Typography>
                Variance:{' '}
                {compare.absoluteVariance != null ? formatMoney(compare.absoluteVariance) : 'N/A'}
              </Typography>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Component</TableCell>
                  <TableCell align="right">SAP</TableCell>
                  <TableCell align="right">Should-cost</TableCell>
                  <TableCell align="right">Variance</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {compare.components.map((c) => (
                  <TableRow key={c.code}>
                    <TableCell>{c.code}</TableCell>
                    <TableCell align="right">
                      {c.sapAmount != null ? formatMoney(c.sapAmount) : 'DATA NOT AVAILABLE'}
                    </TableCell>
                    <TableCell align="right">
                      {c.shouldAmount != null ? formatMoney(c.shouldAmount) : 'DATA NOT AVAILABLE'}
                    </TableCell>
                    <TableCell align="right">
                      {c.variance != null ? formatMoney(c.variance) : '—'}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={c.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {(compare.dataQualityNotes?.length ?? 0) > 0 && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {compare.dataQualityNotes.map((n, i) => (
                    <li key={i}>{n}</li>
                  ))}
                </ul>
              </Alert>
            )}
          </SectionCard>
        )}

        <SectionCard title="Component mapping (SAP → our codes)">
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Example our codes: MATERIAL, PROCESS, TOOLING, PACKAGING_FREIGHT, OVERHEAD, SGA, PROFIT_MARGIN
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mb: 2 }}>
            <TextField size="small" label="SAP component code" value={sapCode} onChange={(e) => setSapCode(e.target.value)} />
            <TextField size="small" label="Our component code" value={ourCode} onChange={(e) => setOurCode(e.target.value)} />
            <Button variant="contained" onClick={() => void onSaveMapping()}>
              Save mapping
            </Button>
          </Stack>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>SAP code</TableCell>
                <TableCell>Our code</TableCell>
                <TableCell>Notes</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {mappings.map((m) => (
                <TableRow key={m.mappingId}>
                  <TableCell>{m.sapComponentCode}</TableCell>
                  <TableCell>{m.ourComponentCode}</TableCell>
                  <TableCell>{m.notes || '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </SectionCard>
      </Stack>
    </Box>
  );
}
