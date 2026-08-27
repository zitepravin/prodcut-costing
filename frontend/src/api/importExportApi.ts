import { httpClient } from './httpClient';

export interface ImportResult {
  totalRows: number;
  succeeded: number;
  failed: number;
  errors: Array<{ rowNumber: number; reason: string }>;
}

export async function bulkImportMaterials(file: File): Promise<ImportResult> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await httpClient.post<ImportResult>('/api/v1/master/bulk-import/materials', form);
  return data;
}

export async function bulkImportMachines(file: File): Promise<ImportResult> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await httpClient.post<ImportResult>('/api/v1/master/bulk-import/machines', form);
  return data;
}

export async function bulkImportLabor(file: File): Promise<ImportResult> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await httpClient.post<ImportResult>('/api/v1/master/bulk-import/labor', form);
  return data;
}

async function downloadCsv(path: string, filename: string): Promise<void> {
  const { data } = await httpClient.get<Blob>(path, { responseType: 'blob' });
  const url = URL.createObjectURL(data);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function exportMaterialsCsv(): Promise<void> {
  return downloadCsv('/api/v1/master/export/materials', 'materials.csv');
}

export function exportMachinesCsv(): Promise<void> {
  return downloadCsv('/api/v1/master/export/machines', 'machines.csv');
}

export function exportLaborCsv(): Promise<void> {
  return downloadCsv('/api/v1/master/export/labor', 'labor.csv');
}
