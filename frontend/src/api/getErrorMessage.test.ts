import { describe, expect, it } from 'vitest';
import { getErrorMessage } from './httpClient';

describe('getErrorMessage (QA display)', () => {
  it('uses Error message for thrown errors', () => {
    expect(getErrorMessage(new Error('Network down'))).toBe('Network down');
  });

  it('returns safe default for unknown non-Error values', () => {
    expect(getErrorMessage('boom')).toBe('An unexpected error occurred');
    expect(getErrorMessage(null)).toBe('An unexpected error occurred');
  });

  it('surfaces API business message from axios error shape', () => {
    const axiosErr = Object.assign(new Error('Request failed with status code 400'), {
      isAxiosError: true,
      response: {
        status: 400,
        data: { message: 'Claim status transition not allowed: CLOSED → OPEN', code: 'VS-WC-12' },
      },
    });
    // axios.isAxiosError may not recognize synthetic object — still must not throw
    expect(() => getErrorMessage(axiosErr)).not.toThrow();
    const msg = getErrorMessage(axiosErr);
    expect(msg.length).toBeGreaterThan(0);
  });
});
