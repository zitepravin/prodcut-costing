import { Paper, Stack, Typography, Box } from '@mui/material';
import type { ReactNode } from 'react';

export interface SectionCardProps {
  title?: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
  dense?: boolean;
}

export function SectionCard({ title, description, action, children, dense }: SectionCardProps) {
  return (
    <Paper variant="outlined" sx={{ p: dense ? 1.5 : 2 }}>
      {(title || action) && (
        <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1} sx={{ mb: title ? 1.5 : 0 }}>
          <Box>
            {title && (
              <Typography variant="subtitle1" component="h2">
                {title}
              </Typography>
            )}
            {description && (
              <Typography variant="body2" color="text.secondary">
                {description}
              </Typography>
            )}
          </Box>
          {action}
        </Stack>
      )}
      {children}
    </Paper>
  );
}
