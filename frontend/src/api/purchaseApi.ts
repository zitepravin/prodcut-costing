import { httpClient } from './httpClient';

export type PurchaseLine = {
  lineId: string;
  lineNo: number;
  materialCode?: string | null;
  materialDescription?: string | null;
  supplierId?: string | null;
  supplierQuoteId?: string | null;
  quantity: string | number;
  uom: string;
  unitPrice?: string | number | null;
  currency?: string | null;
  unitPriceReporting?: string | number | null;
  fxRate?: string | number | null;
  dataStatus: string;
  tcoUnit?: string | number | null;
  tcoStatus: string;
  needByDate?: string | null;
  lineNotes?: string | null;
};

export type PurchaseRequest = {
  purchaseRequestId: string;
  prNumber?: string | null;
  plantCode?: string | null;
  requiredDate?: string | null;
  priorityCode?: string | null;
  purposeText?: string | null;
  departmentCode?: string | null;
  programId: string;
  bomNodeId: string;
  makeVsBuySimulationId?: string | null;
  linkedRfqId?: string | null;
  status: string;
  reportingCurrency: string;
  manualOverrideReason?: string | null;
  decisionCode?: string | null;
  decisionReason?: string | null;
  decidedAt?: string | null;
  releasedAt?: string | null;
  externalPoReference?: string | null;
  poLinkStatus?: string | null;
  externalPoSystem?: string | null;
  poLinkedAt?: string | null;
  poLinkedSource?: string | null;
  notes?: string | null;
  lines: PurchaseLine[];
  messages?: string[];
};

export type CreatePurchaseRequest = {
  makeVsBuySimulationId?: string;
  reportingCurrency?: string;
  notes?: string;
  manualOverrideReason?: string;
  plantCode?: string;
  requiredDate?: string;
  priorityCode?: string;
  departmentCode?: string;
  purposeText?: string;
  lines?: Array<{
    supplierId: string;
    supplierQuoteId?: string;
    quantity: number;
    uom?: string;
    needByDate?: string;
    lineNotes?: string;
  }>;
};

export async function createPurchaseRequest(bomNodeId: string, body: CreatePurchaseRequest) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/bom-nodes/${encodeURIComponent(bomNodeId)}/purchase-requests`,
    body,
  );
  return data;
}

export async function listPurchaseByBom(bomNodeId: string) {
  const { data } = await httpClient.get<PurchaseRequest[]>(
    `/api/v1/bom-nodes/${encodeURIComponent(bomNodeId)}/purchase-requests`,
  );
  return data;
}

export async function getPurchaseRequest(id: string) {
  const { data } = await httpClient.get<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}`,
  );
  return data;
}

export async function submitPurchaseRequest(id: string) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/submit`,
  );
  return data;
}

export async function decidePurchaseRequest(id: string, decisionCode: string, decisionReason: string) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/decide`,
    { decisionCode, decisionReason },
  );
  return data;
}

export async function releasePurchaseRequest(id: string, externalPoReference?: string) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/release`,
    { externalPoReference },
  );
  return data;
}

export async function exportPurchasePackage(id: string) {
  const { data } = await httpClient.get(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/export-package`,
  );
  return data;
}

export type OrderTracking = {
  trackingId: string;
  purchaseRequestId: string;
  purchaseRequestLineId?: string | null;
  externalPoReference?: string | null;
  trackingStatus: string;
  asnDate?: string | null;
  qtyShipped?: string | number | null;
  qtyOrdered?: string | number | null;
  uom?: string | null;
  sourceSystem: string;
  sourceEventId?: string | null;
  dataStatus: string;
  humanCorrected: boolean;
  correctionReason?: string | null;
  correctedBy?: string | null;
  correctedAt?: string | null;
  notes?: string | null;
  policyNotes?: string[];
};

