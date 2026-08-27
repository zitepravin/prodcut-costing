import { Alert, Stack, Typography, Paper } from '@mui/material';

interface PlaceholderPageProps {
  title: string;
  module: string;
  description: string;
  apiHint?: string;
}

export function PlaceholderPage({ title, module, description, apiHint }: PlaceholderPageProps) {
  return (
    <Stack spacing={2}>
      <Typography variant="h5">{title}</Typography>
      <Alert severity="info">
        UI shell for <strong>{module}</strong>. Backend endpoints exist; this screen will be
        completed in a follow-up session.
      </Alert>
      <Paper sx={{ p: 3 }}>
        <Typography variant="body1" sx={{ mb: 2 }}>
          {description}
        </Typography>
        {apiHint && (
          <Typography variant="body2" color="text.secondary">
            Primary API: <code>{apiHint}</code>
          </Typography>
        )}
      </Paper>
    </Stack>
  );
}
