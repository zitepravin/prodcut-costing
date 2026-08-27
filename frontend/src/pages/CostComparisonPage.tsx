import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { fetchCostComparison, type CostComparisonWorkspace } from '../api/costComparisonApi';
import { PageHeader } from '../components/ux';

export function CostComparisonPage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [ws, setWs] = useState<CostComparisonWorkspace | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    if (!bomNodeId.trim()) {
      setError('Enter a BOM node id');
      return;
    }
    setError(null);
    try {
      setWs(await fetchCostComparison(bomNodeId.trim()));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load comparison');
    }
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Cost Comparison"
        description="Side-by-side workspace for one BOM node: should-cost scenarios, supplier quotes, tooling amortization, and targets (aggregates existing module outputs)."
      />

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <TextField
              label="BOM Node ID" required
              size="small"
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              sx={{ minWidth: 320 }}
            />
            <Button variant="contained" onClick={load}>
              Compare
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {ws && (
        <>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="caption" color="text.secondary">
                  Lowest should-cost
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {ws.lowestShouldCost ?? '—'}
                </Typography>
              </CardContent>
            </Card>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="caption" color="text.secondary">
                  Lowest accepted quote
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {ws.lowestAcceptedQuoteUnitPrice ?? '—'}
                </Typography>
              </CardContent>
            </Card>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="caption" color="text.secondary">
                  Active target
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {ws.activeTargetAmount ?? '—'}
                </Typography>
              </CardContent>
            </Card>
          </Stack>

          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Should-cost scenarios
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Scenario</TableCell>
                    <TableCell align="right">Total</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Flag</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ws.shouldCostScenarios.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4}>
                        <Typography color="text.secondary">None</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {ws.shouldCostScenarios.map((s) => (
                    <TableRow key={s.buildUpId}>
                      <TableCell>{s.scenarioLabel}</TableCell>
                      <TableCell align="right">{s.totalCost}</TableCell>
                      <TableCell>{s.status ?? '—'}</TableCell>
                      <TableCell>
                        {s.buildUpId === ws.lowestShouldCostBuildUpId && (
                          <Chip size="small" color="success" label="Lowest" />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Supplier quotes
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Supplier</TableCell>
                    <TableCell align="right">Unit price</TableCell>
                    <TableCell>Currency</TableCell>
                    <TableCell>Lead time</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Flag</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ws.supplierQuotes.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6}>
                        <Typography color="text.secondary">None</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {ws.supplierQuotes.map((q) => (
                    <TableRow key={q.quoteId}>
                      <TableCell>{q.supplierId}</TableCell>
                      <TableCell align="right">{q.unitPrice}</TableCell>
                      <TableCell>{q.currencyCode}</TableCell>
                      <TableCell>{q.leadTimeDays ?? '—'}</TableCell>
                      <TableCell>{q.status}</TableCell>
                      <TableCell>
                        {q.quoteId === ws.lowestAcceptedQuoteId && (
                          <Chip size="small" color="success" label="Lowest accepted" />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Tooling (latest calculation)
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Record</TableCell>
                    <TableCell align="right">Amortized / piece</TableCell>
                    <TableCell>In piece price</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ws.toolingCosts.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={3}>
                        <Typography color="text.secondary">None</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {ws.toolingCosts.map((t) => (
                    <TableRow key={t.calculationId}>
                      <TableCell>{t.toolingRecordId}</TableCell>
                      <TableCell align="right">{t.amortizedCostPerPiece}</TableCell>
                      <TableCell>{t.includedInPiecePrice ? 'Yes' : 'No'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Targets
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Amount</TableCell>
                    <TableCell>Currency</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Approval</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ws.targets.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4}>
                        <Typography color="text.secondary">None</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {ws.targets.map((t) => (
                    <TableRow key={t.targetCostId}>
                      <TableCell>{t.targetAmount}</TableCell>
                      <TableCell>{t.currencyCode}</TableCell>
                      <TableCell>{t.status}</TableCell>
                      <TableCell>{t.approvalStatus}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </>
      )}
    </Stack>
  );
}
