import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
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
import CurrencyExchangeIcon from '@mui/icons-material/CurrencyExchange';
import {
  convertAmount,
  listCurrencies,
  recentRates,
  type ConvertResponse,
  type CurrencyResponse,
  type ExchangeRateResponse,
} from '../api/currencyApi';
import { formatMoney } from '../utils/money';
import { RequiredFieldLegend } from '../components/ux';

export function CurrencyPage() {
  const [currencies, setCurrencies] = useState<CurrencyResponse[]>([]);
  const [rates, setRates] = useState<ExchangeRateResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [amount, setAmount] = useState('100');
  const [fromCcy, setFromCcy] = useState('USD');
  const [toCcy, setToCcy] = useState('INR');
  const [converting, setConverting] = useState(false);
  const [result, setResult] = useState<ConvertResponse | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [ccy, recent] = await Promise.all([listCurrencies(), recentRates()]);
      setCurrencies(ccy);
      setRates(recent);
      if (ccy.length && !ccy.find((c) => c.currencyCode === fromCcy)) {
        setFromCcy(ccy[0].currencyCode);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load currency data');
    } finally {
      setLoading(false);
    }
  }, [fromCcy]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleConvert = async () => {
    setConverting(true);
    setError(null);
    setResult(null);
    try {
      const res = await convertAmount({
        amount,
        fromCurrency: fromCcy,
        toCurrency: toCcy,
      });
      setResult(res);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Conversion failed');
    } finally {
      setConverting(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
        <CurrencyExchangeIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          Currency Management
        </Typography>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={5}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Convert amount
              </Typography>
              <RequiredFieldLegend dense />
          <Stack spacing={2}>
                <TextField
                  label="Amount" required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  fullWidth
                  size="small"
                />
                <TextField
                  select
                  label="From" required
                  value={fromCcy}
                  onChange={(e) => setFromCcy(e.target.value)}
                  fullWidth
                  size="small"
                >
                  {currencies.map((c) => (
                    <MenuItem key={c.currencyCode} value={c.currencyCode}>
                      {c.currencyCode} — {c.description}
                    </MenuItem>
                  ))}
                </TextField>
                <TextField
                  select
                  label="To" required
                  value={toCcy}
                  onChange={(e) => setToCcy(e.target.value)}
                  fullWidth
                  size="small"
                >
                  {currencies.map((c) => (
                    <MenuItem key={c.currencyCode} value={c.currencyCode}>
                      {c.currencyCode} — {c.description}
                    </MenuItem>
                  ))}
                </TextField>
                <Button variant="contained" onClick={() => void handleConvert()} disabled={converting}>
                  {converting ? 'Converting…' : 'Convert'}
                </Button>
                {result && (
                  <Alert severity="success">
                    <Typography variant="body2">
                      {formatMoney(result.originalAmount)} {result.fromCurrency} →{' '}
                      <strong>
                        {formatMoney(result.convertedAmount)} {result.toCurrency}
                      </strong>
                    </Typography>
                    <Typography variant="caption" display="block">
                      Rate {result.rateUsed} ({result.conversionPath}) · effective{' '}
                      {result.rateEffectiveDate}
                    </Typography>
                  </Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={7}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Recent exchange rates
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Pair</TableCell>
                    <TableCell align="right">Rate</TableCell>
                    <TableCell>Effective</TableCell>
                    <TableCell>Source</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rates.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4}>
                        <Typography color="text.secondary">No rates published yet.</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {rates.map((r) => (
                    <TableRow key={r.rateId}>
                      <TableCell>
                        {r.fromCurrency}/{r.toCurrency}
                      </TableCell>
                      <TableCell align="right">{r.rate}</TableCell>
                      <TableCell>{r.effectiveDate}</TableCell>
                      <TableCell>{r.source ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
