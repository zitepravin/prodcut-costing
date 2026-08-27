import {
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import type { AfterSalesMetric } from '../../api/vendorSelectionApi';
import { evidenceChip, yesNoChip } from './evidenceChips';

export type AfterSalesMetricsPanelProps = {
  metrics: AfterSalesMetric[];
  onVerify: (metricId: string) => void;
};

/** 360 View — after-sales metrics list (separate from warranty). */
export function AfterSalesMetricsPanel({ metrics, onVerify }: AfterSalesMetricsPanelProps) {
  return (
    <>
      <Typography variant="subtitle1" sx={{ mt: 1 }}>
        After-sales metrics ({metrics.length})
      </Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Period</TableCell>
            <TableCell>Response hrs</TableCell>
            <TableCell>SLA met %</TableCell>
            <TableCell>Unresolved</TableCell>
            <TableCell>Emergency</TableCell>
            <TableCell>Evidence</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {metrics.map((m) => (
            <TableRow key={m.metricId}>
              <TableCell>
                {m.periodStart} → {m.periodEnd}
              </TableCell>
              <TableCell>{m.responseTimeHours ?? '—'}</TableCell>
              <TableCell>{m.serviceSlaMetPercent ?? '—'}</TableCell>
              <TableCell>{m.unresolvedComplaints ?? '—'}</TableCell>
              <TableCell>{yesNoChip(m.emergencySupportFlag)}</TableCell>
              <TableCell>{evidenceChip(m.evidenceStatus)}</TableCell>
              <TableCell>
                {m.evidenceStatus === 'PENDING' && (
                  <Button size="small" onClick={() => void onVerify(m.metricId)}>
                    Verify
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {metrics.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          No after-sales metrics recorded
        </Typography>
      )}
    </>
  );
}
