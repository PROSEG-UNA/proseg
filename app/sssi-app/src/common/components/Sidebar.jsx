import { useState, useContext, useEffect } from 'react';
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
    useMediaQuery,
    useTheme,
} from '@mui/material';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuOpenIcon from '@mui/icons-material/MenuOpen';
import MenuIcon from '@mui/icons-material/Menu';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { alpha, useColorScheme } from '@mui/material/styles';
import { SidebarContext } from '../context/SidebarContext';
import '../css/Sidebar.css';

export function Sidebar() {
    const navigate = useNavigate();
    const location = useLocation();
    const [expandedMenu, setExpandedMenu] = useState(null);
    const { isMinimized, setIsMinimized } = useContext(SidebarContext);
    const theme = useTheme();
    const { mode } = useColorScheme();

    // Detectar si es pantalla pequeña (tablet/móvil)
    const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

    // Minimizar sidebar en pantallas pequeñas y expandir en grandes
    useEffect(() => {
        if (isMediumOrDown) {
            setIsMinimized(true);
        } else {
            setIsMinimized(false);
        }
    }, [isMediumOrDown, setIsMinimized]);

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
                backgroundColor: 'background.paper',
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
                    backgroundColor: 'background.paper',
                    p: 2,
                    textAlign: 'center',
                    borderBottom: '1px solid', borderBottomColor: 'divider',
                    display: 'flex',
                    flexDirection: isMinimized ? 'column' : 'row',
                    alignItems: 'center',
                    justifyContent: isMinimized ? 'center' : 'space-between',
                    gap: isMinimized ? 1 : 3,
                }}
            >
                {!isMinimized && (
                    <img
                        src={mode === 'dark' ? '/logo_una_blanco.png' : '/logo_una.png'}
                        alt="Logo UNA"
                        onClick={() => navigate('/home')}
                        style={{
                            maxWidth: '60%',
                            height: 'auto',
                            maxHeight: '60px',
                            cursor: 'pointer',
                        }}
                    />
                )}
                <IconButton
                    onClick={() => setIsMinimized(!isMinimized)}
                    sx={(theme) => ({
                        color: 'primary.icon',
                        width: isMinimized ? 48 : 'auto',
                        '&:hover': {
                            backgroundColor: alpha(theme.palette.primary.main, 0.1),
                        },
                    })}
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
                                sx={(theme) => ({
                                    '&&': {
                                        py: 3,
                                        minHeight: 72,
                                    },
                                    borderBottom: '1px solid', borderBottomColor: 'divider',
                                    color: 'text.primary',
                                    '&:hover': {
                                        backgroundColor: alpha(theme.palette.primary.main, 0.05),
                                    },
                                })}
                            >
                                <ListItemText
                                    primary="Gestión Inventarios"
                                    sx={{
                                        color: 'text.primary',
                                        '& .MuiListItemText-primary': {
                                            fontSize: '1.05rem',
                                            fontWeight: 600,
                                            lineHeight: 1.2,
                                            px: 2,
                                        },
                                    }}
                                />
                                {expandedMenu === 'inventory' ? (
                                    <ExpandLessIcon sx={{ color: 'text.primary' }} />
                                ) : (
                                    <ExpandMoreIcon sx={{ color: 'text.primary' }} />
                                )}
                            </ListItemButton>
                            <Collapse in={expandedMenu === 'inventory'} timeout="auto" unmountOnExit>
                                <List component="div" disablePadding sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                                    <ListItem disablePadding>
                                        <ListItemButton
                                            className="sidebar-item"
                                            onClick={() => navigate('/inventario/activos')}
                                            sx={(theme) => ({
                                                '&&': {
                                                    py: 2,
                                                    minHeight: 56,
                                                    px: 4,
                                                },
                                                pl: 5,
                                                color: 'text.primary',
                                                '&:hover': {
                                                    backgroundColor: alpha(theme.palette.primary.main, 0.1),
                                                    fontWeight: 600,
                                                }
                                            })}
                                        >
                                            <WarehouseIcon sx={{ mr: 1.5, fontSize: 20 }} />
                                            <ListItemText
                                                primary="Activos"
                                                sx={{
                                                    '& .MuiListItemText-primary': {
                                                        fontSize: '0.98rem',
                                                        fontWeight: 500,
                                                    },
                                                }}
                                            />
                                        </ListItemButton>
                                    </ListItem>
                                </List>
                            </Collapse>
                        </ListItem>

                        <ListItem disablePadding className="sidebar-menu" sx={{ display: 'block' }}>
                            <ListItemButton
                                className="sidebar-menu-title"
                                onClick={() => toggleMenu('security')}
                                sx={(theme) => ({
                                    '&&': {
                                        py: 3,
                                        minHeight: 72,
                                    },
                                    borderBottom: '1px solid', borderBottomColor: 'divider',
                                    color: 'text.primary',
                                    '&:hover': {
                                        backgroundColor: alpha(theme.palette.primary.main, 0.05),
                                    },
                                })}
                            >
                                <ListItemText
                                    primary="Gestión Seguridad"
                                    sx={{
                                        color: 'text.primary',
                                        '& .MuiListItemText-primary': {
                                            fontSize: '1.05rem',
                                            fontWeight: 600,
                                            lineHeight: 1.2,
                                            px: 2,
                                        },
                                    }}
                                />
                                {expandedMenu === 'security' ? (
                                    <ExpandLessIcon sx={{ color: 'text.primary' }} />
                                ) : (
                                    <ExpandMoreIcon sx={{ color: 'text.primary' }} />
                                )}
                            </ListItemButton>
                            <Collapse in={expandedMenu === 'security'} timeout="auto" unmountOnExit>
                                <List component="div" disablePadding sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                                    <ListItem disablePadding>
                                        <ListItemButton
                                            className="sidebar-item"
                                            onClick={() => navigate('/seguridad/usuarios')}
                                            sx={(theme) => ({
                                                '&&': {
                                                    py: 2,
                                                    minHeight: 56,
                                                    px: 4,
                                                },
                                                pl: 5,
                                                color: 'text.primary',
                                                '&:hover': {
                                                    backgroundColor: alpha(theme.palette.primary.main, 0.1),
                                                    fontWeight: 600,
                                                }
                                            })}
                                        >
                                            <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                                            <ListItemText
                                                primary="Usuarios"
                                                sx={{
                                                    '& .MuiListItemText-primary': {
                                                        fontSize: '0.98rem',
                                                        fontWeight: 500,
                                                    },
                                                }}
                                            />
                                        </ListItemButton>
                                    </ListItem>
                                    <ListItem disablePadding>
                                        <ListItemButton
                                            className="sidebar-item"
                                            onClick={() => navigate('/seguridad/roles')}
                                            sx={(theme) => ({
                                                '&&': {
                                                    py: 2,
                                                    minHeight: 56,
                                                    px: 4,
                                                },
                                                pl: 5,
                                                color: 'text.primary',
                                                '&:hover': {
                                                    backgroundColor: alpha(theme.palette.primary.main, 0.1),
                                                    fontWeight: 600,
                                                }
                                            })}
                                        >
                                            <VerifiedUserIcon sx={{ mr: 1.5, fontSize: 20 }} />
                                            <ListItemText
                                                primary="Roles"
                                                sx={{
                                                    '& .MuiListItemText-primary': {
                                                        fontSize: '0.98rem',
                                                        fontWeight: 500,
                                                    },
                                                }}
                                            />
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
                            sx={(theme) => ({
                                color: 'text.primary',
                                width: 50,
                                height: 50,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: 2,
                                '&:hover': {
                                    backgroundColor: alpha(theme.palette.primary.main, 0.12),
                                },
                            })}
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
                            sx={(theme) => ({
                                color: 'text.primary',
                                width: 50,
                                height: 50,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: 2,
                                '&:hover': {
                                    backgroundColor: alpha(theme.palette.primary.main, 0.12),
                                },
                            })}
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
                <Box className="sidebar-footer" sx={{ p: 2, borderTop: '1px solid', borderTopColor: 'divider' }}>
                    <Button
                        className="sidebar-logout"
                        fullWidth
                        variant="contained"
                        startIcon={<LogoutIcon />}
                        onClick={() => navigate('/login')}
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
            )}
        </Box>
    );
}

export default Sidebar;
