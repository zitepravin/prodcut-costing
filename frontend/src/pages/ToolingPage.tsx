import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import HandymanOutlinedIcon from '@mui/icons-material/HandymanOutlined';
import {
  calculateToolingCost,
  createTooling,
  listToolingAssets,
  retireTooling,
  type ToolingCostResponse,
  type ToolingRecordResponse,
} from '../api/toolingApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';
import { formatMoney } from '../utils/money';

const TOOLING_TYPES = ['PROGRESSIVE_DIE', 'TRANSFER_DIE', 'INJECTION_MOLD', 'CASTING_DIE', 'FORGING_DIE', 'WELDING_FIXTURE', 'CHECKING_FIXTURE'];
const OWNERSHIP = ['CUSTOMER', 'SUPPLIER'];

export function ToolingPage() {
  const [assets, setAssets] = useState<ToolingRecordResponse[]>([]);
  const [plantFilter, setPlantFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [costResult, setCostResult] = useState<ToolingCostResponse | null>(null);

  const [bomNodeId, setBomNodeId] = useState('');
  const [description, setDescription] = useState('');
  const [toolingType, setToolingType] = useState('PROGRESSIVE_DIE');
  const [ownership, setOwnership] = useState('SUPPLIER');
  const [toolCost, setToolCost] = useState('100000');
  const [toolLifeHits, setToolLifeHits] = useState('500000');
  const [cavityCount, setCavityCount] = useState('1');
  const [plantLocation, setPlantLocation] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await listToolingAssets(plantFilter.trim() || undefined);
      setAssets(list);
    } catch (e: unknown) {
      setError(getErrorMessage(e));
      setAssets([]);
    } finally {
      setLoading(false);
    }
  }, [plantFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async () => {
    if (!bomNodeId.trim() || !description.trim() || !toolCost.trim()) {
      setError('BOM node ID, description, and tool cost are required');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await createTooling({
        bomNodeId: bomNodeId.trim(),
        toolingType,
        description: description.trim(),
        toolCost,
        toolLifeHits: toolLifeHits || null,
        cavityCount: cavityCount ? Number(cavityCount) : null,
        ownership,
        plantLocation: plantLocation || null,
      });
      setDescription('');
      await load();
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleCalculate = async (toolingRecordId: string) => {
    setBusy(true);
    setError(null);
    setCostResult(null);
    try {
      const result = await calculateToolingCost(toolingRecordId);
      setCostResult(result);
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleRetire = async (toolingRecordId: string) => {
    setBusy(true);
    setError(null);
    try {
      await retireTooling(toolingRecordId, 'Retired from UI');
      await load();
    } catch (e: unknown) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
        <HandymanOutlinedIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          Tooling, Die &amp; Fixture
        </Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Asset register (TL-07), create investment records (TL-01), amortize per-piece cost (TL-02/04),
        retire tools without hard-delete. Amortization triggers program roll-up when included in piece price.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Create tooling record
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap" useFlexGap>
            <TextField
              label="BOM Node ID"
              size="small"
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              required
              sx={{ minWidth: 280 }}
            />
            <TextField
              label="Description"
              size="small"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              sx={{ minWidth: 220 }}
            />
            <TextField
              select
              label="Type"
              size="small"
              value={toolingType}
              required
              onChange={(e) => setToolingType(e.target.value)}
              sx={{ minWidth: 140 }}
            >
              {TOOLING_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Ownership"
              size="small"
              value={ownership}
              onChange={(e) => setOwnership(e.target.value)}
              sx={{ minWidth: 180 }}
            >
              {OWNERSHIP.map((o) => (
                <MenuItem key={o} value={o}>
                  {o}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Tool cost"
              size="small"
              value={toolCost}
              onChange={(e) => setToolCost(e.target.value)}
              required
            />
            <TextField
              label="Life (hits)"
              size="small"
              value={toolLifeHits}
              onChange={(e) => setToolLifeHits(e.target.value)}
            />
            <TextField
              label="Cavities"
              size="small"
              value={cavityCount}
              onChange={(e) => setCavityCount(e.target.value)}
            />
            <TextField
              label="Plant"
              size="small"
              value={plantLocation}
              onChange={(e) => setPlantLocation(e.target.value)}
            />
            <Button variant="contained" onClick={() => void handleCreate()} disabled={busy}>
              Create
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }} alignItems="center">
            <Typography variant="subtitle1" fontWeight={600} sx={{ flex: 1 }}>
              Asset register
            </Typography>
            <TextField
              label="Plant filter"
              size="small"
              value={plantFilter}
              onChange={(e) => setPlantFilter(e.target.value)}
            />
            <Button variant="outlined" onClick={() => void load()} disabled={loading || busy}>
              Refresh
            </Button>
          </Stack>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : assets.length === 0 ? (
            <Typography color="text.secondary">No tooling assets found.</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Ownership</TableCell>
                  <TableCell align="right">Tool cost</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Plant</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {assets.map((a) => (
                  <TableRow key={a.toolingRecordId}>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: 11 }}>{a.toolingRecordId}</TableCell>
                    <TableCell>{a.description}</TableCell>
                    <TableCell>{a.toolingType}</TableCell>
                    <TableCell>{a.ownership}</TableCell>
                    <TableCell align="right">{formatMoney(a.toolCost)}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={a.assetStatus}
                        color={a.assetStatus === 'ACTIVE' ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>{a.plantLocation ?? '—'}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          variant="contained"
                          disabled={busy || a.assetStatus === 'RETIRED'}
                          onClick={() => void handleCalculate(a.toolingRecordId)}
                        >
                          Amortize
                        </Button>
                        {a.assetStatus !== 'RETIRED' && (
                          <Button
                            size="small"
                            color="warning"
                            disabled={busy}
                            onClick={() => void handleRetire(a.toolingRecordId)}
                          >
                            Retire
                          </Button>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {costResult && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Amortization result
            </Typography>
            <Typography variant="body2">
              Total amortized / piece:{' '}
              <strong>{formatMoney(costResult.totalAmortizedCostPerPiece)}</strong>
            </Typography>
            <Typography variant="body2">
              Base amortized / piece: {formatMoney(costResult.baseAmortizedCostPerPiece)}
            </Typography>
            <Typography variant="body2">
              Refurbishment / piece: {formatMoney(costResult.refurbishmentCostPerPiece)} · Events:{' '}
              {costResult.refurbishmentEventsProjected ?? '—'}
            </Typography>
            <Typography variant="body2">
              Included in piece price: {costResult.includedInPiecePrice ? 'Yes' : 'No'}
            </Typography>
            {costResult.calculationBasis && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                {costResult.calculationBasis}
              </Typography>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
