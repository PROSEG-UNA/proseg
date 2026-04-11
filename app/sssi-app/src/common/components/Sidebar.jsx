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
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
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
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 1,
        }}
      >
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
          {isMinimized ? <MenuOpenIcon /> : <ChevronLeftIcon />}
        </IconButton>
        {!isMinimized && (
          <img
            src="/logo_una.png"
            alt="Logo UNA"
            style={{
              maxWidth: '80%',
              height: 'auto',
              maxHeight: '80px',
            }}
          />
        )}
      </Box>

      <Box className="sidebar-nav" sx={{ flex: 1, overflowY: 'auto', p: 0, display: isMinimized ? 'none' : 'block' }}>
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
