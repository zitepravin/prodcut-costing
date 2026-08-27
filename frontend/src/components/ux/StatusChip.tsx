import { Chip, type ChipProps } from '@mui/material';

const STATUS_MAP: Record<string, { label: string; color: ChipProps['color'] }> = {
  ACTIVE: { label: 'Active', color: 'success' },
  DRAFT: { label: 'Draft', color: 'default' },
  APPROVED: { label: 'Approved', color: 'success' },
  REJECTED: { label: 'Rejected', color: 'error' },
  SUBMITTED: { label: 'Submitted', color: 'info' },
  LOCKED: { label: 'Locked', color: 'warning' },
  VERIFIED: { label: 'Verified', color: 'success' },
  FAILED: { label: 'Failed', color: 'error' },
  EXPIRED: { label: 'Expired', color: 'error' },
  AVAILABLE: { label: 'Available', color: 'success' },
  NOT_AVAILABLE: { label: 'Not available', color: 'default' },
  DATA_NOT_AVAILABLE: { label: 'Data not available', color: 'default' },
  CALCULATED: { label: 'Calculated', color: 'info' },
  FROZEN: { label: 'Frozen', color: 'warning' },
  PARTIAL: { label: 'Partial', color: 'warning' },
  ELIGIBLE: { label: 'Eligible', color: 'success' },
  NOT_ELIGIBLE: { label: 'Not eligible', color: 'error' },
  BLOCKED: { label: 'Blocked', color: 'error' },
  SUSPENDED: { label: 'Suspended', color: 'warning' },
  UNKNOWN: { label: 'Unknown', color: 'default' },
};

export function StatusChip({
  status,
  label,
  size = 'small',
  variant = 'outlined',
}: {
  status?: string | null;
  label?: string;
  size?: ChipProps['size'];
  variant?: ChipProps['variant'];
}) {
  if (status == null || status === '') {
    return <Chip size={size} variant={variant} label={label ?? '—'} color="default" />;
  }
  const key = String(status).toUpperCase().replace(/\s+/g, '_');
  const mapped = STATUS_MAP[key];
  return (
    <Chip
      size={size}
      variant={variant}
      color={mapped?.color ?? 'default'}
      label={label ?? mapped?.label ?? status}
    />
  );
}
