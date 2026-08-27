import { Box, Breadcrumbs, Button, Link, Stack, Typography } from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import type { ReactNode } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';

export interface BreadcrumbItem { label: string; to?: string; }
export interface PageHeaderProps {
  title: string; description?: string; breadcrumbs?: BreadcrumbItem[];
  /** Immediate parent for in-page hierarchy (not Dashboard by default) */
  backTo?: { label: string; to: string };
  primaryAction?: { label: string; onClick: () => void; startIcon?: ReactNode; disabled?: boolean };
  secondaryActions?: ReactNode; titleAdornment?: ReactNode;
}
export function PageHeader({ title, description, breadcrumbs, backTo, primaryAction, secondaryActions, titleAdornment }: PageHeaderProps) {
  const navigate = useNavigate();
  return (
    <Box sx={{ mb: 2 }}>
      {backTo && (
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(backTo.to)}
          variant="text"
          size="small"
          sx={{ mb: 1, ml: -1 }}
          aria-label={`Back to ${backTo.label}`}
        >
          Back to {backTo.label}
        </Button>
      )}
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumbs separator={<NavigateNextIcon fontSize="small" />} aria-label="breadcrumb" sx={{ mb: 1 }}>
          {breadcrumbs.map((b, i) => b.to
            ? <Link key={i} component={RouterLink} to={b.to} underline="hover" color="inherit" variant="body2">{b.label}</Link>
            : <Typography key={i} color="text.primary" variant="body2">{b.label}</Typography>)}
        </Breadcrumbs>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ xs: 'flex-start', sm: 'center' }} justifyContent="space-between">
        <Box sx={{ minWidth: 0 }}>
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
            <Typography variant="h5" component="h1">{title}</Typography>
            {titleAdornment}
          </Stack>
          {description && <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5, maxWidth: 720 }}>{description}</Typography>}
        </Box>
        <Stack direction="row" spacing={1} alignItems="center" flexShrink={0}>
          {secondaryActions}
          {primaryAction && (
            <Button variant="contained" color="primary" onClick={primaryAction.onClick} startIcon={primaryAction.startIcon} disabled={primaryAction.disabled}>
              {primaryAction.label}
            </Button>
          )}
        </Stack>
      </Stack>
    </Box>
  );
}
