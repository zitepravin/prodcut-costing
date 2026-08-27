import { httpClient } from './httpClient';
import type { AppNotification, PageResponse } from '../types/api';

export async function listNotifications(params: {
  unreadOnly?: boolean;
  page?: number;
  size?: number;
}): Promise<PageResponse<AppNotification>> {
  const { data } = await httpClient.get<PageResponse<AppNotification>>('/api/v1/notifications', {
    params: {
      unreadOnly: params.unreadOnly || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
  return data;
}

export async function getUnreadCount(): Promise<number> {
  const { data } = await httpClient.get<{ unreadCount: number }>('/api/v1/notifications/unread-count');
  return data.unreadCount;
}

export async function markNotificationRead(id: string): Promise<AppNotification> {
  const { data } = await httpClient.post<AppNotification>(`/api/v1/notifications/${encodeURIComponent(id)}/read`);
  return data;
}

export async function markAllNotificationsRead(): Promise<void> {
  await httpClient.post('/api/v1/notifications/read-all');
}
