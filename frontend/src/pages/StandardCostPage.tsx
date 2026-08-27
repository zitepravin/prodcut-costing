import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import VerifiedOutlinedIcon from '@mui/icons-material/VerifiedOutlined';
import {
  createFromShouldCost,
  freezeStandardCost,
  listStandardCosts,
  markForRelease,
  releaseStandardCost,
  standardCostHistory,
} from '../api/standardCostApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function StandardCostPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [buildUpId, setBuildUpId] = useState('');
  const [rows, setRows] = useState<Array<Record<string, unknown>>>([]);
  const [current, setCurrent] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = async () => {
    if (!bomNodeId.trim()) return;
    setRows(await listStandardCosts(bomNodeId.trim()));
    const h = await standardCostHistory(bomNodeId.trim());
    setCurrent((h.currentReleased as Record<string, unknown>) || null);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <VerifiedOutlinedIcon color="primary" />
        <Typography variant="h5">Standard cost lifecycle (C1)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        DRAFT → MARKED_FOR_RELEASE → RELEASED → FROZEN → SUPERSEDED. Separate object from should-cost draft.
        Releasing a new version supersedes the previous RELEASED record (never deletes history).
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="BOM node ID" value={bomNodeId} onChange={(e) => setBomNodeId(e.target.value)} required sx={{ minWidth: 260 }} />
            <Button
              variant="outlined"
              onClick={() => {
                void reload().catch((e) => setError(getErrorMessage(e)));
              }}
            >
              Load history
            </Button>
            <TextField
              size="small"
              label="Should-cost build-up ID"
              value={buildUpId}
              onChange={(e) => setBuildUpId(e.target.value)}
              required
              sx={{ minWidth: 280 }}
            />
            <Button
              variant="contained"
              disabled={!buildUpId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createFromShouldCost({ shouldCostBuildUpId: buildUpId.trim() });
                    if (bomNodeId.trim()) await reload();
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create DRAFT from should-cost
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {current && (
        <Alert severity="info">
          Current released/frozen: v{String(current.versionNo)} · {String(current.status)} · unit{' '}
          {String(current.unitCost)} {String(current.currencyCode)}
        </Alert>
      )}

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Ver</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Unit cost</TableCell>
            <TableCell>Source</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={String(r.standardCostId)}>
              <TableCell>{String(r.versionNo)}</TableCell>
              <TableCell>{String(r.status)}</TableCell>
              <TableCell align="right">{String(r.unitCost)}</TableCell>
              <TableCell>
                {String(r.sourceType)} {r.sourceRefId ? `(${String(r.sourceRefId).slice(0, 8)}…)` : ''}
              </TableCell>
              <TableCell>
                <Button
                  size="small"
                  onClick={() => {
                    void markForRelease(String(r.standardCostId))
                      .then(() => reload())
                      .catch((e) => setError(getErrorMessage(e)));
                  }}
                >
                  Mark
                </Button>
                <Button
                  size="small"
                  onClick={() => {
                    void releaseStandardCost(String(r.standardCostId))
                      .then(() => reload())
                      .catch((e) => setError(getErrorMessage(e)));
                  }}
                >
                  Release
                </Button>
                <Button
                  size="small"
                  onClick={() => {
                    void freezeStandardCost(String(r.standardCostId))
                      .then(() => reload())
                      .catch((e) => setError(getErrorMessage(e)));
                  }}
                >
                  Freeze
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  );
}
