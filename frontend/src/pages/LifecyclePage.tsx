import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
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
import { PageHeader, SectionCard, LoadingBlock, ErrorBlock } from '../components/ux';
import { listPrograms } from '../api/programApi';
import {
  activateVolumeCurve,
  fetchLifecycleProjection,
  upsertVolumeCurve,
  type CurveYear,
  type LifecycleProjection,
} from '../api/lifecycleApi';
import { formatMoney } from '../utils/money';
import type { VehicleProgram } from '../types/api';
import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';

const SCENARIOS = ['BASE', 'HIGH', 'LOW'];

export function LifecyclePage() {
  const [programs, setPrograms] = useState<VehicleProgram[]>([]);
  const [programId, setProgramId] = useState('');
  const [scenario, setScenario] = useState('BASE');
  const [years, setYears] = useState<CurveYear[]>([
    { yearIndex: 1, calendarYear: new Date().getFullYear() + 1, plannedVolume: '50000' },
    { yearIndex: 2, calendarYear: new Date().getFullYear() + 2, plannedVolume: '100000' },
    { yearIndex: 3, calendarYear: new Date().getFullYear() + 3, plannedVolume: '120000' },
  ]);
  const [projection, setProjection] = useState<LifecycleProjection | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    setHelpingAgentPageContext({ currentPage: 'lifecycle' });
    const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('pcs.lastProgramId') : null;
    void listPrograms()
      .then((list) => {
        setPrograms(list);
        if (stored && list.some((p) => p.programId === stored)) {
          setProgramId(stored);
        } else if (list[0]) {
          setProgramId(list[0].programId);
        }
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Failed to load programs'));
  }, []);

  const saveCurve = async () => {
    if (!programId) return;
    setError(null);
    setInfo(null);
    try {
      await upsertVolumeCurve(programId, scenario, {
        scenarioLabel: scenario,
        years: years.map((y) => ({
          yearIndex: Number(y.yearIndex),
          calendarYear: y.calendarYear ? Number(y.calendarYear) : undefined,
          plannedVolume: String(y.plannedVolume),
        })),
      });
      await activateVolumeCurve(programId, scenario);
      setInfo(`Curve ${scenario} saved and activated.`);
      localStorage.setItem('pcs.lastProgramId', programId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Save failed');
    }
  };

  const runProjection = useCallback(async () => {
    if (!programId) return;
    setLoading(true);
    setError(null);
    setProjection(null);
    try {
      const p = await fetchLifecycleProjection(programId, scenario);
      setProjection(p);
      localStorage.setItem('pcs.lastProgramId', programId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Projection failed');
    } finally {
      setLoading(false);
    }
  }, [programId, scenario]);

  const updateYear = (idx: number, field: keyof CurveYear, value: string) => {
    setYears((prev) =>
      prev.map((y, i) => (i === idx ? { ...y, [field]: value === '' ? '' : value } : y)),
    );
  };

  return (
    <Stack spacing={2}>
      <PageHeader
        title="Lifecycle cost"
        description="Volume-curve forecasting: unit and program cost by year. Tooling is re-amortized over curve lifetime; other costs held per unit."
        primaryAction={{ label: 'Run projection', onClick: () => void runProjection(), disabled: !programId }}
        secondaryActions={
          <Button variant="outlined" onClick={() => void saveCurve()} disabled={!programId}>
            Save & activate curve
          </Button>
        }
      />

      {error && <ErrorBlock message={error} onRetry={() => void runProjection()} />}
      {info && <Alert severity="success">{info}</Alert>}

      <SectionCard title="Program & scenario" dense>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel id="prog-label">Program</InputLabel>
            <Select
              labelId="prog-label"
              label="Program"
              value={programId}
              onChange={(e) => setProgramId(e.target.value)}
            >
              {programs.map((p) => (
                <MenuItem key={p.programId} value={p.programId}>
                  {p.programCode}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel id="sc-label">Scenario</InputLabel>
            <Select
              labelId="sc-label"
              label="Scenario"
              value={scenario}
              onChange={(e) => setScenario(e.target.value)}
            >
              {SCENARIOS.map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>
      </SectionCard>

      <SectionCard title="Volume curve (years)" description="Planned volume per year. Save & activate before projecting.">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Year index</TableCell>
              <TableCell>Calendar year</TableCell>
              <TableCell align="right">Planned volume</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {years.map((y, i) => (
              <TableRow key={y.yearIndex}>
                <TableCell>{y.yearIndex}</TableCell>
                <TableCell>
                  <TextField
                    size="small"
                    value={y.calendarYear ?? ''}
                    onChange={(e) => updateYear(i, 'calendarYear', e.target.value)}
                    sx={{ width: 100 }}
                  />
                </TableCell>
                <TableCell align="right">
                  <TextField
                    size="small"
                    value={y.plannedVolume}
                    onChange={(e) => updateYear(i, 'plannedVolume', e.target.value)}
                    sx={{ width: 140 }}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </SectionCard>

      {loading && <LoadingBlock label="Calculating lifecycle projection…" />}

      {projection && !loading && (
        <SectionCard
          title={`Projection — ${projection.scenarioCode}`}
          description={`Currency ${projection.reportingCurrency} · Unit status ${projection.unitCostBaselineStatus} · Tooling ${projection.toolingStatus}`}
        >
          <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Unit cost baseline
              </Typography>
              <Typography variant="h6">
                {projection.unitCostBaseline != null ? formatMoney(projection.unitCostBaseline) : 'NOT AVAILABLE'}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Tooling / unit
              </Typography>
              <Typography variant="h6">
                {projection.toolingPerUnit != null ? formatMoney(projection.toolingPerUnit) : 'NOT AVAILABLE'}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Curve lifetime volume
              </Typography>
              <Typography variant="h6">{projection.lifetimeVolumeForCurve ?? '—'}</Typography>
            </Box>
          </Stack>

          {projection.dataQualityNotes?.length > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              {projection.dataQualityNotes.join(' ')}
            </Alert>
          )}

          <Paper variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Year</TableCell>
                  <TableCell>Calendar</TableCell>
                  <TableCell align="right">Volume</TableCell>
                  <TableCell align="right">Unit cost</TableCell>
                  <TableCell align="right">Year total</TableCell>
                  <TableCell align="right">Cumulative cost</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {projection.years.map((y) => (
                  <TableRow key={y.yearIndex}>
                    <TableCell>{y.yearIndex}</TableCell>
                    <TableCell>{y.calendarYear ?? '—'}</TableCell>
                    <TableCell align="right">{y.plannedVolume}</TableCell>
                    <TableCell align="right">{formatMoney(y.unitCost)}</TableCell>
                    <TableCell align="right">{formatMoney(y.yearTotalCost)}</TableCell>
                    <TableCell align="right">{formatMoney(y.cumulativeCost)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </SectionCard>
      )}
    </Stack>
  );
}
