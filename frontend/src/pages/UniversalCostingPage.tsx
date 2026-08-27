import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Checkbox,
  FormControlLabel,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Chip,
  Divider,
} from '@mui/material';
import HubOutlinedIcon from '@mui/icons-material/HubOutlined';
import {
  listCostObjects,
  createCostObject,
  calculate,
  listCalculations,
  transitionLifecycle,
  listNorms,
  createNorm,
  approveNorm,
  createNormVersion,
  listSob,
  createSob,
  listIndustryProfiles,
  type CostObject,
  type CalculateResponse,
  type NormResponse,
  type SobResponse,
  type IndustryProfile,
} from '../api/universalCostingApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';
import { useAuth } from '../auth/useAuth';

const METHODS = [
  'STANDARD', 'SHOULD', 'PROCESS', 'MATERIAL', 'ABC', 'HYBRID',
  'ESTIMATED', 'TARGET', 'ACTUAL', 'QUOTATION', 'PROJECT', 'AVERAGE',
];
const OBJECT_TYPES = ['PRODUCT', 'PART', 'JOB', 'PROJECT', 'SERVICE', 'BATCH', 'ORDER'];

function money(v: number | string | null | undefined): string {
  if (v === null || v === undefined || v === '') return '—';
  const n = typeof v === 'string' ? Number(v) : v;
  if (Number.isNaN(n)) return String(v);
  return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 });
}

