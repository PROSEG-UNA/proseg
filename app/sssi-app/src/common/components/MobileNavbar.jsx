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
import { alpha } from '@mui/material/styles';

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
      <AppBar position="static" sx={{ backgroundColor: 'primary.main' }}>
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

        <Drawer anchor="left" open={drawerOpen} onClose={toggleDrawer(false)}>
          <Box
            sx={{
              width: 280,
              backgroundColor: 'background.paper',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
            }}
            role="presentation"
          >
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
                onClick={() => handleNavigation('/home')}
                style={{
                  maxWidth: '50%',
                  height: 'auto',
                  maxHeight: '50px',
                  cursor: 'pointer',
                }}
              />
              <IconButton onClick={toggleDrawer(false)} sx={{ color: 'primary.main' }}>
                <CloseIcon />
              </IconButton>
            </Box>

            <List sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
              <ListItem disablePadding sx={{ display: 'block' }}>
                <ListItemButton
                  onClick={() => toggleMenu('inventory')}
                  sx={(theme) => ({
                    '&&': {
                      py: 3,
                      minHeight: 72,
                    },
                    borderBottom: '1px solid rgba(0,0,0,0.1)',
                    color: 'text.primary',
                    '&:hover': {
                      backgroundColor: alpha(theme.palette.primary.main, 0.05),
                    },
                  })}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <WarehouseIcon sx={{ mr: 2, fontSize: 20 }} />
                      <ListItemText
                        primary="Gestión Inventarios"
                        sx={{ '& .MuiListItemText-primary': { fontSize: '1.05rem', fontWeight: 600, lineHeight: 1.2 } }}
                      />
                    </Box>
                    {expandedMenu === 'inventory' ? (
                      <ExpandLessIcon sx={{ color: 'text.primary' }} />
                    ) : (
                      <ExpandMoreIcon sx={{ color: 'text.primary' }} />
                    )}
                  </Box>
                </ListItemButton>
                <Collapse in={expandedMenu === 'inventory'} timeout="auto" unmountOnExit>
                  <List component="div" disablePadding sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                    <ListItem disablePadding>
                      <ListItemButton
                        onClick={() => handleNavigation('/inventario/activos')}
                        sx={(theme) => ({
                          '&&': {
                            py: 2,
                            minHeight: 56,
                          },
                          pl: 6,
                          color: 'text.primary',
                          '&:hover': {
                            backgroundColor: alpha(theme.palette.primary.main, 0.1),
                            fontWeight: 600,
                          },
                        })}
                      >
                        <ListItemText
                          primary="Activos"
                          sx={{ '& .MuiListItemText-primary': { fontSize: '0.98rem', fontWeight: 500 } }}
                        />
                      </ListItemButton>
                    </ListItem>
                  </List>
                </Collapse>
              </ListItem>

              <ListItem disablePadding sx={{ display: 'block' }}>
                <ListItemButton
                  onClick={() => toggleMenu('security')}
                  sx={(theme) => ({
                    '&&': {
                      py: 3,
                      minHeight: 72,
                    },
                    borderBottom: '1px solid rgba(0,0,0,0.1)',
                    color: 'text.primary',
                    '&:hover': {
                      backgroundColor: alpha(theme.palette.primary.main, 0.05),
                    },
                  })}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <VerifiedUserIcon sx={{ mr: 2, fontSize: 20 }} />
                      <ListItemText
                        primary="Gestión Seguridad"
                        sx={{ '& .MuiListItemText-primary': { fontSize: '1.05rem', fontWeight: 600, lineHeight: 1.2 } }}
                      />
                    </Box>
                    {expandedMenu === 'security' ? (
                      <ExpandLessIcon sx={{ color: 'text.primary' }} />
                    ) : (
                      <ExpandMoreIcon sx={{ color: 'text.primary' }} />
                    )}
                  </Box>
                </ListItemButton>
                <Collapse in={expandedMenu === 'security'} timeout="auto" unmountOnExit>
                  <List component="div" disablePadding sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                    <ListItem disablePadding>
                      <ListItemButton
                        onClick={() => handleNavigation('/seguridad/usuarios')}
                        sx={(theme) => ({
                          '&&': {
                            py: 2,
                            minHeight: 56,
                          },
                          pl: 6,
                          color: 'text.primary',
                          '&:hover': {
                            backgroundColor: alpha(theme.palette.primary.main, 0.1),
                            fontWeight: 600,
                          },
                        })}
                      >
                        <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                        <ListItemText
                          primary="Usuarios"
                          sx={{ '& .MuiListItemText-primary': { fontSize: '0.98rem', fontWeight: 500 } }}
                        />
                      </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                      <ListItemButton
                        onClick={() => handleNavigation('/seguridad/roles')}
                        sx={(theme) => ({
                          '&&': {
                            py: 2,
                            minHeight: 56,
                          },
                          pl: 6,
                          color: 'text.primary',
                          '&:hover': {
                            backgroundColor: alpha(theme.palette.primary.main, 0.1),
                            fontWeight: 600,
                          },
                        })}
                      >
                        <VerifiedUserIcon sx={{ mr: 1.5, fontSize: 20 }} />
                        <ListItemText
                          primary="Roles"
                          sx={{ '& .MuiListItemText-primary': { fontSize: '0.98rem', fontWeight: 500 } }}
                        />
                      </ListItemButton>
                    </ListItem>
                  </List>
                </Collapse>
              </ListItem>
            </List>

            <Box sx={{ p: 2, borderTop: '1px solid rgba(0,0,0,0.1)' }}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<LogoutIcon />}
                onClick={handleLogout}
                sx={{
                  backgroundColor: 'primary.main',
                  color: 'primary.contrastText',
                  '&:hover': {
                    backgroundColor: 'primary.dark',
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