import { useCallback, useEffect, useState } from 'react';
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
import PrecisionManufacturingOutlinedIcon from '@mui/icons-material/PrecisionManufacturingOutlined';
import {
  addCapacityLoad,
  calculateActivity,
  createActivity,
  createCapacityCenter,
  getUtilization,
  listActivities,
  listCapacityCenters,
} from '../api/mfgIntelApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function MfgIntelligencePage() {
  const [error, setError] = useState<string | null>(null);
  const [activities, setActivities] = useState<Array<Record<string, unknown>>>([]);
  const [centers, setCenters] = useState<Array<Record<string, unknown>>>([]);
  const [actCode, setActCode] = useState('');
  const [actName, setActName] = useState('');
  const [actRate, setActRate] = useState('');
  const [qty, setQty] = useState('1');
  const [actCost, setActCost] = useState<Record<string, unknown> | null>(null);
  const [centerCode, setCenterCode] = useState('');
  const [centerName, setCenterName] = useState('');
  const [availHours, setAvailHours] = useState('160');
  const [period, setPeriod] = useState('2026-08');
  const [selectedCenter, setSelectedCenter] = useState('');
  const [loadHours, setLoadHours] = useState('40');
  const [util, setUtil] = useState<Record<string, unknown> | null>(null);

  const reload = useCallback(async () => {
    try {
      setActivities(await listActivities());
      setCenters(await listCapacityCenters());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <PrecisionManufacturingOutlinedIcon color="primary" />
        <Typography variant="h5">Manufacturing intelligence (D1/D2)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        D1 activity types (qty × rate) · D2 capacity centers with utilization / residual / overload status.
        Rates and available hours are never invented.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            D1 — Activity types
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Code" value={actCode} onChange={(e) => setActCode(e.target.value)} required />
            <TextField size="small" label="Name" value={actName} onChange={(e) => setActName(e.target.value)} required />
            <TextField size="small" label="Default rate" value={actRate} onChange={(e) => setActRate(e.target.value)} />
            <Button
              variant="contained"
              disabled={!actCode.trim() || !actName.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createActivity({
                      activityCode: actCode.trim(),
                      activityName: actName.trim(),
                      defaultRate: actRate.trim() || undefined,
                      category: 'MACHINE',
                    });
                    await reload();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create activity
            </Button>
            <TextField size="small" label="Qty" value={qty} onChange={(e) => setQty(e.target.value)} />
            <Button
              variant="outlined"
              disabled={!actCode.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setActCost(await calculateActivity({ activityCode: actCode.trim(), quantity: qty }));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Calculate cost
            </Button>
          </Stack>
          {actCost && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              {String(actCost.activityCode)}: {String(actCost.activityCost)} {String(actCost.currencyCode)} (
              {String(actCost.calculationBasis)})
            </Typography>
          )}
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Category</TableCell>
                <TableCell align="right">Rate</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {activities.map((a) => (
                <TableRow key={String(a.activityTypeId)}>
                  <TableCell>{String(a.activityCode)}</TableCell>
                  <TableCell>{String(a.activityName)}</TableCell>
                  <TableCell>{String(a.category)}</TableCell>
                  <TableCell align="right">{String(a.defaultRate ?? '—')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            D2 — Capacity / utilization
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Center code" value={centerCode} onChange={(e) => setCenterCode(e.target.value)} required />
            <TextField size="small" label="Name" value={centerName} onChange={(e) => setCenterName(e.target.value)} required />
            <TextField size="small" label="Available hours" value={availHours} onChange={(e) => setAvailHours(e.target.value)} />
            <TextField size="small" label="Period" value={period} onChange={(e) => setPeriod(e.target.value)} />
            <Button
              variant="contained"
              disabled={!centerCode.trim() || !centerName.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createCapacityCenter({
                      centerCode: centerCode.trim(),
                      centerName: centerName.trim(),
                      availableHours: availHours,
                      periodCode: period.trim(),
                    });
                    await reload();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create center
            </Button>
          </Stack>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} flexWrap="wrap" useFlexGap>
            <TextField
              size="small"
              label="Center ID"
              value={selectedCenter}
              onChange={(e) => setSelectedCenter(e.target.value)}
              sx={{ minWidth: 280 }}
            />
            <TextField size="small" label="Load hours" value={loadHours} onChange={(e) => setLoadHours(e.target.value)} />
            <Button
              variant="outlined"
              disabled={!selectedCenter}
              onClick={() => {
                void (async () => {
                  try {
                    await addCapacityLoad(selectedCenter, {
                      requiredHours: loadHours,
                      loadLabel: 'Manual load',
                    });
                    setUtil(await getUtilization(selectedCenter));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Add load
            </Button>
            <Button
              variant="contained"
              disabled={!selectedCenter}
              onClick={() => {
                void (async () => {
                  try {
                    setUtil(await getUtilization(selectedCenter));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Utilization
            </Button>
          </Stack>
          {util && (
            <Alert severity={String(util.status) === 'OVERLOADED' ? 'warning' : 'info'} sx={{ mt: 1 }}>
              {String(util.centerCode)} {String(util.periodCode)}: util {String(util.utilizationPct)}% · residual{' '}
              {String(util.residualHours)} · status {String(util.status)}
            </Alert>
          )}
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Period</TableCell>
                <TableCell align="right">Available h</TableCell>
                <TableCell>ID</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {centers.map((c) => (
                <TableRow key={String(c.capacityCenterId)}>
                  <TableCell>{String(c.centerCode)}</TableCell>
                  <TableCell>{String(c.periodCode)}</TableCell>
                  <TableCell align="right">{String(c.availableHours)}</TableCell>
                  <TableCell>
                    <Button size="small" onClick={() => setSelectedCenter(String(c.capacityCenterId))}>
                      Select
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </Stack>
  );
}
