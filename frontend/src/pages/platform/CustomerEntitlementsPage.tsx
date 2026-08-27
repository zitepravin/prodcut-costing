import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Chip,
} from '@mui/material';
import { ConfigEditableRow } from '../../components/config/ConfigEditableRow';
import * as api from '../../api/entitlementApi';
import { getErrorMessage, httpClient } from '../../api/httpClient';

type Customer = { customerId: string; customerCode: string; customerName: string };

/** SYSTEM_ADMINISTRATOR — platform permission availability per customer. */
export function CustomerEntitlementsPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customerId, setCustomerId] = useState('');
  const [rows, setRows] = useState<api.EntitlementRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    void httpClient
      .get<Customer[]>('/api/v1/platform/customers')
      .then((r) => setCustomers(r.data))
      .catch((e) => setError(getErrorMessage(e)));
  }, []);

  const load = useCallback(async () => {
    if (!customerId) return;
    setError(null);
    try {
      setRows(await api.listPlatformEntitlements(customerId));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, [customerId]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Customer Permission Entitlements</Typography>
      <Typography variant="body2" color="text.secondary">
        Platform layer: ALLOW/DENY permission availability for a customer. Customer Admin cannot exceed DENY.
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {info && (
        <Alert severity="success" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}
      <Stack direction="row" spacing={1} alignItems="center">
        <TextField
          select
          size="small"
          label="Customer"
          value={customerId}
          onChange={(e) => setCustomerId(e.target.value)}
          sx={{ minWidth: 280 }}
        >
          {customers.map((c) => (
            <MenuItem key={c.customerId} value={c.customerId}>
              {c.customerCode} — {c.customerName}
            </MenuItem>
          ))}
        </TextField>
        <Button variant="outlined" onClick={() => void load()} disabled={!customerId}>
          Refresh
        </Button>
      </Stack>
      <Paper sx={{ p: 2 }}>
        {rows.length === 0 && (
          <Typography color="text.secondary">Select a customer to load entitlements.</Typography>
        )}
        {rows.map((r) => (
          <ConfigEditableRow
            key={r.permissionCode}
            label={`${r.moduleCode} → ${r.featureCode} → ${r.functionName}`}
            displayValue={r.platformAllowed ? 'ALLOW' : 'DENY'}
            value={r.platformAllowed ? 'true' : 'false'}
            fieldType="boolean"
            canEdit={Boolean(customerId)}
            onSave={async (next) => {
              const allowed = next === 'true';
              const updated = await api.setPlatformEntitlement(customerId, r.permissionCode, allowed);
              setRows((prev) =>
                prev.map((x) => (x.permissionCode === r.permissionCode ? updated : x)),
              );
              setInfo(`${r.permissionCode} → ${allowed ? 'ALLOW' : 'DENY'}`);
            }}
          />
        ))}
      </Paper>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Module</TableCell>
            <TableCell>Feature</TableCell>
            <TableCell>Function</TableCell>
            <TableCell>Permission</TableCell>
            <TableCell>Platform</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={`t-${r.permissionCode}`}>
              <TableCell>{r.moduleCode}</TableCell>
              <TableCell>{r.featureCode}</TableCell>
              <TableCell>{r.functionName}</TableCell>
              <TableCell>
                <code>{r.permissionCode}</code>
              </TableCell>
              <TableCell>
                <Chip size="small" label={r.platformAllowed ? 'ALLOW' : 'DENY'} color={r.platformAllowed ? 'success' : 'default'} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  );
}
