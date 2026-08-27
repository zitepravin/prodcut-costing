import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import { calculateBusinessCase, createBusinessCase, listBusinessCases, type BusinessCaseScenario } from '../api/businessCaseApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function BusinessCasePage() {
  const [programId, setProgramId] = useState('');
  const [rows, setRows] = useState<BusinessCaseScenario[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [code, setCode] = useState('BASE');
  const [name, setName] = useState('Base case');
  const [price, setPrice] = useState('100');
  const [shouldCost, setShouldCost] = useState('70');
  const [volume, setVolume] = useState('1000');
  const [investment, setInvestment] = useState('50000');

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <RequestQuoteOutlinedIcon color="primary" />
        <Typography variant="h5">Business case / profitability</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Contribution margin, simple NPV and payback from price, variable cost (should-cost), volume and
        investment. Complements product costing — does not replace ERP P&amp;L or Material Ledger.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Program ID" value={programId} onChange={(e) => setProgramId(e.target.value)} required sx={{ minWidth: 280 }} />
            <Button
              variant="outlined"
              onClick={() => {
                void (async () => {
                  try {
                    setRows(await listBusinessCases(programId.trim()));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Load
            </Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 2 }}>
            <TextField size="small" label="Scenario code" value={code} onChange={(e) => setCode(e.target.value)} required />
            <TextField size="small" label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <TextField size="small" label="Unit price" value={price} onChange={(e) => setPrice(e.target.value)} required />
            <TextField size="small" label="Unit should-cost" value={shouldCost} onChange={(e) => setShouldCost(e.target.value)} required />
            <TextField size="small" label="Annual volume" value={volume} onChange={(e) => setVolume(e.target.value)} required />
            <TextField size="small" label="Investment" value={investment} onChange={(e) => setInvestment(e.target.value)} />
            <Button
              variant="contained"
              disabled={!programId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    const created = await createBusinessCase({
                      programId: programId.trim(),
                      scenarioCode: code.trim(),
                      scenarioName: name.trim(),
                      unitSellingPrice: price,
                      unitShouldCost: shouldCost,
                      unitVariableCost: shouldCost,
                      annualVolume: volume,
                      initialInvestment: investment || '0',
                      horizonYears: 5,
                      discountRatePct: '0.10',
                    });
                    const calc = await calculateBusinessCase(created.scenarioId);
                    setRows(await listBusinessCases(programId.trim()));
                    setError(null);
                    void calc;
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create &amp; calculate
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {rows.map((r) => (
        <Card key={r.scenarioId}>
          <CardContent>
            <Typography fontWeight={600}>
              {r.scenarioCode} — {r.scenarioName} ({r.status})
            </Typography>
            <Typography variant="body2" color="text.secondary">
              CM/unit: {r.contributionMarginUnit ?? '—'} · Annual CM: {r.annualContribution ?? '—'} · NPV:{' '}
              {r.npvAmount ?? '—'} · Payback yrs: {r.paybackYears ?? '—'}
            </Typography>
            {r.calculationBasis && (
              <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                {r.calculationBasis}
              </Typography>
            )}
          </CardContent>
        </Card>
      ))}
    </Stack>
  );
}
