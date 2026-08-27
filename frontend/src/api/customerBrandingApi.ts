import { httpClient } from './httpClient';

export interface CustomerLogoMeta {
  customerId: string;
  hasLogo: boolean;
  logoFileId: string;
  fileName?: string;
  contentType?: string;
  sizeBytes?: number;
}

export async function getCustomerLogo(): Promise<CustomerLogoMeta> {
  const { data } = await httpClient.get<CustomerLogoMeta>('/api/v1/customer/branding/logo');
  return data;
}

export async function uploadCustomerLogo(file: File): Promise<CustomerLogoMeta> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await httpClient.post<CustomerLogoMeta>('/api/v1/customer/branding/logo', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function clearCustomerLogo(): Promise<CustomerLogoMeta> {
  const { data } = await httpClient.delete<CustomerLogoMeta>('/api/v1/customer/branding/logo');
  return data;
}

/** Content URL for stored logo (authenticated download via existing files API). */
export function customerLogoContentUrl(logoFileId: string): string {
  return `/api/v1/files/${encodeURIComponent(logoFileId)}/content`;
}
