import { useCallback, useEffect, useState } from 'react';
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
import AccountBalanceOutlinedIcon from '@mui/icons-material/AccountBalanceOutlined';
import {
  getComponentHierarchy,
  getCostedBom,
  getItemization,
  materializeItemization,
} from '../api/costStructureApi';
import { getErrorMessage } from '../api/httpClient';
import { RequiredFieldLegend } from '../components/ux';

type CompNode = {
  componentCode: string;
  displayName: string;
  costBehavior?: string;
  children?: CompNode[];
};

function CompTree({ nodes, depth = 0 }: { nodes: CompNode[]; depth?: number }) {
  return (
    <>
      {nodes.map((n) => (
        <div key={n.componentCode} style={{ marginLeft: depth * 16 }}>
          <Typography variant="body2">
            {n.displayName} ({n.componentCode})
            {n.costBehavior ? ` · ${n.costBehavior}` : ''}
          </Typography>
          {n.children && n.children.length > 0 && <CompTree nodes={n.children} depth={depth + 1} />}
        </div>
      ))}
    </>
  );
}

export function CostStructurePage() {
  const [hierarchy, setHierarchy] = useState<CompNode[]>([]);
  const [rootId, setRootId] = useState('');
  const [buildUpId, setBuildUpId] = useState('');
  const [costed, setCosted] = useState<Record<string, unknown> | null>(null);
  const [items, setItems] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadHierarchy = useCallback(async () => {
    try {
      const h = await getComponentHierarchy();
      setHierarchy((h.roots as CompNode[]) || []);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void loadHierarchy();
  }, [loadHierarchy]);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <AccountBalanceOutlinedIcon color="primary" />
        <Typography variant="h5">Cost structure (A3–A5)</Typography>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Hierarchical cost components (A3), multilevel costed BOM (A4), and should-cost itemization (A5).
        Backend remains the financial source of truth.
      </Typography>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            A3 — Component hierarchy
          </Typography>
          {hierarchy.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No components loaded
            </Typography>
          ) : (
            <CompTree nodes={hierarchy} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            A4 — Multilevel costed BOM
          </Typography>
          <RequiredFieldLegend dense />
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Root BOM node ID" value={rootId} onChange={(e) => setRootId(e.target.value)} required sx={{ minWidth: 280 }} />
            <Button
              variant="contained"
              disabled={!rootId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setCosted(await getCostedBom(rootId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Load costed tree
            </Button>
          </Stack>
          {costed && (
            <Typography variant="body2" sx={{ mt: 2 }}>
              Total rolled: {String(costed.totalRolledCost)} · source tree available in response
            </Typography>
          )}
          {costed?.tree != null && (
            <Typography variant="caption" component="pre" sx={{ display: 'block', mt: 1, whiteSpace: 'pre-wrap' }}>
              {JSON.stringify(costed.tree as object, null, 2).slice(0, 4000)}
            </Typography>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography fontWeight={600} gutterBottom>
            A5 — Itemization
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <TextField size="small" label="Should-cost build-up ID" value={buildUpId} onChange={(e) => setBuildUpId(e.target.value)} required sx={{ minWidth: 280 }} />
            <Button
              variant="contained"
              disabled={!buildUpId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setItems(await materializeItemization(buildUpId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Materialize
            </Button>
            <Button
              variant="outlined"
              disabled={!buildUpId.trim()}
              onClick={() => {
                void (async () => {
                  try {
                    setItems(await getItemization('SHOULD_COST', buildUpId.trim()));
                    setError(null);
                  } catch (e) {
                    setError(getErrorMessage(e));
                  }
                })();
              }}
            >
              Load
            </Button>
          </Stack>
          {items && Array.isArray(items.lines) && (
            <>
              <Typography variant="body2" sx={{ mt: 1 }}>
                Total {String(items.totalAmount)} · Fixed {String(items.totalFixed)} · Variable{' '}
                {String(items.totalVariable)}
              </Typography>
              <Table size="small" sx={{ mt: 1 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Line</TableCell>
                    <TableCell>Component</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Rate</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Source</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(items.lines as Array<Record<string, unknown>>).map((ln) => (
                    <TableRow key={String(ln.itemId || ln.lineNo)}>
                      <TableCell>{String(ln.lineNo)}</TableCell>
                      <TableCell>{String(ln.componentCode)}</TableCell>
                      <TableCell align="right">{String(ln.quantity)}</TableCell>
                      <TableCell align="right">{String(ln.rate)}</TableCell>
                      <TableCell align="right">{String(ln.amount)}</TableCell>
                      <TableCell>{String(ln.priceSource ?? '')}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
