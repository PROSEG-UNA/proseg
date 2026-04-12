import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Collapse,
  Box,
  Typography,
  Button,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import CloseIcon from '@mui/icons-material/Close';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';

export function MobileNavbar() {
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [expandedMenu, setExpandedMenu] = useState(null);

  const toggleDrawer = (open) => (event) => {
    if (event.type === 'keydown' && (event.key === 'Tab' || event.key === 'Shift')) {
      return;
    }
    setDrawerOpen(open);
  };

  const toggleMenu = (menu) => {
    setExpandedMenu(expandedMenu === menu ? null : menu);
  };

  const handleNavigation = (path) => {
    navigate(path);
    setDrawerOpen(false);
  };

  const handleLogout = () => {
    navigate('/login');
    setDrawerOpen(false);
  };

  return (
    <>
      {/* AppBar con hamburguesa */}
      <AppBar position="static" sx={{ backgroundColor: '#C41E3A' }}>
        <Toolbar>
          <IconButton
            edge="start"
            color="inherit"
            aria-label="menu"
            onClick={toggleDrawer(true)}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography 
            variant="h6" 
            sx={{ 
              flexGrow: 1, 
              fontWeight: 700, 
              fontSize: { xs: '0.75rem', sm: '0.95rem' },
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            Sistema de Sección de Seguridad Institucional
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Drawer con navegación */}
      <Drawer anchor="left" open={drawerOpen} onClose={toggleDrawer(false)}>
        <Box
          sx={{
            width: 280,
            backgroundColor: '#ece3e3',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
          }}
          role="presentation"
        >
          {/* Header del drawer */}
          <Box
            sx={{
              p: 2,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderBottom: '1px solid rgba(0,0,0,0.1)',
            }}
          >
            <img
              src="/logo_una.png"
              alt="Logo UNA"
              style={{
                maxWidth: '50%',
                height: 'auto',
                maxHeight: '50px',
              }}
            />
            <IconButton onClick={toggleDrawer(false)} sx={{ color: '#C41E3A' }}>
              <CloseIcon />
            </IconButton>
          </Box>

          {/* Contenido del drawer */}
          <List sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
            {/* Inventarios */}
            <ListItem disablePadding sx={{ display: 'block' }}>
              <ListItemButton
                onClick={() => toggleMenu('inventory')}
                sx={{
                  borderBottom: '1px solid rgba(0,0,0,0.1)',
                  color: '#333',
                  '&:hover': {
                    backgroundColor: 'rgba(196, 30, 58, 0.05)',
                  },
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <WarehouseIcon sx={{ mr: 2, fontSize: 20 }} />
                    <ListItemText primary="Gestión Inventarios" />
                  </Box>
                  {expandedMenu === 'inventory' ? (
                    <ExpandLessIcon sx={{ color: '#333' }} />
                  ) : (
                    <ExpandMoreIcon sx={{ color: '#333' }} />
                  )}
                </Box>
              </ListItemButton>
              <Collapse in={expandedMenu === 'inventory'} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ backgroundColor: 'rgba(196, 30, 58, 0.05)' }}>
                  <ListItem disablePadding>
                    <ListItemButton
                      onClick={() => handleNavigation('/inventario/activos')}
                      sx={{
                        pl: 4,
                        color: '#333',
                        '&:hover': {
                          backgroundColor: 'rgba(196, 30, 58, 0.1)',
                          fontWeight: 600,
                        },
                      }}
                    >
                      <ListItemText primary="Activos" />
                    </ListItemButton>
                  </ListItem>
                </List>
              </Collapse>
            </ListItem>

            {/* Seguridad */}
            <ListItem disablePadding sx={{ display: 'block' }}>
              <ListItemButton
                onClick={() => toggleMenu('security')}
                sx={{
                  borderBottom: '1px solid rgba(0,0,0,0.1)',
                  color: '#333',
                  '&:hover': {
                    backgroundColor: 'rgba(196, 30, 58, 0.05)',
                  },
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <VerifiedUserIcon sx={{ mr: 2, fontSize: 20 }} />
                    <ListItemText primary="Gestión Seguridad" />
                  </Box>
                  {expandedMenu === 'security' ? (
                    <ExpandLessIcon sx={{ color: '#333' }} />
                  ) : (
                    <ExpandMoreIcon sx={{ color: '#333' }} />
                  )}
                </Box>
              </ListItemButton>
              <Collapse in={expandedMenu === 'security'} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ backgroundColor: 'rgba(196, 30, 58, 0.05)' }}>
                  <ListItem disablePadding>
                    <ListItemButton
                      onClick={() => handleNavigation('/seguridad/usuarios')}
                      sx={{
                        pl: 4,
                        color: '#333',
                        '&:hover': {
                          backgroundColor: 'rgba(196, 30, 58, 0.1)',
                          fontWeight: 600,
                        },
                      }}
                    >
                      <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Usuarios" />
                    </ListItemButton>
                  </ListItem>
                  <ListItem disablePadding>
                    <ListItemButton
                      onClick={() => handleNavigation('/seguridad/roles')}
                      sx={{
                        pl: 4,
                        color: '#333',
                        '&:hover': {
                          backgroundColor: 'rgba(196, 30, 58, 0.1)',
                          fontWeight: 600,
                        },
                      }}
                    >
                      <VerifiedUserIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Roles" />
                    </ListItemButton>
                  </ListItem>
                </List>
              </Collapse>
            </ListItem>
          </List>

          {/* Footer con logout */}
          <Box sx={{ p: 2, borderTop: '1px solid rgba(0,0,0,0.1)' }}>
            <Button
              fullWidth
              variant="contained"
              startIcon={<LogoutIcon />}
              onClick={handleLogout}
              sx={{
                backgroundColor: '#C41E3A',
                color: '#FFFFFF',
                '&:hover': {
                  backgroundColor: '#A01A2E',
                },
                fontWeight: 600,
              }}
            >
              Cerrar Sesión
            </Button>
          </Box>
        </Box>
      </Drawer>
    </>
  );
}

export default MobileNavbar;