export async function listOrderTracking(purchaseRequestId: string) {
  const { data } = await httpClient.get<OrderTracking[]>(
    `/api/v1/purchase-requests/${encodeURIComponent(purchaseRequestId)}/tracking`,
  );
  return data;
}

export async function correctOrderTracking(
  purchaseRequestId: string,
  trackingId: string,
  body: {
    asnDate?: string;
    qtyShipped?: number;
    trackingStatus?: string;
    uom?: string;
    correctionReason: string;
    notes?: string;
  },
) {
  const { data } = await httpClient.post<OrderTracking>(
    `/api/v1/purchase-requests/${encodeURIComponent(purchaseRequestId)}/tracking/${encodeURIComponent(trackingId)}/correct`,
    body,
  );
  return data;
}

export type ShipmentPerformance = {
  trackingId: string;
  purchaseRequestId: string;
  purchaseRequestLineId?: string | null;
  supplierId?: string | null;
  trackingStatus: string;
  needByDate?: string | null;
  asnDate?: string | null;
  qtyOrdered?: string | number | null;
  qtyShipped?: string | number | null;
  delayDays?: number | null;
  onTimeComputed?: boolean | null;
  onTimeEffective?: boolean | null;
  humanOverrideApplied: boolean;
  onTimeOverrideReason?: string | null;
  dataStatus: string;
  qtyVariance?: string | number | null;
  qtyVarianceStatus: string;
  notes?: string[];
};

export type PerformanceSummary = {
  purchaseRequestId: string;
  programId: string;
  status: string;
  shipmentCount: number;
  onTimeCount: number;
  lateCount: number;
  dataNotAvailableCount: number;
  shipments: ShipmentPerformance[];
  policyNotes?: string[];
};

export async function getDeliveryPerformance(purchaseRequestId: string) {
  const { data } = await httpClient.get<PerformanceSummary>(
    `/api/v1/purchase-requests/${encodeURIComponent(purchaseRequestId)}/delivery-performance`,
  );
  return data;
}

export async function overrideOnTime(
  purchaseRequestId: string,
  trackingId: string,
  onTimeOverride: boolean,
  reason: string,
) {
  const { data } = await httpClient.post<ShipmentPerformance>(
    `/api/v1/purchase-requests/${encodeURIComponent(purchaseRequestId)}/tracking/${encodeURIComponent(trackingId)}/on-time-override`,
    { onTimeOverride, reason },
  );
  return data;
}

export async function cancelPurchaseRequest(id: string, reason?: string) {
  const q = reason ? `?reason=${encodeURIComponent(reason)}` : '';
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/cancel${q}`,
  );
  return data;
}

export async function startReviewPurchaseRequest(id: string) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/start-review`,
  );
  return data;
}

