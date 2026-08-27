import { Alert, AlertTitle } from '@mui/material';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';

/**
 * V224 — Persistent banner when platform support access is active (READ_ONLY).
 * Shown when backend returns X-Support-Read-Only: true on API responses.
 */
export function SupportSessionBanner({ visible }: { visible: boolean }) {
  if (!visible) return null;
  return (
    <Alert
      severity="warning"
      icon={<VisibilityOutlinedIcon fontSize="inherit" />}
      sx={{
        borderRadius: 0,
        py: 0.75,
        '& .MuiAlert-message': { width: '100%' },
      }}
    >
      <AlertTitle sx={{ mb: 0, fontSize: '0.875rem' }}>
        Support access active — READ ONLY
      </AlertTitle>
      You are viewing this customer under a time-limited, audited support session. Write
      operations are blocked. Revoke the session when finished.
    </Alert>
  );
}
