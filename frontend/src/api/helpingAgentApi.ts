import { httpClient } from './httpClient';

export type HelpingAgentContext = {
  currentPage?: string;
  programId?: string;
  bomNodeId?: string;
  partNumber?: string;
  revision?: string;
  currency?: string;
  costVersionId?: string;
  buildUpId?: string;
  materialCode?: string;
  routingId?: string;
  toolingId?: string;
  targetCostId?: string;
};

export type HelpingAgentSource = { type: string; id: string; label: string };

export type HelpingAgentChatResponse = {
  answer: string;
  classification: string;
  confidence: string;
  sources: HelpingAgentSource[];
  calculations: unknown[];
  missingData: string[];
  suggestedNextSteps: string[];
  correlationId: string;
};

export async function chatWithHelpingAgent(
  question: string,
  context?: HelpingAgentContext,
  correlationId?: string,
): Promise<HelpingAgentChatResponse> {
  const { data } = await httpClient.post<HelpingAgentChatResponse>('/api/v1/helping-agent/chat', {
    question,
    context: context ?? {},
    correlationId,
  });
  return data;
}
