import { useEffect, useState } from 'react';
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
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import { RequiredFieldLegend } from '../components/ux';
import {
  addRfqLine,
  cancelRfq,
  closeRfq,
  createRfq,
  createSupplier,
  getRfq,
  listQuotes,
  fetchQuoteShouldCostComparison,
  type QuoteShouldCostComparison,
  listSuppliers,
  openRfq,
  submitQuote,
  updateQuoteStatus,
  type QuoteStatus,
  type RfqDetail,
  type RfqEvent,
  type Supplier,
  type SupplierQuote,
} from '../api/rfqApi';

function statusColor(
  status: string,
): 'default' | 'success' | 'warning' | 'error' | 'info' | 'primary' {
  switch (status) {
    case 'ACTIVE':
    case 'OPEN':
    case 'ACCEPTED':
    case 'AWARDED':
      return 'success';
    case 'DRAFT':
    case 'SUBMITTED':
    case 'UNDER_REVIEW':
      return 'info';
    case 'CLOSED':
    case 'INACTIVE':
    case 'WITHDRAWN':
      return 'default';
    case 'CANCELLED':
    case 'REJECTED':
    case 'BLOCKED':
      return 'error';
    default:
      return 'default';
  }
}

export function RfqPage() {
  const [tab, setTab] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [rfqId, setRfqId] = useState('');
  const [detail, setDetail] = useState<RfqDetail | null>(null);
  const [quotes, setQuotes] = useState<SupplierQuote[]>([]);
  const [quoteComparison, setQuoteComparison] = useState<QuoteShouldCostComparison | null>(null);

  const [supplierOpen, setSupplierOpen] = useState(false);
  const [rfqOpen, setRfqOpen] = useState(false);
  const [lineOpen, setLineOpen] = useState(false);
  const [quoteOpen, setQuoteOpen] = useState(false);

  const [supplierForm, setSupplierForm] = useState({
    supplierCode: '',
    supplierName: '',
    countryCode: '',
    currencyCode: 'USD',
  });
  const [rfqForm, setRfqForm] = useState({
    programId: '',
    rfqNumber: '',
    title: '',
    dueDate: '',
    currencyCode: 'USD',
  });
  const [lineForm, setLineForm] = useState({
    bomNodeId: '',
    quantity: '1',
    uom: 'EA',
    targetUnitPrice: '',
  });
  const [quoteForm, setQuoteForm] = useState({
    rfqLineId: '',
    supplierId: '',
    unitPrice: '',
    currencyCode: 'USD',
    leadTimeDays: '',
    toolingLumpSum: '',
  });

  async function loadSuppliers() {
    setError(null);
    try {
      setSuppliers(await listSuppliers());
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load suppliers');
    }
  }

  async function loadRfq(id?: string) {
    const target = (id ?? rfqId).trim();
    if (!target) {
      setError('Enter an RFQ id');
      return;
    }
    setError(null);
    try {
      const d = await getRfq(target);
      setDetail(d);
      setRfqId(d.rfq.rfqId);
      const q = await listQuotes(d.rfq.rfqId).catch(() => [] as SupplierQuote[]);
      setQuotes(q);
      try {
        setQuoteComparison(await fetchQuoteShouldCostComparison(d.rfq.rfqId));
      } catch {
        setQuoteComparison(null);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load RFQ');
    }
  }

  useEffect(() => {
    void loadSuppliers();
  }, []);

  async function handleCreateSupplier() {
    setError(null);
    try {
      await createSupplier({
        supplierCode: supplierForm.supplierCode.trim(),
        supplierName: supplierForm.supplierName.trim(),
        countryCode: supplierForm.countryCode || undefined,
        currencyCode: supplierForm.currencyCode || undefined,
      });
      setSupplierOpen(false);
      setSupplierForm({ supplierCode: '', supplierName: '', countryCode: '', currencyCode: 'USD' });
      setInfo('Supplier created');
      await loadSuppliers();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create supplier failed');
    }
  }

  async function handleCreateRfq() {
    setError(null);
    try {
      const created: RfqEvent = await createRfq({
        programId: rfqForm.programId.trim(),
        rfqNumber: rfqForm.rfqNumber.trim(),
        title: rfqForm.title.trim(),
        dueDate: rfqForm.dueDate || undefined,
        currencyCode: rfqForm.currencyCode || undefined,
      });
      setRfqOpen(false);
      setInfo(`RFQ ${created.rfqNumber} created (DRAFT)`);
      setRfqId(created.rfqId);
      await loadRfq(created.rfqId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Create RFQ failed');
    }
  }

  async function handleAddLine() {
    if (!detail) return;
    setError(null);
    try {
      await addRfqLine(detail.rfq.rfqId, {
        bomNodeId: lineForm.bomNodeId.trim(),
        quantity: lineForm.quantity,
        uom: lineForm.uom || undefined,
        targetUnitPrice: lineForm.targetUnitPrice || undefined,
      });
      setLineOpen(false);
      setLineForm({ bomNodeId: '', quantity: '1', uom: 'EA', targetUnitPrice: '' });
      setInfo('Line added');
      await loadRfq(detail.rfq.rfqId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Add line failed');
    }
  }

  async function handleSubmitQuote() {
    if (!detail) return;
    setError(null);
    try {
      await submitQuote(detail.rfq.rfqId, {
        rfqLineId: quoteForm.rfqLineId,
        supplierId: quoteForm.supplierId,
        unitPrice: quoteForm.unitPrice,
        currencyCode: quoteForm.currencyCode || undefined,
        leadTimeDays: quoteForm.leadTimeDays ? Number(quoteForm.leadTimeDays) : undefined,
        toolingLumpSum: quoteForm.toolingLumpSum || undefined,
      });
      setQuoteOpen(false);
      setInfo('Quote submitted');
      await loadRfq(detail.rfq.rfqId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Submit quote failed');
    }
  }

  async function handleQuoteStatus(quoteId: string, status: QuoteStatus) {
    setError(null);
    try {
      await updateQuoteStatus(quoteId, { status });
      setInfo(`Quote set to ${status}`);
      if (detail) await loadRfq(detail.rfq.rfqId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Update quote status failed');
    }
  }

  async function handleRfqAction(action: 'open' | 'close' | 'cancel') {
    if (!detail) return;
    setError(null);
    try {
      const fn = action === 'open' ? openRfq : action === 'close' ? closeRfq : cancelRfq;
      await fn(detail.rfq.rfqId);
      setInfo(`RFQ ${action}ed`);
      await loadRfq(detail.rfq.rfqId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : `RFQ ${action} failed`);
    }
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <RequestQuoteOutlinedIcon color="primary" />
        <Typography variant="h5" fontWeight={700}>
          RFQ & Supplier Costing
        </Typography>
      </Box>
      <Typography color="text.secondary">
        M09 RFQ events and M10 supplier quotes. Accepted quotes drive BUY leaf unit cost in BOM
        roll-up. Quote lists respect SEC-04 confidentiality walls when configured.
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

      <Tabs value={tab} onChange={(_, v) => setTab(v)}>
        <Tab label="RFQ events" />
        <Tab label="Suppliers" />
      </Tabs>

      {tab === 0 && (
        <Stack spacing={2}>
          <Card>
            <CardContent>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
                <TextField
                  label="RFQ ID"
                  size="small"
                  value={rfqId}
                  onChange={(e) => setRfqId(e.target.value)}
                  sx={{ minWidth: 320 }}
                />
                <Button variant="contained" onClick={() => loadRfq()}>
                  Load
                </Button>
                <Button variant="outlined" onClick={() => setRfqOpen(true)}>
                  New RFQ
                </Button>
              </Stack>
            </CardContent>
          </Card>

          {detail && (
            <>
              <Card>
                <CardContent>
                  <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    justifyContent="space-between"
                    spacing={2}
                  >
                    <Box>
                      <Typography variant="h6" fontWeight={600}>
                        {detail.rfq.rfqNumber} — {detail.rfq.title}
                      </Typography>
                      <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap">
                        <Chip size="small" label={detail.rfq.status} color={statusColor(detail.rfq.status)} />
                        <Chip size="small" label={detail.rfq.currencyCode} variant="outlined" />
                        <Chip size="small" label={`Program ${detail.rfq.programId}`} variant="outlined" />
                        {detail.rfq.dueDate && (
                          <Chip size="small" label={`Due ${detail.rfq.dueDate}`} variant="outlined" />
                        )}
                      </Stack>
                    </Box>
                    <Stack direction="row" spacing={1} flexWrap="wrap">
                      {detail.rfq.status === 'DRAFT' && (
                        <Button size="small" variant="contained" onClick={() => handleRfqAction('open')}>
                          Open
                        </Button>
                      )}
                      {(detail.rfq.status === 'OPEN' || detail.rfq.status === 'DRAFT') && (
                        <Button size="small" onClick={() => handleRfqAction('close')}>
                          Close
                        </Button>
                      )}
                      {detail.rfq.status !== 'CANCELLED' && detail.rfq.status !== 'AWARDED' && (
                        <Button size="small" color="error" onClick={() => handleRfqAction('cancel')}>
                          Cancel
                        </Button>
                      )}
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => setLineOpen(true)}
                        disabled={detail.rfq.status === 'CANCELLED'}
                      >
                        Add line
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => {
                          setQuoteForm({
                            rfqLineId: detail.lines[0]?.rfqLineId ?? '',
                            supplierId: suppliers[0]?.supplierId ?? '',
                            unitPrice: '',
                            currencyCode: detail.rfq.currencyCode || 'USD',
                            leadTimeDays: '',
                            toolingLumpSum: '',
                          });
                          setQuoteOpen(true);
                        }}
                        disabled={detail.rfq.status !== 'OPEN' || detail.lines.length === 0}
                      >
                        Submit quote
                      </Button>
                    </Stack>
                  </Stack>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                    Lines
                  </Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>#</TableCell>
                        <TableCell>BOM node</TableCell>
                        <TableCell align="right">Qty</TableCell>
                        <TableCell>UOM</TableCell>
                        <TableCell align="right">Target unit</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {detail.lines.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={5}>
                            <Typography color="text.secondary">No lines.</Typography>
                          </TableCell>
                        </TableRow>
                      )}
                      {detail.lines.map((l) => (
                        <TableRow key={l.rfqLineId}>
                          <TableCell>{l.lineNumber}</TableCell>
                          <TableCell>{l.bomNodeId}</TableCell>
                          <TableCell align="right">{l.quantity}</TableCell>
                          <TableCell>{l.uom ?? '—'}</TableCell>
                          <TableCell align="right">{l.targetUnitPrice ?? '—'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                    Quotes
                  </Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Supplier</TableCell>
                        <TableCell>BOM node</TableCell>
                        <TableCell align="right">Unit price</TableCell>
                        <TableCell>Currency</TableCell>
                        <TableCell>Lead time</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {quotes.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={7}>
                            <Typography color="text.secondary">No quotes.</Typography>
                          </TableCell>
                        </TableRow>
                      )}
                      {quotes.map((q) => (
                        <TableRow key={q.quoteId}>
                          <TableCell>{q.supplierId}</TableCell>
                          <TableCell>{q.bomNodeId}</TableCell>
                          <TableCell align="right">
                            <Typography fontWeight={600}>{q.unitPrice}</Typography>
                          </TableCell>
                          <TableCell>{q.currencyCode}</TableCell>
                          <TableCell>{q.leadTimeDays ?? '—'}</TableCell>
                          <TableCell>
                            <Chip size="small" label={q.status} color={statusColor(q.status)} />
                          </TableCell>
                          <TableCell align="right">
                            {q.status !== 'ACCEPTED' && q.status !== 'REJECTED' && (
                              <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                                <Button size="small" onClick={() => handleQuoteStatus(q.quoteId, 'ACCEPTED')}>
                                  Accept
                                </Button>
                                <Button
                                  size="small"
                                  color="error"
                                  onClick={() => handleQuoteStatus(q.quoteId, 'REJECTED')}
                                >
                                  Reject
                                </Button>
                              </Stack>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <Box mt={2}>
                    <Typography variant="subtitle2" gutterBottom>
                      Quote vs Should-Cost (buyer analysis)
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                      Variance = quote − should-cost total. DATA NOT AVAILABLE when should-cost missing. Lowest price ≠ best supplier.
                    </Typography>
                    {quoteComparison?.rows?.length ? (
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Supplier</TableCell>
                            <TableCell>Quote</TableCell>
                            <TableCell>Should-cost</TableCell>
                            <TableCell>Variance</TableCell>
                            <TableCell>Status</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {quoteComparison.rows.map((r) => (
                            <TableRow key={r.quoteId}>
                              <TableCell>{r.supplierId}</TableCell>
                              <TableCell>
                                {r.quoteUnitPrice != null ? `${r.quoteUnitPrice} ${r.quoteCurrency ?? ''}` : '—'}
                              </TableCell>
                              <TableCell>
                                {r.shouldCostTotal != null ? String(r.shouldCostTotal) : 'DATA NOT AVAILABLE'}
                              </TableCell>
                              <TableCell>
                                {r.comparisonStatus === 'AVAILABLE' && r.varianceAmount != null
                                  ? String(r.varianceAmount)
                                  : 'DATA NOT AVAILABLE'}
                              </TableCell>
                              <TableCell>{r.varianceNote ?? r.comparisonStatus}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        Load an RFQ with quotes to see comparison.
                      </Typography>
                    )}
                  </Box>

                </CardContent>
              </Card>
            </>
          )}
        </Stack>
      )}

      {tab === 1 && (
        <Card>
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
              <Typography variant="subtitle1" fontWeight={600}>
                Active suppliers
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" onClick={() => loadSuppliers()}>
                  Refresh
                </Button>
                <Button size="small" variant="contained" onClick={() => setSupplierOpen(true)}>
                  New supplier
                </Button>
              </Stack>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Country</TableCell>
                  <TableCell>Currency</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {suppliers.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography color="text.secondary">No active suppliers.</Typography>
                    </TableCell>
                  </TableRow>
                )}
                {suppliers.map((s) => (
                  <TableRow key={s.supplierId}>
                    <TableCell>{s.supplierCode}</TableCell>
                    <TableCell>{s.supplierName}</TableCell>
                    <TableCell>{s.countryCode ?? '—'}</TableCell>
                    <TableCell>{s.currencyCode}</TableCell>
                    <TableCell>
                      <Chip size="small" label={s.status} color={statusColor(s.status)} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Dialog open={supplierOpen} onClose={() => setSupplierOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New supplier</DialogTitle>
        <DialogContent>
          <RequiredFieldLegend dense />
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Supplier code"
              value={supplierForm.supplierCode}
              onChange={(e) => setSupplierForm({ ...supplierForm, supplierCode: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Name"
              value={supplierForm.supplierName}
              onChange={(e) => setSupplierForm({ ...supplierForm, supplierName: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Country code"
              value={supplierForm.countryCode}
              onChange={(e) => setSupplierForm({ ...supplierForm, countryCode: e.target.value })}
              fullWidth
            />
            <TextField
              label="Currency"
              value={supplierForm.currencyCode}
              onChange={(e) => setSupplierForm({ ...supplierForm, currencyCode: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSupplierOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateSupplier}
            disabled={!supplierForm.supplierCode.trim() || !supplierForm.supplierName.trim()}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={rfqOpen} onClose={() => setRfqOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New RFQ</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Program ID"
              value={rfqForm.programId}
              onChange={(e) => setRfqForm({ ...rfqForm, programId: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="RFQ number"
              value={rfqForm.rfqNumber}
              onChange={(e) => setRfqForm({ ...rfqForm, rfqNumber: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Title"
              value={rfqForm.title}
              onChange={(e) => setRfqForm({ ...rfqForm, title: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Due date"
              type="date"
              value={rfqForm.dueDate}
              onChange={(e) => setRfqForm({ ...rfqForm, dueDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              label="Currency"
              value={rfqForm.currencyCode}
              onChange={(e) => setRfqForm({ ...rfqForm, currencyCode: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRfqOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateRfq}
            disabled={!rfqForm.programId.trim() || !rfqForm.rfqNumber.trim() || !rfqForm.title.trim()}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={lineOpen} onClose={() => setLineOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add RFQ line</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="BOM node ID"
              value={lineForm.bomNodeId}
              onChange={(e) => setLineForm({ ...lineForm, bomNodeId: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Quantity"
              value={lineForm.quantity}
              onChange={(e) => setLineForm({ ...lineForm, quantity: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="UOM"
              value={lineForm.uom}
              onChange={(e) => setLineForm({ ...lineForm, uom: e.target.value })}
              fullWidth
            />
            <TextField
              label="Target unit price"
              value={lineForm.targetUnitPrice}
              onChange={(e) => setLineForm({ ...lineForm, targetUnitPrice: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLineOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleAddLine}
            disabled={!lineForm.bomNodeId.trim() || !lineForm.quantity.trim()}
          >
            Add
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={quoteOpen} onClose={() => setQuoteOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Submit supplier quote</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              label="RFQ line"
              value={quoteForm.rfqLineId}
              onChange={(e) => setQuoteForm({ ...quoteForm, rfqLineId: e.target.value })}
              fullWidth
            >
              {(detail?.lines ?? []).map((l) => (
                <MenuItem key={l.rfqLineId} value={l.rfqLineId}>
                  #{l.lineNumber} — {l.bomNodeId}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Supplier"
              value={quoteForm.supplierId}
              onChange={(e) => setQuoteForm({ ...quoteForm, supplierId: e.target.value })}
              fullWidth
            >
              {suppliers.map((s) => (
                <MenuItem key={s.supplierId} value={s.supplierId}>
                  {s.supplierCode} — {s.supplierName}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Unit price"
              value={quoteForm.unitPrice}
              onChange={(e) => setQuoteForm({ ...quoteForm, unitPrice: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Currency"
              value={quoteForm.currencyCode}
              onChange={(e) => setQuoteForm({ ...quoteForm, currencyCode: e.target.value })}
              fullWidth
            />
            <TextField
              label="Lead time (days)"
              value={quoteForm.leadTimeDays}
              onChange={(e) => setQuoteForm({ ...quoteForm, leadTimeDays: e.target.value })}
              fullWidth
            />
            <TextField
              label="Tooling lump sum"
              value={quoteForm.toolingLumpSum}
              onChange={(e) => setQuoteForm({ ...quoteForm, toolingLumpSum: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setQuoteOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSubmitQuote}
            disabled={!quoteForm.rfqLineId || !quoteForm.supplierId || !quoteForm.unitPrice.trim()}
          >
            Submit
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
