import { httpClient } from './httpClient';

export interface BenchmarkPack {
  packId: string;
  packCode: string;
  packName: string;
  regionCode: string;
  sourceLabel?: string;
  versionLabel: string;
  status: string;
  rateCount: number;
}

export interface BenchmarkRate {
  rateId: string;
  packId: string;
  category: string;
  processFamily?: string;
  rateCode: string;
  description?: string;
  rateValue: string | number;
  currencyCode: string;
  unitOfMeasure: string;
  confidence: string;
}

export async function listBenchmarkPacks(): Promise<BenchmarkPack[]> {
  const { data } = await httpClient.get<BenchmarkPack[]>('/api/v1/benchmarks/packs');
  return data;
}

export async function getBenchmarkPack(packId: string): Promise<{ pack: BenchmarkPack; rates: BenchmarkRate[] }> {
  const { data } = await httpClient.get(`/api/v1/benchmarks/packs/${packId}`);
  return data;
}

export async function createBenchmarkPack(body: {
  packCode: string;
  packName: string;
  regionCode: string;
  sourceLabel?: string;
  versionLabel?: string;
}): Promise<BenchmarkPack> {
  const { data } = await httpClient.post<BenchmarkPack>('/api/v1/benchmarks/packs', body);
  return data;
}

export async function addBenchmarkRate(
  packId: string,
  body: {
    category: string;
    rateCode: string;
    rateValue: string;
    currencyCode: string;
    unitOfMeasure: string;
    processFamily?: string;
    description?: string;
  },
): Promise<BenchmarkRate> {
  const { data } = await httpClient.post<BenchmarkRate>(`/api/v1/benchmarks/packs/${packId}/rates`, body);
  return data;
}

export async function publishBenchmarkPack(packId: string): Promise<BenchmarkPack> {
  const { data } = await httpClient.post<BenchmarkPack>(`/api/v1/benchmarks/packs/${packId}/publish`);
  return data;
}
