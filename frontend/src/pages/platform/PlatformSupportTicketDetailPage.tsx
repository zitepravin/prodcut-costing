import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, CircularProgress, Divider, MenuItem, Paper, Stack,
  TextField, Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate, useParams } from 'react-router-dom';
import { httpClient, getErrorMessage } from '../../api/httpClient';

type Detail = {
  ticketId: string; ticketNumber: string; customerId: string; subject: string; description: string;
  category: string; priority: string; status: string; assignedToUserId: string | null;
  messages: { messageId: string; authorType: string; message: string; createdAt: string }[];
  internalNotes: { noteId: string; authorUserId: string; note: string; createdAt: string }[];
};

const STATUSES = ['OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];

export function PlatformSupportTicketDetailPage() {
  const { ticketId } = useParams<{ ticketId: string }>();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState<Detail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [reply, setReply] = useState('');
  const [note, setNote] = useState('');
  const [status, setStatus] = useState('');
  const [assignee, setAssignee] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!ticketId) return;
    setLoading(true);
    try {
      const { data } = await httpClient.get<Detail>(`/api/v1/platform/support/tickets/${ticketId}`);
      setTicket(data);
      setStatus(data.status);
      setAssignee(data.assignedToUserId ?? '');
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => { void load(); }, [load]);

  async function post(path: string, body: unknown) {
    setBusy(true);
    setError(null);
    try {
      await httpClient.post(path, body);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Box p={4} display="flex" justifyContent="center"><CircularProgress /></Box>;
  if (!ticket) return <Alert severity="error">{error ?? 'Not found'}</Alert>;

  return (
    <Stack spacing={2}>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/platform/support/tickets')}>Back</Button>
      {error && <Alert severity="error">{error}</Alert>}
      <Paper sx={{ p: 2 }}>
        <Typography variant="overline">{ticket.ticketNumber} · Customer {ticket.customerId}</Typography>
        <Typography variant="h5">{ticket.subject}</Typography>
        <Stack direction="row" spacing={1} my={1}>
          <Chip label={ticket.status} size="small" />
          <Chip label={ticket.priority} size="small" />
          <Chip label={ticket.category} size="small" variant="outlined" />
        </Stack>
        <Typography variant="body2" whiteSpace="pre-wrap">{ticket.description}</Typography>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>Support actions</Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
          <TextField select size="small" label="Status" value={status} onChange={(e) => setStatus(e.target.value)} sx={{ minWidth: 180 }}>
            {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
          </TextField>
          <Button variant="outlined" disabled={busy} onClick={() => void post(`/api/v1/platform/support/tickets/${ticket.ticketId}/status`, { status })}>
            Update status
          </Button>
          <TextField size="small" label="Assign to userId" value={assignee} onChange={(e) => setAssignee(e.target.value)} />
          <Button variant="outlined" disabled={busy || !assignee.trim()}
            onClick={() => void post(`/api/v1/platform/support/tickets/${ticket.ticketId}/assign`, { assigneeUserId: assignee.trim() })}>
            Assign
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="h6">Conversation</Typography>
        <Stack spacing={1} divider={<Divider />} my={1}>
          {ticket.messages.map((m) => (
            <Box key={m.messageId}>
              <Typography variant="caption">{m.authorType} · {new Date(m.createdAt).toLocaleString()}</Typography>
              <Typography variant="body2" whiteSpace="pre-wrap">{m.message}</Typography>
            </Box>
          ))}
        </Stack>
        <Stack direction="row" spacing={1}>
          <TextField fullWidth size="small" multiline minRows={2} value={reply} onChange={(e) => setReply(e.target.value)} placeholder="Support reply…" />
          <Button variant="contained" disabled={busy || !reply.trim()}
            onClick={() => void post(`/api/v1/platform/support/tickets/${ticket.ticketId}/messages`, { message: reply }).then(() => setReply(''))}>
            Send
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="h6">Internal notes (not visible to customer)</Typography>
        <Stack spacing={1} my={1}>
          {ticket.internalNotes.map((n) => (
            <Box key={n.noteId}>
              <Typography variant="caption">{n.authorUserId} · {new Date(n.createdAt).toLocaleString()}</Typography>
              <Typography variant="body2" whiteSpace="pre-wrap">{n.note}</Typography>
            </Box>
          ))}
        </Stack>
        <Stack direction="row" spacing={1}>
          <TextField fullWidth size="small" multiline minRows={2} value={note} onChange={(e) => setNote(e.target.value)} placeholder="Internal note…" />
          <Button variant="outlined" disabled={busy || !note.trim()}
            onClick={() => void post(`/api/v1/platform/support/tickets/${ticket.ticketId}/internal-notes`, { note }).then(() => setNote(''))}>
            Add note
          </Button>
        </Stack>
      </Paper>
    </Stack>
  );
}
