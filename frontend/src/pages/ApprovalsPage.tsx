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
import RefreshIcon from '@mui/icons-material/Refresh';
import { useCallback, useEffect, useState } from 'react';
import { decideStep, fetchHistory, fetchInbox, submitWorkflow } from '../api/workflowApi';
import { getErrorMessage } from '../api/httpClient';
import type { ApprovalInstance, ApprovalStep } from '../types/api';

export function ApprovalsPage() {
  const [inbox, setInbox] = useState<ApprovalStep[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const [entityType, setEntityType] = useState('ShouldCostBuildUp');
  const [entityId, setEntityId] = useState('');
  const [value, setValue] = useState('');
  const [commodityFamily, setCommodityFamily] = useState('');
  const [plantCode, setPlantCode] = useState('');

  const [historyEntityType, setHistoryEntityType] = useState('ShouldCostBuildUp');
  const [historyEntityId, setHistoryEntityId] = useState('');
  const [history, setHistory] = useState<ApprovalInstance[]>([]);

  const [decisionStep, setDecisionStep] = useState<ApprovalStep | null>(null);
  const [comment, setComment] = useState('');
  const [overrideFlag, setOverrideFlag] = useState(false);
  const [overrideJustification, setOverrideJustification] = useState('');

  const loadInbox = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setInbox(await fetchInbox());
    } catch (err) {
      setError(getErrorMessage(err));
      setInbox([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadInbox();
  }, [loadInbox]);

  async function handleSubmit() {
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const instance = await submitWorkflow({
        entityType: entityType.trim(),
        entityId: entityId.trim(),
        value: value.trim(),
        commodityFamily: commodityFamily.trim() || undefined,
        plantCode: plantCode.trim() || undefined,
      });
      setInfo(`Submitted workflow ${instance.workflowInstanceId} · status ${instance.status}`);
      await loadInbox();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleDecide(decision: 'APPROVED' | 'REJECTED' | 'RETURNED') {
    if (!decisionStep) return;
    setLoading(true);
    setError(null);
    try {
      await decideStep(decisionStep.stepInstanceId, {
        decision,
        comment: comment.trim() || undefined,
        overrideFlag,
        overrideJustification: overrideJustification.trim() || undefined,
      });
      setDecisionStep(null);
      setComment('');
      setOverrideFlag(false);
      setOverrideJustification('');
      setInfo(`Recorded ${decision}.`);
      await loadInbox();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleHistory() {
    setLoading(true);
    setError(null);
    try {
      setHistory(await fetchHistory(historyEntityType.trim(), historyEntityId.trim()));
    } catch (err) {
      setError(getErrorMessage(err));
      setHistory([]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom>
          Approvals
        </Typography>
        <Typography variant="body2" color="text.secondary">
          M14 — inbox (WF-03), submit, decide (WF-05), history (WF-07).
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}

      <Paper sx={{ p: 2 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            My inbox
          </Typography>
          <Button startIcon={<RefreshIcon />} onClick={() => void loadInbox()} disabled={loading}>
            Refresh
          </Button>
        </Stack>
        {loading && !inbox.length ? (
          <CircularProgress size={28} />
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Step</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Due</TableCell>
                <TableCell>Flags</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {inbox.map((s) => (
                <TableRow key={s.stepInstanceId} hover>
                  <TableCell>
                    #{s.sequenceNumber}
                    <Typography variant="caption" display="block" color="text.secondary">
                      {s.stepInstanceId}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{s.entityType ?? '—'}</Typography>
                    <Typography variant="caption" display="block" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                      {s.entityId ?? '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>{s.approverRole}</TableCell>
                  <TableCell>{s.dueAt ? new Date(s.dueAt).toLocaleString() : '—'}</TableCell>
                  <TableCell>
                    {s.escalated && <Chip size="small" color="warning" label="Escalated" />}
                  </TableCell>
                  <TableCell align="right">
                    <Button size="small" variant="contained" onClick={() => setDecisionStep(s)}>
                      Decide
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {!inbox.length && (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Typography variant="body2" color="text.secondary">
                      No pending steps for your roles.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Paper sx={{ p: 2, flex: 1 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
            Submit for approval
          </Typography>
          <Stack spacing={1.5}>
            <TextField label="Entity type" size="small" value={entityType} onChange={(e) => setEntityType(e.target.value)} />
            <TextField label="Entity ID" size="small" value={entityId} onChange={(e) => setEntityId(e.target.value)} helperText="e.g. should-cost buildUpId" />
            <TextField label="Value (routing)" size="small" value={value} onChange={(e) => setValue(e.target.value)} helperText="Monetary value for threshold routing" />
            <TextField label="Commodity family" size="small" value={commodityFamily} onChange={(e) => setCommodityFamily(e.target.value)} />
            <TextField label="Plant code" size="small" value={plantCode} onChange={(e) => setPlantCode(e.target.value)} />
            <Button variant="contained" onClick={() => void handleSubmit()} disabled={loading}>
              Submit
            </Button>
          </Stack>
        </Paper>

        <Paper sx={{ p: 2, flex: 1 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
            History (WF-07)
          </Typography>
          <Stack spacing={1.5} sx={{ mb: 2 }}>
            <TextField label="Entity type" size="small" value={historyEntityType} onChange={(e) => setHistoryEntityType(e.target.value)} />
            <TextField label="Entity ID" size="small" value={historyEntityId} onChange={(e) => setHistoryEntityId(e.target.value)} />
            <Button variant="outlined" onClick={() => void handleHistory()} disabled={loading}>
              Load history
            </Button>
          </Stack>
          {history.map((h) => (
            <Box key={h.workflowInstanceId} sx={{ mb: 2, p: 1.5, bgcolor: 'action.hover', borderRadius: 1 }}>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {h.status} · {h.workflowInstanceId}
              </Typography>
              <Typography variant="caption" display="block">
                Submitted {new Date(h.submittedAt).toLocaleString()} by {h.submittedBy}
              </Typography>
              {h.steps.map((s) => (
                <Typography key={s.stepInstanceId} variant="caption" display="block">
                  Step {s.sequenceNumber} · {s.approverRole} · {s.decision ?? 'PENDING'}
                  {s.comment ? ` — ${s.comment}` : ''}
                </Typography>
              ))}
            </Box>
          ))}
        </Paper>
      </Stack>

      <Dialog open={Boolean(decisionStep)} onClose={() => setDecisionStep(null)} fullWidth maxWidth="sm">
        <DialogTitle>Record decision</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2">
              Step #{decisionStep?.sequenceNumber} · role {decisionStep?.approverRole}
            </Typography>
            <Typography variant="caption" color="text.secondary" display="block">
              {decisionStep?.entityType ?? '—'} · {decisionStep?.entityId ?? '—'}
            </Typography>
            <TextField
              label="Comment"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              multiline
              minRows={2}
              fullWidth
            />
            <TextField
              label="Override justification (if using APPROVAL_OVERRIDE)"
              value={overrideJustification}
              onChange={(e) => {
                setOverrideJustification(e.target.value);
                setOverrideFlag(Boolean(e.target.value.trim()));
              }}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDecisionStep(null)}>Cancel</Button>
          <Button color="error" onClick={() => void handleDecide('REJECTED')} disabled={loading}>
            Reject
          </Button>
          <Button onClick={() => void handleDecide('RETURNED')} disabled={loading}>
            Return
          </Button>
          <Button variant="contained" onClick={() => void handleDecide('APPROVED')} disabled={loading}>
            Approve
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
