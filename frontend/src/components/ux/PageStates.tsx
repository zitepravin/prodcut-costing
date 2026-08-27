import { Alert, Box, CircularProgress, Stack } from '@mui/material';

export function LoadingBlock({ label = 'Loading…' }: { label?: string }) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 6 }} role="status" aria-live="polite">
      <Stack spacing={1} alignItems="center">
        <CircularProgress size={32} />
        <Box component="span" sx={{ typography: 'body2', color: 'text.secondary' }}>
          {label}
        </Box>
      </Stack>
    </Box>
  );
}

export function ErrorBlock({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <Alert
      severity="error"
      action={
        onRetry ? (
          <Box
            component="button"
            onClick={onRetry}
            sx={{
              border: 'none',
              bgcolor: 'transparent',
              color: 'inherit',
              textDecoration: 'underline',
              cursor: 'pointer',
              font: 'inherit',
            }}
          >
            Retry
          </Box>
        ) : undefined
      }
    >
      {message}
    </Alert>
  );
}
