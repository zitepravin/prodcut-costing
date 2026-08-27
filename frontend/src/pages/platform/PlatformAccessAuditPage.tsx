import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
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
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useEffect, useState } from 'react';
import { searchAuditLog, type AuditLogEntry } from '../../api/auditApi';
import { getErrorMessage } from '../../api/httpClient';
import { useAuth } from '../../auth/useAuth';

const CAM_ENTITY_TYPES = [
  '',
  'PlatformCustomer',
  'CustomerModuleEntitlement',
  'CustomerUserMembership',
  'PlatformModule',
  'User',
  'Role',
];

function actionColor(action: string): 'success' | 'info' | 'error' | 'warning' | 'default' {
  if (action === 'CREATE') return 'success';
  if (action === 'UPDATE') return 'info';
  if (action === 'TRANSITION') return 'warning';
  if (action === 'DELETE') return 'error';
  return 'default';
}

function truncateJson(value: string | null, max = 80): string {
  if (!value) return '—';
  const s = value.replace(/\s+/g, ' ');
  return s.length > max ? s.slice(0, max) + '…' : s;
}

/**
 * Platform Admin — Access audit (pre-filtered Central Access entity types).
 * Uses existing /api/v1/audit; no duplicate audit system.
 */
export function PlatformAccessAuditPage() {
  const { hasRole } = useAuth();
  const allowed = hasRole('ADMIN', 'SYSTEM_ADMINISTRATOR');

  const [entityType, setEntityType] = useState('PlatformCustomer');
  const [entityId, setEntityId] = useState('');
  const [action, setAction] = useState('');
  const [rows, setRows] = useState<AuditLogEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (pageArg = page, sizeArg = size) => {
      if (!allowed) return;
      setLoading(true);
      setError(null);
      try {
        const data = await searchAuditLog({
          entityType: entityType.trim() || undefined,
          entityId: entityId.trim() || undefined,
          action: action.trim() || undefined,
          page: pageArg,
          size: sizeArg,
        });
        setRows(data.content);
        setTotal(data.totalElements);
      } catch (err) {
        setError(getErrorMessage(err));
        setRows([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    },
    [allowed, entityType, entityId, action, page, size],
  );

  useEffect(() => {
    if (allowed) void load(0, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [allowed]);

  if (!allowed) {
    return (
      <Alert severity="warning">
        Access audit is restricted to ADMIN and SYSTEM_ADMINISTRATOR roles.
      </Alert>
    );
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <HistoryOutlinedIcon /> Platform Access Audit
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Central Access changes: customers, module entitlements, memberships, users/roles. Source: existing audit log
          API (AUD-05).
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} alignItems="flex-start">
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel>Entity type</InputLabel>
            <Select
              label="Entity type"
              value={entityType}
              onChange={(e) => setEntityType(e.target.value)}
            >
              {CAM_ENTITY_TYPES.map((t) => (
                <MenuItem key={t || 'all'} value={t}>
                  {t || '(all types)'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            size="small"
            label="Entity ID"
            value={entityId}
            onChange={(e) => setEntityId(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel>Action</InputLabel>
            <Select label="Action" value={action} onChange={(e) => setAction(e.target.value)}>
              <MenuItem value="">(any)</MenuItem>
              <MenuItem value="CREATE">CREATE</MenuItem>
              <MenuItem value="UPDATE">UPDATE</MenuItem>
              <MenuItem value="TRANSITION">TRANSITION</MenuItem>
              <MenuItem value="DELETE">DELETE</MenuItem>
            </Select>
          </FormControl>
          <Button
            variant="contained"
            startIcon={<SearchIcon />}
            onClick={() => {
              setPage(0);
              void load(0, size);
            }}
            disabled={loading}
          >
            Search
          </Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
          <CircularProgress size={28} />
        </Box>
      )}

      {!loading && (
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Time</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>User</TableCell>
                <TableCell>Change</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.auditId}>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>
                    {r.timestamp ? new Date(r.timestamp).toLocaleString() : '—'}
                  </TableCell>
                  <TableCell>
                    {r.entityType}
                    <Typography variant="caption" display="block" color="text.secondary">
                      {r.entityId}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={r.action} color={actionColor(r.action)} />
                  </TableCell>
                  <TableCell>{r.userId}</TableCell>
                  <TableCell sx={{ maxWidth: 320 }}>
                    <Typography variant="caption" component="div">
                      {truncateJson(r.oldValue)} → {truncateJson(r.newValue)}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography color="text.secondary">No audit rows for this filter</Typography>
                  </TableCell>
                </TableRow>
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
            rowsPerPageOptions={[10, 25, 50]}
          />
        </Paper>
      )}
    </Stack>
  );
}
