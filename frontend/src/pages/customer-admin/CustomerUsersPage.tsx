import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import * as api from '../../api/customerAdminApi';
import { getErrorMessage } from '../../api/httpClient';

function Req() {
  return (
    <Typography component="span" color="error" sx={{ ml: 0.5 }}>
      *
    </Typography>
  );
}

export function CustomerUsersPage() {
  const [users, setUsers] = useState<api.CustomerUser[]>([]);
  const [departments, setDepartments] = useState<api.Department[]>([]);
  const [roles, setRoles] = useState<string[]>([]);
  const [rolePerms, setRolePerms] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [filterRole, setFilterRole] = useState('');
  const [filterDept, setFilterDept] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState<api.CustomerUser | null>(null);

  const [employeeName, setEmployeeName] = useState('');
  const [email, setEmail] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [designation, setDesignation] = useState('');
  const [roleCode, setRoleCode] = useState('');

  const load = useCallback(async () => {
    setError(null);
    try {
      const [u, d, r] = await Promise.all([
        api.listCustomerUsers(),
        api.listDepartments(),
        api.listAssignableRoles(),
      ]);
      setUsers(u);
      setDepartments(d.filter((x) => x.status === 'ACTIVE'));
      setRoles(r);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!roleCode) {
      setRolePerms([]);
      return;
    }
    void api.rolePermissions(roleCode).then(setRolePerms).catch(() => setRolePerms([]));
  }, [roleCode]);

  const filtered = useMemo(() => {
    return users.filter((u) => {
      if (search) {
        const q = search.toLowerCase();
        if (
          !(u.employeeName ?? '').toLowerCase().includes(q) &&
          !u.email.toLowerCase().includes(q) &&
          !(u.employeeId ?? '').toLowerCase().includes(q)
        ) {
          return false;
        }
      }
      if (filterRole && !u.roleCodes.includes(filterRole)) return false;
      if (filterDept && u.departmentId !== filterDept) return false;
      if (filterStatus && u.status.toLowerCase() !== filterStatus.toLowerCase()) return false;
      return true;
    });
  }, [users, search, filterRole, filterDept, filterStatus]);

  function openCreate() {
    setEdit(null);
    setEmployeeName('');
    setEmail('');
    setEmployeeId('');
    setDepartmentId('');
    setDesignation('');
    setRoleCode(roles[0] ?? 'COSTING_ENGINEER');
    setOpen(true);
  }

  function openEdit(u: api.CustomerUser) {
    setEdit(u);
    setEmployeeName(u.employeeName ?? '');
    setEmail(u.email);
    setEmployeeId(u.employeeId ?? '');
    setDepartmentId(u.departmentId ?? '');
    setDesignation(u.designation ?? '');
    setRoleCode(u.roleCodes[0] ?? '');
    setOpen(true);
  }

  async function save() {
    setError(null);
    setInfo(null);
    try {
      if (edit) {
        await api.updateCustomerUser(edit.userId, {
          employeeName,
          employeeId: employeeId || undefined,
          departmentId: departmentId || undefined,
          designation: designation || undefined,
          roleCodes: roleCode ? [roleCode] : undefined,
        });
        setInfo('User updated');
      } else {
        if (!employeeName.trim() || !email.trim() || !roleCode) {
          setError('Employee Name, Official Email and Role are required.');
          return;
        }
        const res = await api.createCustomerUser({
          employeeName: employeeName.trim(),
          email: email.trim(),
          employeeId: employeeId || undefined,
          departmentId: departmentId || undefined,
          designation: designation || undefined,
          roleCodes: [roleCode],
        });
        setInfo(
          res.invitationDeliveryNote
            ? `User created. ${res.invitationDeliveryNote}`
            : 'User created and invitation issued.',
        );
      }
      setOpen(false);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function toggleActive(u: api.CustomerUser) {
    setError(null);
    try {
      if (u.status === 'Active') {
        await api.deactivateCustomerUser(u.userId);
      } else if (u.status === 'Inactive') {
        await api.activateCustomerUser(u.userId);
      }
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Users</Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {info && <Alert severity="info">{info}</Alert>}
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} alignItems="center">
        <TextField
          size="small"
          label="Search employee/email"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ minWidth: 220 }}
        />
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Role</InputLabel>
          <Select label="Role" value={filterRole} onChange={(e) => setFilterRole(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            {roles.map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Department</InputLabel>
          <Select label="Department" value={filterDept} onChange={(e) => setFilterDept(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            {departments.map((d) => (
              <MenuItem key={d.departmentId} value={d.departmentId}>
                {d.departmentName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>Status</InputLabel>
          <Select label="Status" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="Active">Active</MenuItem>
            <MenuItem value="Inactive">Inactive</MenuItem>
            <MenuItem value="Invited">Invited</MenuItem>
          </Select>
        </FormControl>
        <Button variant="contained" onClick={openCreate}>
          + Add User
        </Button>
      </Stack>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Employee</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>Department</TableCell>
            <TableCell>Role</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {filtered.map((u) => (
            <TableRow key={u.userId}>
              <TableCell>{u.employeeName ?? u.username}</TableCell>
              <TableCell>{u.email}</TableCell>
              <TableCell>{u.departmentName ?? '—'}</TableCell>
              <TableCell>{u.roleCodes.join(', ')}</TableCell>
              <TableCell>
                <Chip size="small" label={u.status} color={u.status === 'Active' ? 'success' : 'default'} />
              </TableCell>
              <TableCell>
                <Button size="small" onClick={() => openEdit(u)}>
                  Edit
                </Button>
                {(u.status === 'Active' || u.status === 'Inactive') && (
                  <Button size="small" onClick={() => void toggleActive(u)}>
                    {u.status === 'Active' ? 'Deactivate' : 'Activate'}
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
          {filtered.length === 0 && (
            <TableRow>
              <TableCell colSpan={6}>
                <Typography color="text.secondary">No users</Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{edit ? 'Edit Employee' : 'Add Employee'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label={
                <>
                  Employee Name
                  <Req />
                </>
              }
              value={employeeName}
              onChange={(e) => setEmployeeName(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label={
                <>
                  Official Email
                  <Req />
                </>
              }
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              fullWidth
              disabled={!!edit}
            />
            <TextField label="Employee ID" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} fullWidth />
            <FormControl fullWidth>
              <InputLabel>Department</InputLabel>
              <Select label="Department" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
                <MenuItem value="">—</MenuItem>
                {departments.map((d) => (
                  <MenuItem key={d.departmentId} value={d.departmentId}>
                    {d.departmentName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField label="Designation" value={designation} onChange={(e) => setDesignation(e.target.value)} fullWidth />
            <FormControl fullWidth required>
              <InputLabel>
                Role <Req />
              </InputLabel>
              <Select label="Role *" value={roleCode} onChange={(e) => setRoleCode(e.target.value)}>
                {roles.map((r) => (
                  <MenuItem key={r} value={r}>
                    {r}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {rolePerms.length > 0 && (
              <Stack>
                <Typography variant="subtitle2">Permissions for {roleCode}</Typography>
                <Stack direction="row" flexWrap="wrap" gap={0.5}>
                  {rolePerms.map((p) => (
                    <Chip key={p} size="small" label={p} color="success" variant="outlined" />
                  ))}
                </Stack>
              </Stack>
            )}
            {!edit && (
              <Alert severity="info">
                No password is set by the administrator. An invitation is issued to the official email.
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void save()}>
            {edit ? 'Save' : 'Create User'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
