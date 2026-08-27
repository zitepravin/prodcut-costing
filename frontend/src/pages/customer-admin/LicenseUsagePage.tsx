import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Stack,
  Typography,
} from '@mui/material';
import { fetchLicenseUsage, type LicenseUsageSnapshot } from '../../api/customerAdminApi';

function UsageBar({ label, used, max, over }: { label: string; used: number; max: number | null | undefined; over?: boolean }) {
  const unlimited = max == null || max <= 0;
  const text = unlimited ? `${used} / ∞` : `${used} / ${max}`;
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
          <Typography variant="subtitle2" color="text.secondary">
            {label}
          </Typography>
          {over ? <Chip size="small" color="error" label="OVER LIMIT" /> : null}
        </Stack>
        <Typography variant="h5" sx={{ mt: 1 }}>
          {text}
        </Typography>
      </CardContent>
    </Card>
  );
}

/**
 * Customer Admin — commercial entitlement usage (view only).
 * Cannot edit limits, modules, or features.
 */
export default function LicenseUsagePage() {
  const [snap, setSnap] = useState<LicenseUsageSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await fetchLicenseUsage();
        if (!cancelled) setSnap(data);
      } catch (e: unknown) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load license usage');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  if (!snap) {
    return <Alert severity="info">No usage data available.</Alert>;
  }

  const roleMap = Object.fromEntries((snap.roleUsage || []).map((r) => [r.roleCode, r]));
  const eng = roleMap['COSTING_ENGINEER'];
  const mgr = roleMap['COSTING_MANAGER'];
  const fin = roleMap['FINANCE_CONTROLLER'];

  return (
    <Box p={2}>
      <Typography variant="h5" gutterBottom>
        License &amp; Usage
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        View-only commercial entitlement. Limits are managed by the Platform Owner.
      </Typography>

      {snap.overLimit ? (
        <Alert severity="warning" sx={{ mb: 2 }}>
          This customer is OVER LIMIT. Additional user or plant assignments may be blocked until usage is reduced or the license is upgraded.
        </Alert>
      ) : null}

      <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
        <Chip label={`Status: ${snap.licenseStatus ?? 'Not configured'}`} color={snap.licenseStatus === 'ACTIVE' || snap.licenseStatus === 'REACTIVATED' ? 'success' : 'default'} />
        {snap.planCode ? <Chip label={`Plan: ${snap.planCode}`} variant="outlined" /> : null}
        {snap.validTo ? <Chip label={`Expires: ${snap.validTo}`} variant="outlined" /> : null}
        {snap.graceEndDate ? <Chip label={`Grace until: ${snap.graceEndDate}`} variant="outlined" /> : null}
      </Stack>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={4}>
          <UsageBar label="Total users" used={snap.activeUsers} max={snap.totalUserLimit} over={snap.overLimit} />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <UsageBar
            label="Costing Engineer"
            used={eng?.used ?? 0}
            max={eng?.max}
            over={eng?.overLimit}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <UsageBar
            label="Costing Manager"
            used={mgr?.used ?? 0}
            max={mgr?.max}
            over={mgr?.overLimit}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <UsageBar
            label="Finance Controller"
            used={fin?.used ?? 0}
            max={fin?.max}
            over={fin?.overLimit}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <UsageBar
            label="Plant / Site"
            used={snap.plantSiteUsed ?? 0}
            max={snap.plantSiteLimit}
            over={snap.plantSiteOverLimit}
          />
        </Grid>
      </Grid>
    </Box>
  );
}
