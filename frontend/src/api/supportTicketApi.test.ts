import { describe, expect, it } from 'vitest';
import { hasPermission, PERMISSIONS } from '../auth/permissionMatrix';

/**
 * UI visibility rules for Support Ticket actions (mirrors page gates).
 * Backend remains authoritative.
 */
function canShowNewTicket(authorities: string[]): boolean {
  return hasPermission(authorities, PERMISSIONS.SUPPORT_TICKET_CREATE);
}
function canShowReply(authorities: string[]): boolean {
  return hasPermission(authorities, PERMISSIONS.SUPPORT_TICKET_REPLY);
}
function canShowClose(authorities: string[]): boolean {
  return hasPermission(authorities, PERMISSIONS.SUPPORT_TICKET_CLOSE);
}
function canShowPlatformAdmin(authorities: string[]): boolean {
  return hasPermission(authorities, PERMISSIONS.SUPPORT_TICKET_MANAGE);
}

describe('Support ticket UI visibility', () => {
  it('buyer sees create/reply/close, not platform admin', () => {
    const roles = ['PURCHASING_BUYER'];
    expect(canShowNewTicket(roles)).toBe(true);
    expect(canShowReply(roles)).toBe(true);
    expect(canShowClose(roles)).toBe(true);
    expect(canShowPlatformAdmin(roles)).toBe(false);
  });

  it('system admin sees platform admin', () => {
    expect(canShowPlatformAdmin(['SYSTEM_ADMINISTRATOR'])).toBe(true);
  });

  it('no roles sees nothing', () => {
    expect(canShowNewTicket([])).toBe(false);
    expect(canShowReply([])).toBe(false);
    expect(canShowClose([])).toBe(false);
    expect(canShowPlatformAdmin([])).toBe(false);
  });
});

describe('Support ticket number format', () => {
  it('matches ST-###### pattern', () => {
    const re = /^ST-\d{6}$/;
    expect(re.test('ST-000001')).toBe(true);
    expect(re.test('ST-12')).toBe(false);
    expect(re.test('TICKET-1')).toBe(false);
  });
});
