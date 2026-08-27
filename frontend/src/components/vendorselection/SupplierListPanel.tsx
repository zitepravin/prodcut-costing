import {
  Box,
  Button,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Chip,
} from '@mui/material';
import { useMemo, useState } from 'react';
import type { Supplier } from '../../api/rfqApi';

interface SupplierListPanelProps {
  suppliers: Supplier[];
  selectedId: string;
  onSelect: (supplierId: string) => void;
}

/**
 * Searchable supplier list for Vendor Selection landing.
 * Selection drives 360 / TCO / scorecard supplier context.
 */
export function SupplierListPanel({ suppliers, selectedId, onSelect }: SupplierListPanelProps) {
  const [q, setQ] = useState('');

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return suppliers;
    return suppliers.filter(
      (s) =>
        (s.supplierCode ?? '').toLowerCase().includes(term) ||
        (s.supplierName ?? '').toLowerCase().includes(term) ||
        (s.supplierId ?? '').toLowerCase().includes(term),
    );
  }, [suppliers, q]);

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1.5}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
          <Typography variant="subtitle1" sx={{ flex: 1 }}>
            Suppliers ({filtered.length}
            {q.trim() ? ` of ${suppliers.length}` : ''})
          </Typography>
          <TextField
            size="small"
            label="Search code / name"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            sx={{ minWidth: 240 }}
          />
        </Stack>
        <Box sx={{ maxHeight: 220, overflow: 'auto' }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((s) => {
                const selected = s.supplierId === selectedId;
                return (
                  <TableRow key={s.supplierId} selected={selected} hover>
                    <TableCell>{s.supplierCode}</TableCell>
                    <TableCell>{s.supplierName}</TableCell>
                    <TableCell>
                      <Chip size="small" label={s.status ?? '—'} variant="outlined" />
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        variant={selected ? 'contained' : 'outlined'}
                        onClick={() => onSelect(s.supplierId)}
                      >
                        {selected ? 'Selected' : 'Select'}
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
              {filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>
                    <Typography color="text.secondary">
                      {suppliers.length === 0
                        ? 'No suppliers loaded yet — use List suppliers or wait for auto-load.'
                        : 'No suppliers match the search.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </Box>
      </Stack>
    </Paper>
  );
}
