import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  ListItemText,
  MenuItem,
  OutlinedInput,
  Select,
  type SelectChangeEvent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import * as adminApi from '../../api/adminApi';
import { getErrorMessage } from '../../api/httpClient';

/** Platform Admin — Users (wraps /api/v1/admin/users). SYSTEM_ADMINISTRATOR only. */
export function PlatformUsersPage() {
  const [users, setUsers] = useState<adminApi.AdminUser[]>([]);
  const [roles, setRoles] = useState<adminApi.AdminRole[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [rolesOpen, setRolesOpen] = useState(false);
  const [selected, setSelected] = useState<adminApi.AdminUser | null>(null);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  /** Selected role codes for create dialog (multi-select). */
  const [roleCodes, setRoleCodes] = useState<string[]>(['COSTING_ENGINEER']);
  /** Selected role codes for update-roles dialog (multi-select). */
  const [editRoles, setEditRoles] = useState<string[]>([]);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [u, r] = await Promise.all([adminApi.listAdminUsers(), adminApi.listAdminRoles()]);
      setUsers(u);
      setRoles(r);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function handleCreateRolesChange(event: SelectChangeEvent<string[]>) {
    const value = event.target.value;
    setRoleCodes(typeof value === 'string' ? value.split(',') : value);
  }

  function handleEditRolesChange(event: SelectChangeEvent<string[]>) {
    const value = event.target.value;
    setEditRoles(typeof value === 'string' ? value.split(',') : value);
  }

  async function onCreate() {
    try {
      await adminApi.createAdminUser({
        username: username.trim(),
        email: email.trim(),
        password,
        roleCodes,
      });
      setCreateOpen(false);
      setUsername('');
      setEmail('');
      setPassword('');
      setRoleCodes(['COSTING_ENGINEER']);
      setInfo('User created');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onSaveRoles() {
    if (!selected) return;
    try {
      await adminApi.updateUserRoles(selected.userId, editRoles);
      setRolesOpen(false);
      setInfo('Roles updated');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onDeactivate(u: adminApi.AdminUser) {
    try {
      await adminApi.deactivateUser(u.userId);
      setInfo(`Deactivated ${u.username}`);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onReactivate(u: adminApi.AdminUser) {
    try {
      await adminApi.reactivateUser(u.userId);
      setInfo(`Reactivated ${u.username}`);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }


  return (
    <Stack spacing={2}>
      <Typography variant="h5">Platform Admin — Users</Typography>
      <Typography variant="body2" color="text.secondary">
        Uses existing /api/v1/admin/users. Role assignment does not replace Central Access memberships.
      </Typography>
      {error && (
        <Alert severity="error" action={<Button onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}
      <Stack direction="row" spacing={1}>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Create user
        </Button>
        <Button variant="outlined" onClick={() => void load()}>
          Refresh
        </Button>
      </Stack>
      <Card>
        <CardContent>
          {users.length === 0 ? (
            <Typography color="text.secondary">No users</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Username</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Roles</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map((u) => (
                  <TableRow key={u.userId}>
                    <TableCell>{u.username}</TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>
                      <Chip size="small" label={u.status} />
                    </TableCell>
                    <TableCell>{(u.roleCodes ?? []).join(', ')}</TableCell>
                    <TableCell>
                      <Button
                        size="small"
                        onClick={() => {
                          setSelected(u);
                          setEditRoles([...(u.roleCodes ?? [])]);
                          setRolesOpen(true);
                        }}
                      >
                        Roles
                      </Button>
                      {u.status === 'ACTIVE' ? (
                        <Button size="small" onClick={() => void onDeactivate(u)}>
                          Deactivate
                        </Button>
                      ) : (
                        <Button size="small" onClick={() => void onReactivate(u)}>
                          Reactivate
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create user</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
            <TextField label="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
            <TextField
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <FormControl fullWidth>
              <InputLabel id="create-roles-label">Roles</InputLabel>
              <Select
                labelId="create-roles-label"
                multiple
                value={roleCodes}
                onChange={handleCreateRolesChange}
                input={<OutlinedInput label="Roles" />}
                renderValue={(selected) => selected.join(', ')}
              >
                {roles.map((r) => (
                  <MenuItem key={r.roleId} value={r.roleCode}>
                    <Checkbox checked={roleCodes.includes(r.roleCode)} />
                    <ListItemText
                      primary={r.roleCode}
                      secondary={r.description ?? (r.systemRole ? 'System role' : undefined)}
                    />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void onCreate()} disabled={roleCodes.length === 0}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={rolesOpen} onClose={() => setRolesOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Update roles — {selected?.username}</DialogTitle>
        <DialogContent>
          <FormControl fullWidth sx={{ mt: 1 }}>
            <InputLabel id="edit-roles-label">Roles</InputLabel>
            <Select
              labelId="edit-roles-label"
              multiple
              value={editRoles}
              onChange={handleEditRolesChange}
              input={<OutlinedInput label="Roles" />}
              renderValue={(selected) => selected.join(', ')}
            >
              {roles.map((r) => (
                <MenuItem key={r.roleId} value={r.roleCode}>
                  <Checkbox checked={editRoles.includes(r.roleCode)} />
                  <ListItemText
                    primary={r.roleCode}
                    secondary={r.description ?? (r.systemRole ? 'System role' : undefined)}
                  />
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRolesOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void onSaveRoles()} disabled={editRoles.length === 0}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
