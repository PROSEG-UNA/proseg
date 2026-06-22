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
import BuildIcon from '@mui/icons-material/Build';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../constants/permissions';
import { alpha } from '@mui/material/styles';
export function MobileNavbar() {
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [expandedMenu, setExpandedMenu] = useState(null);
  const { hasAnyPermission } = usePermissions();

  const userPermissions = [
    PERMISSIONS.USERS.CREATE,
    PERMISSIONS.USERS.READ,
    PERMISSIONS.USERS.READ_ALL,
    PERMISSIONS.USERS.READ_ROLES,
    PERMISSIONS.USERS.APPROVE,
    PERMISSIONS.USERS.ASSIGN_ROLE,
    PERMISSIONS.USERS.REMOVE_ROLE,
    PERMISSIONS.USERS.READ_INVITATIONS,
    PERMISSIONS.ROLES.READ_USERS_BY_ROLE,
  ];

  const rolePermissions = [
    PERMISSIONS.ROLES.READ_BASE,
    PERMISSIONS.ROLES.READ_COMPOSITE,
    PERMISSIONS.ROLES.READ_ROLE_COMPOSITES,
    PERMISSIONS.ROLES.CREATE,
    PERMISSIONS.ROLES.UPDATE,
    PERMISSIONS.ROLES.DELETE,
    PERMISSIONS.ROLES.READ_USERS_BY_ROLE,
  ];

  const canViewInventorySection = hasAnyPermission([
    PERMISSIONS.INVENTORY.READ,
    PERMISSIONS.INVENTORY.MANAGE,
    PERMISSIONS.INVENTORY.DELETE,
    PERMISSIONS.INVENTORY.LOCATIONS.READ,
    PERMISSIONS.INVENTORY.LOCATIONS.MANAGE,
    PERMISSIONS.INVENTORY.LOCATIONS.DELETE,
  ]);

  const canViewUsersSubmodule = hasAnyPermission(userPermissions);
  const canViewRolesSubmodule = hasAnyPermission(rolePermissions);
  const canViewSecuritySection = canViewUsersSubmodule || canViewRolesSubmodule;
  const canViewMaintenanceSection = hasAnyPermission([
    PERMISSIONS.MAINTENANCE.COMPANIES.READ,
    PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE,
    PERMISSIONS.MAINTENANCE.COMPANIES.DELETE,
    PERMISSIONS.MAINTENANCE.REQUESTS.READ,
    PERMISSIONS.MAINTENANCE.REQUESTS.MANAGE,
    PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    PERMISSIONS.MAINTENANCE.TECHNICIANS.READ,
    PERMISSIONS.MAINTENANCE.TECHNICIANS.MANAGE,
    PERMISSIONS.MAINTENANCE.TECHNICIANS.DELETE,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.READ,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.DELETE,
    PERMISSIONS.MAINTENANCE.TICKETS.READ,
    PERMISSIONS.MAINTENANCE.TICKETS.CREATE,
    PERMISSIONS.MAINTENANCE.TICKETS.EDIT,
    PERMISSIONS.MAINTENANCE.TICKETS.DELETE,
  ]);

  const inventoryItems = canViewInventorySection
    ? [{ key: 'assets', label: 'Activos', path: '/inventario/activos' }]
    : [];

  const securityItems = canViewSecuritySection
    ? [
        canViewUsersSubmodule ? { key: 'users', label: 'Usuarios', path: '/seguridad/usuarios' } : null,
        canViewRolesSubmodule ? { key: 'roles', label: 'Roles', path: '/seguridad/roles' } : null,
      ]
      .filter(Boolean)
    : [];

  const maintenanceItems = canViewMaintenanceSection
    ? [
        { key: 'module', icon: BuildIcon, label: 'Mantenimiento', path: '/mantenimiento' },
        { key: 'companies', icon: BuildIcon, label: 'Empresas', path: '/mantenimiento/empresas' },
        { key: 'requests', icon: BuildIcon, label: 'Solicitudes', path: '/mantenimiento/solicitudes' },
        { key: 'technicians', icon: BuildIcon, label: 'Técnicos', path: '/mantenimiento/tecnicos' },
        { key: 'tickets', icon: ConfirmationNumberIcon, label: 'Tickets', path: '/mantenimiento/tickets' },
      ]
    : [];

  const showInventorySection = canViewInventorySection;
  const showMaintenanceSection = canViewMaintenanceSection;
  const showSecuritySection = canViewSecuritySection;

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
              Sistema Programa Servicios Generales
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
              {showInventorySection ? (
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
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
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
                      {inventoryItems.map((item) => (
                        <ListItem key={item.key} disablePadding>
                          <ListItemButton
                            onClick={() => handleNavigation(item.path)}
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
                              primary={item.label}
                              sx={{ '& .MuiListItemText-primary': { fontSize: '0.98rem', fontWeight: 500 } }}
                            />
                          </ListItemButton>
                        </ListItem>
                      ))}
                    </List>
                  </Collapse>
                </ListItem>
              ) : null}

              {showSecuritySection ? (
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
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
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
                      {securityItems.map((item) => (
                        <ListItem key={item.key} disablePadding>
                          <ListItemButton
                            onClick={() => handleNavigation(item.path)}
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
                              primary={item.label}
                              sx={{ '& .MuiListItemText-primary': { fontSize: '0.98rem', fontWeight: 500 } }}
                            />
                          </ListItemButton>
                        </ListItem>
                      ))}
                    </List>
                  </Collapse>
                </ListItem>
              ) : null}

              {showMaintenanceSection ? (
                <ListItem disablePadding sx={{ display: 'block' }}>
                  <ListItemButton
                    onClick={() => toggleMenu('maintenance')}
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
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <BuildIcon sx={{ mr: 2, fontSize: 20 }} />
                        <ListItemText
                          primary="Gestión Mantenimiento"
                          sx={{ '& .MuiListItemText-primary': { fontSize: '1.05rem', fontWeight: 600, lineHeight: 1.2 } }}
                        />
                      </Box>
                      {expandedMenu === 'maintenance' ? (
                        <ExpandLessIcon sx={{ color: 'text.primary' }} />
                      ) : (
                        <ExpandMoreIcon sx={{ color: 'text.primary' }} />
                      )}
                    </Box>
                  </ListItemButton>
                  <Collapse in={expandedMenu === 'maintenance'} timeout="auto" unmountOnExit>
                    <List component="div" disablePadding sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                      {maintenanceItems.map((item) => (
                        <ListItem key={item.key} disablePadding>
                          <ListItemButton
                            onClick={() => handleNavigation(item.path)}
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
                              primary={item.label}
                              sx={{ '& .MuiListItemText-primary': { fontSize: '0.98rem', fontWeight: 500 } }}
                            />
                          </ListItemButton>
                        </ListItem>
                      ))}
                    </List>
                  </Collapse>
                </ListItem>
              ) : null}
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