import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Button, Chip, CircularProgress, MenuItem, Paper, Stack,
  Table, TableBody, TableCell, TableHead, TablePagination, TableRow,
  TextField, Typography,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { httpClient, getErrorMessage } from '../../api/httpClient';
import type { PageResponse } from '../../types/api';

type Row = {
  ticketId: string; ticketNumber: string; customerId: string; subject: string;
  category: string; priority: string; status: string; assignedToUserId: string | null;
  createdAt: string; updatedAt: string | null;
};

export function PlatformSupportTicketsPage() {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await httpClient.get<PageResponse<Row>>('/api/v1/platform/support/tickets', {
        params: { page, size, q: q || undefined },
      });
      setRows(data.content ?? []);
      setTotal(data.totalElements ?? 0);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [page, size, q]);

  useEffect(() => { void load(); }, [load]);

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Platform Support Tickets</Typography>
      <Typography variant="body2" color="text.secondary">
        Manage customer support tickets across tenants.
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      <Stack direction="row" spacing={1}>
        <TextField size="small" label="Search" value={q} onChange={(e) => setQ(e.target.value)} />
        <Button variant="outlined" onClick={() => void load()}>Search</Button>
      </Stack>
      <Paper>
        {loading ? (
          <Stack p={4} alignItems="center"><CircularProgress size={28} /></Stack>
        ) : (
          <>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Ticket</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Subject</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Updated</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((r) => (
                  <TableRow key={r.ticketId} hover sx={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/platform/support/tickets/${r.ticketId}`)}>
                    <TableCell>{r.ticketNumber}</TableCell>
                    <TableCell>{r.customerId.slice(0, 8)}…</TableCell>
                    <TableCell>{r.subject}</TableCell>
                    <TableCell><Chip size="small" label={r.priority} /></TableCell>
                    <TableCell>{r.status}</TableCell>
                    <TableCell>{r.updatedAt ? new Date(r.updatedAt).toLocaleString() : '—'}</TableCell>
                  </TableRow>
                ))}
                {rows.length === 0 && (
                  <TableRow><TableCell colSpan={6}>No tickets.</TableCell></TableRow>
                )}
              </TableBody>
            </Table>
            <TablePagination component="div" count={total} page={page}
              onPageChange={(_, p) => setPage(p)} rowsPerPage={size}
              onRowsPerPageChange={(e) => { setSize(parseInt(e.target.value, 10)); setPage(0); }} />
          </>
        )}
      </Paper>
    </Stack>
  );
}
