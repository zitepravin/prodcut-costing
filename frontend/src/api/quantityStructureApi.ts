import { httpClient } from './httpClient';

export interface QuantityStructureEstimate {
  bomNodeId: string;
  routingId: string;
  costingVariantId?: string;
  variantCode?: string;
  lotSizeUsed: number | string;
  scrapPctUsed: number | string;
  yieldPctUsed: number | string;
  quantityStructureSource: string;
  material?: {
    materialCode: string;
    netWeight?: number | string;
    grossWeight?: number | string;
    unitRate?: number | string;
    materialCost?: number | string;
    basis?: string;
  };
  operations: Array<{
    operationId: string;
    sequenceNumber: number;
    processName?: string;
    machineHours?: number | string;
    laborHours?: number | string;
    runCost?: number | string;
    setupCostPerPiece?: number | string;
    totalOperationCost?: number | string;
    batchSizeUsed?: number | string;
  }>;
  totalMaterialCost: number | string;
  totalProcessCost: number | string;
  totalQuantityStructureCost: number | string;
  calculationBasis: string;
}

export async function estimateQuantityStructure(body: {
  bomNodeId: string;
  costingVariantId?: string;
  routingId?: string;
  lotSize?: string;
}): Promise<QuantityStructureEstimate> {
  const { data } = await httpClient.post<QuantityStructureEstimate>('/api/v1/quantity-structure/estimate', body);
  return data;
}
