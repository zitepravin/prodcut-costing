import { useState } from 'react';
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
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import { estimateQuantityStructure, type QuantityStructureEstimate } from '../api/quantityStructureApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

export function QuantityStructurePage() {
  const [bomNodeId, setBomNodeId] = useState('');
  const [variantId, setVariantId] = useState('');
  const [routingId, setRoutingId] = useState('');
  const [lotSize, setLotSize] = useState('');
  const [result, setResult] = useState<QuantityStructureEstimate | null>(null);
  const [error, setError] = useState<string | null>(null);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <AccountTreeOutlinedIcon color="primary" />
        <Typography variant="h5">Quantity-structure costing</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Automatic estimate from BOM node + process routing. Optional costing variant supplies lot size,
        scrap and yield. Uses existing material and process engines — does not invent rates.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      <Card>
        <CardContent>
          <RequiredFieldLegend dense />
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} flexWrap="wrap" useFlexGap>
            <TextField
              size="small"
              label="BOM node ID"
              value={bomNodeId}
              onChange={(e) => setBomNodeId(e.target.value)}
              required
              sx={{ minWidth: 280 }}
            />
            <TextField size="small" label="Costing variant ID" value={variantId} onChange={(e) => setVariantId(e.target.value)} sx={{ minWidth: 280 }} />
            <TextField size="small" label="Routing ID (optional)" value={routingId} onChange={(e) => setRoutingId(e.target.value)} sx={{ minWidth: 280 }} />
            <TextField size="small" label="Lot size override" value={lotSize} onChange={(e) => setLotSize(e.target.value)} />
            <Button
              variant="contained"
              disabled={!bomNodeId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setResult(
                      await estimateQuantityStructure({
                        bomNodeId: bomNodeId.trim(),
                        costingVariantId: variantId.trim() || undefined,
                        routingId: routingId.trim() || undefined,
                        lotSize: lotSize.trim() || undefined,
                      }),
                    );
                    setError(null);
                  } catch (e) {
                    setResult(null);
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Estimate
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {result && (
        <>
          <Card>
            <CardContent>
              <Typography fontWeight={600}>Totals</Typography>
              <Typography variant="body2">
                Material: {result.totalMaterialCost} · Process: {result.totalProcessCost} · QS total:{' '}
                {result.totalQuantityStructureCost}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Lot {result.lotSizeUsed} · scrap {result.scrapPctUsed} · yield {result.yieldPctUsed} · source{' '}
                {result.quantityStructureSource}
                {result.variantCode ? ` · variant ${result.variantCode}` : ''}
              </Typography>
              <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                {result.calculationBasis}
              </Typography>
            </CardContent>
          </Card>
          {result.material && (
            <Card>
              <CardContent>
                <Typography fontWeight={600}>Material</Typography>
                <Typography variant="body2">
                  {result.material.materialCode} · net {result.material.netWeight} · gross{' '}
                  {result.material.grossWeight ?? '—'} · rate {result.material.unitRate ?? '—'} · cost{' '}
                  {result.material.materialCost ?? '—'}
                </Typography>
              </CardContent>
            </Card>
          )}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Seq</TableCell>
                <TableCell>Process</TableCell>
                <TableCell align="right">Machine h</TableCell>
                <TableCell align="right">Labor h</TableCell>
                <TableCell align="right">Run</TableCell>
                <TableCell align="right">Setup/pc</TableCell>
                <TableCell align="right">Total</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {result.operations.map((op) => (
                <TableRow key={op.operationId}>
                  <TableCell>{op.sequenceNumber}</TableCell>
                  <TableCell>{op.processName ?? op.operationId}</TableCell>
                  <TableCell align="right">{op.machineHours}</TableCell>
                  <TableCell align="right">{op.laborHours}</TableCell>
                  <TableCell align="right">{op.runCost}</TableCell>
                  <TableCell align="right">{op.setupCostPerPiece}</TableCell>
                  <TableCell align="right">{op.totalOperationCost}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </>
      )}
    </Stack>
  );
}
