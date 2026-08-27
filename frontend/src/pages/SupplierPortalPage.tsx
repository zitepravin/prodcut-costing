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
import StorefrontOutlinedIcon from '@mui/icons-material/StorefrontOutlined';
import { portalLogin, portalMyRfqs, portalSubmitQuote } from '../api/portalApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

/**
 * Market-gap: dedicated supplier portal UI over existing /api/v1/portal backend.
 * Does not auto-award; quotes still flow through buyer review.
 */
export function SupplierPortalPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [token, setToken] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const [rfqs, setRfqs] = useState<Array<Record<string, unknown>>>([]);
  const [rfqId, setRfqId] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <StorefrontOutlinedIcon color="primary" />
        <Typography variant="h5">Supplier portal</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Market-gap P1: supplier-facing UI for RFQ visibility and quote submit (existing portal APIs). Quotes remain
        subject to buyer review — no automatic award.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="success" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Portal login
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Email" required value={email} onChange={(e) => setEmail(e.target.value)} />
            <TextField
              size="small"
              type="password"
              label="Password" required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button
              variant="contained"
              disabled={!email.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    const r = await portalLogin(email.trim(), password);
                    setToken(String(r.accessToken ?? r.token ?? ''));
                    setSupplierId(String(r.supplierId ?? ''));
                    setInfo('Logged in');
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Login
            </Button>
            <Button
              variant="outlined"
              onClick={() => {
                void (async () => {
                  try {
                    setRfqs(await portalMyRfqs(token || undefined));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              My RFQs
            </Button>
          </Stack>
          <Typography variant="caption" display="block" sx={{ mt: 1 }}>
            Supplier ID: {supplierId || '—'} · RFQs loaded: {rfqs.length}
          </Typography>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Submit quote
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="RFQ ID" required value={rfqId} onChange={(e) => setRfqId(e.target.value)} />
            <TextField size="small" label="Unit price" required value={unitPrice} onChange={(e) => setUnitPrice(e.target.value)} />
            <TextField size="small" label="Supplier ID" value={supplierId} onChange={(e) => setSupplierId(e.target.value)} />
            <Button
              variant="contained"
              disabled={!rfqId.trim() || !unitPrice || !supplierId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await portalSubmitQuote(supplierId.trim(), {
                      rfqId: rfqId.trim(),
                      unitPrice,
                      currencyCode: 'USD',
                    });
                    setInfo('Quote submitted for buyer review');
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Submit
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
