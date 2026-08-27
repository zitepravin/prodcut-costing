import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import * as api from '../../api/platformAccessApi';
import { getErrorMessage } from '../../api/httpClient';

/**
 * Platform Admin — Customers / tenants + module entitlements.
 * Backend remains authoritative; UI only reflects APIs.
 */
export function PlatformCustomersPage() {
  const [customers, setCustomers] = useState<api.PlatformCustomer[]>([]);
  const [modules, setModules] = useState<api.PlatformModule[]>([]);
  const [selected, setSelected] = useState<api.PlatformCustomer | null>(null);
  const [ents, setEnts] = useState<api.Entitlement[]>([]);
  const [members, setMembers] = useState<api.Membership[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [editName, setEditName] = useState('');
  const [userId, setUserId] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [c, m] = await Promise.all([api.listCustomers(), api.listModules()]);
      setCustomers(c);
      setModules(m);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function selectCustomer(c: api.PlatformCustomer) {
    setSelected(c);
    setEditName(c.customerName);
    setError(null);
    try {
      const [e, u] = await Promise.all([
        api.listCustomerModules(c.customerId),
        api.listCustomerUsers(c.customerId),
      ]);
      setEnts(e);
      setMembers(u);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function onCreate() {
    try {
      const c = await api.createCustomer({ customerCode: code.trim(), customerName: name.trim() });
      setCreateOpen(false);
      setCode('');
      setName('');
      setInfo(`Created customer ${c.customerCode}`);
      await load();
      await selectCustomer(c);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onSaveName() {
    if (!selected || !editName.trim()) return;
    try {
      const updated = await api.updateCustomer(selected.customerId, { customerName: editName.trim() });
      setInfo(`Updated name for ${updated.customerCode}`);
      await load();
      await selectCustomer(updated);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onSetStatus(status: string) {
    if (!selected) return;
    const label = status === 'ACTIVE' ? 'activate' : status === 'INACTIVE' ? 'deactivate' : 'suspend';
    if (!window.confirm(`${label} customer ${selected.customerCode}?`)) return;
    try {
      const updated = await api.setCustomerStatus(selected.customerId, status);
      setInfo(`Customer ${updated.customerCode} is now ${updated.status}`);
      await load();
      await selectCustomer(updated);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function toggleModule(moduleCode: string, enabled: boolean) {
    if (!selected) return;
    if (!enabled && !window.confirm(`Disable module ${moduleCode} for ${selected.customerCode}?`)) {
      return;
    }
    try {
      await api.setEntitlement({
        customerId: selected.customerId,
        moduleCode,
        enabled,
      });
      setInfo(`${moduleCode} ${enabled ? 'enabled' : 'disabled'} for ${selected.customerCode}`);
      await selectCustomer(selected);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onAddMember() {
    if (!selected || !userId.trim()) return;
    try {
      await api.addMembership({ customerId: selected.customerId, userId: userId.trim() });
      setUserId('');
      setInfo('Membership added');
      await selectCustomer(selected);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onDeactivateMember(membershipId: string) {
    if (!selected) return;
    if (!window.confirm('Deactivate this membership?')) return;
    try {
      await api.deactivateMembership(membershipId);
      setInfo('Membership deactivated');
      await selectCustomer(selected);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  const filtered = customers.filter(
    (c) =>
      !search ||
      c.customerCode.toLowerCase().includes(search.toLowerCase()) ||
      c.customerName.toLowerCase().includes(search.toLowerCase()),
  );

  const enabledCodes = new Set(ents.filter((e) => e.enabled).map((e) => e.moduleCode ?? ''));

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Platform Admin — Customers</Typography>
      <Typography variant="body2" color="text.secondary">
        Central Access Management: tenants, module entitlements, memberships. Frontend is not a security boundary.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)} action={<Button onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}
      <Stack direction="row" spacing={1} alignItems="center">
        <TextField size="small" label="Search" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Create customer
        </Button>
        <Button variant="outlined" onClick={() => void load()} disabled={loading}>
          Refresh
        </Button>
      </Stack>
      <Card>
        <CardContent>
          {filtered.length === 0 ? (
            <Typography color="text.secondary">No customers found</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.map((c) => (
                  <TableRow key={c.customerId} selected={selected?.customerId === c.customerId} hover>
                    <TableCell>{c.customerCode}</TableCell>
                    <TableCell>{c.customerName}</TableCell>
                    <TableCell>
                      <Chip size="small" label={c.status} color={c.status === 'ACTIVE' ? 'success' : 'default'} />
                    </TableCell>
                    <TableCell>
                      <Button size="small" onClick={() => void selectCustomer(c)}>
                        Open
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {selected && (
        <Card>
          <CardContent>
            <Typography variant="h6">
              {selected.customerCode} — {selected.customerName}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Id: {selected.customerId}
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
              <TextField
                size="small"
                label="Customer name"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                sx={{ minWidth: 240 }}
              />
              <Button variant="outlined" onClick={() => void onSaveName()} disabled={!editName.trim()}>
                Save name
              </Button>
              <Button size="small" variant="outlined" color="success" onClick={() => void onSetStatus('ACTIVE')}>
                Activate
              </Button>
              <Button size="small" variant="outlined" onClick={() => void onSetStatus('SUSPENDED')}>
                Suspend
              </Button>
              <Button size="small" variant="outlined" color="warning" onClick={() => void onSetStatus('INACTIVE')}>
                Deactivate
              </Button>
            </Stack>
            <Typography variant="subtitle2">Module access</Typography>
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
              Disabling a module requires confirmation. API access to a disabled module returns 403 MODULE_NOT_ENABLED.
            </Typography>
            <Stack spacing={0.5}>
              {modules.map((m) => (
                <FormControlLabel
                  key={m.moduleId}
                  control={
                    <Switch
                      checked={enabledCodes.has(m.moduleCode)}
                      onChange={(_, v) => void toggleModule(m.moduleCode, v)}
                    />
                  }
                  label={`${m.moduleCode} — ${m.moduleName} (${m.status})`}
                />
              ))}
              {modules.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No modules registered
                </Typography>
              )}
            </Stack>
            <Typography variant="subtitle2" sx={{ mt: 2 }}>
              Memberships
            </Typography>
            <Stack direction="row" spacing={1} sx={{ my: 1 }}>
              <TextField
                size="small"
                label="User ID"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                sx={{ minWidth: 280 }}
              />
              <Button variant="outlined" onClick={() => void onAddMember()}>
                Add membership
              </Button>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>User ID</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {members.map((m) => (
                  <TableRow key={m.membershipId}>
                    <TableCell>{m.userId}</TableCell>
                    <TableCell>{m.status}</TableCell>
                    <TableCell>
                      {m.status === 'ACTIVE' && (
                        <Button size="small" color="warning" onClick={() => void onDeactivateMember(m.membershipId)}>
                          Deactivate
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {members.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No memberships
              </Typography>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)}>
        <DialogTitle>Create customer</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1, minWidth: 320 }}>
            <TextField label="Customer code" value={code} onChange={(e) => setCode(e.target.value)} />
            <TextField label="Customer name" value={name} onChange={(e) => setName(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void onCreate()} disabled={!code.trim() || !name.trim()}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
