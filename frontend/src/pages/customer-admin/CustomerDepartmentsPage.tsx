import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
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
  Chip,
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

export function CustomerDepartmentsPage() {
  const [rows, setRows] = useState<api.Department[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const load = useCallback(async () => {
    setError(null);
    try {
      setRows(await api.listDepartments());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function create() {
    if (!code.trim() || !name.trim()) {
      setError('Department Code and Name are required.');
      return;
    }
    try {
      await api.createDepartment({
        departmentCode: code.trim(),
        departmentName: name.trim(),
        description: description || undefined,
      });
      setOpen(false);
      setCode('');
      setName('');
      setDescription('');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function deactivate(id: string) {
    try {
      await api.deactivateDepartment(id);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Departments</Typography>
        <Button variant="contained" onClick={() => setOpen(true)}>
          + Add Department
        </Button>
      </Stack>
      {error && <Alert severity="error">{error}</Alert>}
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Description</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((d) => (
            <TableRow key={d.departmentId}>
              <TableCell>{d.departmentCode}</TableCell>
              <TableCell>{d.departmentName}</TableCell>
              <TableCell>{d.description ?? '—'}</TableCell>
              <TableCell>
                <Chip size="small" label={d.status} color={d.status === 'ACTIVE' ? 'success' : 'default'} />
              </TableCell>
              <TableCell>
                {d.status === 'ACTIVE' && (
                  <Button size="small" onClick={() => void deactivate(d.departmentId)}>
                    Deactivate
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Add Department</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label={
                <>
                  Department Code
                  <Req />
                </>
              }
              value={code}
              onChange={(e) => setCode(e.target.value)}
              fullWidth
            />
            <TextField
              label={
                <>
                  Department Name
                  <Req />
                </>
              }
              value={name}
              onChange={(e) => setName(e.target.value)}
              fullWidth
            />
            <TextField
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              fullWidth
              multiline
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void create()}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
