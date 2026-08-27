import { Box, Paper, Stack, Typography } from '@mui/material';
import { formatMoney } from '../../utils/money';
export function CostSummaryStrip({ shouldCost, targetCost, quotedCost, variance, drivers }: {
  shouldCost?: string | number | null; targetCost?: string | number | null;
  quotedCost?: string | number | null; variance?: string | number | null;
  drivers?: { label: string; amount: string | number }[];
}) {
  const items = [
    { label: 'Should-cost', value: shouldCost, emphasize: true },
    { label: 'Target', value: targetCost },
    { label: 'Quote', value: quotedCost },
    { label: 'Variance', value: variance, emphasize: true },
  ].filter((i) => i.value != null && i.value !== '');
  if (!items.length && !drivers?.length) return null;
  return (
    <Paper variant="outlined" sx={{ p: 1.5, mb: 2, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between">
        <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap>
          {items.map((i) => (
            <Box key={i.label}>
              <Typography variant="caption" sx={{ opacity: 0.85 }}>{i.label}</Typography>
              <Typography variant={i.emphasize ? 'h6' : 'subtitle1'} fontWeight={700}>{formatMoney(i.value as string | number)}</Typography>
            </Box>
          ))}
        </Stack>
        {drivers && (
          <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
            {drivers.map((d) => (
              <Box key={d.label}>
                <Typography variant="caption">{d.label}</Typography>
                <Typography variant="body2" fontWeight={600}>{formatMoney(d.amount)}</Typography>
              </Box>
            ))}
          </Stack>
        )}
      </Stack>
    </Paper>
  );
}
