import { setHelpingAgentPageContext } from '../components/helpingagent/helpingAgentContext';
import {
  Alert,
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
  CircularProgress,
  Divider,
} from '@mui/material';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined';
import { useCallback, useEffect, useState } from 'react';
import { getBomTree, triggerProgramRollup } from '../api/bomApi';
import { listPrograms } from '../api/programApi';
import { getErrorMessage } from '../api/httpClient';
import type { BomTreeNode, VehicleProgram } from '../types/api';
import { BomTreeView } from '../components/bom/BomTreeView';
import { formatMoney } from '../utils/money';
import { useAuth } from '../auth/useAuth';
import { PERMISSIONS } from '../auth/permissionMatrix';

const PROGRAM_KEY = 'pcs.lastProgramId';

export function BomPage() {
  const { hasPermission } = useAuth();
  const canEditBom = hasPermission(PERMISSIONS.BOM_EDIT);
  const canViewBom = hasPermission(PERMISSIONS.BOM_VIEW);
  const [programId, setProgramId] = useState(() => localStorage.getItem(PROGRAM_KEY) ?? '');
  const [programs, setPrograms] = useState<VehicleProgram[]>([]);
  const [tree, setTree] = useState<BomTreeNode[]>([]);
  const [selected, setSelected] = useState<BomTreeNode | null>(null);
  const [loading, setLoading] = useState(false);
  const [rolling, setRolling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    void listPrograms()
      .then(setPrograms)
      .catch(() => setPrograms([]));
  }, []);

  // Auto-load tree when a program is selected (dropdown or restored localStorage)
  useEffect(() => {
    if (programId.trim()) {
      void load(programId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- load is stable; only react to programId
  }, [programId]);

  const load = useCallback(async (id: string) => {
    if (!id.trim()) {
      setError('Enter a vehicle program id to load the BOM tree.');
      return;
    }
    setLoading(true);
    setError(null);
    setInfo(null);
    try {
      const data = await getBomTree(id.trim());
      setTree(data);
      setSelected(null);
      localStorage.setItem(PROGRAM_KEY, id.trim());
      if (!data.length) {
        setInfo('Tree loaded — no root nodes found for this program yet.');
      }
    } catch (err) {
      setTree([]);
      setSelected(null);
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);


  useEffect(() => {
    setHelpingAgentPageContext({
      currentPage: 'bom',
      programId: programId || undefined,
      bomNodeId: selected?.bomNodeId,
      partNumber: selected?.partNumber,
    });
  }, [programId, selected]);

  async function handleRollup() {
    if (!programId.trim()) return;
    setRolling(true);
    setError(null);
    setInfo(null);
    try {
      await triggerProgramRollup(programId.trim());
      setInfo('Program roll-up completed.');
      await load(programId.trim());
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setRolling(false);
    }
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5" gutterBottom>
          BOM & product structure
        </Typography>
        <Typography variant="body2" color="text.secondary">
          M02 — unlimited-level tree with rolled unit costs. Source:{' '}
          <code>GET /api/v1/bom/&#123;programId&#125;/tree</code>
        </Typography>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
          <FormControl size="small" sx={{ minWidth: 280 }}>
            <InputLabel id="prog-label">Vehicle program</InputLabel>
            <Select
              labelId="prog-label"
              label="Vehicle program"
              value={programId}
              onChange={(e) => setProgramId(e.target.value)}
            >
              {programs.map((p) => (
                <MenuItem key={p.programId} value={p.programId}>
                  {p.programCode}
                  {p.modelName ? ` — ${p.modelName}` : ''} ({p.status})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            label="Or paste program UUID"
            size="small"
            value={programId}
            onChange={(e) => setProgramId(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') void load(programId);
            }}
            sx={{ minWidth: 260 }}
          />
          <Button
            variant="contained"
            startIcon={<AccountTreeOutlinedIcon />}
            onClick={() => void load(programId)}
            disabled={loading || !programId.trim()}
          >
            Load tree
          </Button>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => void load(programId)}
            disabled={loading || !programId.trim()}
          >
            Refresh
          </Button>
          <Button
            variant="outlined"
            startIcon={<CalculateOutlinedIcon />}
            onClick={() => void handleRollup()}
            disabled={!canEditBom || rolling || !programId.trim()}
            disabled={rolling || !programId.trim()}
          >
            {rolling ? 'Rolling up…' : 'Roll up program'}
          </Button>
        </Stack>
      </Paper>

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

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: 'stretch' }}>
        <Paper sx={{ flex: 2, minHeight: 360, p: 1, overflow: 'auto' }}>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : (
            <BomTreeView nodes={tree} selectedId={selected?.bomNodeId} onSelect={(n) => {
              setSelected(n);
              try {
                if (n?.bomNodeId) localStorage.setItem('pcs.lastBomNodeId', n.bomNodeId);
              } catch { /* ignore */ }
            }} />
          )}
        </Paper>
        <Paper sx={{ flex: 1, p: 2, minHeight: 200 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }} gutterBottom>
            Node detail
          </Typography>
          <Divider sx={{ mb: 2 }} />
          {!selected && (
            <Typography variant="body2" color="text.secondary">
              Select a node in the tree to inspect quantity, status, and rolled cost.
            </Typography>
          )}
          {selected && (
            <Stack spacing={1}>
              <Typography variant="body2">
                <strong>Part</strong> {selected.partNumber}
                {selected.revision ? ` · rev ${selected.revision}` : ''}
              </Typography>
              <Typography variant="body2">
                <strong>Node id</strong>{' '}
                <code style={{ fontSize: 12 }}>{selected.bomNodeId}</code>
              </Typography>
              <Typography variant="body2">
                <strong>Qty / UOM</strong> {selected.quantityPer} {selected.uom}
              </Typography>
              <Typography variant="body2">
                <strong>Make/Buy</strong> {selected.makeBuyFlag ?? '—'}
              </Typography>
              <Typography variant="body2">
                <strong>Material</strong> {selected.materialCode ?? '—'}
              </Typography>
              <Typography variant="body2">
                <strong>Status</strong> {selected.status}
              </Typography>
              <Typography variant="body2" sx={{ fontFamily: 'ui-monospace, monospace' }}>
                <strong>Rolled cost</strong> {formatMoney(selected.rolledUpCost)}
              </Typography>
              {selected.overrideFlag && (
                <Alert severity="warning">Manual cost override is active on this node.</Alert>
              )}
              <Typography variant="caption" color="text.secondary">
                Children: {selected.children?.length ?? 0}
              </Typography>
            </Stack>
          )}
        </Paper>
      </Stack>
    </Stack>
  );
}
