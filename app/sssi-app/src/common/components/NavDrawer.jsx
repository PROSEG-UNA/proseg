import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Collapse,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
} from '@mui/material';
import AppsIcon from '@mui/icons-material/Apps';
import CloseIcon from '@mui/icons-material/Close';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import { alpha } from '@mui/material/styles';

export function NavDrawer({ open, onClose, onLogout }) {
  const navigate = useNavigate();
  const [expandedMenu, setExpandedMenu] = useState(null);

  const toggleMenu = (menu) => {
    setExpandedMenu(expandedMenu === menu ? null : menu);
  };

  const handleNavigation = (path) => {
    navigate(path);
    onClose();
  };

  return (
    <Drawer anchor="left" open={open} onClose={onClose}>
      <Box
        sx={{ width: 280, backgroundColor: 'background.paper', height: '100%', display: 'flex', flexDirection: 'column' }}
        role="presentation"
      >
        <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(0,0,0,0.1)' }}>
          <img src="/logo_una.png" alt="Logo UNA" style={{ maxWidth: '50%', height: 'auto', maxHeight: '50px' }} />
          <IconButton onClick={onClose} sx={{ color: 'primary.main' }}>
            <CloseIcon />
          </IconButton>
        </Box>

        <List sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
          <ListItem disablePadding sx={{ display: 'block' }}>
            <ListItemButton
              onClick={() => toggleMenu('inventory')}
              sx={(theme) => ({ borderBottom: '1px solid rgba(0,0,0,0.1)', color: 'text.primary', '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.05) } })}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <WarehouseIcon sx={{ mr: 2, fontSize: 20 }} />
                  <ListItemText primary="Gestión Inventarios" />
                </Box>
                {expandedMenu === 'inventory' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
              </Box>
            </ListItemButton>
            <Collapse in={expandedMenu === 'inventory'} timeout="auto" unmountOnExit>
              <Box sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                <Box
                  onClick={() => handleNavigation('/inventario/activos')}
                  sx={(theme) => ({
                    pl: 4, color: 'text.primary',
                    '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                    display: 'flex', alignItems: 'center', width: '100%', py: 1.5, cursor: 'pointer',
                  })}
                >
                  <AppsIcon sx={{ mr: 1.5, fontSize: 20 }} />
                  <ListItemText primary="Activos" />
                </Box>
              </Box>
            </Collapse>
          </ListItem>

          <ListItem disablePadding sx={{ display: 'block' }}>
            <ListItemButton
              onClick={() => toggleMenu('security')}
              sx={(theme) => ({ borderBottom: '1px solid rgba(0,0,0,0.1)', color: 'text.primary', '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.05) } })}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <VerifiedUserIcon sx={{ mr: 2, fontSize: 20 }} />
                  <ListItemText primary="Gestión Seguridad" />
                </Box>
                {expandedMenu === 'security' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
              </Box>
            </ListItemButton>
            <Collapse in={expandedMenu === 'security'} timeout="auto" unmountOnExit>
              <Box sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                <Box
                  onClick={() => handleNavigation('/seguridad/usuarios')}
                  sx={(theme) => ({
                    pl: 4, color: 'text.primary',
                    '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                    display: 'flex', alignItems: 'center', width: '100%', py: 1.5, cursor: 'pointer',
                  })}
                >
                  <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                  <ListItemText primary="Usuarios" />
                </Box>
                <Box
                  onClick={() => handleNavigation('/seguridad/roles')}
                  sx={(theme) => ({
                    pl: 4, color: 'text.primary',
                    '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                    display: 'flex', alignItems: 'center', width: '100%', py: 1.5, cursor: 'pointer',
                  })}
                >
                  <VerifiedUserIcon sx={{ mr: 1.5, fontSize: 20 }} />
                  <ListItemText primary="Roles" />
                </Box>
              </Box>
            </Collapse>
          </ListItem>
        </List>

        <Box sx={{ p: 2, borderTop: '1px solid rgba(0,0,0,0.1)' }}>
          <Button
            fullWidth
            variant="contained"
            startIcon={<LogoutIcon />}
            onClick={onLogout}
            sx={{ backgroundColor: 'primary.main', color: 'primary.contrastText', '&:hover': { backgroundColor: 'primary.dark' }, fontWeight: 600 }}
          >
            Cerrar Sesión
          </Button>
        </Box>
      </Box>
    </Drawer>
  );
}

export default NavDrawer;
