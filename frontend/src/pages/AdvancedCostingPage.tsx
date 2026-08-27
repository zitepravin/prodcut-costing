import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
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
import TimelineOutlinedIcon from '@mui/icons-material/TimelineOutlined';
import {
  createCostingRun,
  executeCostingRun,
  listCostingRuns,
  lotSizeCurve,
  varianceCompare,
} from '../api/phaseBApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function AdvancedCostingPage() {
  const [error, setError] = useState<string | null>(null);
  const [runs, setRuns] = useState<Array<Record<string, unknown>>>([]);
  const [runCode, setRunCode] = useState('');
  const [programId, setProgramId] = useState('');
  const [bomNodes, setBomNodes] = useState('');
  const [runDetail, setRunDetail] = useState<Record<string, unknown> | null>(null);

  const [curveBom, setCurveBom] = useState('');
  const [curve, setCurve] = useState<Record<string, unknown> | null>(null);

  const [varBom, setVarBom] = useState('');
  const [baselineType, setBaselineType] = useState('SHOULD_COST');
  const [comparisonType, setComparisonType] = useState('TARGET');
  const [quoteTotal, setQuoteTotal] = useState('');
  const [variance, setVariance] = useState<Record<string, unknown> | null>(null);

  const loadRuns = useCallback(async () => {
    try {
      setRuns(await listCostingRuns());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void loadRuns();
  }, [loadRuns]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <TimelineOutlinedIcon color="primary" />
        <Typography variant="h5">Advanced costing (B1–B3)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Mass costing runs, lot-size unit-cost curves, and driver-level variance. Reuses quantity-structure and
        should-cost data — does not invent rates or actuals.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            B1 — Costing run
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Run code" value={runCode} onChange={(e) => setRunCode(e.target.value)} required />
            <TextField size="small" label="Program ID" value={programId} onChange={(e) => setProgramId(e.target.value)} sx={{ minWidth: 260 }} />
            <TextField
              size="small"
              label="BOM node IDs (comma)"
              value={bomNodes}
              onChange={(e) => setBomNodes(e.target.value)}
              sx={{ minWidth: 280 }}
            />
            <Button
              variant="contained"
              disabled={!runCode.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    const ids = bomNodes
                      .split(',')
                      .map((s) => s.trim())
                      .filter(Boolean);
                    const created = await createCostingRun({
                      runCode: runCode.trim(),
                      programId: programId.trim() || undefined,
                      bomNodeIds: ids.length ? ids : undefined,
                    });
                    setRunDetail(created);
                    await loadRuns();
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create
            </Button>
            <Button
              variant="outlined"
              disabled={!runDetail?.run || !(runDetail.run as Record<string, unknown>).runId}
              onClick={() => {
                void (async () => {
                  try {
                    const id = String((runDetail!.run as Record<string, unknown>).runId);
                    setRunDetail(await executeCostingRun(id));
                    await loadRuns();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Execute
            </Button>
          </Stack>
          {runDetail?.run != null && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              {`Status ${String((runDetail.run as Record<string, unknown>).status)} · success ${String(
                (runDetail.run as Record<string, unknown>).successCount,
              )} · fail ${String((runDetail.run as Record<string, unknown>).failureCount)}`}
            </Typography>
          )}
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">OK</TableCell>
                <TableCell align="right">Fail</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {runs.map((r) => (
                <TableRow key={String(r.runId)}>
                  <TableCell>{String(r.runCode)}</TableCell>
                  <TableCell>{String(r.status)}</TableCell>
                  <TableCell align="right">{String(r.successCount)}</TableCell>
                  <TableCell align="right">{String(r.failureCount)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            B2 — Lot-size curve
          </Typography>
          <Stack direction="row" spacing={1}>
            <TextField size="small" label="BOM node ID" value={curveBom} onChange={(e) => setCurveBom(e.target.value)} required sx={{ minWidth: 280 }} />
            <Button
              variant="contained"
              disabled={!curveBom.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setCurve(await lotSizeCurve({ bomNodeId: curveBom.trim() }));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Curve
            </Button>
          </Stack>
          {curve && Array.isArray(curve.points) && (
            <>
              <Typography variant="body2" sx={{ mt: 1 }}>
                Lowest unit cost lot: {String(curve.lowestUnitCostLotSize ?? '—')}
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell align="right">Lot</TableCell>
                    <TableCell align="right">Unit cost</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(curve.points as Array<Record<string, unknown>>).map((p, i) => (
                    <TableRow key={i}>
                      <TableCell align="right">{String(p.lotSize)}</TableCell>
                      <TableCell align="right">{String(p.unitCost ?? '—')}</TableCell>
                      <TableCell>{String(p.status)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            B3 — Variance
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="BOM node ID" value={varBom} onChange={(e) => setVarBom(e.target.value)} required sx={{ minWidth: 260 }} />
            <TextField select size="small" label="Baseline" value={baselineType} onChange={(e) => setBaselineType(e.target.value)} sx={{ minWidth: 160 }}>
              {['SHOULD_COST', 'PREVIOUS_SHOULD_COST', 'TARGET'].map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <TextField select size="small" label="Comparison" value={comparisonType} onChange={(e) => setComparisonType(e.target.value)} sx={{ minWidth: 160 }}>
              {['SHOULD_COST', 'TARGET', 'QUOTE', 'PREVIOUS_SHOULD_COST'].map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="Quote total" value={quoteTotal} onChange={(e) => setQuoteTotal(e.target.value)} />
            <Button
              variant="contained"
              disabled={!varBom.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setVariance(
                      await varianceCompare({
                        bomNodeId: varBom.trim(),
                        baselineType,
                        comparisonType,
                        comparisonTotal: quoteTotal.trim() || undefined,
                      }),
                    );
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Compare
            </Button>
          </Stack>
          {variance && (
            <>
              <Typography variant="body2" sx={{ mt: 1 }}>
                {String(variance.summary)} · Δ {String(variance.absoluteVariance)} (
                {String(variance.percentVariance)}%)
              </Typography>
              {Array.isArray(variance.drivers) && (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Driver</TableCell>
                      <TableCell align="right">Baseline</TableCell>
                      <TableCell align="right">Compare</TableCell>
                      <TableCell align="right">Δ</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(variance.drivers as Array<Record<string, unknown>>).map((d) => (
                      <TableRow key={String(d.driverCode)}>
                        <TableCell>{String(d.driverName)}</TableCell>
                        <TableCell align="right">{String(d.baselineAmount)}</TableCell>
                        <TableCell align="right">{String(d.comparisonAmount)}</TableCell>
                        <TableCell align="right">{String(d.absoluteVariance)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
