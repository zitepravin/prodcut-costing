import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import {
  listMyPayments,
  listMySubscriptions,
  submitOfflinePayment,
  initiatePayment,
  verifyCheckoutPayment,
  type BillingPayment,
  type CustomerSubscription,
} from '../../api/billingApi';
import { getErrorMessage } from '../../api/httpClient';

const OFFLINE_METHODS = [
  'BANK_TRANSFER',
  'NEFT',
  'RTGS',
  'IMPS',
  'UPI',
  'CHEQUE',
  'PURCHASE_ORDER',
  'MANUAL',
  'OTHER',
];

/**
 * Customer Admin billing view — read subscription/payment status; submit offline payment only.
 * Cannot approve payments, edit prices, or change commercial limits.
 */
export default function CustomerBillingPage() {
  const [subs, setSubs] = useState<CustomerSubscription[]>([]);
  const [payments, setPayments] = useState<BillingPayment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [subscriptionId, setSubscriptionId] = useState('');
  const [method, setMethod] = useState('BANK_TRANSFER');
  const [ref, setRef] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function reload() {
    setLoading(true);
    setError(null);
    try {
      const [s, p] = await Promise.all([listMySubscriptions(), listMyPayments()]);
      setSubs(s);
      setPayments(p);
      if (!subscriptionId && s[0]) setSubscriptionId(s[0].subscriptionId);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onSubmitOffline() {
    if (!subscriptionId) return;
    setSubmitting(true);
    setError(null);
    try {
      await submitOfflinePayment({
        subscriptionId,
        paymentMethod: method,
        externalReference: ref || undefined,
        notes: notes || undefined,
        idempotencyKey: `off-${subscriptionId}-${ref || Date.now()}`,
      });
      setRef('');
      setNotes('');
      await reload();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Box p={2}>
      {info && (
        <Alert severity="success" onClose={() => setInfo(null)} sx={{ mb: 2 }}>
          {info}
        </Alert>
      )}

      <Typography variant="h5" gutterBottom>
        Billing & payments
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        View plan and payment status. Offline payments require Platform verification — you cannot mark them paid.
      </Typography>
      {error ? <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert> : null}
      {loading ? <Typography>Loading…</Typography> : null}

      <Stack spacing={2}>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6">Subscriptions</Typography>
            {subs.length === 0 ? (
              <Typography color="text.secondary">No subscriptions</Typography>
            ) : (
              subs.map((s) => (
                <Typography key={s.subscriptionId} variant="body2">
                  {s.subscriptionId.slice(0, 8)}… — {s.status} — {s.amount} {s.currencyCode}
                </Typography>
              ))
            )}
          </CardContent>
        </Card>

        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6">Payment history</Typography>
            {payments.length === 0 ? (
              <Typography color="text.secondary">No payments</Typography>
            ) : (
              payments.map((p) => (
                <Typography key={p.paymentId} variant="body2">
                  {p.status} — {p.amount} {p.currencyCode} — {p.gatewayCode}
                </Typography>
              ))
            )}
          </CardContent>
        </Card>

        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6">Submit offline payment</Typography>
            <Stack spacing={2} mt={1} maxWidth={480}>
              <TextField
                select
                label="Subscription"
                value={subscriptionId}
                onChange={(e) => setSubscriptionId(e.target.value)}
                size="small"
              >
                {subs.map((s) => (
                  <MenuItem key={s.subscriptionId} value={s.subscriptionId}>
                    {s.subscriptionId.slice(0, 8)}… ({s.status})
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                label="Method"
                value={method}
                onChange={(e) => setMethod(e.target.value)}
                size="small"
              >
                {OFFLINE_METHODS.map((m) => (
                  <MenuItem key={m} value={m}>
                    {m}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="Bank / cheque / PO reference"
                value={ref}
                onChange={(e) => setRef(e.target.value)}
                size="small"
              />
              <TextField
                label="Notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                size="small"
                multiline
              />
              <Button variant="outlined" disabled={submitting || !subscriptionId} onClick={() => {
                  void (async () => {
                    setSubmitting(true); setError(null); setInfo(null);
                    try {
                      const init = await initiatePayment(subscriptionId, 'RAZORPAY');
                      const key = init.keyId;
                      if (!key) {
                        setError('Razorpay Key ID not configured on server (RAZORPAY_KEY_ID)');
                        return;
                      }
                      const amount = init.amountSubunits ?? 0;
                      if (amount < 100) {
                        setError('Amount must be at least 100 paise');
                        return;
                      }
                      await new Promise<void>((resolve, reject) => {
                        const open = () => {
                          const RazorpayCtor = (window as unknown as { Razorpay?: new (o: Record<string, unknown>) => { open: () => void; on: (e: string, fn: (r: unknown) => void) => void } }).Razorpay;
                          if (!RazorpayCtor) {
                            reject(new Error('Razorpay Checkout.js failed to load'));
                            return;
                          }
                          const rzp = new RazorpayCtor({
                            key,
                            amount,
                            currency: init.currencyCode || 'INR',
                            name: 'Product Costing',
                            description: 'Subscription payment',
                            order_id: init.gatewayPaymentRef,
                            handler: (response: {
                              razorpay_payment_id: string;
                              razorpay_order_id: string;
                              razorpay_signature: string;
                            }) => {
                              void (async () => {
                                try {
                                  await verifyCheckoutPayment(
                                    init.paymentId,
                                    response.razorpay_order_id,
                                    response.razorpay_payment_id,
                                    response.razorpay_signature,
                                  );
                                  setInfo('Payment verified. Subscription/license activation is server-side.');
                                  resolve();
                                } catch (err) {
                                  reject(err);
                                }
                              })();
                            },
                            modal: {
                              ondismiss: () => reject(new Error('Payment cancelled')),
                            },
                            theme: { color: '#1976d2' },
                          });
                          rzp.on('payment.failed', () => {
                            reject(new Error('Payment failed'));
                          });
                          rzp.open();
                        };
                        const existing = document.querySelector('script[data-razorpay-checkout]');
                        if (existing) {
                          open();
                          return;
                        }
                        const s = document.createElement('script');
                        s.src = 'https://checkout.razorpay.com/v1/checkout.js';
                        s.async = true;
                        s.dataset.razorpayCheckout = '1';
                        s.onload = () => open();
                        s.onerror = () => reject(new Error('Failed to load Razorpay Checkout.js'));
                        document.body.appendChild(s);
                      });
                    } catch (e) {
                      setError(getErrorMessage(e));
                    } finally {
                      setSubmitting(false);
                    }
                  })();
                }}>Pay with Razorpay</Button>
              <Button variant="contained" disabled={submitting || !subscriptionId} onClick={() => void onSubmitOffline()}>
                Submit for verification
              </Button>
            </Stack>
          </CardContent>
        </Card>
      </Stack>
    </Box>
  );
}
