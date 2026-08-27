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
import LibraryBooksOutlinedIcon from '@mui/icons-material/LibraryBooksOutlined';
import {
  addBenchmarkRate,
  createBenchmarkPack,
  listBenchmarkPacks,
  publishBenchmarkPack,
  type BenchmarkPack,
} from '../api/benchmarkApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function BenchmarkLibraryPage() {
  const [packs, setPacks] = useState<BenchmarkPack[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [region, setRegion] = useState('IN');
  const [selectedPack, setSelectedPack] = useState('');
  const [rateCode, setRateCode] = useState('');
  const [rateValue, setRateValue] = useState('');
  const [category, setCategory] = useState('LABOR');
  const [uom, setUom] = useState('HOUR');

  const load = useCallback(async () => {
    setError(null);
    try {
      setPacks(await listBenchmarkPacks());
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
        <LibraryBooksOutlinedIcon color="primary" />
        <Typography variant="h5">Benchmark knowledge library</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Tenant-maintained manufacturing rate packs (labor, machine, energy, material, overhead).
        This is an internal knowledge base — not a global external market library. Compare rates to
        internal masters; does not invent should-cost values.
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
            Create pack
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Pack code" value={code} onChange={(e) => setCode(e.target.value)} required />
            <TextField size="small" label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <TextField size="small" label="Region" value={region} onChange={(e) => setRegion(e.target.value)} required />
            <Button
              variant="contained"
              disabled={!code.trim() || !name.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createBenchmarkPack({
                      packCode: code.trim(),
                      packName: name.trim(),
                      regionCode: region.trim(),
                      sourceLabel: 'INTERNAL',
                    });
                    setInfo('Pack created (DRAFT)');
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

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Add rate / publish
          </Typography>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField
              select
              size="small"
              label="Pack"
              value={selectedPack}
              onChange={(e) => setSelectedPack(e.target.value)}
              sx={{ minWidth: 220 }}
              required
            >
              {packs.map((p) => (
                <MenuItem key={p.packId} value={p.packId}>
                  {p.packCode} v{p.versionLabel} ({p.status})
                </MenuItem>
              ))}
            </TextField>
            <TextField select size="small" label="Category" value={category} onChange={(e) => setCategory(e.target.value)}>
              {['LABOR', 'MACHINE', 'ENERGY', 'MATERIAL', 'OVERHEAD', 'TOOLING', 'OTHER'].map((c) => (
                <MenuItem key={c} value={c}>
                  {c}
                </MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="Rate code" value={rateCode} onChange={(e) => setRateCode(e.target.value)} required />
            <TextField size="small" label="Rate value" value={rateValue} onChange={(e) => setRateValue(e.target.value)} required />
            <TextField size="small" label="UOM" value={uom} onChange={(e) => setUom(e.target.value)} required />
            <Button
              variant="outlined"
              disabled={!selectedPack || !rateCode || !rateValue}
              onClick={() => {
                void (async () => {
                  try {
                    await addBenchmarkRate(selectedPack, {
                      category,
                      rateCode: rateCode.trim(),
                      rateValue: rateValue.trim(),
                      currencyCode: 'USD',
                      unitOfMeasure: uom.trim(),
                    });
                    setInfo('Rate added');
                    setRateCode('');
                    setRateValue('');
                    await load();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Add rate
            </Button>
            <Button
              variant="contained"
              disabled={!selectedPack}
              onClick={() => {
                void (async () => {
                  try {
                    await publishBenchmarkPack(selectedPack);
                    setInfo('Pack published');
                    await load();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Publish
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Region</TableCell>
            <TableCell>Version</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Rates</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {packs.map((p) => (
            <TableRow key={p.packId}>
              <TableCell>{p.packCode}</TableCell>
              <TableCell>{p.packName}</TableCell>
              <TableCell>{p.regionCode}</TableCell>
              <TableCell>{p.versionLabel}</TableCell>
              <TableCell>{p.status}</TableCell>
              <TableCell align="right">{p.rateCount}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  );
}
