import { createTheme } from '@mui/material/styles';

/**
 * Industrial costing product theme — neutral, high-contrast, dense tables.
 *
 * Mandatory fields: any TextField / FormControl / InputLabel with `required`
 * shows a red asterisk (*) after the label. Use the `required` prop on every
 * field the business rules treat as mandatory — do not rely on helper text alone.
 */
export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0d47a1',
      dark: '#002171',
      light: '#5472d3',
    },
    secondary: {
      main: '#455a64',
    },
    error: {
      main: '#c62828',
    },
    background: {
      default: '#f4f6f8',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"IBM Plex Sans", "Roboto", "Helvetica", "Arial", sans-serif',
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
    },
    MuiTableCell: {
      styleOverrides: {
        head: { fontWeight: 600, backgroundColor: '#e8eef5' },
      },
    },
    /** Red * on every required FormLabel (covers TextField labels). */
    MuiFormLabel: {
      styleOverrides: {
        asterisk: {
          color: '#c62828',
          fontWeight: 700,
        },
      },
    },
    /** Red * on InputLabel used with Select / FormControl required. */
    MuiInputLabel: {
      styleOverrides: {
        asterisk: {
          color: '#c62828',
          fontWeight: 700,
        },
      },
    },
    MuiFormControl: {
      defaultProps: {
        // Consumers still set required per field; no global force
      },
    },
  },
});
