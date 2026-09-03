import { AppBar, Toolbar, Typography, IconButton, Button, Box } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import { useAuth } from '../../features/auth/hooks/useAuth';
import { headerSurfaceSx as surfaceSx } from '../theme/sxStyles';

const iconButtonSx = (t) => ({
  color: 'text.secondary',
  borderRadius: '10px',
  transition: 'color 0.2s ease, background 0.2s ease',
  '&:hover': {
    color: 'text.primary',
    bgcolor: 'hsla(220, 20%, 50%, 0.08)',
  },
  ...t.applyStyles('dark', {
    '&:hover': {
      color: 'text.primary',
      bgcolor: 'hsla(220, 20%, 80%, 0.06)',
    },
  }),
});

function ColorModeToggle() {
  const { mode, systemMode, setMode } = useColorScheme();
  const resolved = mode === 'system' ? systemMode : mode;
  return (
    <IconButton onClick={() => setMode(resolved === 'dark' ? 'light' : 'dark')} sx={iconButtonSx}>
      {resolved === 'dark' ? <LightModeIcon sx={{ fontSize: 20 }} /> : <DarkModeIcon sx={{ fontSize: 20 }} />}
    </IconButton>
  );
}

export function Header({ title, onMenuClick, navButtons = [] }) {
  const { handleLogout } = useAuth();

  return (
    <AppBar
      position="static"
      elevation={0}
      color="transparent"
      sx={(t) => ({
        position: 'relative',
        overflow: 'hidden',
        color: 'text.primary',
        ...surfaceSx(t),
        backdropFilter: 'blur(14px) saturate(140%)',
        WebkitBackdropFilter: 'blur(14px) saturate(140%)',
      })}
    >
      <Toolbar sx={{ position: 'relative', zIndex: 1, gap: 0.5, minHeight: { xs: 60, sm: 64 } }}>
        {onMenuClick && (
          <IconButton edge="start" onClick={onMenuClick} sx={(t) => ({ ...iconButtonSx(t), mr: 1 })}>
            <MenuIcon />
          </IconButton>
        )}

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, flexGrow: 1, minWidth: 0 }}>
          <Box
            sx={(t) => ({
              width: 3, height: 20, borderRadius: '3px',
              background: t.palette.tones.rose.fg,
              boxShadow: `0 0 10px ${t.palette.tones.rose.glow}`,
              flexShrink: 0,
            })}
          />
          <Typography
            noWrap
            sx={{
              fontWeight: 700,
              fontSize: { xs: '0.95rem', sm: '1.05rem' },
              letterSpacing: '-0.01em',
              color: 'text.primary',
            }}
          >
            {title}
          </Typography>
        </Box>

        {navButtons.map((btn) => (
          <Button
            key={btn.label}
            onClick={btn.onClick}
            sx={(t) => ({
              mr: 0.5,
              textTransform: 'none',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: 'text.secondary',
              borderRadius: '10px',
              px: 1.5,
              transition: 'color 0.2s ease, background 0.2s ease',
              '&:hover': {
                color: 'text.primary',
                bgcolor: 'hsla(220, 20%, 50%, 0.08)',
              },
              ...t.applyStyles('dark', {
                '&:hover': {
                  color: 'text.primary',
                  bgcolor: 'hsla(220, 20%, 80%, 0.06)',
                },
              }),
            })}
          >
            {btn.label}
          </Button>
        ))}

        <ColorModeToggle />

        <IconButton
          onClick={handleLogout}
          sx={(t) => ({
            color: 'text.secondary',
            borderRadius: '10px',
            transition: 'color 0.2s ease, background 0.2s ease',
            '&:hover': {
              color: t.palette.tones.rose.fg,
              bgcolor: t.vars.palette.tones.rose.softSubtle,
            },
          })}
        >
          <LogoutIcon sx={{ fontSize: 20 }} />
        </IconButton>
      </Toolbar>
    </AppBar>
  );
}

export default Header;
