import { describe, expect, it } from 'vitest';
import { actionColor } from './auditAction';

describe('actionColor', () => {
  it('maps known actions', () => {
    expect(actionColor('CREATE')).toBe('success');
    expect(actionColor('UPDATE')).toBe('info');
    expect(actionColor('TRANSITION')).toBe('warning');
    expect(actionColor('VERIFY')).toBe('info');
    expect(actionColor('DELETE')).toBe('error');
  });

  it('defaults unknown', () => {
    expect(actionColor('LOGIN')).toBe('default');
    expect(actionColor('')).toBe('default');
  });
});
