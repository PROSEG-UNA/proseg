import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  Button,
  Container,
  Card,
  CardContent,
  CardActions,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Collapse,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import StorageIcon from '@mui/icons-material/Storage';
import MenuIcon from '@mui/icons-material/Menu';
import CloseIcon from '@mui/icons-material/Close';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import AppsIcon from '@mui/icons-material/Apps';
import { alpha } from '@mui/material/styles';
import AppTheme from '../../../common/theme/AppTheme';
import '../css/InventoryPage.css';

export function InventoryPage() {
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [expandedMenu, setExpandedMenu] = useState(null);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const handleLogout = () => {
    navigate('/login');
  };

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

  return (
    <AppTheme>
      <Box className="inventory-page">
        <AppBar position="static" sx={{ backgroundColor: 'primary.main' }}>
          <Toolbar>
            {isMediumOrDown && (
              <IconButton
                edge="start"
                color="inherit"
                onClick={toggleDrawer(true)}
                sx={{ mr: 1 }}
              >
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
              Gestión de Inventario
            </Typography>
            <IconButton color="inherit" onClick={handleLogout}>
              <LogoutIcon />
            </IconButton>
          </Toolbar>
        </AppBar>

        <Drawer anchor="left" open={drawerOpen} onClose={toggleDrawer(false)}>
          <Box
            sx={{ width: 280, backgroundColor: 'background.paper', height: '100%', display: 'flex', flexDirection: 'column' }}
            role="presentation"
          >
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(0,0,0,0.1)' }}>
              <img src="/logo_una.png" alt="Logo UNA" style={{ maxWidth: '50%', height: 'auto', maxHeight: '50px' }} />
              <IconButton onClick={toggleDrawer(false)} sx={{ color: 'primary.main' }}>
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
                        pl: 4,
                        color: 'text.primary',
                        '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        py: 1.5,
                        cursor: 'pointer',
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
                        pl: 4,
                        color: 'text.primary',
                        '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        py: 1.5,
                        cursor: 'pointer',
                      })}
                    >
                      <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Usuarios" />
                    </Box>
                    <Box
                      onClick={() => handleNavigation('/seguridad/roles')}
                      sx={(theme) => ({
                        pl: 4,
                        color: 'text.primary',
                        '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        py: 1.5,
                        cursor: 'pointer',
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
                onClick={() => { handleLogout(); setDrawerOpen(false); }}
                sx={{ backgroundColor: 'primary.main', color: 'primary.contrastText', '&:hover': { backgroundColor: 'primary.dark' }, fontWeight: 600 }}
              >
                Cerrar Sesión
              </Button>
            </Box>
          </Box>
        </Drawer>

        <Container maxWidth="lg" className="inventory-content">
          <Box sx={{ py: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
            <Typography
              variant="h4"
              sx={{
                fontWeight: 'bold',
                mb: 3,
                color: 'primary.main',
                fontSize: { xs: '1.8rem', sm: '2.2rem' },
                letterSpacing: '0.3px',
              }}
            >
              Bienvenido
            </Typography>
            <Typography
              variant="body1"
              sx={{
                color: 'text.secondary',
                mb: 4,
                maxWidth: '600px',
                fontSize: { xs: '0.9rem', sm: '1rem' },
              }}
            >
              Selecciona una opción para comenzar a gestionar los activos de tu organización.
            </Typography>

            <Box sx={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
              <Card
                sx={(theme) => ({
                  width: { xs: '100%', sm: '90%', md: '400px' },
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'box-shadow 0.3s ease',
                  '&:hover': {
                    boxShadow: `0 8px 16px ${alpha(theme.palette.primary.main, 0.15)}`,
                  },
                })}
              >
                <CardContent sx={{ flex: 1, textAlign: 'center' }}>
                  <StorageIcon sx={{ fontSize: { xs: 50, sm: 60 }, color: 'primary.main', mb: 2 }} />
                  <Typography
                    variant="h6"
                    sx={{ fontWeight: 'bold', mb: 1, color: 'text.primary' }}
                  >
                    Gestión de Activos
                  </Typography>
                  <Typography
                    variant="body2"
                    sx={{ color: 'text.secondary', fontSize: { xs: '0.85rem', sm: '0.875rem' } }}
                  >
                    Administra todos los activos de la organización
                  </Typography>
                </CardContent>
                <CardActions sx={{ justifyContent: 'center', pb: 2 }}>
                  <Button
                    variant="contained"
                    sx={{
                      backgroundColor: 'primary.main',
                      '&:hover': { backgroundColor: 'primary.dark' },
                      textTransform: 'none',
                      fontWeight: 600,
                    }}
                    onClick={() => navigate('/inventario/activos')}
                  >
                    Ir a Activos
                  </Button>
                </CardActions>
              </Card>
            </Box>
          </Box>
        </Container>
      </Box>
    </AppTheme>
  );
}

export default InventoryPage;