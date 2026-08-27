import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import {
  type ChangeTrigger,
  type TimelineEntry,
  createSnapshot,
  fetchTimeline,
  rollbackVersion,
  setBaseline,
} from '../api/versionApi';
import { getErrorMessage } from '../api/httpClient';

const TRIGGERS: ChangeTrigger[] = [
  'ECN',
  'RFQ_ROUND',
  'RATE_UPDATE',
  'VOLUME_REVISION',
  'MANUAL_REVISION',
  'APPROVAL_BASELINE',
];

export function VersionControlPage() {
  const [entityType, setEntityType] = useState('ShouldCostBuildUp');
  const [entityId, setEntityId] = useState('');
  const [trigger, setTrigger] = useState<ChangeTrigger>('MANUAL_REVISION');
  const [reference, setReference] = useState('');
  const [comment, setComment] = useState('');
  const [rows, setRows] = useState<TimelineEntry[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    setError(null);
    setBusy(true);
    try {
      setRows(await fetchTimeline(entityType, entityId.trim()));
    } catch (e) {
      setError(getErrorMessage(e));
      setRows([]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom>
          Version Control
        </Typography>
        <Typography variant="body2" color="text.secondary">
          M15 — immutable cost version timeline, program baseline, and rollback (VER-01..VER-06).
          Automated snapshots are created by the backend on should-cost generate, supplier quote ACCEPTED,
          target-cost activation, and BOM release (ECN). Manual snapshots remain available below.
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: 'flex-start' }}>
          <TextField
            select
            label="Entity type"
            size="small"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            {['ShouldCostBuildUp', 'SupplierQuote', 'TargetCostRecord', 'BomNode'].map((t) => (
              <MenuItem key={t} value={t}>
                {t}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Entity ID"
            size="small"
            value={entityId}
            onChange={(e) => setEntityId(e.target.value)}
            sx={{ minWidth: 280 }}
          />
          <Button variant="contained" disabled={!entityId.trim() || busy} onClick={() => void load()}>
            Load timeline
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Create snapshot
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ flexWrap: 'wrap' }}>
          <TextField
            select
            label="Change trigger"
            size="small"
            value={trigger}
            onChange={(e) => setTrigger(e.target.value as ChangeTrigger)}
            sx={{ minWidth: 180 }}
          >
            {TRIGGERS.map((t) => (
              <MenuItem key={t} value={t}>
                {t}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Reference (ECN / RFQ)"
            size="small"
            value={reference}
            onChange={(e) => setReference(e.target.value)}
          />
          <TextField
            label="Comment"
            size="small"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            sx={{ minWidth: 220 }}
          />
          <Button
            variant="outlined"
            disabled={!entityId.trim() || busy}
            onClick={() => {
              void (async () => {
                setBusy(true);
                setError(null);
                try {
                  await createSnapshot({
                    entityType,
                    entityId: entityId.trim(),
                    changeTrigger: trigger,
                    changeReference: reference || undefined,
                    changeComment: comment || undefined,
                    snapshot: { capturedAt: new Date().toISOString() },
                  });
                  await load();
                } catch (e) {
                  setError(getErrorMessage(e));
                } finally {
                  setBusy(false);
                }
              })();
            }}
          >
            Save version
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
              <TableCell>Ver</TableCell>
              <TableCell>Trigger</TableCell>
              <TableCell>Reference</TableCell>
              <TableCell>Comment</TableCell>
              <TableCell>When</TableCell>
              <TableCell>Baseline</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.versionId} hover>
                <TableCell>v{r.versionNumber}</TableCell>
                <TableCell>
                  <Chip size="small" label={r.changeTrigger} />
                </TableCell>
                <TableCell>{r.changeReference ?? '—'}</TableCell>
                <TableCell>{r.changeComment ?? '—'}</TableCell>
                <TableCell>{r.createdAt ? new Date(r.createdAt).toLocaleString() : '—'}</TableCell>
                <TableCell>{r.programBaseline ? <Chip size="small" color="success" label="Baseline" /> : '—'}</TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    disabled={r.programBaseline || busy}
                    onClick={() => {
                      void (async () => {
                        try {
                          await setBaseline(r.versionId);
                          await load();
                        } catch (e) {
                          setError(getErrorMessage(e));
                        }
                      })();
                    }}
                  >
                    Set baseline
                  </Button>
                  <Button
                    size="small"
                    disabled={busy}
                    onClick={() => {
                      void (async () => {
                        try {
                          await rollbackVersion(r.versionId, `Rollback to v${r.versionNumber}`);
                          await load();
                        } catch (e) {
                          setError(getErrorMessage(e));
                        }
                      })();
                    }}
                  >
                    Rollback
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {!rows.length && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography color="text.secondary">No versions — load an entity or create a snapshot.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Stack>
  );
}
