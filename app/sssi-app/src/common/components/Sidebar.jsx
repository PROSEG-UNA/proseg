import { useState, useContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Collapse,
  Button,
  Typography,
  IconButton,
} from '@mui/material';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuOpenIcon from '@mui/icons-material/MenuOpen';
import MenuIcon from '@mui/icons-material/Menu';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { SidebarContext } from '../context/SidebarContext';
import '../css/Sidebar.css';

export function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [expandedMenu, setExpandedMenu] = useState(null);
  const { isMinimized, setIsMinimized } = useContext(SidebarContext);

  const toggleMenu = (menu) => {
    setExpandedMenu(expandedMenu === menu ? null : menu);
  };

  const isActive = (path) => location.pathname === path;
  const sidebarWidth = isMinimized ? 80 : 280;

  return (
    <Box
      component="nav"
      className="sidebar"
      sx={{
        width: sidebarWidth,
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#ece3e3',
        boxShadow: '2px 0 8px rgba(0, 0, 0, 0.1)',
        zIndex: 1210,
        position: 'fixed',
        left: 0,
        top: 0,
        overflowY: 'auto',
        transition: 'width 0.3s ease-in-out',
      }}
    >
      <Box
        className="sidebar-logo"
        sx={{
          backgroundColor: '#ece3e3',
          p: 2,
          textAlign: 'center',
          borderBottom: '1px solid rgba(0,0,0,0.1)',
          display: 'flex',
          flexDirection: isMinimized ? 'column' : 'row',
          alignItems: 'center',
          justifyContent: isMinimized ? 'center' : 'space-between',
          gap: isMinimized ? 1 : 3,
        }}
      >
        {!isMinimized && (
          <img
            src="/logo_una.png"
            alt="Logo UNA"
            style={{
              maxWidth: '60%',
              height: 'auto',
              maxHeight: '60px',
            }}
          />
        )}
        <IconButton
          onClick={() => setIsMinimized(!isMinimized)}
          sx={{
            color: '#C41E3A',
            width: isMinimized ? 48 : 'auto',
            '&:hover': {
              backgroundColor: 'rgba(196, 30, 58, 0.1)',
            },
          }}
        >
          {isMinimized ? <MenuOpenIcon /> : <MenuIcon />}
        </IconButton>
      </Box>

      {!isMinimized && (
        <Box className="sidebar-nav" sx={{ flex: 1, overflowY: 'auto', p: 0, display: 'block' }}>
          <List sx={{ p: 0 }}>
            <ListItem disablePadding className="sidebar-menu" sx={{ display: 'block' }}>
              <ListItemButton
                className="sidebar-menu-title"
                onClick={() => toggleMenu('inventory')}
                sx={{
                  borderBottom: '1px solid rgba(0,0,0,0.1)',
                  color: '#333',
                  '&:hover': {
                    backgroundColor: 'rgba(196, 30, 58, 0.05)',
                  },
                }}
              >
                <ListItemText 
                  primary="Gestión Inventarios"
                  sx={{ color: '#333' }}
                />
                {expandedMenu === 'inventory' ? (
                  <ExpandLessIcon sx={{ color: '#333' }} />
                ) : (
                  <ExpandMoreIcon sx={{ color: '#333' }} />
                )}
              </ListItemButton>
              <Collapse in={expandedMenu === 'inventory'} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ backgroundColor: 'rgba(196, 30, 58, 0.05)' }}>
                  <ListItem disablePadding>
                    <ListItemButton 
                      className="sidebar-item"
                      onClick={() => navigate('/inventario/activos')}
                      sx={{ 
                        pl: 4, 
                        color: '#333',
                        '&:hover': { 
                          backgroundColor: 'rgba(196, 30, 58, 0.1)',
                          fontWeight: 600,
                        }
                      }}
                    >
                      <WarehouseIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Activos" />
                    </ListItemButton>
                  </ListItem>
                </List>
              </Collapse>
            </ListItem>

            <ListItem disablePadding className="sidebar-menu" sx={{ display: 'block' }}>
              <ListItemButton
                className="sidebar-menu-title"
                onClick={() => toggleMenu('security')}
                sx={{
                  borderBottom: '1px solid rgba(0,0,0,0.1)',
                  color: '#333',
                  '&:hover': {
                    backgroundColor: 'rgba(196, 30, 58, 0.05)',
                  },
                }}
              >
                <ListItemText 
                  primary="Gestión Seguridad"
                  sx={{ color: '#333' }}
                />
                {expandedMenu === 'security' ? (
                  <ExpandLessIcon sx={{ color: '#333' }} />
                ) : (
                  <ExpandMoreIcon sx={{ color: '#333' }} />
                )}
              </ListItemButton>
              <Collapse in={expandedMenu === 'security'} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ backgroundColor: 'rgba(196, 30, 58, 0.05)' }}>
                  <ListItem disablePadding>
                    <ListItemButton 
                      className="sidebar-item"
                      onClick={() => navigate('/seguridad/usuarios')}
                      sx={{ 
                        pl: 4, 
                        color: '#333',
                        '&:hover': { 
                          backgroundColor: 'rgba(196, 30, 58, 0.1)',
                          fontWeight: 600,
                        }
                      }}
                    >
                      <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Usuarios" />
                    </ListItemButton>
                  </ListItem>
                  <ListItem disablePadding>
                    <ListItemButton 
                      className="sidebar-item"
                      onClick={() => navigate('/seguridad/roles')}
                      sx={{ 
                        pl: 4, 
                        color: '#333',
                        '&:hover': { 
                          backgroundColor: 'rgba(196, 30, 58, 0.1)',
                          fontWeight: 600,
                        }
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
        </Box>
      )}

      {isMinimized && (
        <Box className="sidebar-nav-minimized" sx={{ flex: 1, overflowY: 'auto', p: 1, display: 'flex', flexDirection: 'column', gap: 1, alignItems: 'center' }}>
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 1,
              width: '100%',
            }}
          >
            <IconButton
              onClick={() => {
                setIsMinimized(false);
                toggleMenu('inventory');
              }}
              title="Gestión Inventarios"
              sx={{
                color: '#333',
                width: 50,
                height: 50,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                '&:hover': {
                  backgroundColor: 'rgba(196, 30, 58, 0.1)',
                },
              }}
            >
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
                <WarehouseIcon sx={{ fontSize: 24 }} />
                <ExpandMoreIcon sx={{ fontSize: 16 }} />
              </Box>
            </IconButton>

            <IconButton
              onClick={() => {
                setIsMinimized(false);
                toggleMenu('security');
              }}
              title="Gestión Seguridad"
              sx={{
                color: '#333',
                width: 50,
                height: 50,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                '&:hover': {
                  backgroundColor: 'rgba(196, 30, 58, 0.1)',
                },
              }}
            >
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
                <VerifiedUserIcon sx={{ fontSize: 24 }} />
                <ExpandMoreIcon sx={{ fontSize: 16 }} />
              </Box>
            </IconButton>
          </Box>
        </Box>
      )}

      {!isMinimized && (
        <Box className="sidebar-footer" sx={{ p: 2, borderTop: '1px solid rgba(0,0,0,0.1)' }}>
          <Button
            className="sidebar-logout"
            fullWidth
            variant="contained"
            startIcon={<LogoutIcon />}
            onClick={() => navigate('/login')}
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
      )}
    </Box>
  );
}

export default Sidebar;
