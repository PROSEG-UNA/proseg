import { createTheme, alpha } from '@mui/material/styles';
import { brand, gray, green, orange, red } from './primitives';

const defaultTheme = createTheme();

const customShadows = [...defaultTheme.shadows];

export { brand, gray, green, orange, red };

export const getDesignTokens = (mode) => {
  customShadows[1] =
      mode === 'dark'
          ? 'hsla(220, 30%, 5%, 0.7) 0px 4px 16px 0px, hsla(220, 25%, 10%, 0.8) 0px 8px 16px -5px'
          : 'hsla(220, 30%, 5%, 0.07) 0px 4px 16px 0px, hsla(220, 25%, 10%, 0.07) 0px 8px 16px -5px';

  return {
    palette: {
      mode,
      primary: {
        light: red[200],
        main: red[400],
        dark: red[700],
        contrastText: red[50],
        ...(mode === 'dark' && {
          contrastText: red[50],
          light: red[300],
          main: red[400],
          dark: red[700],
        }),
      },
      info: {
        light: red[100],
        main: red[300],
        dark: red[600],
        contrastText: gray[50],
        ...(mode === 'dark' && {
          contrastText: red[300],
          light: red[200],
          main: red[700],
          dark: red[900],
        }),
      },
      warning: {
        light: orange[300],
        main: orange[400],
        dark: orange[800],
        ...(mode === 'dark' && {
          light: orange[200],
          main: orange[500],
          dark: orange[700],
        }),
      },
      error: {
        light: red[300],
        main: red[400],
        dark: red[800],
        ...(mode === 'dark' && {
          light: red[200],
          main: red[500],
          dark: red[700],
        }),
      },
      success: {
        light: green[300],
        main: green[400],
        dark: green[800],
        ...(mode === 'dark' && {
          light: green[200],
          main: green[500],
          dark: green[700],
        }),
      },
      grey: { ...gray },
      divider: mode === 'dark' ? alpha(gray[700], 0.6) : alpha(gray[300], 0.4),
      background: {
        default: 'hsl(0, 0%, 99%)',
        paper: 'hsl(220, 35%, 97%)',
        ...(mode === 'dark' && { default: gray[900], paper: 'hsl(220, 30%, 7%)', paperWarm: '#0d0608' }),
        ...(mode !== 'dark' && { paperWarm: '#ffffff' }),
      },
      text: {
        primary: gray[800],
        secondary: gray[600],
        warning: orange[400],
        ...(mode === 'dark' && { primary: 'hsl(0, 0%, 100%)', secondary: gray[400] }),
      },
      action: {
        hover: alpha(gray[200], 0.2),
        selected: `${alpha(gray[200], 0.3)}`,
        ...(mode === 'dark' && {
          hover: alpha(gray[600], 0.2),
          selected: alpha(gray[600], 0.3),
        }),
      },
    },
    typography: {
      fontFamily: 'Inter, sans-serif',
      h1: {
        fontSize: defaultTheme.typography.pxToRem(48),
        fontWeight: 600,
        lineHeight: 1.2,
        letterSpacing: -0.5,
      },
      h2: { fontSize: defaultTheme.typography.pxToRem(36), fontWeight: 600, lineHeight: 1.2 },
      h3: { fontSize: defaultTheme.typography.pxToRem(30), lineHeight: 1.2 },
      h4: { fontSize: defaultTheme.typography.pxToRem(24), fontWeight: 600, lineHeight: 1.5 },
      h5: { fontSize: defaultTheme.typography.pxToRem(20), fontWeight: 600 },
      h6: { fontSize: defaultTheme.typography.pxToRem(18), fontWeight: 600 },
      subtitle1: { fontSize: defaultTheme.typography.pxToRem(18) },
      subtitle2: { fontSize: defaultTheme.typography.pxToRem(14), fontWeight: 500 },
      body1: { fontSize: defaultTheme.typography.pxToRem(14) },
      body2: { fontSize: defaultTheme.typography.pxToRem(14), fontWeight: 400 },
      caption: { fontSize: defaultTheme.typography.pxToRem(12), fontWeight: 400 },
    },
    shape: { borderRadius: 8 },
    shadows: customShadows,
  };
};

