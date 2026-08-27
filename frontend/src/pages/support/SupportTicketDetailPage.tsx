import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate, useParams } from 'react-router-dom';
import * as api from '../../api/supportTicketApi';
import { getErrorMessage } from '../../api/httpClient';
import { useAuth } from '../../auth/useAuth';

export function SupportTicketDetailPage() {
  const { hasPermission } = useAuth();
  const canReply = hasPermission('SUPPORT_TICKET_REPLY');
  const canClose = hasPermission('SUPPORT_TICKET_CLOSE');
  const { ticketId } = useParams<{ ticketId: string }>();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState<api.TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reply, setReply] = useState('');
  const [sending, setSending] = useState(false);

  const load = useCallback(async () => {
    if (!ticketId) return;
    setLoading(true);
    setError(null);
    try {
      setTicket(await api.getTicket(ticketId));
    } catch (e) {
      setError(getErrorMessage(e));
      setTicket(null);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function sendReply() {
    if (!ticketId || !reply.trim()) return;
    setSending(true);
    try {
      await api.replyTicket(ticketId, reply.trim());
      setReply('');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setSending(false);
    }
  }

  async function handleClose() {
    if (!ticketId) return;
    setSending(true);
    try {
      await api.closeTicket(ticketId);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setSending(false);
    }
  }

  if (loading) {
    return (
      <Box p={4} display="flex" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }

  if (!ticket) {
    return (
      <Stack spacing={2}>
        {error && <Alert severity="error">{error}</Alert>}
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/support/my-tickets')}>
          Back to tickets
        </Button>
      </Stack>
    );
  }

  const closed = ticket.status === 'CLOSED';

  return (
    <Stack spacing={2}>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/support/my-tickets')} sx={{ alignSelf: 'flex-start' }}>
        Back
      </Button>
      {error && <Alert severity="error">{error}</Alert>}

      <Paper sx={{ p: 2 }}>
        <Typography variant="overline">{ticket.ticketNumber}</Typography>
        <Typography variant="h5" gutterBottom>
          {ticket.subject}
        </Typography>
        <Stack direction="row" spacing={1} mb={2}>
          <Chip label={ticket.status.replace(/_/g, ' ')} size="small" />
          <Chip label={ticket.priority} size="small" color={ticket.priority === 'HIGH' || ticket.priority === 'CRITICAL' ? 'error' : 'default'} />
          <Chip label={ticket.category.replace(/_/g, ' ')} size="small" variant="outlined" />
        </Stack>
        <Typography variant="body2" color="text.secondary" whiteSpace="pre-wrap">
          {ticket.description}
        </Typography>
        {!closed && canClose && (
          <Box mt={2}>
            <Button size="small" color="warning" onClick={() => void handleClose()} disabled={sending}>
              Close ticket
            </Button>
          </Box>
        )}
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Conversation
        </Typography>
        <Stack spacing={2} divider={<Divider />}>
          {(ticket.messages ?? []).map((m) => (
            <Box key={m.messageId}>
              <Typography variant="caption" color="text.secondary">
                {m.authorType} · {new Date(m.createdAt).toLocaleString()}
              </Typography>
              <Typography variant="body2" whiteSpace="pre-wrap">
                {m.message}
              </Typography>
            </Box>
          ))}
          {(ticket.messages ?? []).length === 0 && (
            <Typography color="text.secondary">No messages yet.</Typography>
          )}
        </Stack>
        {!closed && canReply && (
          <Stack direction="row" spacing={1} mt={2}>
            <TextField
              fullWidth
              size="small"
              placeholder="Write a reply…"
              value={reply}
              onChange={(e) => setReply(e.target.value)}
              multiline
              minRows={2}
            />
            <Button variant="contained" onClick={() => void sendReply()} disabled={sending || !reply.trim()}>
              {sending ? 'Sending…' : 'Send'}
            </Button>
          </Stack>
        )}
      </Paper>
    </Stack>
  );
}
