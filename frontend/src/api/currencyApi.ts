import { httpClient } from './httpClient';

export interface CurrencyResponse {
  currencyCode: string;
  description: string | null;
  decimalPlaces: number | null;
}

export interface ExchangeRateResponse {
  rateId: string;
  fromCurrency: string;
  toCurrency: string;
  rate: string;
  effectiveDate: string;
  source: string | null;
  createdAt: string | null;
}

export interface ConvertRequest {
  amount: string;
  fromCurrency: string;
  toCurrency: string;
  asOfDate?: string | null;
}

export interface ConvertResponse {
  originalAmount: string;
  fromCurrency: string;
  convertedAmount: string;
  toCurrency: string;
  rateUsed: string;
  rateEffectiveDate: string;
  conversionPath: string;
}

export interface PublishRateRequest {
  fromCurrency: string;
  toCurrency: string;
  rate: string;
  effectiveDate: string;
  source?: string;
}

export async function listCurrencies(): Promise<CurrencyResponse[]> {
  const { data } = await httpClient.get<CurrencyResponse[]>('/api/v1/currency');
  return data;
}

export async function convertAmount(req: ConvertRequest): Promise<ConvertResponse> {
  const { data } = await httpClient.post<ConvertResponse>('/api/v1/currency/convert', req);
  return data;
}

export async function publishRate(req: PublishRateRequest): Promise<ExchangeRateResponse> {
  const { data } = await httpClient.post<ExchangeRateResponse>('/api/v1/currency/rates', req);
  return data;
}

export async function rateHistory(from: string, to: string): Promise<{ rates: ExchangeRateResponse[] }> {
  const { data } = await httpClient.get<{ fromCurrency: string; toCurrency: string; rates: ExchangeRateResponse[] }>(
    `/api/v1/currency/rates/${from}/${to}`,
  );
  return data;
}

export async function recentRates(since?: string): Promise<ExchangeRateResponse[]> {
  const { data } = await httpClient.get<ExchangeRateResponse[]>('/api/v1/currency/rates', {
    params: since ? { since } : undefined,
  });
  return data;
}
