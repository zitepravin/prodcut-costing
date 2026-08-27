import { Box, LinearProgress, Stack, Typography } from '@mui/material';
import type { WaterfallPoint } from '../../types/api';
import { formatMoney } from '../../utils/money';

interface WaterfallChartProps {
  elements: WaterfallPoint[];
}

/** Simple proportional bar waterfall — amounts are decimal strings from the API. */
export function WaterfallChart({ elements }: WaterfallChartProps) {
  const amounts = elements.map((e) => Math.abs(Number(e.amount) || 0));
  const max = Math.max(...amounts, 0);

  if (!elements.length) {
    return (
      <Typography variant="body2" color="text.secondary">
        No waterfall elements.
      </Typography>
    );
  }

  return (
    <Stack spacing={1.5}>
      {elements.map((el) => {
        const n = Math.abs(Number(el.amount) || 0);
        const pct = max > 0 ? Math.min(100, (n / max) * 100) : 0;
        return (
          <Box key={el.element}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {el.element}
              </Typography>
              <Typography variant="body2" sx={{ fontFamily: 'ui-monospace, monospace' }}>
                {formatMoney(el.amount)}
              </Typography>
            </Stack>
            <LinearProgress
              variant="determinate"
              value={pct}
              sx={{ height: 10, borderRadius: 1, bgcolor: 'action.hover' }}
            />
          </Box>
        );
      })}
    </Stack>
  );
}
