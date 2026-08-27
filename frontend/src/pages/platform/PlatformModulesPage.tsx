import { useEffect, useState } from 'react';
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
  Typography,
} from '@mui/material';
import * as api from '../../api/platformAccessApi';
import { getErrorMessage } from '../../api/httpClient';

/** Platform module registry (read-only catalog). Future modules may appear without activating features. */
export function PlatformModulesPage() {
  const [modules, setModules] = useState<api.PlatformModule[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      setModules(await api.listModules());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Platform Admin — Modules</Typography>
      <Typography variant="body2" color="text.secondary">
        Registry only. Enabling a module for a customer is done under Customers. Purchase / Order Tracking remain future
        modules until product features ship.
      </Typography>
      {error && (
        <Alert severity="error" action={<Button onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      <Card>
        <CardContent>
          {modules.length === 0 ? (
            <Typography color="text.secondary">No modules registered</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {modules.map((m) => (
                  <TableRow key={m.moduleId}>
                    <TableCell>{m.moduleCode}</TableCell>
                    <TableCell>{m.moduleName}</TableCell>
                    <TableCell>{m.description ?? '—'}</TableCell>
                    <TableCell>
                      <Chip size="small" label={m.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
