import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useCallback, useEffect, useState } from 'react';
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notificationApi';
import { getErrorMessage } from '../api/httpClient';
import type { AppNotification } from '../types/api';

function severityColor(s: string): 'default' | 'warning' | 'error' | 'info' {
  if (s === 'CRITICAL') return 'error';
  if (s === 'WARNING') return 'warning';
  return 'info';
}

export function NotificationsPage() {
  const [rows, setRows] = useState<AppNotification[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [pageData, count] = await Promise.all([
        listNotifications({ unreadOnly, page, size }),
        getUnreadCount(),
      ]);
      setRows(pageData.content);
      setTotal(pageData.totalElements);
      setUnread(count);
    } catch (err) {
      setError(getErrorMessage(err));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [unreadOnly, page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom>
          Notifications
        </Typography>
        <Typography variant="body2" color="text.secondary">
          In-app alerts for workflow submission, decisions, and WF-04 escalations.
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Chip label={`${unread} unread`} color={unread ? 'warning' : 'default'} />
          <Button size="small" variant={unreadOnly ? 'contained' : 'outlined'} onClick={() => { setUnreadOnly((v) => !v); setPage(0); }}>
            {unreadOnly ? 'Showing unread' : 'Show unread only'}
          </Button>
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void load()}>
            Refresh
          </Button>
          <Button
            size="small"
            startIcon={<DoneAllIcon />}
            onClick={() => {
              void (async () => {
                try {
                  await markAllNotificationsRead();
                  await load();
                } catch (err) {
                  setError(getErrorMessage(err));
                }
              })();
            }}
          >
            Mark all read
          </Button>
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Paper>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>When</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Title</TableCell>
              <TableCell>Severity</TableCell>
              <TableCell align="right">Action</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                  <CircularProgress size={28} />
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              rows.map((n) => (
                <TableRow key={n.notificationId} hover selected={!n.readFlag}>
                  <TableCell>{n.createdAt ? new Date(n.createdAt).toLocaleString() : '—'}</TableCell>
                  <TableCell>
                    <Chip size="small" label={n.notificationType} variant="outlined" />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: n.readFlag ? 400 : 700 }}>
                      {n.title}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      {n.body}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={n.severity} color={severityColor(n.severity)} />
                  </TableCell>
                  <TableCell align="right">
                    {!n.readFlag && (
                      <Button
                        size="small"
                        onClick={() => {
                          void (async () => {
                            try {
                              await markNotificationRead(n.notificationId);
                              await load();
                            } catch (err) {
                              setError(getErrorMessage(err));
                            }
                          })();
                        }}
                      >
                        Mark read
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            {!loading && !rows.length && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography color="text.secondary">No notifications.</Typography>
                </TableCell>
              </TableRow>
            )}
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
      </Paper>
    </Stack>
  );
}
