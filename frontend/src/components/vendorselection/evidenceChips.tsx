import { Chip } from '@mui/material';
import type { ChipProps } from '@mui/material/Chip';

/** Shared evidence status chip for Vendor Selection lists (behavior preserved from page helpers). */
export function evidenceChip(status: string | null | undefined) {
  const color: ChipProps['color'] =
    status === 'VERIFIED'
      ? 'success'
      : status === 'FAILED' || status === 'EXPIRED'
        ? 'error'
        : status === 'WAIVED'
          ? 'default'
          : 'warning';
  return <Chip size="small" label={status ?? '—'} color={color} variant="outlined" />;
}

export function yesNoChip(ok: boolean | null | undefined) {
  if (ok == null) return <Chip size="small" label="—" />;
  return <Chip size="small" label={ok ? 'YES' : 'NO'} color={ok ? 'success' : 'default'} variant="outlined" />;
}
