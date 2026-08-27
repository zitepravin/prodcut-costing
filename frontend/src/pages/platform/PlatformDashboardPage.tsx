import { useEffect, useState } from 'react';
import { Alert, Card, CardContent, Grid, Stack, Typography, Button } from '@mui/material';
import * as api from '../../api/platformAccessApi';
import { listAdminUsers, listAdminRoles } from '../../api/adminApi';
import { getErrorMessage } from '../../api/httpClient';

/**
 * Platform Admin dashboard — summary counts from Central Access + admin APIs.
 * Backend remains authoritative.
 */
export function PlatformDashboardPage() {
  const [error, setError] = useState<string | null>(null);
  const [customers, setCustomers] = useState(0);
  const [modules, setModules] = useState(0);
  const [users, setUsers] = useState(0);
  const [roles, setRoles] = useState(0);
  const [activeCustomers, setActiveCustomers] = useState(0);

  async function load() {
    setError(null);
    try {
      const [c, m, u, r] = await Promise.all([
        api.listCustomers(),
        api.listModules(),
        listAdminUsers(),
        listAdminRoles(),
      ]);
      setCustomers(c.length);
      setActiveCustomers(c.filter((x) => x.status === 'ACTIVE').length);
      setModules(m.length);
      setUsers(u.length);
      setRoles(r.length);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const tiles = [
    { label: 'Customers', value: customers, sub: `${activeCustomers} active` },
    { label: 'Modules', value: modules, sub: 'Registry' },
    { label: 'Users', value: users, sub: 'Platform users' },
    { label: 'Roles', value: roles, sub: 'System + custom' },
  ];

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Platform Administration</Typography>
      <Typography variant="body2" color="text.secondary">
        Central Access overview. Module entitlements and membership are managed under Customers.
      </Typography>
      {error && (
        <Alert severity="error" action={<Button onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      <Grid container spacing={2}>
        {tiles.map((t) => (
          <Grid item xs={12} sm={6} md={3} key={t.label}>
            <Card>
              <CardContent>
                <Typography variant="overline" color="text.secondary">
                  {t.label}
                </Typography>
                <Typography variant="h4">{t.value}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {t.sub}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Stack>
  );
}