export async function reopenPurchaseRequest(id: string) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/reopen`,
  );
  return data;
}

export async function createRfqFromPurchaseRequest(id: string) {
  const { data } = await httpClient.post<PurchaseRequest>(
    `/api/v1/purchase-requests/${encodeURIComponent(id)}/create-rfq`,
  );
  return data;
}

// --- Wave D: Purchase Orders ---

export type PurchaseOrderLine = {
  lineId: string;
  lineNo: number;
  materialCode?: string | null;
  materialDescription?: string | null;
  quantity: string | number;
  uom: string;
  unitPrice?: string | number | null;
  currencyCode?: string | null;
  taxAmount?: string | number | null;
  discountAmount?: string | number | null;
  deliveryDate?: string | null;
  plantCode?: string | null;
  storageLocation?: string | null;
  dataStatus: string;
  lineNotes?: string | null;
};

export type PurchaseOrder = {
  purchaseOrderId: string;
  poNumber?: string | null;
  customerId: string;
  purchaseRequestId?: string | null;
  linkedRfqId?: string | null;
  supplierId: string;
  plantCode?: string | null;
  buyerUserId?: string | null;
  currencyCode: string;
  paymentTerms?: string | null;
  incoterms?: string | null;
  deliveryTerms?: string | null;
  orderDate?: string | null;
  requiredDeliveryDate?: string | null;
  status: string;
  decisionReason?: string | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  issuedAt?: string | null;
  issuedBy?: string | null;
  externalPoReference?: string | null;
  notes?: string | null;
  lines: PurchaseOrderLine[];
  messages?: string[];
};

export type CreatePurchaseOrderBody = {
  purchaseRequestId?: string;
  linkedRfqId?: string;
  supplierId: string;
  plantCode?: string;
  buyerUserId?: string;
  currencyCode?: string;
  paymentTerms?: string;
  incoterms?: string;
  deliveryTerms?: string;
  orderDate?: string;
  requiredDeliveryDate?: string;
  notes?: string;
  lines: Array<{
    materialCode?: string;
    materialDescription?: string;
    quantity: number;
    uom?: string;
    unitPrice?: number;
    currencyCode?: string;
    taxAmount?: number;
    discountAmount?: number;
    deliveryDate?: string;
    plantCode?: string;
    storageLocation?: string;
    lineNotes?: string;
  }>;
};

export async function listPurchaseOrders(status?: string) {
  const { data } = await httpClient.get<PurchaseOrder[]>('/api/v1/purchase-orders', {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function getPurchaseOrder(id: string) {
  const { data } = await httpClient.get<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}`,
  );
  return data;
}

export async function createPurchaseOrder(body: CreatePurchaseOrderBody) {
  const { data } = await httpClient.post<PurchaseOrder>('/api/v1/purchase-orders', body);
  return data;
}

export async function submitPurchaseOrder(id: string) {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}/submit`,
  );
  return data;
}

export async function decidePurchaseOrder(id: string, decisionCode: string, decisionReason: string) {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}/decide`,
    { decisionCode, decisionReason },
  );
  return data;
}

export async function reworkPurchaseOrder(id: string) {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}/rework`,
  );
  return data;
}

export async function issuePurchaseOrder(id: string, externalPoReference?: string) {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}/issue`,
    { externalPoReference: externalPoReference ?? null },
  );
  return data;
}

export async function cancelPurchaseOrder(id: string, reason?: string) {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}/cancel`,
    null,
    { params: reason ? { reason } : undefined },
  );
  return data;
}


/** Seed DRAFT PO from APPROVED / RFQ_LINKED / RELEASED PR */
export async function createPurchaseOrderFromPr(
  prId: string,
  body: {
    supplierId: string;
    plantCode?: string;
    currencyCode?: string;
    paymentTerms?: string;
    incoterms?: string;
    notes?: string;
  },
): Promise<PurchaseOrder> {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-requests/${encodeURIComponent(prId)}/create-purchase-order`,
    body,
  );
  return data;
}


export async function closePurchaseOrder(
  id: string,
  body?: { reason?: string; partial?: boolean },
): Promise<PurchaseOrder> {
  const { data } = await httpClient.post<PurchaseOrder>(
    `/api/v1/purchase-orders/${encodeURIComponent(id)}/close`,
    body ?? { partial: false },
  );
  return data;
}


/** PURCHASE-native PR (no BOM / Product Costing required) */
export async function createStandalonePurchaseRequest(body: {
  plantCode?: string;
  departmentCode?: string;
  requiredDate?: string;
  priorityCode?: string;
  purposeText?: string;
  reportingCurrency?: string;
  notes?: string;
  lines: Array<{
    materialCode?: string;
    materialDescription?: string;
    quantity: number;
    uom?: string;
    targetPrice?: number;
    targetCurrency?: string;
    supplierId?: string;
  }>;
}) {
  const { data } = await httpClient.post('/api/v1/purchase-requests', body);
  return data;
}

export async function listPurchaseRequestsForCustomer() {
  const { data } = await httpClient.get('/api/v1/purchase-requests');
  return data;
}
