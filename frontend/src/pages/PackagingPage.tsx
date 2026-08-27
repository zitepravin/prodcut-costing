import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import { RequiredFieldLegend } from '../components/ux';
import {
  calculatePackaging,
  createPackagingProfile,
  deactivatePackagingProfile,
  listPackagingProfiles,
  type FreightMode,
  type PackagingCalculation,
  type PackagingProfile,
  type PackagingType,
} from '../api/packagingApi';

const PACKAGING_TYPES: PackagingType[] = [
  'EXPENDABLE_CARTON',
  'RETURNABLE_CONTAINER',
  'BLISTER_TRAY',
  'PALLETIZED',
  'BULK_BIN',
  'PROTECTIVE_WRAP',
  'CUSTOM',
];

const FREIGHT_MODES: FreightMode[] = ['NONE', 'INBOUND', 'OUTBOUND', 'BOTH'];

export function PackagingPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [profiles, setProfiles] = useState<PackagingProfile[]>([]);
  const [lastCalc, setLastCalc] = useState<PackagingCalculation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  const [form, setForm] = useState({
    profileLabel: 'Standard',
    packagingType: 'EXPENDABLE_CARTON' as PackagingType,
    primaryPackCostPerPiece: '0.50',
    secondaryPackCostPerPiece: '0.10',
    returnableContainerCost: '',
    containerLifeCycles: '',
    netWeightKg: '1.0',
    freightMode: 'OUTBOUND' as FreightMode,
    freightRatePerKg: '0.80',
    fixedFreightPerUnit: '0.00',
    dutyPct: '',
  });

  async function loadProfiles() {
    if (!bomNodeId.trim()) {
      setError('Enter a BOM node id');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await listPackagingProfiles(bomNodeId.trim());
      setProfiles(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load profiles');
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate() {
    setError(null);
    try {
      await createPackagingProfile({
        bomNodeId: bomNodeId.trim(),
        profileLabel: form.profileLabel,
        packagingType: form.packagingType,
        primaryPackCostPerPiece: form.primaryPackCostPerPiece,
        secondaryPackCostPerPiece: form.secondaryPackCostPerPiece,
        returnableContainerCost: form.returnableContainerCost || null,
        containerLifeCycles: form.containerLifeCycles || null,
        netWeightKg: form.netWeightKg || null,
        freightMode: form.freightMode,
        freightRatePerKg: form.freightRatePerKg || null,
        fixedFreightPerUnit: form.fixedFreightPerUnit || null,
        dutyPct: form.dutyPct || null,
      });
      setCreateOpen(false);
      await loadProfiles();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create failed');
    }
  }

  async function handleCalculate(profileId: string) {
    setError(null);
    try {
      const calc = await calculatePackaging(profileId);
      setLastCalc(calc);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Calculate failed');
    }
  }

  async function handleDeactivate(profileId: string) {
    setError(null);
    try {
      await deactivatePackagingProfile(profileId);
      await loadProfiles();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Deactivate failed');
    }
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <LocalShippingOutlinedIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          Packaging & Logistics
        </Typography>
      </Box>
      <Typography color="text.secondary">
        M07 profiles and piece-cost calculation (packaging + freight + duty). Results feed
        Should-Cost packaging_freight_cost.
      </Typography>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <TextField
              label="BOM Node ID"
              required
              size="small"
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              sx={{ minWidth: 320 }}
            />
            <Button variant="contained" onClick={loadProfiles} disabled={loading}>
              Load profiles
            </Button>
            <Button
              variant="outlined"
              onClick={() => setCreateOpen(true)}
              disabled={!bomNodeId.trim()}
            >
              New profile
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <RequiredFieldLegend dense />
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Profiles
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Label</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Freight</TableCell>
                <TableCell align="right">Primary pack</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {profiles.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Typography color="text.secondary">No profiles loaded.</Typography>
                  </TableCell>
                </TableRow>
              )}
              {profiles.map((p) => (
                <TableRow key={p.profileId}>
                  <TableCell>{p.profileLabel}</TableCell>
                  <TableCell>{p.packagingType}</TableCell>
                  <TableCell>{p.freightMode}</TableCell>
                  <TableCell align="right">{p.primaryPackCostPerPiece}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={p.active ? 'Active' : 'Inactive'}
                      color={p.active ? 'success' : 'default'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => handleCalculate(p.profileId)} disabled={!p.active}>
                      Calculate
                    </Button>
                    {p.active && (
                      <Button size="small" color="warning" onClick={() => handleDeactivate(p.profileId)}>
                        Deactivate
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {lastCalc && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Last calculation
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Packaging
                </Typography>
                <Typography fontWeight={600}>{lastCalc.packagingCostPerPiece}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Freight
                </Typography>
                <Typography fontWeight={600}>{lastCalc.freightCostPerPiece}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Duty
                </Typography>
                <Typography fontWeight={600}>{lastCalc.dutyCostPerPiece}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total
                </Typography>
                <Typography fontWeight={700} color="primary.main">
                  {lastCalc.totalPackagingFreightCost}
                </Typography>
              </Box>
            </Stack>
            {lastCalc.calculationBasis && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                {lastCalc.calculationBasis}
              </Typography>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New packaging profile</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Label"
              value={form.profileLabel}
              onChange={(e) => setForm({ ...form, profileLabel: e.target.value })}
              fullWidth
            />
            <TextField
              select
              label="Packaging type"
              value={form.packagingType}
              onChange={(e) => setForm({ ...form, packagingType: e.target.value as PackagingType })}
              fullWidth
            >
              {PACKAGING_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Primary pack cost / piece"
              value={form.primaryPackCostPerPiece}
              onChange={(e) => setForm({ ...form, primaryPackCostPerPiece: e.target.value })}
              fullWidth
            />
            <TextField
              label="Secondary pack cost / piece"
              value={form.secondaryPackCostPerPiece}
              onChange={(e) => setForm({ ...form, secondaryPackCostPerPiece: e.target.value })}
              fullWidth
            />
            <TextField
              label="Returnable container cost"
              value={form.returnableContainerCost}
              onChange={(e) => setForm({ ...form, returnableContainerCost: e.target.value })}
              fullWidth
            />
            <TextField
              label="Container life cycles"
              value={form.containerLifeCycles}
              onChange={(e) => setForm({ ...form, containerLifeCycles: e.target.value })}
              fullWidth
            />
            <TextField
              select
              label="Freight mode"
              value={form.freightMode}
              onChange={(e) => setForm({ ...form, freightMode: e.target.value as FreightMode })}
              fullWidth
            >
              {FREIGHT_MODES.map((m) => (
                <MenuItem key={m} value={m}>
                  {m}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Net weight (kg)"
              value={form.netWeightKg}
              onChange={(e) => setForm({ ...form, netWeightKg: e.target.value })}
              fullWidth
            />
            <TextField
              label="Freight rate / kg"
              value={form.freightRatePerKg}
              onChange={(e) => setForm({ ...form, freightRatePerKg: e.target.value })}
              fullWidth
            />
            <TextField
              label="Fixed freight / unit"
              value={form.fixedFreightPerUnit}
              onChange={(e) => setForm({ ...form, fixedFreightPerUnit: e.target.value })}
              fullWidth
            />
            <TextField
              label="Duty % (fraction, e.g. 0.05)"
              value={form.dutyPct}
              onChange={(e) => setForm({ ...form, dutyPct: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
