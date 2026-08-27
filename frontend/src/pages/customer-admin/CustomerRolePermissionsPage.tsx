import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { ConfigEditableRow } from '../../components/config/ConfigEditableRow';
import * as api from '../../api/entitlementApi';
import { getErrorMessage } from '../../api/httpClient';
import { useAuth } from '../../auth/useAuth';

const CUSTOMER_ROLES = [
  'CUSTOMER_ADMIN',
  'COSTING_ENGINEER',
  'COSTING_MANAGER',
  'PURCHASING_BUYER',
  'SOURCING_LEAD',
  'MASTER_DATA_ADMIN',
  'FINANCE_CONTROLLER',
  'PROGRAM_COST_MANAGER',
  'PROCESS_ENGINEER',
  'TOOLING_ENGINEER',
  'VIEWER',
];

/** CUSTOMER_ADMIN — assign available permissions to customer roles. */
export function CustomerRolePermissionsPage() {
  const { activeCustomerId } = useAuth();
  const [roleCode, setRoleCode] = useState('PURCHASING_BUYER');
  const [rows, setRows] = useState<api.RolePermissionRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setRows(await api.listRolePermissions(roleCode));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, [roleCode]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Roles &amp; Permissions</Typography>
      <Typography variant="body2" color="text.secondary">
        Assign permissions to customer roles. Locked rows are denied by the platform and cannot be enabled.
        Active tenant: {activeCustomerId ?? 'none — select tenant in sidebar'}
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {info && (
        <Alert severity="success" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}
      <Stack direction="row" spacing={1}>
        <TextField
          select
          size="small"
          label="Role"
          value={roleCode}
          onChange={(e) => setRoleCode(e.target.value)}
          sx={{ minWidth: 220 }}
        >
          {CUSTOMER_ROLES.map((r) => (
            <MenuItem key={r} value={r}>
              {r}
            </MenuItem>
          ))}
        </TextField>
        <Button variant="outlined" onClick={() => void load()}>
          Refresh
        </Button>
      </Stack>
      <Paper sx={{ p: 2 }}>
        {rows.map((r) =>
          r.locked ? (
            <Stack
              key={r.permissionCode}
              direction="row"
              spacing={1}
              alignItems="center"
              sx={{ py: 1, borderBottom: 1, borderColor: 'divider', minHeight: 48 }}
            >
              <Typography variant="body2" sx={{ flex: '0 0 280px', fontWeight: 500 }}>
                {r.moduleCode} → {r.featureCode} → {r.functionName}
              </Typography>
              <Typography variant="body2" sx={{ flex: 1 }}>
                LOCKED (platform DENY)
              </Typography>
              <LockOutlinedIcon fontSize="small" color="disabled" />
              <Chip size="small" label={r.permissionCode} variant="outlined" />
            </Stack>
          ) : (
            <ConfigEditableRow
              key={r.permissionCode}
              label={`${r.moduleCode} → ${r.featureCode} → ${r.functionName}`}
              displayValue={r.roleAllowed ? 'ALLOW' : 'DENY'}
              value={r.roleAllowed ? 'true' : 'false'}
              fieldType="boolean"
              canEdit
              onSave={async (next) => {
                const allowed = next === 'true';
                try {
                  const updated = await api.setRolePermission(roleCode, r.permissionCode, allowed);
                  setRows((prev) =>
                    prev.map((x) => (x.permissionCode === r.permissionCode ? updated : x)),
                  );
                  setInfo(`${roleCode} · ${r.permissionCode} → ${allowed ? 'ALLOW' : 'DENY'}`);
                  setError(null);
                } catch (e) {
                  throw new Error(getErrorMessage(e));
                }
              }}
            />
          ),
        )}
      </Paper>
    </Stack>
  );
}
