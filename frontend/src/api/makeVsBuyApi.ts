import { httpClient } from './httpClient';

export type MakeVsBuyLeg = {
  legSeq: number;
  strategyType: string;
  sourceRefId: string;
  supplierId?: string | null;
  unitCostSnapshot?: string | number | null;
  currencySnapshot?: string | null;
  tcoUnitSnapshot?: string | number | null;
  tcoStatus: string;
  eligibilityStatus: string;
  eligibilityNotes?: string | null;
  riskSummary?: string | null;
  dataStatus: string;
};

export type MakeVsBuySimulation = {
  simulationId: string;
  programId: string;
  bomNodeId: string;
  label: string;
  status: string;
  selectedBuildUpId?: string | null;
  mixedCostId?: string | null;
  recommendationCode?: string | null;
  recommendationSummary?: string | null;
  decisionCode?: string | null;
  decisionReason?: string | null;
  decidedAt?: string | null;
  bomFlagApplied: boolean;
  notes?: string | null;
  legs: MakeVsBuyLeg[];
  dataQualityNotes?: string[];
};

export async function listMakeVsBuy(bomNodeId: string): Promise<MakeVsBuySimulation[]> {
  const { data } = await httpClient.get<MakeVsBuySimulation[]>(
    `/api/v1/bom-nodes/${encodeURIComponent(bomNodeId)}/make-vs-buy`,
  );
  return data;
}

export async function createMakeVsBuy(
  bomNodeId: string,
  body: {
    label: string;
    selectedBuildUpId?: string;
    buyQuoteIds?: string[];
    mixedCostId?: string;
    notes?: string;
  },
): Promise<MakeVsBuySimulation> {
  const { data } = await httpClient.post<MakeVsBuySimulation>(
    `/api/v1/bom-nodes/${encodeURIComponent(bomNodeId)}/make-vs-buy`,
    body,
  );
  return data;
}

export async function compareMakeVsBuy(simulationId: string): Promise<MakeVsBuySimulation> {
  const { data } = await httpClient.post<MakeVsBuySimulation>(
    `/api/v1/make-vs-buy/${encodeURIComponent(simulationId)}/compare`,
  );
  return data;
}

export async function decideMakeVsBuy(
  simulationId: string,
  decisionCode: string,
  decisionReason: string,
): Promise<MakeVsBuySimulation> {
  const { data } = await httpClient.post<MakeVsBuySimulation>(
    `/api/v1/make-vs-buy/${encodeURIComponent(simulationId)}/decide`,
    { decisionCode, decisionReason },
  );
  return data;
}

export async function applyMakeVsBuyToBom(simulationId: string): Promise<MakeVsBuySimulation> {
  const { data } = await httpClient.post<MakeVsBuySimulation>(
    `/api/v1/make-vs-buy/${encodeURIComponent(simulationId)}/apply-to-bom`,
  );
  return data;
}

/** P3-04: read-only decision package for CPI/SAP process teams (does not post to SAP CO). */
export type DecisionExportPackage = {
  exportType: string;
  simulationId: string;
  programId: string;
  bomNodeId: string;
  status: string;
  recommendationCode?: string | null;
  decisionCode?: string | null;
  decisionReason?: string | null;
  decidedAt?: string | null;
  bomFlagApplied: boolean;
  reportingCurrency?: string | null;
  legs: MakeVsBuyLeg[];
  generatedAt: string;
  notes: string[];
};

export async function exportMakeVsBuyPackage(simulationId: string): Promise<DecisionExportPackage> {
  const { data } = await httpClient.get<DecisionExportPackage>(
    `/api/v1/make-vs-buy/${encodeURIComponent(simulationId)}/export-package`,
  );
  return data;
}
