import { useState } from 'react';
import { Alert, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import PublicOutlinedIcon from '@mui/icons-material/PublicOutlined';
import { ftaAnalyze } from '../api/ftaApi';
import { getErrorMessage } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';

/** Visible only when route is reachable — backend still enforces FTA_TRADE_AGREEMENT. */
export function FtaPage() {
  const { hasRole } = useAuth();
  const can = hasRole('COST_ANALYST', 'COST_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR');
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<Record<string, unknown> | null>(null);
  const [code, setCode] = useState('AITIGA');
  const [hs, setHs] = useState('7208.51');
  const [origin, setOrigin] = useState('VN');
  const [exportC, setExportC] = useState('VN');
  const [dest, setDest] = useState('IN');

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1} alignItems="center">
        <PublicOutlinedIcon color="primary" />
        <Typography variant="h5">FTA / Trade Agreement (optional)</Typography>
      </Stack>
      <Alert severity="info">
        Preferential analysis only. Normal Import Costing works independently when this feature is OFF.
        Origin country is not the same as export country. No fabricated production tariff rates in UI.
      </Alert>
      {error && <Alert severity="error">{error}</Alert>}
      <Card><CardContent>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <TextField size="small" label="Agreement code" value={code} onChange={(e) => setCode(e.target.value)} />
          <TextField size="small" label="HS" value={hs} onChange={(e) => setHs(e.target.value)} />
          <TextField size="small" label="Origin" value={origin} onChange={(e) => setOrigin(e.target.value)} sx={{ width: 80 }} />
          <TextField size="small" label="Export" value={exportC} onChange={(e) => setExportC(e.target.value)} sx={{ width: 80 }} />
          <TextField size="small" label="Dest" value={dest} onChange={(e) => setDest(e.target.value)} sx={{ width: 80 }} />
          <Button variant="contained" disabled={!can} onClick={() => {
            setError(null);
            void ftaAnalyze({
              agreementCode: code, hsCode: hs, originCountry: origin, exportCountry: exportC,
              destinationCountry: dest, assessmentDate: new Date().toISOString().slice(0, 10),
              customsValue: 1000, quantity: 10, fob: 100, vnm: 30, proofVerified: true,
              finishedHs: hs, inputHs: hs, thirdCountryMaterial: false, substantialTransformation: true,
            }).then(setResult).catch((e) => setError(getErrorMessage(e)));
          }}>Analyze</Button>
        </Stack>
        {result && <Typography sx={{ mt: 2 }} component="pre" variant="body2">{JSON.stringify(result, null, 2)}</Typography>}
      </CardContent></Card>
    </Stack>
  );
}
