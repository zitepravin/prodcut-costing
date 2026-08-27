import { httpClient } from './httpClient';

export interface BillingPlan {
  planId: string;
  planCode: string;
  planName: string;
  description: string | null;
  billingFrequency: string;
  price: string;
  currencyCode: string;
  trialDays: number;
  graceDays: number;
  status: string;
}

export interface CustomerSubscription {
  subscriptionId: string;
  customerId: string;
  planId: string;
  status: string;
  startDate: string;
  endDate: string | null;
  amount: string;
  currencyCode: string;
  autoRenewal: boolean;
}

export interface BillingPayment {
  paymentId: string;
  invoiceId: string;
  subscriptionId: string;
  customerId: string;
  gatewayCode: string;
  gatewayPaymentRef: string | null;
  amount: string;
  currencyCode: string;
  status: string;
  initiatedAt: string;
  paidAt: string | null;
  failureReason: string | null;
}

export interface BillingRefund {
  refundId: string;
  paymentId: string;
  amount: string;
  currencyCode: string;
  reason: string | null;
  status: string;
  accessPolicy: string;
  refundedAt: string | null;
}

export interface BillingChargeback {
  chargebackId: string;
  paymentId: string;
  amount: string;
  currencyCode: string;
  gatewayCbRef: string | null;
  status: string;
  accessAction: string | null;
  reviewNotes: string | null;
}

export interface RevenueSnapshot {
  paidTotal: string;
  refundTotal: string;
  netTotal: string;
  paidPaymentCount: number;
  completedRefundCount: number;
  openChargebackCount: number;
}

export type AccessPolicy = 'KEEP_UNTIL_PERIOD_END' | 'IMMEDIATE_REVOKE' | 'GRACE_PERIOD';

export async function listBillingPlans(): Promise<BillingPlan[]> {
  const { data } = await httpClient.get<BillingPlan[]>('/api/v1/billing/plans');
  return data;
}

export async function listSubscriptions(customerId: string): Promise<CustomerSubscription[]> {
  const { data } = await httpClient.get<CustomerSubscription[]>(
    `/api/v1/billing/customers/${encodeURIComponent(customerId)}/subscriptions`,
  );
  return data;
}

export async function listPayments(customerId: string): Promise<BillingPayment[]> {
  const { data } = await httpClient.get<BillingPayment[]>(
    `/api/v1/billing/customers/${encodeURIComponent(customerId)}/payments`,
  );
  return data;
}

export async function createSubscription(
  customerId: string,
  planId: string,
  autoRenewal: boolean,
): Promise<CustomerSubscription> {
  const { data } = await httpClient.post<CustomerSubscription>('/api/v1/billing/subscriptions', {
    customerId,
    planId,
    autoRenewal,
  });
  return data;
}

export type PaymentInitiation = {
  paymentId: string;
  invoiceId: string;
  gatewayPaymentRef: string;
  redirectUrl: string;
  keyId?: string;
  amountSubunits?: number;
  currencyCode?: string;
  amount?: string;
};

export async function initiatePayment(
  subscriptionId: string,
  gatewayCode = 'STUB',
): Promise<PaymentInitiation> {
  const { data } = await httpClient.post<PaymentInitiation>(
    `/api/v1/billing/subscriptions/${encodeURIComponent(subscriptionId)}/payments`,
    {
      gatewayCode,
      returnUrl: window.location.href,
    },
  );
  return data;
}

/** Razorpay Standard Checkout — server verifies HMAC(order_id|payment_id). */
export async function verifyCheckoutPayment(
  paymentId: string,
  razorpayOrderId: string,
  razorpayPaymentId: string,
  razorpaySignature: string,
): Promise<BillingPayment> {
  const { data } = await httpClient.post<BillingPayment>(
    `/api/v1/billing/payments/${encodeURIComponent(paymentId)}/verify-checkout`,
    { razorpayOrderId, razorpayPaymentId, razorpaySignature },
  );
  return data;
}

/** Admin-only. Entitlements change only via backend policy — never FE success. */
export async function recordRefund(
  paymentId: string,
  amount: string,
  reason: string,
  accessPolicy: AccessPolicy,
): Promise<BillingRefund> {
  const { data } = await httpClient.post<BillingRefund>(
    `/api/v1/billing/payments/${encodeURIComponent(paymentId)}/refunds`,
    { amount, reason, accessPolicy },
  );
  return data;
}

export async function recordChargeback(
  paymentId: string,
  amount: string,
  gatewayCbRef: string,
  accessPolicy: AccessPolicy,
): Promise<BillingChargeback> {
  const { data } = await httpClient.post<BillingChargeback>(
    `/api/v1/billing/payments/${encodeURIComponent(paymentId)}/chargebacks`,
    { amount, gatewayCbRef, accessPolicy },
  );
  return data;
}

export async function listRefunds(): Promise<BillingRefund[]> {
  const { data } = await httpClient.get<BillingRefund[]>('/api/v1/billing/refunds');
  return data;
}

export async function listChargebacks(): Promise<BillingChargeback[]> {
  const { data } = await httpClient.get<BillingChargeback[]>('/api/v1/billing/chargebacks');
  return data;
}

export async function fetchRevenueSnapshot(): Promise<RevenueSnapshot> {
  const { data } = await httpClient.get<RevenueSnapshot>('/api/v1/billing/reports/revenue-snapshot');
  return data;
}

/** Customer Admin — own tenant only */
export async function listMySubscriptions(): Promise<CustomerSubscription[]> {
  const { data } = await httpClient.get<CustomerSubscription[]>('/api/v1/billing/my/subscriptions');
  return data;
}

export async function listMyPayments(): Promise<BillingPayment[]> {
  const { data } = await httpClient.get<BillingPayment[]>('/api/v1/billing/my/payments');
  return data;
}

export async function submitOfflinePayment(body: {
  subscriptionId: string;
  paymentMethod: string;
  externalReference?: string;
  notes?: string;
  idempotencyKey?: string;
}): Promise<BillingPayment> {
  const { data } = await httpClient.post<BillingPayment>('/api/v1/billing/my/offline-payments', body);
  return data;
}

export async function listPendingOfflinePayments(): Promise<BillingPayment[]> {
  const { data } = await httpClient.get<BillingPayment[]>('/api/v1/billing/offline-payments/pending');
  return data;
}

export async function verifyOfflinePayment(
  paymentId: string,
  approve: boolean,
  notes?: string,
): Promise<BillingPayment> {
  const { data } = await httpClient.post<BillingPayment>(
    `/api/v1/billing/payments/${encodeURIComponent(paymentId)}/verify-offline`,
    { approve, notes },
  );
  return data;
}
