import {
  Box,
  CircularProgress,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { useEffect, useState } from 'react';

export type ConfigFieldType = 'text' | 'number' | 'boolean' | 'select';

export interface ConfigEditableRowProps {
  label: string;
  displayValue: string;
  value: string;
  fieldType?: ConfigFieldType;
  selectOptions?: { value: string; label: string }[];
  canEdit: boolean;
  lockedHint?: string;
  disabled?: boolean;
  saving?: boolean;
  onSave: (next: string) => Promise<void> | void;
  validate?: (next: string) => string | null;
}

/** Inline pencil edit for one configuration value. Only the pencil starts edit mode. */
export function ConfigEditableRow({
  label,
  displayValue,
  value,
  fieldType = 'text',
  selectOptions,
  canEdit,
  lockedHint = 'Controlled by System Administrator',
  disabled,
  saving,
  onSave,
  validate,
}: ConfigEditableRowProps) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(value);
  const [localError, setLocalError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!editing) setDraft(value);
  }, [value, editing]);

  async function handleSave() {
    const err = validate?.(draft) ?? null;
    if (err) {
      setLocalError(err);
      return;
    }
    setLocalError(null);
    setBusy(true);
    try {
      await onSave(draft);
      setEditing(false);
    } catch (e) {
      setLocalError(e instanceof Error ? e.message : 'Save failed');
    } finally {
      setBusy(false);
    }
  }

  function handleCancel() {
    setDraft(value);
    setLocalError(null);
    setEditing(false);
  }

  return (
    <Stack
      direction="row"
      spacing={1}
      alignItems="center"
      sx={{ py: 1, borderBottom: 1, borderColor: 'divider', minHeight: 48 }}
    >
      <Typography variant="body2" sx={{ flex: '0 0 220px', fontWeight: 500 }}>
        {label}
      </Typography>

      {!editing ? (
        <>
          <Typography variant="body2" sx={{ flex: 1 }} color="text.primary">
            {displayValue || '—'}
          </Typography>
          {canEdit ? (
            <Tooltip title="Edit">
              <span>
                <IconButton
                  size="small"
                  aria-label={`Edit ${label}`}
                  onClick={() => setEditing(true)}
                  disabled={disabled || saving}
                >
                  <EditOutlinedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
          ) : (
            <Tooltip title={lockedHint}>
              <LockOutlinedIcon fontSize="small" color="disabled" aria-label={`${label} locked`} />
            </Tooltip>
          )}
        </>
      ) : (
        <>
          <Box sx={{ flex: 1, maxWidth: 280 }}>
            {fieldType === 'select' && selectOptions ? (
              <TextField
                select
                size="small"
                fullWidth
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                disabled={busy}
                inputProps={{ 'aria-label': label }}
              >
                {selectOptions.map((o) => (
                  <MenuItem key={o.value} value={o.value}>
                    {o.label}
                  </MenuItem>
                ))}
              </TextField>
            ) : fieldType === 'boolean' ? (
              <TextField
                select
                size="small"
                fullWidth
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                disabled={busy}
                inputProps={{ 'aria-label': label }}
              >
                <MenuItem value="true">Yes</MenuItem>
                <MenuItem value="false">No</MenuItem>
              </TextField>
            ) : (
              <TextField
                size="small"
                fullWidth
                type={fieldType === 'number' ? 'number' : 'text'}
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                disabled={busy}
                error={Boolean(localError)}
                helperText={localError ?? undefined}
                inputProps={{ 'aria-label': label }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void handleSave();
                  if (e.key === 'Escape') handleCancel();
                }}
              />
            )}
            {localError && (fieldType === 'select' || fieldType === 'boolean') && (
              <Typography variant="caption" color="error" display="block">
                {localError}
              </Typography>
            )}
          </Box>
          <Tooltip title="Save">
            <span>
              <IconButton
                size="small"
                color="primary"
                aria-label={`Save ${label}`}
                onClick={() => void handleSave()}
                disabled={busy}
              >
                {busy ? <CircularProgress size={18} /> : <CheckIcon fontSize="small" />}
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Cancel">
            <span>
              <IconButton
                size="small"
                aria-label={`Cancel edit ${label}`}
                onClick={handleCancel}
                disabled={busy}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </>
      )}
    </Stack>
  );
}
