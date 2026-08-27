import { httpClient } from './httpClient';

export interface CurveYear {
  yearIndex: number;
  calendarYear?: number | null;
  plannedVolume: string | number;
}

export interface VolumeCurve {
  curveId: string;
  programId: string;
  scenarioCode: string;
  scenarioLabel?: string;
  status: string;
  notes?: string;
  years: CurveYear[];
}

export interface YearProjection {
  yearIndex: number;
  calendarYear?: number | null;
  plannedVolume: string | number;
  unitCost: string | number;
  yearTotalCost: string | number;
  cumulativeVolume: string | number;
  cumulativeCost: string | number;
}

export interface LifecycleProjection {
  programId: string;
  scenarioCode: string;
  curveId: string;
  reportingCurrency: string;
  unitCostBaselineStatus: string;
  unitCostBaseline: string | number | null;
  volumeInsensitiveUnit: string | number | null;
  toolingPerUnit: string | number | null;
  toolingStatus: string;
  lifetimeVolumeForCurve: string | number | null;
  years: YearProjection[];
  dataQualityNotes: string[];
}

export async function listVolumeCurves(programId: string): Promise<VolumeCurve[]> {
  const { data } = await httpClient.get<VolumeCurve[]>(
    `/api/v1/programs/${encodeURIComponent(programId)}/volume-curves`,
  );
  return data;
}

export async function upsertVolumeCurve(
  programId: string,
  scenarioCode: string,
  body: { scenarioLabel?: string; notes?: string; years: CurveYear[] },
): Promise<VolumeCurve> {
  const { data } = await httpClient.put<VolumeCurve>(
    `/api/v1/programs/${encodeURIComponent(programId)}/volume-curves/${encodeURIComponent(scenarioCode)}`,
    body,
  );
  return data;
}

export async function activateVolumeCurve(programId: string, scenarioCode: string): Promise<VolumeCurve> {
  const { data } = await httpClient.post<VolumeCurve>(
    `/api/v1/programs/${encodeURIComponent(programId)}/volume-curves/${encodeURIComponent(scenarioCode)}/activate`,
  );
  return data;
}

export async function fetchLifecycleProjection(
  programId: string,
  scenarioCode = 'BASE',
): Promise<LifecycleProjection> {
  const { data } = await httpClient.get<LifecycleProjection>(
    `/api/v1/programs/${encodeURIComponent(programId)}/lifecycle-projection`,
    { params: { scenarioCode } },
  );
  return data;
}
