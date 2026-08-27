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

/** Platform Admin — Roles catalog (wraps /api/v1/admin/roles). */
export function PlatformRolesPage() {
  const [roles, setRoles] = useState<adminApi.AdminRole[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');

  const load = useCallback(async () => {
    setError(null);
    try {
      setRoles(await adminApi.listAdminRoles());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function onCreate() {
    try {
      await adminApi.createAdminRole({
        roleCode: code.trim(),
        description: description.trim() || undefined,
      });
      setOpen(false);
      setCode('');
      setDescription('');
      setInfo('Role created');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Platform Admin — Roles</Typography>
      <Typography variant="body2" color="text.secondary">
        Role codes used in JWT authorities. Permission mapping is enforced by backend @PreAuthorize / services.
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
        <Button variant="contained" onClick={() => setOpen(true)}>
          Create role
        </Button>
        <Button variant="outlined" onClick={() => void load()}>
          Refresh
        </Button>
      </Stack>
      <Card>
        <CardContent>
          {roles.length === 0 ? (
            <Typography color="text.secondary">No roles</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>System</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {roles.map((r) => (
                  <TableRow key={r.roleId}>
                    <TableCell>{r.roleCode}</TableCell>
                    <TableCell>{r.description ?? '—'}</TableCell>
                    <TableCell>
                      <Chip size="small" label={r.systemRole ? 'SYSTEM' : 'CUSTOM'} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)}>
        <DialogTitle>Create role</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1, minWidth: 320 }}>
            <TextField label="Role code" value={code} onChange={(e) => setCode(e.target.value)} />
            <TextField
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void onCreate()} disabled={!code.trim()}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
