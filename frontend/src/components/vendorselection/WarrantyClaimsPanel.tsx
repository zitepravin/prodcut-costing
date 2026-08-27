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
import type { WarrantyClaim, WarrantyClaimStatus } from '../../api/vendorSelectionApi';
import { evidenceChip } from './evidenceChips';

export type WarrantyClaimsPanelProps = {
  claims: WarrantyClaim[];
  claimStatusFilter: string;
  evidenceFilter: string;
  onClaimStatusFilter: (v: string) => void;
  onEvidenceFilter: (v: string) => void;
  onLoad: () => void;
  onVerify: (claimId: string) => void;
  onTransition: (claimId: string, status: WarrantyClaimStatus) => void;
};

/**
 * Presentational warranty claims list + filters (360 View).
 * All mutations stay in the parent page handlers.
 */
export function WarrantyClaimsPanel(props: WarrantyClaimsPanelProps) {
  const {
    claims,
    claimStatusFilter,
    evidenceFilter,
    onClaimStatusFilter,
    onEvidenceFilter,
    onLoad,
    onVerify,
    onTransition,
  } = props;

  return (
    <>
      <Typography variant="subtitle1">Warranty claims ({claims.length})</Typography>
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 1 }}>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Claim status</InputLabel>
          <Select label="Claim status" value={claimStatusFilter} onChange={(e) => onClaimStatusFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="OPEN">OPEN</MenuItem>
            <MenuItem value="ACCEPTED">ACCEPTED</MenuItem>
            <MenuItem value="REJECTED">REJECTED</MenuItem>
            <MenuItem value="CLOSED">CLOSED</MenuItem>
            <MenuItem value="WITHDRAWN">WITHDRAWN</MenuItem>
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
          Load claims
        </Button>
      </Stack>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Date</TableCell>
            <TableCell>Number</TableCell>
            <TableCell>Part</TableCell>
            <TableCell>Cost</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Evidence</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {claims.map((c) => (
            <TableRow key={c.claimId}>
              <TableCell>{c.claimDate}</TableCell>
              <TableCell>{c.claimNumber ?? '—'}</TableCell>
              <TableCell>{c.partNumber ?? '—'}</TableCell>
              <TableCell>{c.claimCost != null ? `${c.claimCost} ${c.currencyCode ?? ''}` : '—'}</TableCell>
              <TableCell>{c.claimStatus}</TableCell>
              <TableCell>{evidenceChip(c.evidenceStatus)}</TableCell>
              <TableCell>
                {c.evidenceStatus === 'PENDING' && (
                  <Button size="small" onClick={() => void onVerify(c.claimId)}>
                    Verify
                  </Button>
                )}
                {c.claimStatus === 'OPEN' && (
                  <>
                    <Button size="small" onClick={() => void onTransition(c.claimId, 'ACCEPTED')}>
                      Accept
                    </Button>
                    <Button size="small" onClick={() => void onTransition(c.claimId, 'REJECTED')}>
                      Reject
                    </Button>
                    <Button size="small" onClick={() => void onTransition(c.claimId, 'WITHDRAWN')}>
                      Withdraw
                    </Button>
                  </>
                )}
                {(c.claimStatus === 'ACCEPTED' || c.claimStatus === 'REJECTED' || c.claimStatus === 'WITHDRAWN') && (
                  <Button size="small" onClick={() => void onTransition(c.claimId, 'CLOSED')}>
                    Close
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {claims.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          No warranty claims recorded
        </Typography>
      )}
    </>
  );
}
