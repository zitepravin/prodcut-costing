import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useNavigate } from 'react-router-dom';
import * as api from '../../api/supportTicketApi';
import { getErrorMessage } from '../../api/httpClient';
import { useAuth } from '../../auth/useAuth';

const CATEGORIES: api.TicketCategory[] = [
  'TECHNICAL_ISSUE',
  'LOGIN_ACCESS',
  'CONFIGURATION',
  'COSTING',
  'BOM',
  'RFQ',
  'SUPPLIER',
  'DATA_ISSUE',
  'OTHER',
];
const PRIORITIES: api.TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

function priorityColor(p: string): 'default' | 'warning' | 'error' | 'info' {
  if (p === 'CRITICAL' || p === 'HIGH') return 'error';
  if (p === 'MEDIUM') return 'warning';
  return 'default';
}

export function MySupportTicketsPage() {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const canCreate = hasPermission('SUPPORT_TICKET_CREATE');
  const canRead = hasPermission('SUPPORT_TICKET_READ');
  const [rows, setRows] = useState<api.TicketSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState<api.TicketCategory>('TECHNICAL_ISSUE');
  const [priority, setPriority] = useState<api.TicketPriority>('MEDIUM');
  const [description, setDescription] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listMyTickets({ page, size });
      setRows(data.content ?? []);
      setTotal(data.totalElements ?? 0);
    } catch (e) {
      setError(getErrorMessage(e));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCreate() {
    if (!subject.trim() || !description.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const created = await api.createTicket({
        subject: subject.trim(),
        category,
        priority,
        description: description.trim(),
      });
      setOpen(false);
      setSubject('');
      setDescription('');
      navigate(`/support/my-tickets/${created.ticketId}`);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Box>
          <Typography variant="h5">My Support Tickets</Typography>
          <Typography variant="body2" color="text.secondary">
            Create and track support requests for your organization.
          </Typography>
        </Box>
        {canCreate && (
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Ticket
        </Button>
        )}
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      <Paper>
        {loading ? (
          <Box p={4} display="flex" justifyContent="center">
            <CircularProgress size={28} />
          </Box>
        ) : rows.length === 0 ? (
          <Box p={4} textAlign="center">
            <Typography gutterBottom>No support tickets yet.</Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Need help with the application?
            </Typography>
            {canCreate && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
              Create Support Ticket
            </Button>
            )}
          </Box>
        ) : (
          <>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Ticket</TableCell>
                  <TableCell>Subject</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Updated</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((r) => (
                  <TableRow
                    key={r.ticketId}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/support/my-tickets/${r.ticketId}`)}
                  >
                    <TableCell>{r.ticketNumber}</TableCell>
                    <TableCell>{r.subject}</TableCell>
                    <TableCell>{r.category.replace(/_/g, ' ')}</TableCell>
                    <TableCell>
                      <Chip size="small" label={r.priority} color={priorityColor(r.priority)} />
                    </TableCell>
                    <TableCell>{r.status.replace(/_/g, ' ')}</TableCell>
                    <TableCell>
                      {r.updatedAt ? new Date(r.updatedAt).toLocaleString() : '—'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={total}
              page={page}
              onPageChange={(_, p) => setPage(p)}
              rowsPerPage={size}
              onRowsPerPageChange={(e) => {
                setSize(parseInt(e.target.value, 10));
                setPage(0);
              }}
              rowsPerPageOptions={[10, 20, 50]}
            />
          </>
        )}
      </Paper>

      <Dialog open={open} onClose={() => !saving && setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New Support Ticket</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Subject"
              required
              fullWidth
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              inputProps={{ maxLength: 200 }}
            />
            <TextField
              select
              label="Category"
              fullWidth
              value={category}
              onChange={(e) => setCategory(e.target.value as api.TicketCategory)}
            >
              {CATEGORIES.map((c) => (
                <MenuItem key={c} value={c}>
                  {c.replace(/_/g, ' ')}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Priority"
              fullWidth
              value={priority}
              onChange={(e) => setPriority(e.target.value as api.TicketPriority)}
            >
              {PRIORITIES.map((p) => (
                <MenuItem key={p} value={p}>
                  {p}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Description"
              required
              fullWidth
              multiline
              minRows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              inputProps={{ maxLength: 4000 }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)} disabled={saving}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={() => void handleCreate()}
            disabled={saving || !subject.trim() || !description.trim()}
          >
            {saving ? 'Submitting…' : 'Create Ticket'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
