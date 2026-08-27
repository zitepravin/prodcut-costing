import {
  Card,
  CardActionArea,
  CardContent,
  Grid,
  Stack,
  Typography,
} from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';
import { getModuleForPath, MODULES, type ModuleDef } from '../navigation/routeHierarchy';

/**
 * Intermediate module landing (Master Data, Product Costing, Sourcing, Operations).
 * Lists child screens — does not replace the side nav.
 */
export function ModuleLandingPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const mod: ModuleDef | undefined =
    getModuleForPath(location.pathname) ?? MODULES.find((m) => m.path === location.pathname);

  if (!mod) {
    return (
      <Typography color="text.secondary">Unknown module.</Typography>
    );
  }

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h1">
        {mod.label}
      </Typography>
      <Typography color="text.secondary" variant="body2">
        Select a screen in this module. Use the side navigation or the cards below.
      </Typography>
      <Grid container spacing={2}>
        {mod.children.map((c) => (
          <Grid key={c.path} item xs={12} sm={6} md={4}>
            <Card variant="outlined">
              <CardActionArea onClick={() => navigate(c.path)} aria-label={c.label}>
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={600}>
                    {c.label}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Open {c.label}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Stack>
  );
}
