import { httpClient } from './httpClient';

export type PackagingType =
  | 'EXPENDABLE_CARTON'
  | 'RETURNABLE_CONTAINER'
  | 'BLISTER_TRAY'
  | 'PALLETIZED'
  | 'BULK_BIN'
  | 'PROTECTIVE_WRAP'
  | 'CUSTOM';

export type FreightMode = 'NONE' | 'INBOUND' | 'OUTBOUND' | 'BOTH';

export interface PackagingProfile {
  profileId: string;
  bomNodeId: string;
  profileLabel: string;
  packagingType: PackagingType;
  primaryPackCostPerPiece: string;
  secondaryPackCostPerPiece: string;
  returnableContainerCost: string | null;
  containerLifeCycles: string | null;
  netWeightKg: string | null;
  freightMode: FreightMode;
  freightRatePerKg: string | null;
  fixedFreightPerUnit: string | null;
  dutyPct: string | null;
  originPlantId: string | null;
  destinationFreightZone: string | null;
  notes: string | null;
  active: boolean;
}

export interface CreatePackagingProfileRequest {
  bomNodeId: string;
  profileLabel?: string;
  packagingType: PackagingType;
  primaryPackCostPerPiece: string;
  secondaryPackCostPerPiece: string;
  returnableContainerCost?: string | null;
  containerLifeCycles?: string | null;
  netWeightKg?: string | null;
  freightMode: FreightMode;
  freightRatePerKg?: string | null;
  fixedFreightPerUnit?: string | null;
  dutyPct?: string | null;
  originPlantId?: string | null;
  destinationFreightZone?: string | null;
  notes?: string | null;
}

export interface PackagingCalculation {
  calculationId: string;
  profileId: string;
  bomNodeId: string;
  programId: string;
  packagingCostPerPiece: string;
  freightCostPerPiece: string;
  dutyCostPerPiece: string;
  totalPackagingFreightCost: string;
  calculationBasis: string | null;
  calculatedAt: string | null;
}

export async function listPackagingProfiles(bomNodeId: string): Promise<PackagingProfile[]> {
  const { data } = await httpClient.get<PackagingProfile[]>('/api/v1/packaging-logistics/profiles', {
    params: { bomNodeId },
  });
  return data;
}

export async function createPackagingProfile(
  req: CreatePackagingProfileRequest,
): Promise<PackagingProfile> {
  const { data } = await httpClient.post<PackagingProfile>('/api/v1/packaging-logistics/profiles', req);
  return data;
}

export async function calculatePackaging(profileId: string): Promise<PackagingCalculation> {
  const { data } = await httpClient.post<PackagingCalculation>('/api/v1/packaging-logistics/calculate', {
    profileId,
  });
  return data;
}

export async function latestPackagingCalculation(bomNodeId: string): Promise<PackagingCalculation> {
  const { data } = await httpClient.get<PackagingCalculation>(
    '/api/v1/packaging-logistics/calculations/latest',
    { params: { bomNodeId } },
  );
  return data;
}

export async function deactivatePackagingProfile(profileId: string): Promise<PackagingProfile> {
  const { data } = await httpClient.post<PackagingProfile>(
    `/api/v1/packaging-logistics/profiles/${encodeURIComponent(profileId)}/deactivate`,
  );
  return data;
}
