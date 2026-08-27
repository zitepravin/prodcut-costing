import { httpClient } from './httpClient';
import type { PageResponse } from '../types/api';

export interface AuditLogEntry {
  auditId: string;
  entityType: string;
  entityId: string;
  action: string;
  oldValue: string | null;
  newValue: string | null;
  userId: string;
  timestamp: string;
  ipAddress: string | null;
}

/** Supported filter actions include CREATE, UPDATE, DELETE, TRANSITION, VERIFY, plus auth events. */
export async function searchAuditLog(params: {
  entityType?: string;
  entityId?: string;
  userId?: string;
  action?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<AuditLogEntry>> {
  const { data } = await httpClient.get<PageResponse<AuditLogEntry>>('/api/v1/audit', {
    params: {
      entityType: params.entityType || undefined,
      entityId: params.entityId || undefined,
      userId: params.userId || undefined,
      action: params.action || undefined,
      from: params.from || undefined,
      to: params.to || undefined,
      page: params.page ?? 0,
      size: params.size ?? 50,
      sort: 'timestamp,desc',
    },
  });
  return data;
}
