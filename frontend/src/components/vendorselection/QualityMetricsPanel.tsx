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
import type { QualityMetric } from '../../api/vendorSelectionApi';
import { evidenceChip } from './evidenceChips';

export type QualityMetricsPanelProps = {
  metrics: QualityMetric[];
  evidenceFilter: string;
  onEvidenceFilter: (v: string) => void;
  onLoad: () => void;
  onVerify: (metricId: string) => void;
};

export function QualityMetricsPanel(props: QualityMetricsPanelProps) {
  const { metrics, evidenceFilter, onEvidenceFilter, onLoad, onVerify } = props;

  return (
    <>
      <Typography variant="subtitle1">Quality metrics</Typography>
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
          Load quality metrics
        </Button>
      </Stack>
      {metrics.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Type</TableCell>
              <TableCell>Value</TableCell>
              <TableCell>Period</TableCell>
              <TableCell>Evidence</TableCell>
              <TableCell>Action</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {metrics.map((m) => (
              <TableRow key={m.metricId}>
                <TableCell>{m.metricType}</TableCell>
                <TableCell>
                  {m.metricValue} {m.unit ?? ''}
                </TableCell>
                <TableCell>
                  {m.periodStart} → {m.periodEnd}
                </TableCell>
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
      )}
      {metrics.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          No quality metrics recorded
        </Typography>
      )}
    </>
  );
}
