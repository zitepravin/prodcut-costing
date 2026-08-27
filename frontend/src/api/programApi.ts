import { httpClient } from './httpClient';
import type { VehicleProgram } from '../types/api';

export async function listPrograms(): Promise<VehicleProgram[]> {
  const { data } = await httpClient.get<VehicleProgram[]>('/api/v1/master/programs');
  return data;
}
