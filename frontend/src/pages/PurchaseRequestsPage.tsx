import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
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
import {
  correctOrderTracking,
  createPurchaseRequest,
  createStandalonePurchaseRequest,
  listPurchaseRequestsForCustomer,
  getDeliveryPerformance,
  overrideOnTime,
  decidePurchaseRequest,
  exportPurchasePackage,
  getPurchaseRequest,
  listOrderTracking,
  listPurchaseByBom,
  releasePurchaseRequest,
  createRfqFromPurchaseRequest,
  createPurchaseOrderFromPr,
  submitPurchaseRequest,
  type OrderTracking,
  type PerformanceSummary,
  type PurchaseRequest,
} from '../api/purchaseApi';

export function PurchaseRequestsPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [mvbId, setMvbId] = useState('');
  const [overrideReason, setOverrideReason] = useState('');
  const [plantCode, setPlantCode] = useState('');
  const [materialCode, setMaterialCode] = useState('');
  const [materialDesc, setMaterialDesc] = useState('');
  const [lineQty, setLineQty] = useState('1');
  const [unitPrice, setUnitPrice] = useState('');
  const [standaloneMode, setStandaloneMode] = useState(true);
  const [requiredDate, setRequiredDate] = useState('');
  const [priorityCode, setPriorityCode] = useState('NORMAL');
  const [otOverrideReason, setOtOverrideReason] = useState('');
  const [items, setItems] = useState<PurchaseRequest[]>([]);
  const [poSupplierId, setPoSupplierId] = useState('');
  const [selected, setSelected] = useState<PurchaseRequest | null>(null);
  const [decisionReason, setDecisionReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [tracking, setTracking] = useState<OrderTracking[]>([]);
  const [correctionReason, setCorrectionReason] = useState('');
  const [perf, setPerf] = useState<PerformanceSummary | null>(null);

  async function onList() {
    setError(null);
    try {
      if (standaloneMode || !bomNodeId.trim()) {
        const list = await listPurchaseRequestsForCustomer();
        setItems(list as PurchaseRequest[]);
      } else {
        const list = await listPurchaseByBom(bomNodeId.trim());
        setItems(list);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'List failed');
    }
  }

  async function onCreateStandalone() {
    setError(null);
    setInfo(null);
    const q = Number(lineQty);
    if (!plantCode.trim() || !requiredDate.trim()) {
      setError('Plant and required date are required for standalone PR');
      return;
    }
    if (!materialCode.trim() || !Number.isFinite(q) || q <= 0) {
      setError('Material code and quantity > 0 required');
      return;
    }
    try {
      const price = unitPrice.trim() === '' ? undefined : Number(unitPrice);
      const pr = await createStandalonePurchaseRequest({
        plantCode: plantCode.trim(),
        requiredDate: requiredDate.trim(),
        priorityCode: priorityCode.trim() || undefined,
        reportingCurrency: 'USD',
        lines: [
          {
            materialCode: materialCode.trim(),
            materialDescription: materialDesc.trim() || undefined,
            quantity: q,
            uom: 'EA',
            targetPrice: price !== undefined && Number.isFinite(price) ? price : undefined,
            targetCurrency: 'USD',
          },
        ],
      });
      setSelected(pr as PurchaseRequest);
      setInfo('Standalone PR created (PURCHASE module — no BOM required)');
      await onList();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Standalone create failed');
    }
  }

  async function onCreate() {
    setError(null);
    setInfo(null);
    try {
      const body: Parameters<typeof createPurchaseRequest>[1] = {
        makeVsBuySimulationId: mvbId.trim() || undefined,
        manualOverrideReason: overrideReason.trim() || undefined,
        plantCode: plantCode.trim() || undefined,
        requiredDate: requiredDate.trim() || undefined,
        priorityCode: priorityCode.trim() || undefined,
      };
      const pr = await createPurchaseRequest(bomNodeId.trim(), body);
      setSelected(pr);
      setInfo(pr.messages?.join(' ') ?? 'Created DRAFT');
      await onList();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Create failed');
    }
  }

  async function onOpen(id: string) {
    setError(null);
    try {
      const pr = await getPurchaseRequest(id);
      setSelected(pr);
      if (pr.status === 'RELEASED') {
        try {
          setTracking(await listOrderTracking(id));
        } catch {
          setTracking([]);
        }
        try {
          setPerf(await getDeliveryPerformance(id));
        } catch {
          setPerf(null);
        }
      } else {
        setTracking([]);
        setPerf(null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Load failed');
    }
  }

  async function onLoadTracking() {
    if (!selected) return;
    try {
      setTracking(await listOrderTracking(selected.purchaseRequestId));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Tracking load failed');
    }
  }

  async function onLoadPerf() {
    if (!selected) return;
    try {
      setPerf(await getDeliveryPerformance(selected.purchaseRequestId));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Performance load failed');
    }
  }

  async function onOverrideOnTime(trackingId: string, onTime: boolean) {
    if (!selected || !otOverrideReason.trim()) {
      setError('reason is required for on-time override');
      return;
    }
    try {
      await overrideOnTime(selected.purchaseRequestId, trackingId, onTime, otOverrideReason.trim());
      setInfo('On-time override saved (human only; no SAP write)');
      setOtOverrideReason('');
      await onLoadPerf();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Override failed');
    }
  }

  async function onCorrectTracking(trackingId: string) {

    if (!selected || !correctionReason.trim()) {
      setError('correctionReason is required for human correction');
      return;
    }
    try {
      await correctOrderTracking(selected.purchaseRequestId, trackingId, {
        correctionReason: correctionReason.trim(),
      });
      setInfo('Human tracking correction saved (no SAP write)');
      setCorrectionReason('');
      await onLoadTracking();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Correction failed');
    }
  }

  async function onSubmit() {
    if (!selected) return;
    try {
      setSelected(await submitPurchaseRequest(selected.purchaseRequestId));
      setInfo('Submitted — human approval required');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Submit failed');
    }
  }

  async function onDecide(code: 'APPROVE' | 'REJECT') {
    if (!selected) return;
    try {
      setSelected(await decidePurchaseRequest(selected.purchaseRequestId, code, decisionReason));
      setInfo(`Decision: ${code}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Decide failed');
    }
  }

  async function onCreateRfq() {
    if (!selected) return;
    try {
      setSelected(await createRfqFromPurchaseRequest(selected.purchaseRequestId));
      setInfo('RFQ created from PR via existing RFQ service (Wave C)');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Create RFQ failed');
    }
  }


  async function onCreatePoFromPr() {
    if (!selected) return;
    setError(null);
    setInfo(null);
    if (!poSupplierId.trim()) {
      setError('Supplier ID is required to create PO from PR');
      return;
    }
    try {
      const po = await createPurchaseOrderFromPr(selected.purchaseRequestId, {
        supplierId: poSupplierId.trim(),
      });
      setInfo(`PO ${po.poNumber} created as DRAFT from PR`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Create PO from PR failed');
    }
  }

  async function onRelease() {
    if (!selected) return;
    try {
      setSelected(await releasePurchaseRequest(selected.purchaseRequestId));
      setInfo('Released for CPI/ERP export (does not post to SAP MM)');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Release failed');
    }
  }

  async function onExport() {
    if (!selected) return;
    try {
      const pkg = await exportPurchasePackage(selected.purchaseRequestId);
      const blob = new Blob([JSON.stringify(pkg, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `purchase-request-${selected.purchaseRequestId}.json`;
      a.click();
      URL.revokeObjectURL(url);
      setInfo('Export package downloaded (read-only; does not post to SAP MM)');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Export failed');
    }
  }

  return (
    <Box p={2}>
      <Typography variant="h5" gutterBottom>
        Purchase Requests
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        PURCHASE module — full feature without Product Costing or Vendor Selection.
        Human decision only. Export does not post to SAP MM. Not GR/IR.
      </Typography>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {info}
        </Alert>
      )}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Create PR (PURCHASE-native — no BOM required)
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} mb={1}>
          <TextField label="Plant *" required value={plantCode} onChange={(e) => setPlantCode(e.target.value)} size="small" fullWidth />
          <TextField label="Required date *" type="date" required value={requiredDate} onChange={(e) => setRequiredDate(e.target.value)} size="small" InputLabelProps={{ shrink: true }} fullWidth />
          <TextField label="Priority" value={priorityCode} onChange={(e) => setPriorityCode(e.target.value)} size="small" fullWidth />
          <TextField label="Material code *" value={materialCode} onChange={(e) => setMaterialCode(e.target.value)} size="small" fullWidth />
          <TextField label="Description" value={materialDesc} onChange={(e) => setMaterialDesc(e.target.value)} size="small" fullWidth />
          <TextField label="Qty *" value={lineQty} onChange={(e) => setLineQty(e.target.value)} size="small" fullWidth />
          <TextField label="Target price" value={unitPrice} onChange={(e) => setUnitPrice(e.target.value)} size="small" fullWidth />
        </Stack>
        <Stack direction="row" spacing={1} mb={2}>
          <Button variant="contained" onClick={() => void onCreateStandalone()}>
            Create standalone PR
          </Button>
          <Button variant="outlined" onClick={() => { setStandaloneMode(true); void onList(); }}>
            List my PRs
          </Button>
        </Stack>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Optional — Product Costing path (BOM / MvB) when that module is also entitled
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
          <TextField label="BOM Node ID" value={bomNodeId} onChange={(e) => setBomNodeId(e.target.value)} size="small" fullWidth />
          <TextField label="Make-vs-Buy Simulation ID" value={mvbId} onChange={(e) => setMvbId(e.target.value)} size="small" fullWidth />
          <TextField label="Manual override reason" value={overrideReason} onChange={(e) => setOverrideReason(e.target.value)} size="small" fullWidth />
        </Stack>
        <Stack direction="row" spacing={1} mt={2}>
          <Button variant="outlined" disabled={!bomNodeId.trim()} onClick={() => { setStandaloneMode(false); void onCreate(); }}>
            Create from BOM
          </Button>
          <Button variant="outlined" disabled={!bomNodeId.trim()} onClick={() => { setStandaloneMode(false); void onList(); }}>
            List by BOM
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Requests
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Currency</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((it) => (
              <TableRow key={it.purchaseRequestId}>
                <TableCell>{it.purchaseRequestId}</TableCell>
                <TableCell>{it.status}</TableCell>
                <TableCell>{it.reportingCurrency}</TableCell>
                <TableCell>
                  <Button size="small" onClick={() => void onOpen(it.purchaseRequestId)}>
                    Open
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      {selected && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1">
            {selected.prNumber || selected.purchaseRequestId} — {selected.status}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            MvB: {selected.makeVsBuySimulationId ?? '—'} | Decision: {selected.decisionCode ?? '—'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            SAP PO: {selected.externalPoReference ?? 'DATA NOT AVAILABLE'} · Link:{' '}
            {selected.poLinkStatus ?? 'NOT_LINKED'}
            {selected.poLinkedSource ? ` (${selected.poLinkedSource})` : ''}
          </Typography>
          <Stack direction="row" spacing={1} mt={2} flexWrap="wrap" useFlexGap>
            <Button variant="contained" disabled={selected.status !== 'DRAFT'} onClick={() => void onSubmit()}>
              Submit
            </Button>
            <TextField
              label="Decision reason"
              size="small"
              value={decisionReason}
              onChange={(e) => setDecisionReason(e.target.value)}
            />
            <Button
              variant="contained"
              color="success"
              disabled={!decisionReason.trim() || !['SUBMITTED', 'APPROVAL_PENDING'].includes(selected.status)}
              onClick={() => void onDecide('APPROVE')}
            >
              Approve
            </Button>
            <Button
              variant="outlined"
              color="error"
              disabled={!decisionReason.trim() || !['SUBMITTED', 'APPROVAL_PENDING'].includes(selected.status)}
              onClick={() => void onDecide('REJECT')}
            >
              Reject
            </Button>
            <Button
              variant="outlined"
              disabled={selected.status !== 'APPROVED' || !!selected.linkedRfqId}
              onClick={() => void onCreateRfq()}
            >
              Create RFQ
            </Button>
            <Button variant="contained" disabled={selected.status !== 'APPROVED' && selected.status !== 'RFQ_LINKED'} onClick={() => void onRelease()}>
              Release
            </Button>
            <Button variant="outlined" onClick={() => void onExport()}>
              Export package
            </Button>
            <TextField
              size="small"
              label="Supplier ID for PO"
              value={poSupplierId}
              onChange={(e) => setPoSupplierId(e.target.value)}
              sx={{ minWidth: 180 }}
            />
            <Button
              variant="contained"
              color="secondary"
              disabled={
                !poSupplierId.trim() ||
                !['APPROVED', 'RFQ_LINKED', 'RELEASED'].includes(selected.status)
              }
              onClick={() => void onCreatePoFromPr()}
            >
              Create PO from PR
            </Button>
          </Stack>
          <Table size="small" sx={{ mt: 2 }}>
            <TableHead>
              <TableRow>
                <TableCell>#</TableCell>
                <TableCell>Supplier</TableCell>
                <TableCell>Qty</TableCell>
                <TableCell>Unit</TableCell>
                <TableCell>Reporting</TableCell>
                <TableCell>Data</TableCell>
                <TableCell>TCO</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {selected.lines?.map((l) => (
                <TableRow key={l.lineId}>
                  <TableCell>{l.lineNo}</TableCell>
                  <TableCell>{l.supplierId}</TableCell>
                  <TableCell>
                    {String(l.quantity)} {l.uom}
                  </TableCell>
                  <TableCell>
                    {l.unitPrice != null ? `${l.unitPrice} ${l.currency ?? ''}` : 'DATA NOT AVAILABLE'}
                  </TableCell>
                  <TableCell>
                    {l.unitPriceReporting != null ? String(l.unitPriceReporting) : 'DATA NOT AVAILABLE'}
                  </TableCell>
                  <TableCell>{l.dataStatus}</TableCell>
                  <TableCell>{l.tcoStatus}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {selected.status === 'RELEASED' && (
            <Box sx={{ mt: 3 }}>
              <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                <Typography variant="subtitle2">Order tracking (P3-05 · CPI inbound · no SAP write)</Typography>
                <Button size="small" onClick={() => void onLoadTracking()}>
                  Refresh tracking
                </Button>
              </Stack>
              <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                ASN date / qty shipped from CPI. Missing fields show NOT_AVAILABLE. Human correction requires reason.
              </Typography>
              <TextField
                label="Correction reason"
                size="small"
                fullWidth
                required
                sx={{ mb: 1 }}
                value={correctionReason}
                onChange={(e) => setCorrectionReason(e.target.value)}
                helperText="Required for human edit"
              />
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Status</TableCell>
                    <TableCell>ASN</TableCell>
                    <TableCell>Qty shipped</TableCell>
                    <TableCell>Data</TableCell>
                    <TableCell>Source</TableCell>
                    <TableCell>Corrected</TableCell>
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tracking.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7}>No tracking rows yet (await CPI ingest)</TableCell>
                    </TableRow>
                  )}
                  {tracking.map((t) => (
                    <TableRow key={t.trackingId}>
                      <TableCell>{t.trackingStatus}</TableCell>
                      <TableCell>{t.asnDate ?? 'DATA NOT AVAILABLE'}</TableCell>
                      <TableCell>
                        {t.qtyShipped != null ? String(t.qtyShipped) : 'DATA NOT AVAILABLE'}
                      </TableCell>
                      <TableCell>{t.dataStatus}</TableCell>
                      <TableCell>
                        {t.sourceSystem}
                        {t.humanCorrected ? ' + human' : ''}
                      </TableCell>
                      <TableCell>{t.correctedBy ?? '—'}</TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          disabled={!correctionReason.trim()}
                          onClick={() => void onCorrectTracking(t.trackingId)}
                        >
                          Mark corrected
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <Box sx={{ mt: 3 }}>
                <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                  <Typography variant="subtitle2">
                    Delivery performance (P3-06 · from tracking)
                  </Typography>
                  <Button size="small" onClick={() => void onLoadPerf()}>
                    Refresh performance
                  </Button>
                </Stack>
                {perf && (
                  <Typography variant="body2" color="text.secondary" mb={1}>
                    Shipments: {perf.shipmentCount} · On-time: {perf.onTimeCount} · Late:{' '}
                    {perf.lateCount} · Data N/A: {perf.dataNotAvailableCount}
                  </Typography>
                )}
                <TextField
                  label="On-time override reason"
                  size="small"
                  fullWidth
                  required
                  sx={{ mb: 1 }}
                  value={otOverrideReason}
                  onChange={(e) => setOtOverrideReason(e.target.value)}
                />
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Need-by</TableCell>
                      <TableCell>ASN</TableCell>
                      <TableCell>Delay days</TableCell>
                      <TableCell>On-time</TableCell>
                      <TableCell>Qty var</TableCell>
                      <TableCell>Data</TableCell>
                      <TableCell />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(perf?.shipments ?? []).length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7}>No performance rows (need tracking + need-by)</TableCell>
                      </TableRow>
                    )}
                    {(perf?.shipments ?? []).map((s) => (
                      <TableRow key={s.trackingId}>
                        <TableCell>{s.needByDate ?? 'DATA NOT AVAILABLE'}</TableCell>
                        <TableCell>{s.asnDate ?? 'DATA NOT AVAILABLE'}</TableCell>
                        <TableCell>
                          {s.delayDays != null ? s.delayDays : 'DATA NOT AVAILABLE'}
                        </TableCell>
                        <TableCell>
                          {s.onTimeEffective == null
                            ? 'DATA NOT AVAILABLE'
                            : s.onTimeEffective
                              ? 'YES'
                              : 'NO'}
                          {s.humanOverrideApplied ? ' (override)' : ''}
                        </TableCell>
                        <TableCell>
                          {s.qtyVarianceStatus === 'NOT_AVAILABLE'
                            ? 'DATA NOT AVAILABLE'
                            : String(s.qtyVariance)}
                        </TableCell>
                        <TableCell>{s.dataStatus}</TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            disabled={!otOverrideReason.trim()}
                            onClick={() => void onOverrideOnTime(s.trackingId, true)}
                          >
                            Override OT
                          </Button>
                          <Button
                            size="small"
                            disabled={!otOverrideReason.trim()}
                            onClick={() => void onOverrideOnTime(s.trackingId, false)}
                          >
                            Override late
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            </Box>
          )}
        </Paper>
      )}
    </Box>
  );
}
