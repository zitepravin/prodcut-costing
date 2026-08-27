import { Box, Button, Breadcrumbs, Link, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import {
  backButtonLabel,
  resolveRouteNavMeta,
  shouldShowBackButton,
} from './routeHierarchy';

/**
 * Single shell control for breadcrumbs + hierarchical Back.
 * Destination is deterministic from routeHierarchy (deep-link safe).
 * Does not use browser history as primary navigation.
 */
export function BackToParentBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const meta = resolveRouteNavMeta(location.pathname);

  if (meta.isDashboard || location.pathname === '/login') {
    return null;
  }

  const showBack = shouldShowBackButton(meta);

  return (
    <Box sx={{ mb: 2 }}>
      <Breadcrumbs
        separator={<NavigateNextIcon fontSize="small" />}
        aria-label="breadcrumb"
        sx={{ mb: showBack ? 1 : 0 }}
      >
        {meta.breadcrumbs.map((b, i) =>
          b.to ? (
            <Link
              key={`${b.label}-${i}`}
              component={RouterLink}
              to={b.to}
              underline="hover"
              color="inherit"
              variant="body2"
            >
              {b.label}
            </Link>
          ) : (
            <Typography key={`${b.label}-${i}`} color="text.primary" variant="body2">
              {b.label}
            </Typography>
          ),
        )}
      </Breadcrumbs>
      {showBack && meta.parentPath && (
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(meta.parentPath!)}
          variant="outlined"
          size="small"
          aria-label={backButtonLabel(meta)}
        >
          {backButtonLabel(meta)}
        </Button>
      )}
    </Box>
  );
}
