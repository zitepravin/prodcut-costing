import { httpClient } from './httpClient';
import type { MachineMaster, MasterDataStatus, PageResponse, RateBuildUpResponse } from '../types/api';

export async function searchMachines(params: {
  q?: string;
  status?: MasterDataStatus | '';
  page?: number;
  size?: number;
}): Promise<PageResponse<MachineMaster>> {
  const { data } = await httpClient.get<PageResponse<MachineMaster>>('/api/v1/master/machines', {
    params: {
      q: params.q || undefined,
      status: params.status || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'code,asc',
    },
  });
  return data;
}

export async function getRateBuildUp(id: string): Promise<RateBuildUpResponse> {
  const { data } = await httpClient.get<RateBuildUpResponse>(
    `/api/v1/master/machines/${encodeURIComponent(id)}/rate-buildup`,
  );
  return data;
}