export const colorSchemes = {
  light: {
    palette: {
      primary: { light: red[200], main: red[400], dark: red[700], contrastText: red[50], icon: red[400] },
      info: { light: red[100], main: red[300], dark: red[600], contrastText: gray[50] },
      warning: { light: orange[300], main: orange[400], dark: orange[800] },
      error: { light: red[300], main: red[400], dark: red[800] },
      success: { light: green[300], main: green[400], dark: green[800] },
      grey: { ...gray },
      divider: alpha(gray[300], 0.4),
      background: { default: 'hsl(0, 0%, 99%)', paper: 'hsl(220, 35%, 97%)', paperWarm: '#ffffff' },
      text: { primary: gray[800], secondary: gray[600], warning: orange[400] },
      action: { hover: alpha(gray[200], 0.2), selected: `${alpha(gray[200], 0.3)}` },
      baseShadow:
          'hsla(220, 30%, 5%, 0.07) 0px 4px 16px 0px, hsla(220, 25%, 10%, 0.07) 0px 8px 16px -5px',
      tones: {
        rose: {
          fg: '#dc2626', soft: '#fff1f2', softSubtle: 'rgba(220,38,38,0.06)',
          headerBg: '#dc2626', headerBgEnd: '#991b1b', hoverBg: '#B41D1D',
          footerBg: '#fff1f2', secondaryHoverBg: 'rgba(220,38,38,0.05)',
          ring: 'rgba(220,38,38,0.18)', glow: 'rgba(220,38,38,0.18)',
          shadowResting: '0 4px 14px rgba(0,0,0,0.05)',
          shadowHover: '0 10px 28px rgba(220,38,38,0.18), 0 0 0 1px rgba(220,38,38,0.18)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.14)',
          buttonShadow: '0 4px 14px rgba(100,10,10,0.45)',
          buttonShadowHover: '0 6px 18px rgba(100,10,10,0.55)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        amber: {
          fg: '#b45309', soft: '#fffbeb',
          ring: 'rgba(180,83,9,0.18)', glow: 'rgba(217,119,6,0.18)',
          shadowResting: '0 4px 14px rgba(0,0,0,0.05)',
          shadowHover: '0 10px 28px rgba(217,119,6,0.18), 0 0 0 1px rgba(180,83,9,0.18)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.14)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        violet: {
          fg: '#7c3aed', soft: '#f5f3ff',
          ring: 'rgba(124,58,237,0.18)', glow: 'rgba(124,58,237,0.18)',
          shadowResting: '0 4px 14px rgba(0,0,0,0.05)',
          shadowHover: '0 10px 28px rgba(124,58,237,0.18), 0 0 0 1px rgba(124,58,237,0.18)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.14)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        teal: {
          fg: '#0f766e', soft: '#f0fdfa',
          ring: 'rgba(15,118,110,0.18)', glow: 'rgba(20,184,166,0.18)',
          shadowResting: '0 4px 14px rgba(0,0,0,0.05)',
          shadowHover: '0 10px 28px rgba(20,184,166,0.18), 0 0 0 1px rgba(15,118,110,0.18)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.14)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        indigo: {
          fg: '#4338ca', soft: '#eef2ff',
          ring: 'rgba(67,56,202,0.18)', glow: 'rgba(99,102,241,0.18)',
          shadowResting: '0 4px 14px rgba(0,0,0,0.05)',
          shadowHover: '0 10px 28px rgba(99,102,241,0.18), 0 0 0 1px rgba(67,56,202,0.18)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.14)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
      },
    },
  },
  dark: {
    palette: {
      primary: { contrastText: red[50], light: red[300], main: red[400], dark: red[700], icon: 'hsl(0, 0%, 100%)' },
      info: { contrastText: red[300], light: red[300], main: red[700], dark: red[900] },
      warning: { light: orange[300], main: orange[500], dark: orange[700] },
      error: { light: red[300], main: red[500], dark: red[700] },
      success: { light: green[300], main: green[500], dark: green[700] },
      grey: { ...gray },
      divider: alpha(gray[700], 0.6),
      background: { default: gray[900], paper: 'hsl(220, 30%, 7%)', paperWarm: '#0d0608' },
      text: { primary: 'hsl(0, 0%, 100%)', secondary: gray[400] },
      action: { hover: alpha(gray[600], 0.2), selected: alpha(gray[600], 0.3) },
      baseShadow:
          'hsla(220, 30%, 5%, 0.7) 0px 4px 16px 0px, hsla(220, 25%, 10%, 0.8) 0px 8px 16px -5px',
      tones: {
        rose: {
          fg: '#FBE9E9', soft: 'rgba(127,29,29,0.35)', softSubtle: 'rgba(248,113,113,0.10)',
          headerBg: '#7f1d1d', headerBgEnd: '#991b1b', hoverBg: '#dc2626',
          footerBg: 'rgb(6, 5, 6)', secondaryHoverBg: '#2D3748',
          ring: 'rgba(248,113,113,0.28)', glow: 'rgba(248,113,113,0.18)',
          shadowResting: '0 4px 16px rgba(0,0,0,0.35)',
          shadowHover: '0 10px 28px rgba(0,0,0,0.45), 0 0 0 1px rgba(248,113,113,0.28)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.08)',
          buttonShadow: '0 4px 14px rgba(0,0,0,0.15)',
          buttonShadowHover: '0 6px 18px rgba(0,0,0,0.22)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        amber: {
          fg: '#fbbf24', soft: 'rgba(120,53,15,0.35)',
          ring: 'rgba(251,191,36,0.28)', glow: 'rgba(251,191,36,0.16)',
          shadowResting: '0 4px 16px rgba(0,0,0,0.35)',
          shadowHover: '0 10px 28px rgba(0,0,0,0.45), 0 0 0 1px rgba(251,191,36,0.28)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.08)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        violet: {
          fg: '#a78bfa', soft: 'rgba(76,29,149,0.35)',
          ring: 'rgba(167,139,250,0.28)', glow: 'rgba(167,139,250,0.16)',
          shadowResting: '0 4px 16px rgba(0,0,0,0.35)',
          shadowHover: '0 10px 28px rgba(0,0,0,0.45), 0 0 0 1px rgba(167,139,250,0.28)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.08)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        teal: {
          fg: '#5eead4', soft: 'rgba(19,78,74,0.35)',
          ring: 'rgba(94,234,212,0.28)', glow: 'rgba(94,234,212,0.16)',
          shadowResting: '0 4px 16px rgba(0,0,0,0.35)',
          shadowHover: '0 10px 28px rgba(0,0,0,0.45), 0 0 0 1px rgba(94,234,212,0.28)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.08)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
        indigo: {
          fg: '#a5b4fc', soft: 'rgba(49,46,129,0.35)',
          ring: 'rgba(165,180,252,0.28)', glow: 'rgba(165,180,252,0.16)',
          shadowResting: '0 4px 16px rgba(0,0,0,0.35)',
          shadowHover: '0 10px 28px rgba(0,0,0,0.45), 0 0 0 1px rgba(165,180,252,0.28)',
          shadowModal: '0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.08)',
          headerOverlay: 'rgba(255,255,255,0.15)', headerOverlayHover: 'rgba(255,255,255,0.15)',
          headerBorder: 'rgba(255,255,255,0.22)', headerTextBorder: 'rgba(255,255,255,0.20)',
          headerTextMuted: 'rgba(255,255,255,0.65)', headerTextSubtle: 'rgba(255,255,255,0.75)',
        },
      },
    },
  },
};

export const typography = {
  fontFamily: 'Inter, sans-serif',
  h1: { fontSize: defaultTheme.typography.pxToRem(48), fontWeight: 600, lineHeight: 1.2, letterSpacing: -0.5 },
  h2: { fontSize: defaultTheme.typography.pxToRem(36), fontWeight: 600, lineHeight: 1.2 },
  h3: { fontSize: defaultTheme.typography.pxToRem(30), lineHeight: 1.2 },
  h4: { fontSize: defaultTheme.typography.pxToRem(24), fontWeight: 600, lineHeight: 1.5 },
  h5: { fontSize: defaultTheme.typography.pxToRem(20), fontWeight: 600 },
  h6: { fontSize: defaultTheme.typography.pxToRem(18), fontWeight: 600 },
  subtitle1: { fontSize: defaultTheme.typography.pxToRem(18) },
  subtitle2: { fontSize: defaultTheme.typography.pxToRem(14), fontWeight: 500 },
  body1: { fontSize: defaultTheme.typography.pxToRem(14) },
  body2: { fontSize: defaultTheme.typography.pxToRem(14), fontWeight: 400 },
  caption: { fontSize: defaultTheme.typography.pxToRem(12), fontWeight: 400 },
};

export const shape = { borderRadius: 8 };

const defaultShadows = [
  'none',
  'var(--template-palette-baseShadow)',
  ...defaultTheme.shadows.slice(2),
];
export const shadows = defaultShadows;