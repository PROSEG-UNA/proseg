import { AppBar, Toolbar, Typography, IconButton, Button } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import { useAuth } from '../../features/auth/hooks/useAuth';

function ColorModeToggle() {
  const { mode, setMode } = useColorScheme();
  return (
    <IconButton color="inherit" onClick={() => setMode(mode === 'dark' ? 'light' : 'dark')} sx={{ mr: 1 }}>
      {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
    </IconButton>
  );
}

export function Header({ title, onMenuClick, navButtons = [] }) {
  const { handleLogout } = useAuth();
  return (
    <AppBar position="static" sx={{ backgroundColor: 'primary.main' }}>
      <Toolbar>
        {onMenuClick && (
          <IconButton edge="start" color="inherit" onClick={onMenuClick} sx={{ mr: 1 }}>
            <MenuIcon />
          </IconButton>
        )}
        <Typography
          variant="h5"
          sx={{
            flexGrow: 1,
            fontWeight: 700,
            fontSize: { xs: '1.2rem', sm: '1.5rem' },
            letterSpacing: '0.5px',
            textShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
            color: 'primary.contrastText',
          }}
        >
          {title}
        </Typography>
        {navButtons.map((btn) => (
          <Button
            key={btn.label}
            color="inherit"
            onClick={btn.onClick}
            sx={{ mr: 2, textTransform: 'none', fontSize: '1rem' }}
          >
            {btn.label}
          </Button>
        ))}
        <ColorModeToggle />
        <IconButton color="inherit" onClick={handleLogout}>
          <LogoutIcon />
        </IconButton>
      </Toolbar>
    </AppBar>
  );
}

export default Header;