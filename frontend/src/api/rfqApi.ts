import { httpClient } from './httpClient';

export type SupplierStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type RfqStatus = 'DRAFT' | 'OPEN' | 'CLOSED' | 'CANCELLED' | 'AWARDED';
export type QuoteStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export interface Supplier {
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  countryCode: string | null;
  currencyCode: string;
  status: SupplierStatus;
  notes: string | null;
}

export interface RfqEvent {
  rfqId: string;
  programId: string;
  rfqNumber: string;
  title: string;
  status: RfqStatus;
  dueDate: string | null;
  currencyCode: string;
  notes: string | null;
  createdAt: string | null;
}

export interface RfqLine {
  rfqLineId: string;
  rfqId: string;
  bomNodeId: string;
  lineNumber: number;
  quantity: string;
  uom: string | null;
  targetUnitPrice: string | null;
  notes: string | null;
}

export interface RfqDetail {
  rfq: RfqEvent;
  lines: RfqLine[];
}

export interface SupplierQuote {
  quoteId: string;
  rfqId: string;
  rfqLineId: string;
  supplierId: string;
  bomNodeId: string;
  unitPrice: string;
  currencyCode: string;
  toolingLumpSum: string | null;
  leadTimeDays: number | null;
  validFrom: string | null;
  validTo: string | null;
  status: QuoteStatus;
  negotiationReference: string | null;
  notes: string | null;
}

export async function listSuppliers(): Promise<Supplier[]> {
  const { data } = await httpClient.get<Supplier[]>('/api/v1/rfq/suppliers');
  return data;
}

export async function createSupplier(req: {
  supplierCode: string;
  supplierName: string;
  countryCode?: string;
  currencyCode?: string;
  notes?: string;
}): Promise<Supplier> {
  const { data } = await httpClient.post<Supplier>('/api/v1/rfq/suppliers', req);
  return data;
}

export async function setSupplierStatus(supplierId: string, status: SupplierStatus): Promise<Supplier> {
  const { data } = await httpClient.post<Supplier>(
    `/api/v1/rfq/suppliers/${encodeURIComponent(supplierId)}/status`,
    null,
    { params: { status } },
  );
  return data;
}

export async function createRfq(req: {
  programId: string;
  rfqNumber: string;
  title: string;
  dueDate?: string;
  currencyCode?: string;
  notes?: string;
}): Promise<RfqEvent> {
  const { data } = await httpClient.post<RfqEvent>('/api/v1/rfq/events', req);
  return data;
}

export async function getRfq(rfqId: string): Promise<RfqDetail> {
  const { data } = await httpClient.get<RfqDetail>(`/api/v1/rfq/events/${encodeURIComponent(rfqId)}`);
  return data;
}

export async function openRfq(rfqId: string): Promise<RfqEvent> {
  const { data } = await httpClient.post<RfqEvent>(`/api/v1/rfq/events/${encodeURIComponent(rfqId)}/open`);
  return data;
}

export async function closeRfq(rfqId: string): Promise<RfqEvent> {
  const { data } = await httpClient.post<RfqEvent>(`/api/v1/rfq/events/${encodeURIComponent(rfqId)}/close`);
  return data;
}

export async function cancelRfq(rfqId: string): Promise<RfqEvent> {
  const { data } = await httpClient.post<RfqEvent>(`/api/v1/rfq/events/${encodeURIComponent(rfqId)}/cancel`);
  return data;
}

export async function addRfqLine(
  rfqId: string,
  req: {
    bomNodeId: string;
    lineNumber?: number;
    quantity: string;
    uom?: string;
    targetUnitPrice?: string;
    notes?: string;
  },
): Promise<RfqLine> {
  const { data } = await httpClient.post<RfqLine>(
    `/api/v1/rfq/events/${encodeURIComponent(rfqId)}/lines`,
    req,
  );
  return data;
}

export async function submitQuote(
  rfqId: string,
  req: {
    rfqLineId: string;
    supplierId: string;
    unitPrice: string;
    currencyCode?: string;
    toolingLumpSum?: string;
    leadTimeDays?: number;
    validFrom?: string;
    validTo?: string;
    negotiationReference?: string;
    notes?: string;
  },
): Promise<SupplierQuote> {
  const { data } = await httpClient.post<SupplierQuote>(
    `/api/v1/rfq/events/${encodeURIComponent(rfqId)}/quotes`,
    req,
  );
  return data;
}

export async function listQuotes(rfqId: string): Promise<SupplierQuote[]> {
  const { data } = await httpClient.get<SupplierQuote[]>(
    `/api/v1/rfq/events/${encodeURIComponent(rfqId)}/quotes`,
  );
  return data;
}

export async function updateQuoteStatus(
  quoteId: string,
  req: { status: QuoteStatus; negotiationReference?: string },
): Promise<SupplierQuote> {
  const { data } = await httpClient.post<SupplierQuote>(
    `/api/v1/rfq/quotes/${encodeURIComponent(quoteId)}/status`,
    req,
  );
  return data;
}

export async function acceptedQuotesForBomNode(bomNodeId: string): Promise<SupplierQuote[]> {
  const { data } = await httpClient.get<SupplierQuote[]>('/api/v1/rfq/quotes/accepted', {
    params: { bomNodeId },
  });
  return data;
}

export type QuoteShouldCostRow = {
  quoteId: string;
  rfqLineId: string;
  supplierId: string;
  bomNodeId?: string | null;
  quoteUnitPrice?: string | number | null;
  quoteCurrency?: string | null;
  quoteStatus?: string;
  shouldCostTotal?: string | number | null;
  shouldCostBuildUpId?: string | null;
  comparisonStatus: string;
  varianceAmount?: string | number | null;
  varianceNote?: string | null;
};

export type QuoteShouldCostComparison = {
  rfqId: string;
  rows: QuoteShouldCostRow[];
  messages?: string[];
};

export async function fetchQuoteShouldCostComparison(rfqId: string) {
  const { data } = await httpClient.get<QuoteShouldCostComparison>(
    `/api/v1/rfq/events/${encodeURIComponent(rfqId)}/quote-comparison`,
  );
  return data;
}
