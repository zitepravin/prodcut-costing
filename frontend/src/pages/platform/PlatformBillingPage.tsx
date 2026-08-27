import {
  Alert,
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
import { useCallback, useEffect, useState } from 'react';
import {
  createSubscription,
  fetchRevenueSnapshot,
  initiatePayment,
  listBillingPlans,
  listChargebacks,
  listPayments,
  listRefunds,
  listSubscriptions,
  recordChargeback,
  recordRefund,
  type AccessPolicy,
  type BillingChargeback,
  type BillingPayment,
  type BillingPlan,
  type BillingRefund,
  type CustomerSubscription,
  type RevenueSnapshot,
} from '../../api/billingApi';
import { listCustomers, type PlatformCustomer } from '../../api/platformAccessApi';
import { getErrorMessage } from '../../api/httpClient';

/**
 * Admin commercial view: plans, subscriptions, payments, refunds/chargebacks, revenue snapshot.
 * Access activation only after verified backend webhook (never FE payment success).
 */
export function PlatformBillingPage() {
  const [plans, setPlans] = useState<BillingPlan[]>([]);
  const [customers, setCustomers] = useState<PlatformCustomer[]>([]);
  const [subs, setSubs] = useState<CustomerSubscription[]>([]);
  const [payments, setPayments] = useState<BillingPayment[]>([]);
  const [refunds, setRefunds] = useState<BillingRefund[]>([]);
  const [chargebacks, setChargebacks] = useState<BillingChargeback[]>([]);
  const [snapshot, setSnapshot] = useState<RevenueSnapshot | null>(null);
  const [customerId, setCustomerId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const [refundPaymentId, setRefundPaymentId] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');
  const [refundPolicy, setRefundPolicy] = useState<AccessPolicy>('KEEP_UNTIL_PERIOD_END');

  const [cbPaymentId, setCbPaymentId] = useState('');
  const [cbAmount, setCbAmount] = useState('');
  const [cbRef, setCbRef] = useState('');
  const [cbPolicy, setCbPolicy] = useState<AccessPolicy>('IMMEDIATE_REVOKE');

  const loadAdmin = useCallback(async () => {
    const [r, cb, snap] = await Promise.all([
      listRefunds().catch(() => [] as BillingRefund[]),
      listChargebacks().catch(() => [] as BillingChargeback[]),
      fetchRevenueSnapshot().catch(() => null),
    ]);
    setRefunds(r);
    setChargebacks(cb);
    setSnapshot(snap);
  }, []);

  const load = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([listBillingPlans(), listCustomers()]);
      setPlans(p);
      setCustomers(c);
      if (!customerId && c[0]) {
        setCustomerId(c[0].customerId);
      }
      await loadAdmin();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, [customerId, loadAdmin]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!customerId) return;
    void listSubscriptions(customerId)
      .then(setSubs)
      .catch((e) => setError(getErrorMessage(e)));
    void listPayments(customerId)
      .then(setPayments)
      .catch((e) => setError(getErrorMessage(e)));
  }, [customerId]);

  async function subscribe(planId: string) {
    if (!customerId) return;
    setError(null);
    try {
      const sub = await createSubscription(customerId, planId, false);
      setInfo(
        `Subscription ${sub.subscriptionId} status=${sub.status}. Payment required for non-trial activation.`,
      );
      setSubs(await listSubscriptions(customerId));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function pay(subscriptionId: string) {
    setError(null);
    try {
      const init = await initiatePayment(subscriptionId, 'STUB');
      setInfo(
        `Payment initiated ${init.paymentId}. Modules activate only after verified webhook PAID — not this FE response.`,
      );
      if (customerId) {
        setPayments(await listPayments(customerId));
      }
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function submitRefund() {
    setError(null);
    try {
      const r = await recordRefund(refundPaymentId, refundAmount, refundReason, refundPolicy);
      setInfo(`Refund ${r.refundId} recorded; access policy=${r.accessPolicy}`);
      setRefundAmount('');
      await loadAdmin();
      if (customerId) setPayments(await listPayments(customerId));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function submitChargeback() {
    setError(null);
    try {
      const c = await recordChargeback(cbPaymentId, cbAmount, cbRef, cbPolicy);
      setInfo(`Chargeback ${c.chargebackId} recorded; access action=${c.accessAction ?? cbPolicy}`);
      setCbAmount('');
      await loadAdmin();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  const policyMenu = (
    <>
      <MenuItem value="KEEP_UNTIL_PERIOD_END">KEEP_UNTIL_PERIOD_END</MenuItem>
      <MenuItem value="IMMEDIATE_REVOKE">IMMEDIATE_REVOKE</MenuItem>
      <MenuItem value="GRACE_PERIOD">GRACE_PERIOD</MenuItem>
    </>
  );

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Billing & subscriptions</Typography>
      <Alert severity="info">
        Commercial module access is activated only after a <strong>verified payment webhook</strong> (backend).
        Frontend payment success does not grant entitlements.
      </Alert>
      {error && <Alert severity="error">{error}</Alert>}
      {info && <Alert severity="success">{info}</Alert>}

      {snapshot && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1" gutterBottom>
            Revenue snapshot
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
            <Typography variant="body2">
              Paid total: <strong>{snapshot.paidTotal}</strong> ({snapshot.paidPaymentCount} payments)
            </Typography>
            <Typography variant="body2">
              Refunds: <strong>{snapshot.refundTotal}</strong> ({snapshot.completedRefundCount})
            </Typography>
            <Typography variant="body2">
              Net: <strong>{snapshot.netTotal}</strong>
            </Typography>
            <Typography variant="body2">
              Open chargebacks: <strong>{snapshot.openChargebackCount}</strong>
            </Typography>
          </Stack>
        </Paper>
      )}

      <FormControl size="small" sx={{ minWidth: 280 }}>
        <InputLabel>Customer</InputLabel>
        <Select
          label="Customer"
          value={customerId}
          onChange={(e) => setCustomerId(e.target.value)}
        >
          {customers.map((c) => (
            <MenuItem key={c.customerId} value={c.customerId}>
              {c.customerCode ?? c.customerId} — {c.customerName ?? ''}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Plans
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Code</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Frequency</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Trial / grace</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {plans.map((p) => (
              <TableRow key={p.planId}>
                <TableCell>{p.planCode}</TableCell>
                <TableCell>{p.planName}</TableCell>
                <TableCell>{p.billingFrequency}</TableCell>
                <TableCell>
                  {p.price} {p.currencyCode}
                </TableCell>
                <TableCell>
                  {p.trialDays}d / {p.graceDays}d
                </TableCell>
                <TableCell>
                  <Button size="small" variant="outlined" onClick={() => void subscribe(p.planId)}>
                    Subscribe
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Subscriptions
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Amount</TableCell>
              <TableCell>End</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {subs.map((s) => (
              <TableRow key={s.subscriptionId}>
                <TableCell>{s.subscriptionId.slice(0, 8)}…</TableCell>
                <TableCell>{s.status}</TableCell>
                <TableCell>
                  {s.amount} {s.currencyCode}
                </TableCell>
                <TableCell>{s.endDate ?? '—'}</TableCell>
                <TableCell>
                  {(s.status === 'PENDING' || s.status === 'TRIAL' || s.status === 'PAST_DUE') && (
                    <Button size="small" variant="contained" onClick={() => void pay(s.subscriptionId)}>
                      Initiate payment
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Payments (selected customer)
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Payment</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Amount</TableCell>
              <TableCell>Gateway</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {payments.map((p) => (
              <TableRow key={p.paymentId}>
                <TableCell>{p.paymentId.slice(0, 8)}…</TableCell>
                <TableCell>{p.status}</TableCell>
                <TableCell>
                  {p.amount} {p.currencyCode}
                </TableCell>
                <TableCell>{p.gatewayCode}</TableCell>
                <TableCell>
                  {p.status === 'PAID' && (
                    <Button
                      size="small"
                      onClick={() => {
                        setRefundPaymentId(p.paymentId);
                        setCbPaymentId(p.paymentId);
                        setRefundAmount(p.amount);
                        setCbAmount(p.amount);
                      }}
                    >
                      Use for refund/CB
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Record refund
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
          <TextField
            size="small"
            label="Payment ID"
            value={refundPaymentId}
            onChange={(e) => setRefundPaymentId(e.target.value)}
            sx={{ minWidth: 280 }}
          />
          <TextField
            size="small"
            label="Amount"
            value={refundAmount}
            onChange={(e) => setRefundAmount(e.target.value)}
          />
          <TextField
            size="small"
            label="Reason"
            value={refundReason}
            onChange={(e) => setRefundReason(e.target.value)}
          />
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel>Access policy</InputLabel>
            <Select
              label="Access policy"
              value={refundPolicy}
              onChange={(e) => setRefundPolicy(e.target.value as AccessPolicy)}
            >
              {policyMenu}
            </Select>
          </FormControl>
          <Button variant="contained" color="warning" onClick={() => void submitRefund()}>
            Record refund
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Record chargeback
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
          <TextField
            size="small"
            label="Payment ID"
            value={cbPaymentId}
            onChange={(e) => setCbPaymentId(e.target.value)}
            sx={{ minWidth: 280 }}
          />
          <TextField size="small" label="Amount" value={cbAmount} onChange={(e) => setCbAmount(e.target.value)} />
          <TextField
            size="small"
            label="Gateway CB ref"
            value={cbRef}
            onChange={(e) => setCbRef(e.target.value)}
          />
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel>Access policy</InputLabel>
            <Select
              label="Access policy"
              value={cbPolicy}
              onChange={(e) => setCbPolicy(e.target.value as AccessPolicy)}
            >
              {policyMenu}
            </Select>
          </FormControl>
          <Button variant="contained" color="error" onClick={() => void submitChargeback()}>
            Record chargeback
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Refunds
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Refund</TableCell>
              <TableCell>Payment</TableCell>
              <TableCell>Amount</TableCell>
              <TableCell>Policy</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {refunds.map((r) => (
              <TableRow key={r.refundId}>
                <TableCell>{r.refundId.slice(0, 8)}…</TableCell>
                <TableCell>{r.paymentId.slice(0, 8)}…</TableCell>
                <TableCell>
                  {r.amount} {r.currencyCode}
                </TableCell>
                <TableCell>{r.accessPolicy}</TableCell>
                <TableCell>{r.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Chargebacks
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Chargeback</TableCell>
              <TableCell>Payment</TableCell>
              <TableCell>Amount</TableCell>
              <TableCell>Access action</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {chargebacks.map((c) => (
              <TableRow key={c.chargebackId}>
                <TableCell>{c.chargebackId.slice(0, 8)}…</TableCell>
                <TableCell>{c.paymentId.slice(0, 8)}…</TableCell>
                <TableCell>
                  {c.amount} {c.currencyCode}
                </TableCell>
                <TableCell>{c.accessAction ?? '—'}</TableCell>
                <TableCell>{c.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Stack>
  );
}
