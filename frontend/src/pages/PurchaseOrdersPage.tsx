import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  MenuItem,
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
  cancelPurchaseOrder,
  closePurchaseOrder,
  createPurchaseOrder,
  decidePurchaseOrder,
  getPurchaseOrder,
  issuePurchaseOrder,
  listPurchaseOrders,
  reworkPurchaseOrder,
  submitPurchaseOrder,
  type PurchaseOrder,
} from '../api/purchaseApi';

/**
 * Wave D — Purchase Orders (module PURCHASE).
 * P2P operational cycle center: Create PO → submit → decide → issue → close.
 * Hierarchical parent: Purchase (not Dashboard).
 * Human-only approve/issue; does not post to SAP MM.
 */
export function PurchaseOrdersPage() {
  const [items, setItems] = useState<PurchaseOrder[]>([]);
  const [selected, setSelected] = useState<PurchaseOrder | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [decisionReason, setDecisionReason] = useState('');
  const [externalRef, setExternalRef] = useState('');

  // Create form
  const [supplierId, setSupplierId] = useState('');
  const [plantCode, setPlantCode] = useState('');
  const [currencyCode, setCurrencyCode] = useState('USD');
  const [prId, setPrId] = useState('');
  const [rfqId, setRfqId] = useState('');
  const [materialCode, setMaterialCode] = useState('');
  const [qty, setQty] = useState('1');
  const [uom, setUom] = useState('EA');
  const [unitPrice, setUnitPrice] = useState('');
  const [requiredDate, setRequiredDate] = useState('');

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const list = await listPurchaseOrders(statusFilter.trim() || undefined);
      setItems(list);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'List failed');
    }
  }, [statusFilter]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function onCreate() {
    setError(null);
    setInfo(null);
    const q = Number(qty);
    if (!supplierId.trim()) {
      setError('Supplier ID * is required');
      return;
    }
    if (!Number.isFinite(q) || q <= 0) {
      setError('Quantity * must be > 0');
      return;
    }
    try {
      const price = unitPrice.trim() === '' ? undefined : Number(unitPrice);
      const po = await createPurchaseOrder({
        supplierId: supplierId.trim(),
        plantCode: plantCode.trim() || undefined,
        currencyCode: currencyCode.trim() || 'USD',
        purchaseRequestId: prId.trim() || undefined,
        linkedRfqId: rfqId.trim() || undefined,
        requiredDeliveryDate: requiredDate.trim() || undefined,
        lines: [
          {
            materialCode: materialCode.trim() || undefined,
            quantity: q,
            uom: uom.trim() || 'EA',
            unitPrice: price !== undefined && Number.isFinite(price) ? price : undefined,
            currencyCode: currencyCode.trim() || 'USD',
          },
        ],
      });
      setSelected(po);
      setInfo(po.messages?.join(' ') ?? 'Created DRAFT');
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Create failed');
    }
  }

  async function onOpen(id: string) {
    setError(null);
    try {
      setSelected(await getPurchaseOrder(id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Open failed');
    }
  }

  async function run(action: () => Promise<PurchaseOrder>, okMsg?: string) {
    setError(null);
    setInfo(null);
    try {
      const po = await action();
      setSelected(po);
      setInfo(okMsg ?? po.messages?.join(' ') ?? po.status);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Action failed');
    }
  }

  return (
    <Box sx={{ p: 2, maxWidth: 1200 }}>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Purchase Orders
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Module PURCHASE · Human approve/issue only · Does not post to SAP MM
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
          Create PO (DRAFT)
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} flexWrap="wrap" useFlexGap>
          <TextField
            label="Supplier ID *"
            size="small"
            value={supplierId}
            onChange={(e) => setSupplierId(e.target.value)}
            required
            sx={{ minWidth: 180 }}
          />
          <TextField
            label="Plant"
            size="small"
            value={plantCode}
            onChange={(e) => setPlantCode(e.target.value)}
            sx={{ minWidth: 120 }}
          />
          <TextField
            label="Currency"
            size="small"
            value={currencyCode}
            onChange={(e) => setCurrencyCode(e.target.value)}
            sx={{ width: 100 }}
          />
          <TextField
            label="PR ID (optional)"
            size="small"
            value={prId}
            onChange={(e) => setPrId(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <TextField
            label="RFQ ID (optional)"
            size="small"
            value={rfqId}
            onChange={(e) => setRfqId(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <TextField
            label="Material"
            size="small"
            value={materialCode}
            onChange={(e) => setMaterialCode(e.target.value)}
            sx={{ minWidth: 140 }}
          />
          <TextField
            label="Quantity *"
            size="small"
            value={qty}
            onChange={(e) => setQty(e.target.value)}
            required
            sx={{ width: 100 }}
          />
          <TextField label="UOM" size="small" value={uom} onChange={(e) => setUom(e.target.value)} sx={{ width: 80 }} />
          <TextField
            label="Unit price"
            size="small"
            value={unitPrice}
            onChange={(e) => setUnitPrice(e.target.value)}
            helperText="Empty → NOT_AVAILABLE"
            sx={{ width: 140 }}
          />
          <TextField
            label="Required delivery"
            type="date"
            size="small"
            InputLabelProps={{ shrink: true }}
            value={requiredDate}
            onChange={(e) => setRequiredDate(e.target.value)}
          />
          <Button variant="contained" onClick={() => void onCreate()}>
            Create DRAFT
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, flex: 1 }}>
            Orders
          </Typography>
          <TextField
            select
            size="small"
            label="Status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            sx={{ width: 160 }}
          >
            <MenuItem value="">All</MenuItem>
            {['DRAFT', 'SUBMITTED', 'APPROVAL', 'APPROVED', 'ISSUED', 'REJECTED', 'REWORK', 'CANCELLED'].map(
              (s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ),
            )}
          </TextField>
          <Button onClick={() => void refresh()}>Refresh</Button>
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>PO Number</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Supplier</TableCell>
              <TableCell>Currency</TableCell>
              <TableCell>Plant</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((po) => (
              <TableRow key={po.purchaseOrderId} hover>
                <TableCell>{po.poNumber ?? po.purchaseOrderId}</TableCell>
                <TableCell>{po.status}</TableCell>
                <TableCell>{po.supplierId}</TableCell>
                <TableCell>{po.currencyCode}</TableCell>
                <TableCell>{po.plantCode ?? '—'}</TableCell>
                <TableCell>
                  <Button size="small" onClick={() => void onOpen(po.purchaseOrderId)}>
                    Open
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {items.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>
                  <Typography variant="body2" color="text.secondary">
                    No purchase orders for this tenant.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      {selected && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {selected.poNumber ?? selected.purchaseOrderId} · {selected.status}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Supplier {selected.supplierId}
            {selected.purchaseRequestId ? ` · PR ${selected.purchaseRequestId}` : ''}
            {selected.linkedRfqId ? ` · RFQ ${selected.linkedRfqId}` : ''}
          </Typography>

          <Table size="small" sx={{ mb: 2 }}>
            <TableHead>
              <TableRow>
                <TableCell>#</TableCell>
                <TableCell>Material</TableCell>
                <TableCell>Qty</TableCell>
                <TableCell>UOM</TableCell>
                <TableCell>Unit price</TableCell>
                <TableCell>Data</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {selected.lines.map((l) => (
                <TableRow key={l.lineId}>
                  <TableCell>{l.lineNo}</TableCell>
                  <TableCell>{l.materialCode ?? '—'}</TableCell>
                  <TableCell>{String(l.quantity)}</TableCell>
                  <TableCell>{l.uom}</TableCell>
                  <TableCell>{l.unitPrice != null ? String(l.unitPrice) : '—'}</TableCell>
                  <TableCell>{l.dataStatus}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
            {(selected.status === 'DRAFT' || selected.status === 'REWORK') && (
              <Button variant="contained" onClick={() => void run(() => submitPurchaseOrder(selected.purchaseOrderId))}>
                Submit
              </Button>
            )}
            {selected.status === 'APPROVAL' && (
              <>
                <TextField
                  size="small"
                  label="Decision reason *"
                  value={decisionReason}
                  onChange={(e) => setDecisionReason(e.target.value)}
                  sx={{ minWidth: 220 }}
                />
                <Button
                  color="success"
                  variant="contained"
                  onClick={() =>
                    void run(() =>
                      decidePurchaseOrder(selected.purchaseOrderId, 'APPROVE', decisionReason.trim() || 'Approved'),
                    )
                  }
                >
                  Approve
                </Button>
                <Button
                  color="warning"
                  variant="outlined"
                  onClick={() =>
                    void run(() =>
                      decidePurchaseOrder(selected.purchaseOrderId, 'REJECT', decisionReason.trim() || 'Rejected'),
                    )
                  }
                >
                  Reject
                </Button>
              </>
            )}
            {selected.status === 'REJECTED' && (
              <Button onClick={() => void run(() => reworkPurchaseOrder(selected.purchaseOrderId))}>Rework</Button>
            )}
            {selected.status === 'APPROVED' && (
              <>
                <TextField
                  size="small"
                  label="External PO ref"
                  value={externalRef}
                  onChange={(e) => setExternalRef(e.target.value)}
                  sx={{ minWidth: 180 }}
                />
                <Button
                  variant="contained"
                  onClick={() =>
                    void run(() => issuePurchaseOrder(selected.purchaseOrderId, externalRef.trim() || undefined))
                  }
                >
                  Issue
                </Button>
              </>
            )}
            {(selected.status === 'ISSUED' || selected.status === 'PARTIALLY_RECEIVED') && (
              <>
                {selected.status === 'ISSUED' && (
                  <Button
                    variant="outlined"
                    onClick={() =>
                      void run(() =>
                        closePurchaseOrder(selected.purchaseOrderId, {
                          partial: true,
                          reason: 'Partial receipt recorded',
                        }),
                      )
                    }
                  >
                    Partial receive
                  </Button>
                )}
                <Button
                  variant="outlined"
                  onClick={() =>
                    void run(() =>
                      closePurchaseOrder(selected.purchaseOrderId, {
                        partial: false,
                        reason: 'Closed',
                      }),
                    )
                  }
                >
                  Close PO
                </Button>
              </>
            )}
            {!['ISSUED', 'PARTIALLY_RECEIVED', 'CLOSED', 'CANCELLED'].includes(selected.status) && (
              <Button color="inherit" onClick={() => void run(() => cancelPurchaseOrder(selected.purchaseOrderId))}>
                Cancel
              </Button>
            )}
          </Stack>
        </Paper>
      )}
    </Box>
  );
}

export default PurchaseOrdersPage;
