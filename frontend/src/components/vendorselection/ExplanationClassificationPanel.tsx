import { Alert, Chip, Stack, Typography, Box } from '@mui/material';
import type { RecommendationExplanation } from '../../api/vendorSelectionApi';

function parseJsonBlock(raw: string | null | undefined): unknown {
  if (!raw || !raw.trim()) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

function Block({
  title,
  color,
  data,
}: {
  title: string;
  color: 'default' | 'success' | 'info' | 'warning' | 'error';
  data: unknown;
}) {
  if (data == null) {
    return null;
  }
  const text = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
  if (!text || text === 'null' || text === '{}') return null;
  return (
    <Box>
      <Chip size="small" label={title} color={color} sx={{ mb: 0.5 }} />
      <Typography
        component="pre"
        variant="caption"
        sx={{
          m: 0,
          p: 1,
          bgcolor: 'action.hover',
          borderRadius: 1,
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
        }}
      >
        {text}
      </Typography>
    </Box>
  );
}

/**
 * Surfaces FACT / CALCULATED / AI INFERENCE / MISSING DATA classification
 * from recommendation explanation JSON fields (AI information classification).
 */
export function ExplanationClassificationPanel({ explanation }: { explanation: RecommendationExplanation }) {
  const fact = parseJsonBlock(explanation.factJson);
  const calculated = parseJsonBlock(explanation.calculatedJson);
  const inference = parseJsonBlock(explanation.inferenceJson);
  const missing = parseJsonBlock(explanation.missingJson);

  return (
    <Stack spacing={1.5}>
      <Alert severity="info">
        Classification: <strong>FACT</strong> = verified data · <strong>CALCULATED</strong> = derived ·{' '}
        <strong>INFERENCE</strong> = AI interpretation (not award) · <strong>MISSING</strong> = unavailable
      </Alert>
      <Block title="FACT" color="success" data={fact} />
      <Block title="CALCULATED RESULT" color="info" data={calculated} />
      <Block title="AI INFERENCE" color="warning" data={inference} />
      <Block title="MISSING DATA" color="error" data={missing} />
      {!fact && !calculated && !inference && !missing && (
        <Typography variant="body2" color="text.secondary">
          No structured classification payloads on this explanation; narrative fields above still apply.
        </Typography>
      )}
    </Stack>
  );
}
