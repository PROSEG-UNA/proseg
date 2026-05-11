import { createTheme } from '@mui/material/styles';

const unaRed = '#C41E3A';
const unaBlue = '#0047AB';
const unaGray = '#999999';
const black = '#000000';
const white = '#FFFFFF';

export const theme = createTheme({
  palette: {
    primary: {
      main: unaRed,
      light: 'rgba(196, 30, 58, 0.1)',
      dark: '#A01A2E',
    },
    secondary: {
      main: unaBlue,
      light: 'rgba(0, 71, 171, 0.1)',
      dark: '#003580',
    },
    warning: {
      main: unaGray,
    },
    text: {
      primary: black,
      secondary: unaGray,
    },
    background: {
      default: '#f5f5f5',
      paper: white,
    },
  },
  typography: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "Oxygen", "Ubuntu", "Cantarell", "Fira Sans", "Droid Sans", "Helvetica Neue", sans-serif',
    h1: {
      color: unaRed,
      fontWeight: 600,
    },
    h2: {
      color: unaRed,
      fontWeight: 600,
    },
    h3: {
      color: unaRed,
      fontWeight: 600,
    },
    h4: {
      color: unaRed,
      fontWeight: 600,
    },
    h5: {
      color: black,
      fontWeight: 500,
    },
    h6: {
      color: black,
      fontWeight: 500,
    },
  },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: unaRed,
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        contained: {
          backgroundColor: unaRed,
          '&:hover': {
            backgroundColor: '#A01A2E',
          },
        },
        outlined: {
          borderColor: unaRed,
          color: unaRed,
          '&:hover': {
            backgroundColor: 'rgba(196, 30, 58, 0.05)',
            borderColor: '#A01A2E',
          },
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            '&:hover fieldset': {
              borderColor: unaRed,
            },
            '&.Mui-focused fieldset': {
              borderColor: unaRed,
            },
          },
          '& .MuiOutlinedInput-input': {
            '&::placeholder': {
              color: unaGray,
              opacity: 1,
            },
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderTop: `5px solid ${unaRed}`,
          boxShadow: `0 8px 32px rgba(196, 30, 58, 0.1)`,
        },
      },
    },
    MuiTable: {
      styleOverrides: {
        root: {
          '& thead th': {
            backgroundColor: '#f5f5f5',
            color: black,
            fontWeight: 600,
            borderBottom: `2px solid ${unaRed}`,
          },
          '& tbody tr:hover': {
            backgroundColor: 'rgba(196, 30, 58, 0.05)',
          },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        colorPrimary: {
          color: unaRed,
          '&:hover': {
            backgroundColor: 'rgba(196, 30, 58, 0.1)',
          },
        },
        colorError: {
          color: '#D32F2F',
          '&:hover': {
            backgroundColor: 'rgba(211, 47, 47, 0.1)',
          },
        },
      },
    },
  },
});

export default theme;
