import { useState } from 'react';
import { Alert, Button, Card, CardContent, Stack, TextField, Typography, Table, TableHead, TableRow, TableCell, TableBody } from '@mui/material';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import { calculateImport, lifecycleImport, type ImportCalcResponse } from '../api/importCostingApi';
import { getErrorMessage } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';

export function ImportCostingPage() {
  const { hasRole } = useAuth();
  const canMutate = hasRole('COST_ANALYST', 'COST_MANAGER', 'COST_ENGINEER', 'ADMIN', 'SYSTEM_ADMINISTRATOR', 'MASTER_DATA_ADMIN');
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportCalcResponse | null>(null);
  const [origin, setOrigin] = useState('CN');
  const [exportC, setExportC] = useState('CN');
  const [dest, setDest] = useState('IN');
  const [hs, setHs] = useState('8471.30');
  const [qty, setQty] = useState('10');
  const [invoice, setInvoice] = useState('1000');
  const [ccy, setCcy] = useState('USD');
  const [freight, setFreight] = useState('50');
  const [insurance, setInsurance] = useState('10');
  const [costingDate, setCostingDate] = useState(new Date().toISOString().slice(0, 10));

  async function onCalculate() {
    setError(null);
    try {
      const r = await calculateImport({
        originCountry: origin, exportCountry: exportC, destinationCountry: dest,
        hsCode: hs, quantity: Number(qty), invoiceCurrency: ccy, targetCurrency: ccy,
        invoiceValue: Number(invoice), internationalFreight: Number(freight) || undefined,
        insurance: Number(insurance) || undefined, costingDate, fxAsOfDate: costingDate,
        customsDate: costingDate, shipmentDate: costingDate, incoterm: 'FOB', shipmentMode: 'SEA',
      });
      setResult(r);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <LocalShippingOutlinedIcon color="primary" />
        <Typography variant="h5">Import costing (normal landed cost)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Industry-neutral normal import. No FTA / preferential tariff. Origin and export countries are separate fields.
      </Typography>
      {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}
      {!canMutate && <Alert severity="info">Read-only — import calculate requires costing roles.</Alert>}
      <Card>
        <CardContent>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Origin" value={origin} onChange={(e) => setOrigin(e.target.value)} sx={{ width: 80 }} />
            <TextField size="small" label="Export" value={exportC} onChange={(e) => setExportC(e.target.value)} sx={{ width: 80 }} />
            <TextField size="small" label="Dest" value={dest} onChange={(e) => setDest(e.target.value)} sx={{ width: 80 }} />
            <TextField size="small" label="HS" value={hs} onChange={(e) => setHs(e.target.value)} sx={{ width: 120 }} />
            <TextField size="small" label="Qty" value={qty} onChange={(e) => setQty(e.target.value)} sx={{ width: 90 }} />
            <TextField size="small" label="Invoice" value={invoice} onChange={(e) => setInvoice(e.target.value)} sx={{ width: 110 }} />
            <TextField size="small" label="CCY" value={ccy} onChange={(e) => setCcy(e.target.value)} sx={{ width: 80 }} />
            <TextField size="small" label="Freight" value={freight} onChange={(e) => setFreight(e.target.value)} sx={{ width: 90 }} />
            <TextField size="small" label="Insurance" value={insurance} onChange={(e) => setInsurance(e.target.value)} sx={{ width: 90 }} />
            <TextField size="small" type="date" label="Costing date" InputLabelProps={{ shrink: true }} value={costingDate} onChange={(e) => setCostingDate(e.target.value)} />
            <Button variant="contained" disabled={!canMutate} onClick={() => void onCalculate()}>Calculate</Button>
          </Stack>
        </CardContent>
      </Card>
      {result && (
        <Card>
          <CardContent>
            <Typography fontWeight={600}>Result · {result.lifecycleStatus} · {result.reviewStatus}</Typography>
            <Typography variant="body2">Invoice {String(result.invoiceValue)} · Customs {String(result.customsValue)} · Landed {String(result.totalLandedCost)} · Unit {String(result.unitLandedCost)}</Typography>
            {result.missingData?.length > 0 && (
              <Alert severity="warning" sx={{ mt: 1 }}>Missing: {result.missingData.join(', ')}</Alert>
            )}
            <Table size="small" sx={{ mt: 1 }}>
              <TableHead><TableRow><TableCell>Component</TableCell><TableCell align="right">Amount</TableCell><TableCell>Note</TableCell></TableRow></TableHead>
              <TableBody>
                {(result.components || []).map((c, i) => (
                  <TableRow key={i}><TableCell>{c.componentCode}</TableCell><TableCell align="right">{String(c.amount)}</TableCell><TableCell>{c.formulaNote}</TableCell></TableRow>
                ))}
              </TableBody>
            </Table>
            <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
              <Button size="small" disabled={!canMutate} onClick={() => void lifecycleImport(result.calculationId, 'submit').then(setResult).catch((e) => setError(getErrorMessage(e)))}>Submit</Button>
              <Button size="small" disabled={!canMutate} onClick={() => void lifecycleImport(result.calculationId, 'approve').then(setResult).catch((e) => setError(getErrorMessage(e)))}>Approve</Button>
              <Button size="small" disabled={!canMutate} onClick={() => void lifecycleImport(result.calculationId, 'lock').then(setResult).catch((e) => setError(getErrorMessage(e)))}>Lock</Button>
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
