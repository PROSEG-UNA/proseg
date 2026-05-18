import { Box, Typography, Button } from '@mui/material';
import BlockIcon from '@mui/icons-material/Block';
import { useTheme } from '@mui/material/styles';

export default function AccessDeniedState({
  title = 'No cuentas con permisos suficientes para realizar esta acción.',
  description = 'Tu perfil actual no tiene acceso a esta sección. Si crees que esto es un error, contacta al administrador del sistema.',
  actionLabel,
  onAction,
}) {
  const theme = useTheme();
  const rose = theme.vars.palette.tones.rose;

  return (
    <Box
      sx={{
        minHeight: 'calc(100vh - 180px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 2,
        py: 4,
      }}
    >
      <Box
        sx={{
          width: '100%',
          maxWidth: 720,
          borderRadius: '24px',
          border: '1px solid',
          borderColor: 'divider',
          background: `linear-gradient(180deg, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 92%, transparent) 0%, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 80%, transparent) 100%)`,
          boxShadow: rose.shadowResting,
          px: { xs: 3, sm: 5 },
          py: { xs: 4, sm: 5 },
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
          '&::before': {
            content: '""',
            position: 'absolute',
            inset: 0,
            background: `radial-gradient(circle at top, ${rose.glow} 0%, transparent 62%)`,
            pointerEvents: 'none',
            opacity: 0.75,
          },
        }}
      >
        <Box
          sx={{
            width: 76,
            height: 76,
            mx: 'auto',
            mb: 2.5,
            borderRadius: '22px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: `linear-gradient(135deg, ${rose.headerBg} 0%, ${rose.headerBgEnd} 100%)`,
            boxShadow: rose.shadowHover,
            color: '#fff',
          }}
        >
          <BlockIcon sx={{ fontSize: 36 }} />
        </Box>

        <Typography
          variant="h5"
          sx={{
            fontWeight: 800,
            color: 'text.primary',
            letterSpacing: '-0.02em',
            mb: 1.5,
          }}
        >
          {title}
        </Typography>

        <Typography
          variant="body1"
          sx={{
            color: 'text.secondary',
            lineHeight: 1.8,
            maxWidth: 560,
            mx: 'auto',
          }}
        >
          {description}
        </Typography>

        {actionLabel && onAction ? (
          <Button
            variant="contained"
            onClick={onAction}
            sx={{
              mt: 3,
              borderRadius: '999px',
              px: 3,
              py: 1,
              textTransform: 'none',
              fontWeight: 700,
              boxShadow: rose.shadowResting,
              background: `linear-gradient(135deg, ${rose.headerBg} 0%, ${rose.headerBgEnd} 100%)`,
            }}
          >
            {actionLabel}
          </Button>
        ) : null}
      </Box>
    </Box>
  );
}