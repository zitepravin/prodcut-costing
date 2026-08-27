import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import DirectionsCarOutlinedIcon from '@mui/icons-material/DirectionsCarOutlined';
import { analyzeEcn, listEcn, listWhatIf, runWhatIf } from '../api/autoIntelApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function AutomotiveIntelPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [ecnNumber, setEcnNumber] = useState('');
  const [proposedCost, setProposedCost] = useState('');
  const [annualVol, setAnnualVol] = useState('10000');
  const [ecnRows, setEcnRows] = useState<Array<Record<string, unknown>>>([]);
  const [matPct, setMatPct] = useState('10');
  const [volPct, setVolPct] = useState('-20');
  const [fxPct, setFxPct] = useState('0');
  const [whatIf, setWhatIf] = useState<Record<string, unknown> | null>(null);
  const [whatIfRows, setWhatIfRows] = useState<Array<Record<string, unknown>>>([]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <DirectionsCarOutlinedIcon color="primary" />
        <Typography variant="h5">Automotive intelligence (E3/E4)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        E3 ECN cost impact (unit + annual program $) · E4 central what-if (material / process / tooling / FX /
        volume). Baseline costs come from should-cost — never invented. E1 benchmark library and E2 supplier
        intelligence remain on their existing modules.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            E3 — ECN cost impact
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="BOM node ID" value={bomNodeId} onChange={(e) => setBomNodeId(e.target.value)} required sx={{ minWidth: 260 }} />
            <TextField size="small" label="ECN number" value={ecnNumber} onChange={(e) => setEcnNumber(e.target.value)} required />
            <TextField size="small" label="Proposed unit cost" value={proposedCost} onChange={(e) => setProposedCost(e.target.value)} />
            <TextField size="small" label="Annual volume" value={annualVol} onChange={(e) => setAnnualVol(e.target.value)} />
            <Button
              variant="contained"
              disabled={!bomNodeId.trim() || !ecnNumber.trim() || !proposedCost}
              onClick={() => {
                void (async () => {
                  try {
                    await analyzeEcn({
                      bomNodeId: bomNodeId.trim(),
                      ecnNumber: ecnNumber.trim(),
                      proposedUnitCost: proposedCost,
                      annualVolume: annualVol || undefined,
                    });
                    setEcnRows(await listEcn(bomNodeId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Analyze ECN
            </Button>
            <Button
              variant="outlined"
              disabled={!bomNodeId.trim()}
              onClick={() => {
                void listEcn(bomNodeId.trim())
                  .then(setEcnRows)
                  .catch((e) => setError(getErrorMessage(e)));
              }}
            >
              Load ECN history
            </Button>
          </Stack>
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>ECN</TableCell>
                <TableCell align="right">Baseline</TableCell>
                <TableCell align="right">Proposed</TableCell>
                <TableCell align="right">Unit Δ</TableCell>
                <TableCell align="right">Annual Δ</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {ecnRows.map((r) => (
                <TableRow key={String(r.impactId)}>
                  <TableCell>{String(r.ecnNumber)}</TableCell>
                  <TableCell align="right">{String(r.baselineUnitCost ?? '—')}</TableCell>
                  <TableCell align="right">{String(r.proposedUnitCost ?? '—')}</TableCell>
                  <TableCell align="right">{String(r.unitDelta ?? '—')}</TableCell>
                  <TableCell align="right">{String(r.annualProgramDelta ?? '—')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            E4 — What-if simulation
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Material %" value={matPct} onChange={(e) => setMatPct(e.target.value)} />
            <TextField size="small" label="Volume %" value={volPct} onChange={(e) => setVolPct(e.target.value)} />
            <TextField size="small" label="FX %" value={fxPct} onChange={(e) => setFxPct(e.target.value)} />
            <TextField size="small" label="Annual volume" value={annualVol} onChange={(e) => setAnnualVol(e.target.value)} />
            <Button
              variant="contained"
              disabled={!bomNodeId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    const r = await runWhatIf({
                      bomNodeId: bomNodeId.trim(),
                      materialPctChange: matPct || undefined,
                      volumePctChange: volPct || undefined,
                      fxPctChange: fxPct || undefined,
                      annualVolume: annualVol || undefined,
                      label: 'UI what-if',
                    });
                    setWhatIf(r);
                    setWhatIfRows(await listWhatIf(bomNodeId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Run what-if
            </Button>
          </Stack>
          {whatIf && (
            <Alert severity="info" sx={{ mt: 1 }}>
              Simulated unit {String(whatIf.simulatedUnitCost)} (Δ {String(whatIf.unitDelta)}) · annual Δ{' '}
              {String(whatIf.annualDelta ?? '—')}
            </Alert>
          )}
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>Label</TableCell>
                <TableCell align="right">Baseline</TableCell>
                <TableCell align="right">Simulated</TableCell>
                <TableCell align="right">Unit Δ</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {whatIfRows.map((r) => (
                <TableRow key={String(r.simulationId)}>
                  <TableCell>{String(r.label)}</TableCell>
                  <TableCell align="right">{String(r.baselineUnitCost)}</TableCell>
                  <TableCell align="right">{String(r.simulatedUnitCost)}</TableCell>
                  <TableCell align="right">{String(r.unitDelta)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </Stack>
  );
}
