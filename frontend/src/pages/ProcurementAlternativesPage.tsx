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
import CompareArrowsOutlinedIcon from '@mui/icons-material/CompareArrowsOutlined';
import { decideProcurement, evaluateProcurement, listProcurementSets } from '../api/procurementApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function ProcurementAlternativesPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [subCost, setSubCost] = useState('');
  const [extCost, setExtCost] = useState('');
  const [result, setResult] = useState<Record<string, unknown> | null>(null);
  const [history, setHistory] = useState<Array<Record<string, unknown>>>([]);
  const [decision, setDecision] = useState('');
  const [error, setError] = useState<string | null>(null);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <CompareArrowsOutlinedIcon color="primary" />
        <Typography variant="h5">Procurement alternatives (B6)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        First-class MAKE / BUY / SUBCONTRACT / EXTERNAL_PROCESS / MIXED set for a BOM node. Ranked by unit cost.
        Recommendation is a draft only — human decision is mandatory; no automatic award.
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
            <TextField
              size="small"
              label="BOM node ID"
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              required
              sx={{ minWidth: 280 }}
            />
            <TextField size="small" label="Subcontract unit cost" value={subCost} onChange={(e) => setSubCost(e.target.value)} />
            <TextField size="small" label="External process unit cost" value={extCost} onChange={(e) => setExtCost(e.target.value)} />
            <Button
              variant="contained"
              disabled={!bomNodeId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    const r = await evaluateProcurement({
                      bomNodeId: bomNodeId.trim(),
                      subcontractUnitCost: subCost.trim() || undefined,
                      externalProcessUnitCost: extCost.trim() || undefined,
                    });
                    setResult(r);
                    setHistory(await listProcurementSets(bomNodeId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Evaluate
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {result && (
        <Card>
          <CardContent>
            <Typography fontWeight={600}>
              Recommendation: {String(result.recommendationCode ?? '—')}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {String(result.recommendationSummary ?? '')}
            </Typography>
            {Array.isArray(result.lines) && (
              <Table size="small" sx={{ mt: 1 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Type</TableCell>
                    <TableCell align="right">Unit cost</TableCell>
                    <TableCell align="right">Rank</TableCell>
                    <TableCell>Data</TableCell>
                    <TableCell>Notes</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(result.lines as Array<Record<string, unknown>>).map((l) => (
                    <TableRow key={String(l.lineId)}>
                      <TableCell>{String(l.alternativeType)}</TableCell>
                      <TableCell align="right">{String(l.unitCost ?? '—')}</TableCell>
                      <TableCell align="right">{String(l.rankNo ?? '—')}</TableCell>
                      <TableCell>{String(l.dataStatus)}</TableCell>
                      <TableCell>{String(l.notes ?? '')}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
              <TextField
                size="small"
                label="Decision code (MAKE/BUY/…)"
                value={decision}
                onChange={(e) => setDecision(e.target.value)}
              />
              <Button
                variant="outlined"
                disabled={!result.setId || !decision.trim()}
                onClick={() => {
                  void (async () => {
                    try {
                      setResult(
                        await decideProcurement(String(result.setId), {
                          decisionCode: decision.trim(),
                          decisionReason: 'Human selection after alternative review',
                        }),
                      );
                      setHistory(await listProcurementSets(bomNodeId.trim()));
                    } catch (e) {
                      setError(getErrorMessage(e));
                    }
                  })();
                }}
              >
                Record human decision
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}

      {history.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Label</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Recommendation</TableCell>
              <TableCell>Decision</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {history.map((h) => (
              <TableRow key={String(h.setId)}>
                <TableCell>{String(h.label)}</TableCell>
                <TableCell>{String(h.status)}</TableCell>
                <TableCell>{String(h.recommendationCode ?? '—')}</TableCell>
                <TableCell>{String(h.decisionCode ?? '—')}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Stack>
  );
}
