import { httpClient } from './httpClient';
import type {
  ApprovalInstance,
  ApprovalStep,
  DecisionRequest,
  SubmitWorkflowRequest,
} from '../types/api';

export async function fetchInbox(): Promise<ApprovalStep[]> {
  const { data } = await httpClient.get<ApprovalStep[]>('/api/v1/workflow/inbox');
  return data;
}

export async function submitWorkflow(request: SubmitWorkflowRequest): Promise<ApprovalInstance> {
  const { data } = await httpClient.post<ApprovalInstance>('/api/v1/workflow/submit', request);
  return data;
}

export async function decideStep(stepInstanceId: string, request: DecisionRequest): Promise<ApprovalInstance> {
  const { data } = await httpClient.post<ApprovalInstance>(
    `/api/v1/workflow/steps/${encodeURIComponent(stepInstanceId)}/decision`,
    request,
  );
  return data;
}

export async function fetchHistory(entityType: string, entityId: string): Promise<ApprovalInstance[]> {
  const { data } = await httpClient.get<ApprovalInstance[]>('/api/v1/workflow/history', {
    params: { entityType, entityId },
  });
  return data;
}
