import { httpClient } from './httpClient';

export type ChangeTrigger =
  | 'ECN'
  | 'RFQ_ROUND'
  | 'RATE_UPDATE'
  | 'VOLUME_REVISION'
  | 'MANUAL_REVISION'
  | 'ROLLBACK'
  | 'APPROVAL_BASELINE';

export interface TimelineEntry {
  versionId: string;
  versionNumber: number;
  changeTrigger: ChangeTrigger;
  changeReference: string | null;
  changeComment: string | null;
  programBaseline: boolean;
  createdBy: string | null;
  createdAt: string;
}

export interface VersionResponse extends TimelineEntry {
  entityType: string;
  entityId: string;
  snapshot: Record<string, unknown>;
}

export async function fetchTimeline(entityType: string, entityId: string): Promise<TimelineEntry[]> {
  const { data } = await httpClient.get<TimelineEntry[]>('/api/v1/versions/timeline', {
    params: { entityType, entityId },
  });
  return data;
}

export async function createSnapshot(body: {
  entityType: string;
  entityId: string;
  changeTrigger: ChangeTrigger;
  changeReference?: string;
  changeComment?: string;
  snapshot?: Record<string, unknown>;
}): Promise<VersionResponse> {
  const { data } = await httpClient.post<VersionResponse>('/api/v1/versions', body);
  return data;
}

export async function setBaseline(versionId: string): Promise<VersionResponse> {
  const { data } = await httpClient.post<VersionResponse>(`/api/v1/versions/${encodeURIComponent(versionId)}/baseline`);
  return data;
}

export async function rollbackVersion(versionId: string, comment?: string): Promise<VersionResponse> {
  const { data } = await httpClient.post<VersionResponse>(
    `/api/v1/versions/${encodeURIComponent(versionId)}/rollback`,
    comment ? { comment } : {},
  );
  return data;
}

export async function compareVersions(
  entityType: string,
  entityId: string,
  leftVersion: number,
  rightVersion: number,
) {
  const { data } = await httpClient.get('/api/v1/versions/compare', {
    params: { entityType, entityId, leftVersion, rightVersion },
  });
  return data;
}
