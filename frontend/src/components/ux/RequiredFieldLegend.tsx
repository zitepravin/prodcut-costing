import { Typography } from '@mui/material';

/**
 * Standard legend for forms with mandatory fields.
 * Use together with the MUI `required` prop on each mandatory field.
 * Theme styles the asterisk in error.main (red). Optional fields must omit `required`.
 */
export function RequiredFieldLegend({ dense = false }: { dense?: boolean }) {
  return (
    <Typography
      variant="caption"
      color="text.secondary"
      component="p"
      sx={{ mb: dense ? 0.5 : 1.5 }}
      role="note"
    >
      <Typography
        component="span"
        variant="caption"
        sx={{ color: 'error.main', fontWeight: 700, mr: 0.5 }}
        aria-hidden
      >
        *
      </Typography>
      Required field
    </Typography>
  );
}
