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
import type { PerformanceFeedback, PerformanceFeedbackListItem } from '../../api/vendorSelectionApi';
import { evidenceChip, yesNoChip } from './evidenceChips';

/** Accept full feedback or 360 list item shape. */
export type PerformanceFeedbackRow = PerformanceFeedback | PerformanceFeedbackListItem;

export type PerformanceFeedbackPanelProps = {
  items: PerformanceFeedbackRow[];
  evidenceFilter: string;
  appliedFilter: string;
  onEvidenceFilter: (v: string) => void;
  onAppliedFilter: (v: string) => void;
  onLoad: () => void;
  onVerify: (feedbackId: string) => void;
  onApplyToHistory: (feedbackId: string) => void;
};

/**
 * 360 View — performance feedback list.
 * Apply-to-history is explicit only; never auto-mutates scorecards.
 */
export function PerformanceFeedbackPanel(props: PerformanceFeedbackPanelProps) {
  const {
    items,
    evidenceFilter,
    appliedFilter,
    onEvidenceFilter,
    onAppliedFilter,
    onLoad,
    onVerify,
    onApplyToHistory,
  } = props;

  return (
    <>
      <Typography variant="subtitle1" sx={{ mt: 1 }}>
        Performance feedback ({items.length})
      </Typography>
      <Typography variant="caption" color="text.secondary" display="block">
        C3 intake only — apply to history does not auto-change scorecards.
      </Typography>
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 1 }}>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Evidence</InputLabel>
          <Select label="Evidence" value={evidenceFilter} onChange={(e) => onEvidenceFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="PENDING">PENDING</MenuItem>
            <MenuItem value="VERIFIED">VERIFIED</MenuItem>
            <MenuItem value="FAILED">FAILED</MenuItem>
            <MenuItem value="WAIVED">WAIVED</MenuItem>
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Applied</InputLabel>
          <Select label="Applied" value={appliedFilter} onChange={(e) => onAppliedFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="true">Applied only</MenuItem>
            <MenuItem value="false">Not applied</MenuItem>
          </Select>
        </FormControl>
        <Button variant="outlined" onClick={() => void onLoad()}>
          Load feedback
        </Button>
      </Stack>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Date</TableCell>
            <TableCell>Type</TableCell>
            <TableCell>Metric</TableCell>
            <TableCell>Value</TableCell>
            <TableCell>Evidence</TableCell>
            <TableCell>Applied</TableCell>
            <TableCell>History metric</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {items.map((f) => (
            <TableRow key={f.feedbackId}>
              <TableCell>{f.feedbackDate}</TableCell>
              <TableCell>{f.feedbackType}</TableCell>
              <TableCell>{f.metricCode ?? '—'}</TableCell>
              <TableCell>{f.metricValue != null ? `${f.metricValue} ${f.unit ?? ''}` : '—'}</TableCell>
              <TableCell>{evidenceChip(f.evidenceStatus)}</TableCell>
              <TableCell>{yesNoChip(f.appliedToHistory)}</TableCell>
              <TableCell>
                {f.historyMetricKind
                  ? `${f.historyMetricKind}${f.historyMetricId ? ` · ${f.historyMetricId.slice(0, 8)}…` : ''}`
                  : '—'}
              </TableCell>
              <TableCell>
                <Stack direction="row" spacing={0.5}>
                  {f.evidenceStatus === 'PENDING' && (
                    <Button size="small" onClick={() => void onVerify(f.feedbackId)}>
                      Verify
                    </Button>
                  )}
                  {!f.appliedToHistory &&
                    (f.evidenceStatus === 'VERIFIED' || f.evidenceStatus === 'WAIVED') && (
                      <Button size="small" onClick={() => void onApplyToHistory(f.feedbackId)}>
                        Apply history
                      </Button>
                    )}
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {items.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          No performance feedback recorded
        </Typography>
      )}
    </>
  );
}
