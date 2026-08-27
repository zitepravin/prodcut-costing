import { httpClient } from './httpClient';
import type { PageResponse } from '../types/api';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_FOR_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory =
  | 'TECHNICAL_ISSUE'
  | 'LOGIN_ACCESS'
  | 'CONFIGURATION'
  | 'COSTING'
  | 'BOM'
  | 'RFQ'
  | 'SUPPLIER'
  | 'DATA_ISSUE'
  | 'OTHER';

export type TicketSummary = {
  ticketId: string;
  ticketNumber: string;
  subject: string;
  category: TicketCategory;
  priority: TicketPriority;
  status: TicketStatus;
  assignedToUserId: string | null;
  createdAt: string;
  updatedAt: string | null;
};

export type TicketMessage = {
  messageId: string;
  authorUserId: string;
  authorType: 'CUSTOMER' | 'SUPPORT' | 'SYSTEM';
  message: string;
  createdAt: string;
};

export type TicketDetail = TicketSummary & {
  customerId: string;
  description: string;
  createdByUserId: string;
  resolvedAt: string | null;
  closedAt: string | null;
  messages: TicketMessage[];
  internalNotes: { noteId: string; authorUserId: string; note: string; createdAt: string }[];
};

export async function listMyTickets(params?: {
  status?: TicketStatus;
  page?: number;
  size?: number;
}): Promise<PageResponse<TicketSummary>> {
  const { data } = await httpClient.get<PageResponse<TicketSummary>>('/api/v1/support/tickets', {
    params: { status: params?.status, page: params?.page ?? 0, size: params?.size ?? 20 },
  });
  return data;
}

export async function createTicket(body: {
  subject: string;
  category: TicketCategory;
  priority: TicketPriority;
  description: string;
}): Promise<TicketDetail> {
  const { data } = await httpClient.post<TicketDetail>('/api/v1/support/tickets', body);
  return data;
}

export async function getTicket(ticketId: string): Promise<TicketDetail> {
  const { data } = await httpClient.get<TicketDetail>(
    `/api/v1/support/tickets/${encodeURIComponent(ticketId)}`,
  );
  return data;
}

export async function replyTicket(ticketId: string, message: string): Promise<TicketMessage> {
  const { data } = await httpClient.post<TicketMessage>(
    `/api/v1/support/tickets/${encodeURIComponent(ticketId)}/messages`,
    { message },
  );
  return data;
}

export async function closeTicket(ticketId: string): Promise<TicketDetail> {
  const { data } = await httpClient.post<TicketDetail>(
    `/api/v1/support/tickets/${encodeURIComponent(ticketId)}/close`,
  );
  return data;
}

export async function listPlatformTickets(page = 0, size = 20): Promise<PageResponse<TicketSummary & { customerId: string }>> {
  const { data } = await httpClient.get(`/api/v1/platform/support/tickets`, { params: { page, size } });
  return data;
}