export function UniversalCostingPage() {
  const { hasRole } = useAuth();
  /** Write roles for norms, SOB, calculate, lifecycle — backend remains authoritative. */
  const canMutate = hasRole(
    'COST_ANALYST',
    'COST_MANAGER',
    'COST_ENGINEER',
    'MASTER_DATA_ADMIN',
    'ADMIN',
    'SYSTEM_ADMINISTRATOR',
  );
  const canApprove = hasRole(
    'COST_MANAGER',
    'ADMIN',
    'SYSTEM_ADMINISTRATOR',
  );
  const [objects, setObjects] = useState<CostObject[]>([]);
  const [calcs, setCalcs] = useState<CalculateResponse[]>([]);
  const [norms, setNorms] = useState<NormResponse[]>([]);
  const [sobRows, setSobRows] = useState<SobResponse[]>([]);
  const [profiles, setProfiles] = useState<IndustryProfile[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const [objType, setObjType] = useState('JOB');
  const [objCode, setObjCode] = useState('');
  const [objName, setObjName] = useState('');
  const [objCurrency, setObjCurrency] = useState('USD');

  const [costObjectId, setCostObjectId] = useState('');
  const [method, setMethod] = useState('STANDARD');
  const [industryProfile, setIndustryProfile] = useState('');
  const [qty, setQty] = useState('1');
  const [compCode, setCompCode] = useState('MATERIAL');
  const [compAmount, setCompAmount] = useState('100');
  const [discount, setDiscount] = useState('0');
  const [royalty, setRoyalty] = useState('0');
  const [profit, setProfit] = useState('0');
  const [commExpense, setCommExpense] = useState('0');

  const [normCode, setNormCode] = useState('');
  const [normValue, setNormValue] = useState('');
  const [normFrom, setNormFrom] = useState(new Date().toISOString().slice(0, 10));
  const [versionNormId, setVersionNormId] = useState('');
  const [versionValue, setVersionValue] = useState('');
  const [versionFrom, setVersionFrom] = useState(new Date().toISOString().slice(0, 10));

  const [sobSupplier, setSobSupplier] = useState('');
  const [sobPct, setSobPct] = useState('100');
  const [sobGroup, setSobGroup] = useState('DEFAULT');
  const [sobFrom, setSobFrom] = useState(new Date().toISOString().slice(0, 10));
  const [allowPartial, setAllowPartial] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [o, c, n, s, p] = await Promise.all([
        listCostObjects(),
        listCalculations(),
        listNorms(),
        listSob(),
        listIndustryProfiles().catch(() => [] as IndustryProfile[]),
      ]);
      setObjects(o);
      setCalcs(c);
      setNorms(n);
      setSobRows(s);
      setProfiles(p);
      if (!costObjectId && o.length > 0) setCostObjectId(o[0].costObjectId);
      if (!industryProfile && p.length > 0) setIndustryProfile(p[0].profileCode);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, [costObjectId, industryProfile]);

  useEffect(() => {
    void load();
  }, [load]);

  // When profile changes, prefer its first default method if present
  useEffect(() => {
    if (!industryProfile || profiles.length === 0) return;
    const p = profiles.find((x) => x.profileCode === industryProfile);
    if (!p?.defaultMethodsJson) return;
    try {
      const methods = JSON.parse(p.defaultMethodsJson) as string[];
      if (methods.length > 0 && METHODS.includes(methods[0])) {
        setMethod(methods[0]);
      }
      const comps = p.defaultComponentsJson
        ? (JSON.parse(p.defaultComponentsJson) as string[])
        : [];
      if (comps.length > 0) setCompCode(comps[0]);
    } catch {
      /* ignore malformed profile JSON */
    }
  }, [industryProfile, profiles]);

  async function onCreateObject() {
    setError(null);
    setInfo(null);
    try {
      await createCostObject({
        objectType: objType,
        objectCode: objCode.trim(),
        objectName: objName.trim(),
        currencyCode: objCurrency.trim() || 'USD',
      });
      setInfo(`Cost object ${objCode} created.`);
      setObjCode('');
      setObjName('');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onCalculate() {
    setError(null);
    setInfo(null);
    try {
      const body: Record<string, unknown> = {
        costObjectId,
        costMethod: method,
        calculationDate: new Date().toISOString().slice(0, 10),
        quantity: Number(qty) || 1,
        industryProfile: industryProfile || undefined,
        directComponents: [
          {
            componentCode: compCode.trim() || 'MATERIAL',
            amount: Number(compAmount) || 0,
          },
        ],
        commercial: {
          discountAmount: Number(discount) || 0,
          negotiationAdjustment: 0,
          royaltyAmount: Number(royalty) || 0,
          commercialExpenseAmount: Number(commExpense) || 0,
          profitMarginAmount: Number(profit) || 0,
        },
      };
      const r = await calculate(body);
      setInfo(
        `Calculation ${r.calculationId.slice(0, 8)}… — technical ${money(r.technicalTotal)} + commercial ${money(r.commercialTotal)} = total ${money(r.totalCost)} (${r.lifecycleStatus})` +
          (industryProfile ? ` · profile ${industryProfile}` : ''),
      );
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onLifecycle(id: string, action: string) {
    setError(null);
    try {
      await transitionLifecycle(id, action);
      setInfo(`Lifecycle ${action} applied.`);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onCreateNorm() {
    setError(null);
    try {
      await createNorm({
        normCode: normCode.trim(),
        description: normCode.trim(),
        normValue: Number(normValue),
        effectiveFrom: normFrom,
        unit: '%',
      });
      setInfo(`Norm ${normCode} created (DRAFT).`);
      setNormCode('');
      setNormValue('');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onApproveNorm(id: string) {
    setError(null);
    try {
      await approveNorm(id);
      setInfo('Norm approved (immutable). Use New version to change value.');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onCreateNormVersion() {
    setError(null);
    try {
      if (!versionNormId) {
        setError('Select an approved norm to version.');
        return;
      }
      const prior = norms.find((n) => n.normId === versionNormId);
      const r = await createNormVersion(versionNormId, {
        normCode: prior?.normCode,
        description: prior?.description ?? prior?.normCode,
        normValue: Number(versionValue),
        effectiveFrom: versionFrom,
        unit: prior?.unit ?? '%',
      });
      setInfo(`Norm ${r.normCode} version ${r.versionNo} created (DRAFT).`);
      setVersionValue('');
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  async function onCreateSob() {
    setError(null);
    try {
      await createSob(
        {
          supplierId: sobSupplier.trim(),
          allocationPercent: Number(sobPct),
          allocationBasis: 'VOLUME',
          allocationGroupCode: sobGroup.trim(),
          effectiveFrom: sobFrom,
        },
        allowPartial,
      );
      setInfo(`SOB row created (allowPartial=${allowPartial}).`);
      await load();
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  const approvedNorms = norms.filter((n) => n.approvalStatus === 'APPROVED');

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <HubOutlinedIcon color="primary" />
        <Typography variant="h5">Universal costing</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Industry-neutral cost objects and methods. Select an industry profile to apply default methods and
        components. Technical and commercial totals stay separate. Lifecycle: DRAFT → SUBMITTED → APPROVED → LOCKED.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>
      )}
      {info && (
        <Alert severity="success" onClose={() => setInfo(null)}>{info}</Alert>
      )}
      {!canMutate && (
        <Alert severity="info">
          You have read-only access to Universal Costing. Creating objects, calculating, norms, and SOB changes require COST_ANALYST, COST_MANAGER, COST_ENGINEER, MASTER_DATA_ADMIN, or ADMIN.
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>Industry profile</Typography>
          <TextField
            select
            size="small"
            label="Profile"
            value={industryProfile}
            onChange={(e) => setIndustryProfile(e.target.value)}
            sx={{ minWidth: 320 }}
            helperText="Drives default method and component suggestions"
          >
            {profiles.map((p) => (
              <MenuItem key={p.profileId} value={p.profileCode}>
                {p.profileCode} — {p.profileName}
              </MenuItem>
            ))}
          </TextField>
          {profiles.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              No profiles returned from API (seed via V151/V153 migrations when DB is available).
            </Typography>
          )}
          {industryProfile && (
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
              {profiles
                .filter((p) => p.profileCode === industryProfile)
                .map((p) => (
                  <Chip key={p.profileId} size="small" label={p.description || p.profileName} />
                ))}
            </Stack>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>Create cost object</Typography>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField select size="small" label="Type" value={objType} onChange={(e) => setObjType(e.target.value)} sx={{ minWidth: 140 }}>
              {OBJECT_TYPES.map((t) => (
                <MenuItem key={t} value={t}>{t}</MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="Code" required value={objCode} onChange={(e) => setObjCode(e.target.value)} />
            <TextField size="small" label="Name" required value={objName} onChange={(e) => setObjName(e.target.value)} />
            <TextField size="small" label="Currency" value={objCurrency} onChange={(e) => setObjCurrency(e.target.value)} sx={{ width: 100 }} />
            <Button variant="contained" onClick={() => void onCreateObject()} disabled={!canMutate || !objCode.trim() || !objName.trim()}>
              Create
            </Button>
          </Stack>
          <Divider sx={{ my: 2 }} />
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Currency</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {objects.map((o) => (
                <TableRow
                  key={o.costObjectId}
                  hover
                  selected={o.costObjectId === costObjectId}
                  onClick={() => setCostObjectId(o.costObjectId)}
                  sx={{ cursor: 'pointer' }}
                >
                  <TableCell>{o.objectCode}</TableCell>
                  <TableCell>{o.objectName}</TableCell>
                  <TableCell>{o.objectType}</TableCell>
                  <TableCell>{o.currencyCode}</TableCell>
                  <TableCell>{o.status}</TableCell>
                </TableRow>
              ))}
              {objects.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography variant="body2" color="text.secondary">No cost objects yet.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>Calculate (component + commercial)</Typography>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField select size="small" label="Cost object" value={costObjectId} onChange={(e) => setCostObjectId(e.target.value)} sx={{ minWidth: 180 }}>
              {objects.map((o) => (
                <MenuItem key={o.costObjectId} value={o.costObjectId}>{o.objectCode}</MenuItem>
              ))}
            </TextField>
            <TextField select size="small" label="Industry profile" value={industryProfile} onChange={(e) => setIndustryProfile(e.target.value)} sx={{ minWidth: 200 }}>
              {profiles.map((p) => (
                <MenuItem key={p.profileId} value={p.profileCode}>{p.profileCode}</MenuItem>
              ))}
            </TextField>
            <TextField select size="small" label="Method" value={method} onChange={(e) => setMethod(e.target.value)} sx={{ minWidth: 130 }}>
              {METHODS.map((m) => (
                <MenuItem key={m} value={m}>{m}</MenuItem>
              ))}
            </TextField>
            <TextField size="small" label="Qty" value={qty} onChange={(e) => setQty(e.target.value)} sx={{ width: 90 }} />
            <TextField size="small" label="Component" value={compCode} onChange={(e) => setCompCode(e.target.value)} sx={{ width: 120 }} />
            <TextField size="small" label="Amount" value={compAmount} onChange={(e) => setCompAmount(e.target.value)} sx={{ width: 110 }} />
            <TextField size="small" label="Discount" value={discount} onChange={(e) => setDiscount(e.target.value)} sx={{ width: 100 }} />
            <TextField size="small" label="Royalty" value={royalty} onChange={(e) => setRoyalty(e.target.value)} sx={{ width: 100 }} />
            <TextField size="small" label="Profit" value={profit} onChange={(e) => setProfit(e.target.value)} sx={{ width: 100 }} />
            <TextField size="small" label="Comm. exp." value={commExpense} onChange={(e) => setCommExpense(e.target.value)} sx={{ width: 100 }} />
            <Button variant="contained" onClick={() => void onCalculate()} disabled={!canMutate || !costObjectId}>
              Calculate
            </Button>
          </Stack>
          <Divider sx={{ my: 2 }} />
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Method</TableCell>
                <TableCell>Profile</TableCell>
                <TableCell align="right">Technical</TableCell>
                <TableCell align="right">Commercial</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {calcs.map((c) => (
                <TableRow key={c.calculationId}>
                  <TableCell>{c.costMethod}</TableCell>
                  <TableCell>{c.industryProfile || '—'}</TableCell>
                  <TableCell align="right">{money(c.technicalTotal)}</TableCell>
                  <TableCell align="right">{money(c.commercialTotal)}</TableCell>
                  <TableCell align="right">{money(c.totalCost)}</TableCell>
                  <TableCell>
                    <Chip size="small" label={c.lifecycleStatus} color={c.lifecycleStatus === 'LOCKED' ? 'success' : 'default'} />
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5}>
                      {c.lifecycleStatus === 'DRAFT' && canMutate && (
                        <>
                          <Button size="small" onClick={() => void onLifecycle(c.calculationId, 'SUBMIT')}>Submit</Button>
                          {canApprove && (
                            <Button size="small" onClick={() => void onLifecycle(c.calculationId, 'APPROVE')}>Approve</Button>
                          )}
                        </>
                      )}
                      {c.lifecycleStatus === 'SUBMITTED' && canApprove && (
                        <Button size="small" onClick={() => void onLifecycle(c.calculationId, 'APPROVE')}>Approve</Button>
                      )}
                      {c.lifecycleStatus === 'APPROVED' && canApprove && (
                        <Button size="small" variant="outlined" onClick={() => void onLifecycle(c.calculationId, 'LOCK')}>Lock</Button>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
              {calcs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Typography variant="body2" color="text.secondary">No calculations yet.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography fontWeight={600} gutterBottom>Norms</Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap mb={1}>
              <TextField size="small" label="Code" value={normCode} onChange={(e) => setNormCode(e.target.value)} />
              <TextField size="small" label="Value" value={normValue} onChange={(e) => setNormValue(e.target.value)} sx={{ width: 100 }} />
              <TextField size="small" type="date" label="Effective from" InputLabelProps={{ shrink: true }} value={normFrom} onChange={(e) => setNormFrom(e.target.value)} />
              <Button size="small" variant="contained" onClick={() => void onCreateNorm()} disabled={!canMutate || !normCode.trim() || !normValue}>Add</Button>
            </Stack>
            <Typography variant="subtitle2" sx={{ mt: 1, mb: 0.5 }}>New version (approved norms only)</Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap mb={1}>
              <TextField select size="small" label="Prior norm" value={versionNormId} onChange={(e) => setVersionNormId(e.target.value)} sx={{ minWidth: 160 }}>
                {approvedNorms.map((n) => (
                  <MenuItem key={n.normId} value={n.normId}>{n.normCode} v{n.versionNo}</MenuItem>
                ))}
              </TextField>
              <TextField size="small" label="New value" value={versionValue} onChange={(e) => setVersionValue(e.target.value)} sx={{ width: 100 }} />
              <TextField size="small" type="date" label="Effective from" InputLabelProps={{ shrink: true }} value={versionFrom} onChange={(e) => setVersionFrom(e.target.value)} />
              <Button size="small" variant="outlined" onClick={() => void onCreateNormVersion()} disabled={!canMutate || !versionNormId || !versionValue}>
                New version
              </Button>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell align="right">Value</TableCell>
                  <TableCell>Ver</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {norms.map((n) => (
                  <TableRow key={n.normId}>
                    <TableCell>{n.normCode}</TableCell>
                    <TableCell align="right">{money(n.normValue)}</TableCell>
                    <TableCell>{n.versionNo}</TableCell>
                    <TableCell>{n.approvalStatus}</TableCell>
                    <TableCell>
                      {n.approvalStatus === 'DRAFT' && canApprove && (
                        <Button size="small" onClick={() => void onApproveNorm(n.normId)}>Approve</Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography fontWeight={600} gutterBottom>Supplier share of business</Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap mb={1} alignItems="center">
              <TextField size="small" label="Supplier ID" value={sobSupplier} onChange={(e) => setSobSupplier(e.target.value)} />
              <TextField size="small" label="%" value={sobPct} onChange={(e) => setSobPct(e.target.value)} sx={{ width: 80 }} />
              <TextField size="small" label="Group" value={sobGroup} onChange={(e) => setSobGroup(e.target.value)} sx={{ width: 120 }} />
              <TextField size="small" type="date" label="From" InputLabelProps={{ shrink: true }} value={sobFrom} onChange={(e) => setSobFrom(e.target.value)} />
              <FormControlLabel
                control={<Checkbox checked={allowPartial} onChange={(e) => setAllowPartial(e.target.checked)} size="small" />}
                label="Allow partial (&lt;100%)"
              />
              <Button size="small" variant="contained" onClick={() => void onCreateSob()} disabled={!canMutate || !sobSupplier.trim()}>Add</Button>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Supplier</TableCell>
                  <TableCell>Group</TableCell>
                  <TableCell align="right">%</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sobRows.map((s) => (
                  <TableRow key={s.sobId}>
                    <TableCell>{s.supplierId}</TableCell>
                    <TableCell>{s.allocationGroupCode}</TableCell>
                    <TableCell align="right">{money(s.allocationPercent)}</TableCell>
                    <TableCell>{s.status}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </Stack>
    </Stack>
  );
}
