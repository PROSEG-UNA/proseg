import { Button } from '@mui/material';

export function RoseButton({ children, ...rest }) {
  return (
    <Button
      variant="contained"
      disableElevation
      {...rest}
      sx={(t) => ({
        textTransform: 'none',
        fontWeight: 700,
        fontSize: '0.86rem',
        borderRadius: '10px',
        py: 1,
        letterSpacing: '0.01em',
        color: '#fff',
        background: `linear-gradient(135deg, ${t.vars.palette.tones.rose.headerBg} 0%, ${t.vars.palette.tones.rose.headerBg} 100%)`,
        boxShadow: t.palette.tones.rose.shadowResting,
        transition: 'box-shadow 0.22s ease, transform 0.22s ease, background 0.22s ease',
        '&:hover': {
          background: `linear-gradient(135deg, ${t.palette.primary.dark} 0%, ${t.palette.primary.dark} 100%)`,
          boxShadow: t.palette.tones.rose.shadowHover,
          transform: 'translateY(-1px)',
        },
        ...(typeof rest.sx === 'function' ? rest.sx(t) : rest.sx),
      })}
    >
      {children}
    </Button>
  );
}

export default RoseButton;
