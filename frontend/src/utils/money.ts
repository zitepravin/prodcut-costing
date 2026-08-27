/**
 * Display helpers for backend NUMERIC(18,6) / BigDecimal values.
 * Never parse money with parseFloat for arithmetic — display only.
 */

export function formatMoney(value: string | number | null | undefined, fractionDigits = 6): string {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  const raw = typeof value === 'number' ? value.toFixed(fractionDigits) : String(value);
  const negative = raw.startsWith('-');
  const abs = negative ? raw.slice(1) : raw;
  const [intPart, fracPart = ''] = abs.split('.');
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const frac = (fracPart + '0'.repeat(fractionDigits)).slice(0, fractionDigits);
  return `${negative ? '-' : ''}${grouped}.${frac}`;
}

export function formatRate(value: string | number | null | undefined, uom?: string | null): string {
  const amount = formatMoney(value, 6);
  if (amount === '—') return amount;
  return uom ? `${amount} / ${uom}` : amount;
}
