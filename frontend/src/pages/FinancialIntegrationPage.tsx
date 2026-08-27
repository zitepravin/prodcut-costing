import { useCallback, useEffect, useState } from 'react';
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
import AccountBalanceOutlinedIcon from '@mui/icons-material/AccountBalanceOutlined';
import {
  costTriangle,
  createValuationPolicy,
  importActual,
  listValuationPolicies,
  valuationConvert,
} from '../api/financialApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function FinancialIntegrationPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [unitCost, setUnitCost] = useState('');
  const [triangle, setTriangle] = useState<Record<string, unknown> | null>(null);
  const [policies, setPolicies] = useState<Array<Record<string, unknown>>>([]);
  const [error, setError] = useState<string | null>(null);
  const [policyCode, setPolicyCode] = useState('DEFAULT');
  const [companyCcy, setCompanyCcy] = useState('USD');
  const [costingCcy, setCostingCcy] = useState('USD');
  const [groupCcy, setGroupCcy] = useState('USD');
  const [convAmt, setConvAmt] = useState('100');
  const [convFrom, setConvFrom] = useState('EUR');
  const [convResult, setConvResult] = useState<Record<string, unknown> | null>(null);

  const loadPolicies = useCallback(async () => {
    try {
      setPolicies(await listValuationPolicies());
    } catch {
      /* empty until first create */
    }
  }, []);

  useEffect(() => {
    void loadPolicies();
  }, [loadPolicies]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <AccountBalanceOutlinedIcon color="primary" />
        <Typography variant="h5">Financial integration (C2–C4)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        C2 Planned/Standard/Actual (actuals import-only) · C3 SAP staged triangle (no Material Ledger) · C4
        valuation currency policy matrix.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            C2 — Import actual + C3 triangle
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="BOM node ID" value={bomNodeId} onChange={(e) => setBomNodeId(e.target.value)} required sx={{ minWidth: 260 }} />
            <TextField size="small" label="Actual unit cost" value={unitCost} onChange={(e) => setUnitCost(e.target.value)} />
            <Button
              variant="outlined"
              disabled={!bomNodeId.trim() || !unitCost}
              onClick={() => {
                void (async () => {
                  try {
                    await importActual({ bomNodeId: bomNodeId.trim(), unitCost, sourceSystem: 'IMPORT' });
                    setTriangle(await costTriangle(bomNodeId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Import actual
            </Button>
            <Button
              variant="contained"
              disabled={!bomNodeId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setTriangle(await costTriangle(bomNodeId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Load triangle
            </Button>
          </Stack>
          {triangle && Array.isArray(triangle.buckets) && (
            <>
              <Typography variant="body2" sx={{ mt: 1 }}>
                Std vs Should: {String(triangle.standardVsShould ?? '—')} · Std vs Actual:{' '}
                {String(triangle.standardVsActual ?? '—')} · Should vs Actual:{' '}
                {String(triangle.shouldVsActual ?? '—')}
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Bucket</TableCell>
                    <TableCell align="right">Unit cost</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Notes</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(triangle.buckets as Array<Record<string, unknown>>).map((b) => (
                    <TableRow key={String(b.bucket)}>
                      <TableCell>{String(b.bucket)}</TableCell>
                      <TableCell align="right">{String(b.unitCost ?? '—')}</TableCell>
                      <TableCell>{String(b.dataStatus)}</TableCell>
                      <TableCell>{String(b.notes ?? '')}</TableCell>
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
            C4 — Valuation currency policy
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Policy code" value={policyCode} onChange={(e) => setPolicyCode(e.target.value)} />
            <TextField size="small" label="Company" value={companyCcy} onChange={(e) => setCompanyCcy(e.target.value)} />
            <TextField size="small" label="Group" value={groupCcy} onChange={(e) => setGroupCcy(e.target.value)} />
            <TextField size="small" label="Costing" value={costingCcy} onChange={(e) => setCostingCcy(e.target.value)} />
            <Button
              variant="contained"
              onClick={() => {
                void (async () => {
                  try {
                    await createValuationPolicy({
                      policyCode: policyCode.trim(),
                      policyName: policyCode.trim(),
                      companyCurrency: companyCcy.trim(),
                      groupCurrency: groupCcy.trim(),
                      costingCurrency: costingCcy.trim(),
                      valuationDatePolicy: 'COSTING_DATE',
                    });
                    await loadPolicies();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create policy
            </Button>
          </Stack>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Amount" value={convAmt} onChange={(e) => setConvAmt(e.target.value)} />
            <TextField size="small" label="From CCY" value={convFrom} onChange={(e) => setConvFrom(e.target.value)} />
            <Button
              variant="outlined"
              onClick={() => {
                void (async () => {
                  try {
                    setConvResult(
                      await valuationConvert({
                        policyCode: policyCode.trim(),
                        amount: convAmt,
                        fromCurrency: convFrom.trim(),
                      }),
                    );
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Convert
            </Button>
          </Stack>
          {convResult && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              Company {String(convResult.companyAmount)} {String(convResult.companyCurrency)} · Group{' '}
              {String(convResult.groupAmount ?? '—')} · Costing {String(convResult.costingAmount)}{' '}
              {String(convResult.costingCurrency)} · as of {String(convResult.valuationDateUsed)}
            </Typography>
          )}
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Company</TableCell>
                <TableCell>Group</TableCell>
                <TableCell>Costing</TableCell>
                <TableCell>Date policy</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {policies.map((p) => (
                <TableRow key={String(p.policyId)}>
                  <TableCell>{String(p.policyCode)}</TableCell>
                  <TableCell>{String(p.companyCurrency)}</TableCell>
                  <TableCell>{String(p.groupCurrency ?? '—')}</TableCell>
                  <TableCell>{String(p.costingCurrency)}</TableCell>
                  <TableCell>{String(p.valuationDatePolicy)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </Stack>
  );
}
