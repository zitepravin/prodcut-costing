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
import type { GuaranteeEvent } from '../../api/vendorSelectionApi';
import { evidenceChip, yesNoChip } from './evidenceChips';

export type GuaranteeEventsPanelProps = {
  events: GuaranteeEvent[];
  unresolvedOnly: string;
  evidenceFilter: string;
  onUnresolvedOnly: (v: string) => void;
  onEvidenceFilter: (v: string) => void;
  onLoad: () => void;
  onVerify: (eventId: string) => void;
};

export function GuaranteeEventsPanel(props: GuaranteeEventsPanelProps) {
  const { events, unresolvedOnly, evidenceFilter, onUnresolvedOnly, onEvidenceFilter, onLoad, onVerify } = props;

  return (
    <>
      <Typography variant="subtitle1">Guarantee events ({events.length})</Typography>
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 1 }}>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Resolved</InputLabel>
          <Select label="Resolved" value={unresolvedOnly} onChange={(e) => onUnresolvedOnly(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="true">Unresolved only</MenuItem>
            <MenuItem value="false">Resolved only</MenuItem>
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Evidence</InputLabel>
          <Select label="Evidence" value={evidenceFilter} onChange={(e) => onEvidenceFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="PENDING">PENDING</MenuItem>
            <MenuItem value="VERIFIED">VERIFIED</MenuItem>
            <MenuItem value="FAILED">FAILED</MenuItem>
          </Select>
        </FormControl>
        <Button variant="outlined" onClick={() => void onLoad()}>
          Load events
        </Button>
      </Stack>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Date</TableCell>
            <TableCell>Type</TableCell>
            <TableCell>Description</TableCell>
            <TableCell>Penalty</TableCell>
            <TableCell>Resolved</TableCell>
            <TableCell>Evidence</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {events.map((e) => (
            <TableRow key={e.eventId}>
              <TableCell>{e.eventDate}</TableCell>
              <TableCell>{e.eventType}</TableCell>
              <TableCell>{e.description ?? '—'}</TableCell>
              <TableCell>{e.penaltyAmount != null ? `${e.penaltyAmount} ${e.currencyCode ?? ''}` : '—'}</TableCell>
              <TableCell>{yesNoChip(e.resolvedFlag)}</TableCell>
              <TableCell>{evidenceChip(e.evidenceStatus)}</TableCell>
              <TableCell>
                {e.evidenceStatus === 'PENDING' && (
                  <Button size="small" onClick={() => void onVerify(e.eventId)}>
                    Verify
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {events.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          No guarantee events recorded
        </Typography>
      )}
    </>
  );
}
