import { useCallback, useEffect, useState } from 'react';
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
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import * as adminApi from '../../api/adminApi';
import { getErrorMessage, httpClient } from '../../api/httpClient';

/**
 * V224 — Full permission matrix UI.
 * Backend authorizes via role codes (@PreAuthorize). Matrix is the explicit catalogue
 * for display and Customer Admin guidance — not a second security boundary.
 */

type PermissionDef = { code: string; group: string; description: string };
type RolePermissionView = {
  roleCode: string;
  description: string;
  grantedPermissions: string[];
  deniedHighlights: string[];
};
type MatrixResponse = { catalogue: PermissionDef[]; roles: RolePermissionView[] };

export function PlatformPermissionsPage() {
  const [roles, setRoles] = useState<adminApi.AdminRole[]>([]);
  const [matrix, setMatrix] = useState<MatrixResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [r, m] = await Promise.all([
        adminApi.listAdminRoles(),
        httpClient
          .get<MatrixResponse>('/api/v1/admin/permission-matrix')
          .then((res) => res.data)
          .catch(async () => {
            // Customer Admin path
            const res = await httpClient.get<MatrixResponse>('/api/v1/customer/permission-matrix');
            return res.data;
          }),
      ]);
      setRoles(r);
      setMatrix(m);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Permission Catalog &amp; Role Matrix</Typography>
      <Typography variant="body2" color="text.secondary">
        Authorization model: User → Roles (role_code) → JWT authorities → backend @PreAuthorize + module
        entitlement. Frontend is not a security boundary. The matrix below is the approved logical
        capability catalogue (V224) used for UI guidance and regression tests.
      </Typography>
      {error && (
        <Alert severity="error" action={<Button onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}

      {matrix && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Role → permission matrix
            </Typography>
            {matrix.roles.map((role) => (
              <Accordion key={role.roleCode} disableGutters>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Chip label={role.roleCode} size="small" color="primary" variant="outlined" />
                    <Typography variant="body2">{role.description}</Typography>
                  </Stack>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="subtitle2" gutterBottom>
                    Granted
                  </Typography>
                  <Stack direction="row" flexWrap="wrap" gap={0.5} mb={1}>
                    {role.grantedPermissions.map((p) => (
                      <Chip key={p} label={p} size="small" color="success" variant="outlined" />
                    ))}
                  </Stack>
                  {role.deniedHighlights.length > 0 && (
                    <>
                      <Typography variant="subtitle2" gutterBottom>
                        Platform-only (not granted)
                      </Typography>
                      <Stack direction="row" flexWrap="wrap" gap={0.5}>
                        {role.deniedHighlights.map((p) => (
                          <Chip key={p} label={p} size="small" color="default" />
                        ))}
                      </Stack>
                    </>
                  )}
                </AccordionDetails>
              </Accordion>
            ))}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Seeded roles (database)
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Role code</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>System</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {roles.map((r) => (
                <TableRow key={r.roleId}>
                  <TableCell>
                    <Chip label={r.roleCode} size="small" />
                  </TableCell>
                  <TableCell>{r.description ?? '—'}</TableCell>
                  <TableCell>{r.systemRole ? 'Yes' : 'No'}</TableCell>
                </TableRow>
              ))}
              {roles.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3}>
                    <Typography color="text.secondary">No roles loaded</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </Stack>
  );
}
