import {
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import type { DeliveryMetric } from '../../api/vendorSelectionApi';
import { evidenceChip } from './evidenceChips';

export type DeliveryMetricsPanelProps = {
  metrics: DeliveryMetric[];
  evidenceFilter: string;
  onEvidenceFilter: (v: string) => void;
  onLoad: () => void;
  onVerify: (metricId: string) => void;
};

export function DeliveryMetricsPanel(props: DeliveryMetricsPanelProps) {
  const { metrics, evidenceFilter, onEvidenceFilter, onLoad, onVerify } = props;

  return (
    <>
      <Typography variant="subtitle1" sx={{ mt: 1 }}>
        Delivery metrics
      </Typography>
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 1 }}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>Evidence</InputLabel>
          <Select label="Evidence" value={evidenceFilter} onChange={(e) => onEvidenceFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="PENDING">PENDING</MenuItem>
            <MenuItem value="VERIFIED">VERIFIED</MenuItem>
            <MenuItem value="FAILED">FAILED</MenuItem>
            <MenuItem value="EXPIRED">EXPIRED</MenuItem>
          </Select>
        </FormControl>
        <Button variant="outlined" onClick={() => void onLoad()}>
          Load delivery metrics
        </Button>
      </Stack>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>OTD %</TableCell>
            <TableCell>OTIF %</TableCell>
            <TableCell>Late count</TableCell>
            <TableCell>Evidence</TableCell>
            <TableCell>Action</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {metrics.map((m) => (
            <TableRow key={m.metricId}>
              <TableCell>{m.otdPercent ?? '—'}</TableCell>
              <TableCell>{m.otifPercent ?? '—'}</TableCell>
              <TableCell>{m.lateDeliveryCount ?? '—'}</TableCell>
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
          No delivery metrics recorded
        </Typography>
      )}
    </>
  );
}
