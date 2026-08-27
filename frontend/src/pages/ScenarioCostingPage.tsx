import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
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
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import {
  addAdditiveLine,
  approveScenario,
  branchScenario,
  createScenario,
  getScenario,
  listScenarios,
} from '../api/scenarioApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

const SCENARIO_TYPES = [
  'BASE', 'SUPPLIER_A', 'SUPPLIER_B', 'MAKE', 'BUY', 'MIXED',
  'ENGINEERING_CHANGE', 'VOLUME_CHANGE', 'PLANT_CHANGE', 'MATERIAL_CHANGE', 'CURRENCY_CHANGE', 'CUSTOM',
];

const ADDITIVE_TYPES = [
  'PROTOTYPE', 'LAUNCH_PREMIUM', 'SPECIAL_TOOLING', 'ONE_TIME_ENGINEERING',
  'SPECIAL_FREIGHT', 'CUSTOMER_SPECIFIC', 'OTHER',
];

export function ScenarioCostingPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [rows, setRows] = useState<Array<Record<string, unknown>>>([]);
  const [error, setError] = useState<string | null>(null);
  const [code, setCode] = useState('BASE');
  const [name, setName] = useState('Base case');
  const [type, setType] = useState('BASE');
  const [selected, setSelected] = useState('');
  const [branchCode, setBranchCode] = useState('');
  const [detail, setDetail] = useState<Record<string, unknown> | null>(null);
  const [addCode, setAddCode] = useState('');
  const [addName, setAddName] = useState('');
  const [addType, setAddType] = useState('PROTOTYPE');
  const [addAmt, setAddAmt] = useState('');
  const [amortVol, setAmortVol] = useState('');

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <AccountTreeOutlinedIcon color="primary" />
        <Typography variant="h5">Scenario &amp; additive costing (B4/B5)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Typed scenarios (Base, Supplier, Make/Buy, ECN, …). APPROVED scenarios are locked — branch to change.
        Additive lines (prototype, launch, special tooling, …) stay separately identifiable.
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
            <TextField size="small" label="BOM node ID" value={bomNodeId} onChange={(e) => setBomNodeId(e.target.value)} required sx={{ minWidth: 280 }} />
            <Button
              variant="outlined"
              onClick={() => {
                void (async () => {
                  try {
                    setRows(await listScenarios(bomNodeId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Load
            </Button>
            <TextField size="small" label="Code" value={code} onChange={(e) => setCode(e.target.value)} required />
            <TextField size="small" label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <TextField select size="small" label="Type" value={type} onChange={(e) => setType(e.target.value)} sx={{ minWidth: 180 }}>
              {SCENARIO_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <Button
              variant="contained"
              disabled={!bomNodeId.trim() || !code.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createScenario({
                      bomNodeId: bomNodeId.trim(),
                      scenarioCode: code.trim(),
                      scenarioName: name.trim(),
                      scenarioType: type,
                    });
                    setRows(await listScenarios(bomNodeId.trim()));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create scenario
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Type</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Additive Σ</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={String(r.scenarioId)}>
              <TableCell>{String(r.scenarioCode)}</TableCell>
              <TableCell>{String(r.scenarioName)}</TableCell>
              <TableCell>{String(r.scenarioType)}</TableCell>
              <TableCell>
                {String(r.status)}
                {r.locked ? ' 🔒' : ''}
              </TableCell>
              <TableCell align="right">{String(r.additiveTotal)}</TableCell>
              <TableCell>
                <Button
                  size="small"
                  onClick={() => {
                    setSelected(String(r.scenarioId));
                    void getScenario(String(r.scenarioId)).then(setDetail).catch((e) => setError(getErrorMessage(e)));
                  }}
                >
                  Open
                </Button>
                <Button
                  size="small"
                  onClick={() => {
                    void (async () => {
                      try {
                        await approveScenario(String(r.scenarioId));
                        setRows(await listScenarios(bomNodeId.trim()));
                      } catch (e) {
                        setError(getErrorMessage(e));
                      }
                    })();
                  }}
                >
                  Approve
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Branch (from selected / approved)
          </Typography>
          <Stack direction="row" spacing={1}>
            <TextField size="small" label="Source scenario ID" value={selected} onChange={(e) => setSelected(e.target.value)} sx={{ minWidth: 280 }} />
            <TextField size="small" label="New code" value={branchCode} onChange={(e) => setBranchCode(e.target.value)} />
            <Button
              variant="contained"
              disabled={!selected || !branchCode.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await branchScenario({
                      sourceScenarioId: selected,
                      newScenarioCode: branchCode.trim(),
                      newScenarioName: branchCode.trim(),
                      scenarioType: 'ENGINEERING_CHANGE',
                    });
                    setRows(await listScenarios(bomNodeId.trim()));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Branch
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            B5 — Additive line
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Scenario ID" value={selected} onChange={(e) => setSelected(e.target.value)} sx={{ minWidth: 260 }} />
            <TextField size="small" label="Line code" value={addCode} onChange={(e) => setAddCode(e.target.value)} />
            <TextField size="small" label="Name" value={addName} onChange={(e) => setAddName(e.target.value)} />
            <TextField select size="small" label="Type" value={addType} onChange={(e) => setAddType(e.target.value)} sx={{ minWidth: 180 }}>
              {ADDITIVE_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="Amount" value={addAmt} onChange={(e) => setAddAmt(e.target.value)} />
            <TextField size="small" label="Amort volume" value={amortVol} onChange={(e) => setAmortVol(e.target.value)} />
            <Button
              variant="contained"
              disabled={!selected || !addCode.trim() || !addAmt}
              onClick={() => {
                void (async () => {
                  try {
                    await addAdditiveLine(selected, {
                      lineCode: addCode.trim(),
                      lineName: addName.trim() || addCode.trim(),
                      additiveType: addType,
                      amount: addAmt,
                      amortized: Boolean(amortVol.trim()),
                      amortizationVolume: amortVol.trim() || undefined,
                    });
                    setDetail(await getScenario(selected));
                    setRows(await listScenarios(bomNodeId.trim()));
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Add line
            </Button>
          </Stack>
          {detail && Array.isArray(detail.additiveLines) && (
            <Table size="small" sx={{ mt: 2 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell align="right">Per unit</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(detail.additiveLines as Array<Record<string, unknown>>).map((l) => (
                  <TableRow key={String(l.lineId)}>
                    <TableCell>{String(l.lineCode)}</TableCell>
                    <TableCell>{String(l.additiveType)}</TableCell>
                    <TableCell align="right">{String(l.amount)}</TableCell>
                    <TableCell align="right">{String(l.amountPerUnit ?? '—')}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
