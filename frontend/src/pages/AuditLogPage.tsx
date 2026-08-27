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
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
} from '@mui/material';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useState } from 'react';
import { searchAuditLog, type AuditLogEntry } from '../api/auditApi';
import { getErrorMessage } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';

function actionColor(action: string): 'success' | 'info' | 'error' | 'warning' | 'default' {
  if (action === 'CREATE') return 'success';
  if (action === 'UPDATE') return 'info';
  if (action === 'TRANSITION') return 'warning';
  if (action === 'VERIFY') return 'info';
  if (action === 'DELETE') return 'error';
  return 'default';
}

function truncateJson(value: string | null, max = 80): string {
  if (!value) return '—';
  const s = value.replace(/\s+/g, ' ');
  return s.length > max ? s.slice(0, max) + '…' : s;
}

export function AuditLogPage() {
  const { hasRole } = useAuth();
  const allowed = hasRole('ADMIN', 'SYSTEM_ADMINISTRATOR');

  const [entityType, setEntityType] = useState('ErpInboundBatch');
  const [entityId, setEntityId] = useState('');
  const [action, setAction] = useState('');
  const [userId, setUserId] = useState('');
  const [rows, setRows] = useState<AuditLogEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);

  const load = useCallback(async (pageArg = page, sizeArg = size) => {
    if (!allowed) return;
    setLoading(true);
    setError(null);
    try {
      const data = await searchAuditLog({
        entityType: entityType.trim() || undefined,
        entityId: entityId.trim() || undefined,
        action: action.trim() || undefined,
        userId: userId.trim() || undefined,
        page: pageArg,
        size: sizeArg,
      });
      setRows(data.content);
      setTotal(data.totalElements);
      setSearched(true);
    } catch (err) {
      setError(getErrorMessage(err));
      setRows([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [allowed, entityType, entityId, action, userId, page, size]);

  if (!allowed) {
    return (
      <Alert severity="warning">
        Audit log access is restricted to ADMIN and SYSTEM_ADMINISTRATOR roles.
      </Alert>
    );
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <HistoryOutlinedIcon /> Audit Log
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Append-only compliance trail (AUD-05). Filter by entity type + id, or by user. Write path is
          server-side only.
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ alignItems: 'flex-start' }}>
          <TextField
            size="small"
            label="Entity type"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
            placeholder="e.g. MaterialMaster"
            sx={{ minWidth: 180 }}
          />
          <TextField
            size="small"
            label="Entity ID"
            value={entityId}
            onChange={(e) => setEntityId(e.target.value)}
            placeholder="UUID (optional with type)"
            sx={{ minWidth: 220 }}
          />
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Action</InputLabel>
            <Select
              label="Action"
              value={action}
              onChange={(e) => setAction(e.target.value)}
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="CREATE">CREATE</MenuItem>
              <MenuItem value="UPDATE">UPDATE</MenuItem>
              <MenuItem value="TRANSITION">TRANSITION</MenuItem>
              <MenuItem value="VERIFY">VERIFY</MenuItem>
              <MenuItem value="DELETE">DELETE</MenuItem>
            </Select>
          </FormControl>
          <TextField
            size="small"
            label="User ID"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="username"
            sx={{ minWidth: 140 }}
          />
          <Button
            variant="contained"
            startIcon={<SearchIcon />}
            disabled={loading || (!entityType.trim() && !userId.trim())}
            onClick={() => {
              setPage(0);
              void load(0, size);
            }}
          >
            Search
          </Button>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            disabled={loading || !searched}
            onClick={() => void load()}
          >
            Refresh
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Provide entity type (optionally + id) or a user id. At least one filter is required by the API.
        </Typography>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      <Paper>
        {loading ? (
          <Box sx={{ p: 4, display: 'flex', justifyContent: 'center' }}>
            <CircularProgress size={32} />
          </Box>
        ) : (
          <>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Timestamp</TableCell>
                  <TableCell>Action</TableCell>
                  <TableCell>Entity</TableCell>
                  <TableCell>User</TableCell>
                  <TableCell>IP</TableCell>
                  <TableCell>New value</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                        {searched ? 'No audit entries match the filter.' : 'Run a search to load audit entries.'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((r) => (
                    <TableRow key={r.auditId} hover>
                      <TableCell sx={{ whiteSpace: 'nowrap' }}>
                        {new Date(r.timestamp).toLocaleString()}
                      </TableCell>
                      <TableCell>
                        <Chip size="small" label={r.action} color={actionColor(r.action)} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {r.entityType}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {r.entityId}
                        </Typography>
                      </TableCell>
                      <TableCell>{r.userId}</TableCell>
                      <TableCell>{r.ipAddress ?? '—'}</TableCell>
                      <TableCell>
                        <Typography variant="caption" component="span" title={r.newValue ?? undefined}>
                          {truncateJson(r.newValue)}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={total}
              page={page}
              onPageChange={(_, p) => {
                setPage(p);
                void load(p, size);
              }}
              rowsPerPage={size}
              onRowsPerPageChange={(e) => {
                const s = parseInt(e.target.value, 10);
                setSize(s);
                setPage(0);
                void load(0, s);
              }}
              rowsPerPageOptions={[10, 25, 50, 100]}
            />
          </>
        )}
      </Paper>
    </Stack>
  );
}
