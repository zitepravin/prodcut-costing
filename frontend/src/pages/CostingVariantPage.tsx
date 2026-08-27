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
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import {
  activateCostingVariant,
  createCostingVariant,
  listCostingVariants,
  retireCostingVariant,
  type CostingVariant,
} from '../api/costingVariantApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function CostingVariantPage() {
  const [rows, setRows] = useState<CostingVariant[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [lotSize, setLotSize] = useState('1');
  const [ohPool, setOhPool] = useState('');

  const load = useCallback(async () => {
    setError(null);
    try {
      setRows(await listCostingVariants());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <TuneOutlinedIcon color="primary" />
        <Typography variant="h5">Costing variants</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Reusable SAP-style costing rules: valuation strategy, price sources, quantity structure, lot size,
        scrap/yield, overhead pool. Activate a variant, then pass its ID when generating should-cost.
        Variants do not replace the should-cost engine.
      </Typography>
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

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Create variant
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Code" value={code} onChange={(e) => setCode(e.target.value)} required />
            <TextField size="small" label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <TextField size="small" label="Currency" value={currency} onChange={(e) => setCurrency(e.target.value)} required />
            <TextField size="small" label="Lot size" value={lotSize} onChange={(e) => setLotSize(e.target.value)} required />
            <TextField size="small" label="Overhead pool code" value={ohPool} onChange={(e) => setOhPool(e.target.value)} />
            <Button
              variant="contained"
              disabled={!code.trim() || !name.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createCostingVariant({
                      variantCode: code.trim(),
                      variantName: name.trim(),
                      currencyCode: currency.trim(),
                      lotSize,
                      overheadPoolCode: ohPool.trim() || undefined,
                      valuationStrategy: 'STANDARD',
                      materialPriceSource: 'MATERIAL_MASTER',
                      activityPriceSource: 'MACHINE_LABOR_MASTER',
                      quantityStructureSource: 'BOM_AND_ROUTING',
                    });
                    setInfo('Variant created (DRAFT). Activate before use in should-cost.');
                    setCode('');
                    setName('');
                    await load();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Valuation</TableCell>
            <TableCell>Currency</TableCell>
            <TableCell align="right">Lot</TableCell>
            <TableCell>OH pool</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.variantId}>
              <TableCell>{r.variantCode}</TableCell>
              <TableCell>{r.variantName}</TableCell>
              <TableCell>{r.status}</TableCell>
              <TableCell>{r.valuationStrategy}</TableCell>
              <TableCell>{r.currencyCode}</TableCell>
              <TableCell align="right">{r.lotSize}</TableCell>
              <TableCell>{r.overheadPoolCode ?? '—'}</TableCell>
              <TableCell>
                <Stack direction="row" spacing={1}>
                  {r.status === 'DRAFT' && (
                    <Button size="small" onClick={() => void activateCostingVariant(r.variantId).then(load).catch((e) => setError(getErrorMessage(e)))}>
                      Activate
                    </Button>
                  )}
                  {r.status !== 'RETIRED' && (
                    <Button size="small" color="warning" onClick={() => void retireCostingVariant(r.variantId).then(load).catch((e) => setError(getErrorMessage(e)))}>
                      Retire
                    </Button>
                  )}
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  );
}
