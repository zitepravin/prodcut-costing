import { describe, expect, it } from 'vitest';

/** Mirror of allowed evidence transitions used for UI enablement checks. */
const ALLOWED: Record<string, string[]> = {
  PENDING: ['VERIFIED', 'FAILED', 'WAIVED', 'NOT_REQUIRED', 'EXPIRED'],
  VERIFIED: ['FAILED', 'EXPIRED', 'WAIVED'],
  FAILED: ['VERIFIED', 'PENDING', 'WAIVED', 'EXPIRED'],
  EXPIRED: ['VERIFIED', 'PENDING', 'FAILED', 'WAIVED'],
  WAIVED: ['VERIFIED', 'EXPIRED', 'PENDING'],
  NOT_REQUIRED: ['PENDING', 'VERIFIED', 'WAIVED'],
};

function canTransition(from: string, to: string): boolean {
  if (!to) return false;
  if (!from || from === to) return true;
  return (ALLOWED[from] ?? []).includes(to);
}

describe('QA evidence transition matrix (FE mirror)', () => {
  it('allows PENDING → VERIFIED', () => {
    expect(canTransition('PENDING', 'VERIFIED')).toBe(true);
  });

  it('forbids VERIFIED → PENDING', () => {
    expect(canTransition('VERIFIED', 'PENDING')).toBe(false);
  });

  it('allows EXPIRED → VERIFIED (re-verify path)', () => {
    expect(canTransition('EXPIRED', 'VERIFIED')).toBe(true);
  });

  it('null target is not allowed', () => {
    expect(canTransition('PENDING', '')).toBe(false);
  });
});
