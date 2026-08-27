import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { RequiredFieldLegend } from '../components/ux';
import PrecisionManufacturingOutlinedIcon from '@mui/icons-material/PrecisionManufacturingOutlined';
import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { getErrorMessage } from '../api/httpClient';

export function LoginPage() {
  const { authenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [showMfa, setShowMfa] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (authenticated) {
    return <Navigate to={from} replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password, mfaCode.trim() || undefined);
      navigate(from, { replace: true });
    } catch (err) {
      const message = getErrorMessage(err);
      // Backend uses a generic message for MFA-required failures; surface the MFA field.
      if (/mfa|totp|one-time|second factor/i.test(message) || message.toLowerCase().includes('code')) {
        setShowMfa(true);
      }
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={2} sx={{ mb: 3, alignItems: "center" }}>
            <PrecisionManufacturingOutlinedIcon color="primary" sx={{ fontSize: 40 }} />
            <Typography variant="h5" sx={{ textAlign: "center" }}>
              Product Costing Software
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center" }}>
              Sign in with your enterprise account. MFA code is required when enabled on your user.
            </Typography>
          </Stack>

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <Stack spacing={2}>
              {error && (
                <Alert severity="error" onClose={() => setError(null)}>
                  {error}
                </Alert>
              )}
              <RequiredFieldLegend dense />
              <TextField
                label="Username"
                name="username"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                fullWidth
                autoFocus
              />
              <TextField
                label="Password"
                name="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                fullWidth
              />
              {(showMfa || mfaCode) && (
                <TextField
                  label="MFA code"
                  name="mfaCode"
                  value={mfaCode}
                  onChange={(e) => setMfaCode(e.target.value)}
                  fullWidth
                  slotProps={{ htmlInput: { inputMode: 'numeric', maxLength: 8 } }}
                  helperText="Six-digit TOTP from your authenticator app"
                />
              )}
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={submitting || !username || !password}
                fullWidth
              >
                {submitting ? <CircularProgress size={24} color="inherit" /> : 'Sign in'}
              </Button>
              {!showMfa && (
                <Button type="button" size="small" onClick={() => setShowMfa(true)}>
                  Enter MFA code
                </Button>
              )}
            </Stack>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
