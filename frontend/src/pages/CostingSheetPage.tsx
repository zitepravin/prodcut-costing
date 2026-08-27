import { useCallback, useEffect, useState } from 'react';
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
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import {
  activateCostingSheet,
  addCostingSheetRule,
  applyCostingSheet,
  createCostingSheet,
  listCostingSheets,
  type CostingSheet,
} from '../api/costingSheetApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function CostingSheetPage() {
  const [sheets, setSheets] = useState<CostingSheet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [selected, setSelected] = useState('');
  const [seq, setSeq] = useState('10');
  const [ruleCode, setRuleCode] = useState('');
  const [ruleName, setRuleName] = useState('');
  const [ruleType, setRuleType] = useState('PERCENT_OF_BASE');
  const [baseCode, setBaseCode] = useState('SUBTOTAL');
  const [pct, setPct] = useState('0.10');
  const [fixed, setFixed] = useState('');
  const [component, setComponent] = useState('FACTORY_OH');
  const [mat, setMat] = useState('100');
  const [proc, setProc] = useState('35');
  const [applyResult, setApplyResult] = useState<Awaited<ReturnType<typeof applyCostingSheet>> | null>(null);

  const load = useCallback(async () => {
    try {
      setSheets(await listCostingSheets());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <ReceiptLongOutlinedIcon color="primary" />
        <Typography variant="h5">Costing sheet / overhead engine</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Sequential percentage and fixed overhead rules (A6). Backend applies rules in order with full audit
        lines. Example: Material 100 + Process 35 + 10% factory OH + 3% admin OH on subtotal → 152.55.
      </Typography>
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

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Create sheet
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Code" value={code} onChange={(e) => setCode(e.target.value)} required />
            <TextField size="small" label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <Button
              variant="contained"
              disabled={!code.trim() || !name.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await createCostingSheet({ sheetCode: code.trim(), sheetName: name.trim() });
                    setInfo('Sheet created (DRAFT)');
                    setCode('');
                    setName('');
                    await load();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Create
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Add rule / activate
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField select size="small" label="Sheet" value={selected} onChange={(e) => setSelected(e.target.value)} sx={{ minWidth: 220 }} required>
              {sheets.map((s) => (
                <MenuItem key={s.sheetId} value={s.sheetId}>
                  {s.sheetCode} ({s.status})
                </MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="Seq" value={seq} onChange={(e) => setSeq(e.target.value)} sx={{ width: 80 }} />
            <TextField size="small" label="Rule code" value={ruleCode} onChange={(e) => setRuleCode(e.target.value)} />
            <TextField size="small" label="Rule name" value={ruleName} onChange={(e) => setRuleName(e.target.value)} />
            <TextField select size="small" label="Type" value={ruleType} onChange={(e) => setRuleType(e.target.value)} sx={{ minWidth: 160 }}>
              <MenuItem value="PERCENT_OF_BASE">PERCENT_OF_BASE</MenuItem>
              <MenuItem value="FIXED_AMOUNT">FIXED_AMOUNT</MenuItem>
            </TextField>
            <TextField select size="small" label="Base" value={baseCode} onChange={(e) => setBaseCode(e.target.value)} sx={{ minWidth: 140 }}>
              {['MATERIAL', 'PROCESS', 'TOOLING', 'PACKAGING', 'SUBTOTAL', 'TOTAL_SO_FAR'].map((b) => (
                <MenuItem key={b} value={b}>
                  {b}
                </MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="% rate" value={pct} onChange={(e) => setPct(e.target.value)} />
            <TextField size="small" label="Fixed amt" value={fixed} onChange={(e) => setFixed(e.target.value)} />
            <TextField size="small" label="Component" value={component} onChange={(e) => setComponent(e.target.value)} />
            <Button
              variant="outlined"
              disabled={!selected || !ruleCode.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    await addCostingSheetRule(selected, {
                      sequenceNo: Number(seq),
                      ruleCode: ruleCode.trim(),
                      ruleName: ruleName.trim() || ruleCode.trim(),
                      ruleType,
                      baseCode: ruleType === 'PERCENT_OF_BASE' ? baseCode : undefined,
                      percentRate: ruleType === 'PERCENT_OF_BASE' ? pct : undefined,
                      fixedAmount: ruleType === 'FIXED_AMOUNT' ? fixed : undefined,
                      componentCode: component.trim(),
                      costBehavior: 'FIXED',
                    });
                    setInfo('Rule added');
                    await load();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Add rule
            </Button>
            <Button
              variant="contained"
              disabled={!selected}
              onClick={() => {
                void (async () => {
                  try {
                    await activateCostingSheet(selected);
                    setInfo('Sheet activated');
                    await load();
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Activate
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            Apply sheet to cost base
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Material" value={mat} onChange={(e) => setMat(e.target.value)} />
            <TextField size="small" label="Process" value={proc} onChange={(e) => setProc(e.target.value)} />
            <Button
              variant="contained"
              disabled={!selected}
              onClick={() => {
                void (async () => {
                  try {
                    setApplyResult(
                      await applyCostingSheet({
                        sheetId: selected,
                        material: mat,
                        process: proc,
                        tooling: '0',
                        packaging: '0',
                      }),
                    );
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Apply
            </Button>
          </Stack>
          {applyResult && (
            <>
              <Typography variant="body2" sx={{ mt: 2 }}>
                Subtotal {applyResult.baseSubtotal} · Overhead {applyResult.totalOverhead} · Grand total{' '}
                {applyResult.grandTotal}
              </Typography>
              <Typography variant="caption" display="block">
                {applyResult.calculationBasis}
              </Typography>
              <Table size="small" sx={{ mt: 1 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Seq</TableCell>
                    <TableCell>Rule</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell align="right">Base</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Component</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {applyResult.lines.map((ln) => (
                    <TableRow key={String(ln.ruleCode)}>
                      <TableCell>{String(ln.sequenceNo)}</TableCell>
                      <TableCell>{String(ln.ruleCode)}</TableCell>
                      <TableCell>{String(ln.ruleType)}</TableCell>
                      <TableCell align="right">{String(ln.baseAmount)}</TableCell>
                      <TableCell align="right">{String(ln.amount)}</TableCell>
                      <TableCell>{String(ln.componentCode)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </CardContent>
      </Card>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Rules</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sheets.map((s) => (
            <TableRow key={s.sheetId}>
              <TableCell>{s.sheetCode}</TableCell>
              <TableCell>{s.sheetName}</TableCell>
              <TableCell>{s.status}</TableCell>
              <TableCell align="right">{s.ruleCount}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  );
}
