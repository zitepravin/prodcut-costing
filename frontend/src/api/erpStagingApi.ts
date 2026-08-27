import { httpClient } from './httpClient';

export type ErpBatchStatus =
  | 'RECEIVED'
  | 'VALIDATING'
  | 'READY'
  | 'PROMOTED'
  | 'FAILED'
  | 'CANCELLED';

export type ErpBatchType = 'PART' | 'BOM' | 'MATERIAL_PRICE' | 'MIXED';

export type ErpStagingRowStatus = 'PENDING' | 'VALID' | 'INVALID' | 'PROMOTED' | 'SKIPPED';

export interface BatchResponse {
  batchId: string;
  sourceSystem: string;
  batchType: ErpBatchType;
  status: ErpBatchStatus;
  rowCount: number;
  errorCount: number;
  sourceFileName: string | null;
  receivedAt: string;
  processedAt: string | null;
}

export interface PartStagingResponse {
  stagingId: string;
  batchId: string;
  partNumber: string;
  revision: string | null;
  netWeightKg: number | null;
  status: ErpStagingRowStatus;
  errorMessage: string | null;
}

export interface BatchDetailResponse {
  batch: BatchResponse;
  parts: PartStagingResponse[];
  bomLineCount: number;
  materialPriceCount: number;
}

export interface ValidateResult {
  batchId: string;
  status: ErpBatchStatus;
  validRows: number;
  invalidRows: number;
  messages: string[];
}

export interface PromoteResult {
  batchId: string;
  status: ErpBatchStatus;
  materialPricesPromoted: number;
  materialPricesSkipped: number;
  partsAcknowledged: number;
  bomLinesPromoted: number;
  bomLinesSkipped: number;
  messages: string[];
}

export interface IngestRequest {
  sourceSystem: string;
  batchType: ErpBatchType;
  sourceFileName?: string;
  notes?: string;
  parts?: Array<{
    externalPartId: string;
    partNumber: string;
    revision?: string;
    description?: string;
    netWeightKg?: number;
    uom?: string;
    materialCode?: string;
    makeBuyCode?: string;
    plantCode?: string;
    programCode?: string;
  }>;
  bomLines?: Array<{
    parentPartNumber: string;
    childPartNumber: string;
    quantityPer: number;
    parentRevision?: string;
    childRevision?: string;
    uom?: string;
    lineNumber?: number;
    plantCode?: string;
    programCode?: string;
  }>;
  materialPrices?: Array<{
    materialCode: string;
    unitPrice: number;
    currencyCode: string;
    effectiveFrom: string;
    plantCode?: string;
    uom?: string;
    effectiveTo?: string;
    supplierCode?: string;
  }>;
}

export async function listBatches(): Promise<BatchResponse[]> {
  const { data } = await httpClient.get<BatchResponse[]>('/api/v1/erp/staging/batches');
  return data;
}

export async function getBatch(batchId: string): Promise<BatchDetailResponse> {
  const { data } = await httpClient.get<BatchDetailResponse>(
    `/api/v1/erp/staging/batches/${encodeURIComponent(batchId)}`,
  );
  return data;
}

export async function validateBatch(batchId: string): Promise<ValidateResult> {
  const { data } = await httpClient.post<ValidateResult>(
    `/api/v1/erp/staging/batches/${encodeURIComponent(batchId)}/validate`,
  );
  return data;
}

export async function promoteBatch(batchId: string): Promise<PromoteResult> {
  const { data } = await httpClient.post<PromoteResult>(
    `/api/v1/erp/staging/batches/${encodeURIComponent(batchId)}/promote`,
  );
  return data;
}

export async function ingestBatch(request: IngestRequest): Promise<BatchResponse> {
  const { data } = await httpClient.post<BatchResponse>('/api/v1/erp/staging/batches', request);
  return data;
}

export async function cancelBatch(batchId: string): Promise<BatchResponse> {
  const { data } = await httpClient.post<BatchResponse>(
    `/api/v1/erp/staging/batches/${encodeURIComponent(batchId)}/cancel`,
  );
  return data;
}

export async function reopenBatch(batchId: string): Promise<BatchResponse> {
  const { data } = await httpClient.post<BatchResponse>(
    `/api/v1/erp/staging/batches/${encodeURIComponent(batchId)}/reopen`,
  );
  return data;
}

export interface CsvRowError {
  rowNumber: number;
  reason: string;
}

export interface CsvIngestResult {
  batch: BatchResponse | null;
  totalRows: number;
  acceptedRows: number;
  rejectedRows: number;
  errors: CsvRowError[];
}

export async function importMaterialPricesCsv(
  file: File,
  sourceSystem?: string,
): Promise<CsvIngestResult> {
  const form = new FormData();
  form.append('file', file);
  if (sourceSystem) form.append('sourceSystem', sourceSystem);
  const { data } = await httpClient.post<CsvIngestResult>(
    '/api/v1/erp/staging/batches/csv/material-prices',
    form,
  );
  return data;
}

export async function importPartsCsv(file: File, sourceSystem?: string): Promise<CsvIngestResult> {
  const form = new FormData();
  form.append('file', file);
  if (sourceSystem) form.append('sourceSystem', sourceSystem);
  const { data } = await httpClient.post<CsvIngestResult>(
    '/api/v1/erp/staging/batches/csv/parts',
    form,
  );
  return data;
}

export async function importBomCsv(file: File, sourceSystem?: string): Promise<CsvIngestResult> {
  const form = new FormData();
  form.append('file', file);
  if (sourceSystem) form.append('sourceSystem', sourceSystem);
  const { data } = await httpClient.post<CsvIngestResult>(
    '/api/v1/erp/staging/batches/csv/bom',
    form,
  );
  return data;
}
