import { describe, expect, it } from 'vitest';
import { formatMoney, formatRate } from './money';

describe('formatMoney', () => {
  it('renders em dash for nullish and empty', () => {
    expect(formatMoney(null)).toBe('—');
    expect(formatMoney(undefined)).toBe('—');
    expect(formatMoney('')).toBe('—');
  });

  it('formats string decimals with grouping', () => {
    expect(formatMoney('1234.5', 2)).toBe('1,234.50');
    expect(formatMoney('1000000.123456', 6)).toBe('1,000,000.123456');
  });

  it('formats number without scientific notation path', () => {
    expect(formatMoney(42, 2)).toBe('42.00');
  });

  it('preserves negative sign', () => {
    expect(formatMoney('-99.1', 2)).toBe('-99.10');
  });

  it('pads fractional part', () => {
    expect(formatMoney('1', 4)).toBe('1.0000');
  });
});

describe('formatRate', () => {
  it('returns dash when money is empty', () => {
    expect(formatRate(null)).toBe('—');
  });

  it('appends uom when provided', () => {
    expect(formatRate('10', 'hr')).toBe('10.000000 / hr');
  });

  it('omits uom when absent', () => {
    expect(formatRate('10')).toBe('10.000000');
  });
});
