/**
 * Shared page context for the Helping Agent FAB.
 * Pages may call setHelpingAgentPageContext when selection changes.
 * Values are advisory only; backend still enforces auth/tenant/module.
 */
export type HelpingAgentPageContext = {
  currentPage?: string;
  programId?: string;
  bomNodeId?: string;
  buildUpId?: string;
  materialCode?: string;
  targetCostId?: string;
  partNumber?: string;
};

const STORAGE_KEY = 'helpingAgent.pageContext';

export function setHelpingAgentPageContext(partial: HelpingAgentPageContext): void {
  try {
    const prev = getHelpingAgentPageContext();
    const next = { ...prev, ...partial };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    /* ignore quota / private mode */
  }
}

export function getHelpingAgentPageContext(): HelpingAgentPageContext {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return {};
    return JSON.parse(raw) as HelpingAgentPageContext;
  } catch {
    return {};
  }
}

/** Path → suggested prompts for Product Costing help */
export function suggestedPromptsForPath(pathname: string): string[] {
  const p = pathname.replace(/^\//, '') || 'dashboard';
  if (p.startsWith('bom')) {
    return [
      'Explain this BOM tree',
      'What drives rolled-up cost?',
      'Which nodes are BUY vs MAKE?',
      'What is missing for a complete cost?',
    ];
  }
  if (p.startsWith('should-cost')) {
    return [
      'How is the should-cost built?',
      'Explain the waterfall',
      'Why is should-cost above quote?',
      'What data is missing?',
    ];
  }
  if (p.startsWith('target-costing')) {
    return ['Are we above target?', 'Explain target variance', 'What is missing for target costing?'];
  }
  if (p.startsWith('materials')) {
    return ['Which material drives cost?', 'Show material price trend', 'What is missing for material cost?'];
  }
  if (p.startsWith('process')) {
    return ['Why is process cost high?', 'Explain routing and cycle time', 'What is missing for process cost?'];
  }
  if (p.startsWith('tooling')) {
    return ['Why is tooling cost high?', 'Explain tooling amortization', 'What is missing for tooling?'];
  }
  if (p.startsWith('cost-comparison')) {
    return ['Compare should-cost vs quote', 'Explain cost comparison', 'What data is missing?'];
  }
  if (p.startsWith('reports') || p === '' || p === 'dashboard') {
    return ['Explain the dashboard KPIs', 'Summarize operational report', 'What is missing in costing data?'];
  }
  return [
    'Explain this cost breakdown',
    'What is missing for a complete cost?',
    'How is the should-cost built?',
    'Are we above target?',
  ];
}
