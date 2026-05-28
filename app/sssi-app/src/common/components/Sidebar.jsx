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
    Tooltip,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuOpenIcon from '@mui/icons-material/MenuOpen';
import MenuIcon from '@mui/icons-material/Menu';
import BuildIcon from '@mui/icons-material/Build';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import ShieldIcon from '@mui/icons-material/Shield';
import AppsIcon from '@mui/icons-material/Apps';
import { useColorScheme } from '@mui/material/styles';
import { SidebarContext } from '../context/SidebarContext';
import { useAuth } from '../../features/auth/hooks/useAuth';
import { PERMISSIONS } from '../constants/permissions';
import '../css/Sidebar.css';
import {usePermissions} from "../hooks/index.js";
import BusinessIcon from '@mui/icons-material/Business';
import ConstructionIcon from '@mui/icons-material/Construction';

const panelSurfaceSx = (t) => ({
    background: `
        radial-gradient(ellipse 80% 40% at 50% 0%, hsla(0, 70%, 55%, 0.05) 0%, transparent 70%),
        linear-gradient(180deg, hsl(220, 30%, 99%) 0%, hsl(220, 28%, 97%) 100%)
    `,
    ...t.applyStyles('dark', {
        background: `
            radial-gradient(ellipse 80% 40% at 50% 0%, hsla(0, 65%, 45%, 0.1) 0%, transparent 70%),
            linear-gradient(180deg, hsl(228, 16%, 10%) 0%, hsl(228, 16%, 7%) 100%)
        `,
    }),
});

const neutralHoverSx = (t) => ({
    bgcolor: 'hsla(220, 20%, 50%, 0.05)',
    ...t.applyStyles('dark', {
        bgcolor: 'hsla(220, 20%, 80%, 0.04)',
    }),
});

function NavSection({ icon: Icon, label, expanded, onToggle, hasActive }) {
    return (
        <ListItemButton
            onClick={onToggle}
            sx={(t) => ({
                paddingTop: '10px !important',
                paddingBottom: '10px !important',
                px: 2,
                color: 'text.primary',
                transition: 'background 0.18s ease',
                '&:hover': neutralHoverSx(t),
                '&:hover .nav-section-icon': {
                    borderColor: t.vars.palette.tones.rose.ring,
                },
                '&:hover .nav-section-icon svg': {
                    color: t.vars.palette.text.primary,
                },
            })}
        >
            <Box
                className="nav-section-icon"
                sx={(t) => ({
                    width: 34, height: 34, mr: 1.5,
                    borderRadius: '10px',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: hasActive
                        ? t.vars.palette.tones.rose.softSubtle
                        : 'hsla(220, 20%, 50%, 0.06)',
                    border: '1px solid',
                    borderColor: hasActive ? t.vars.palette.tones.rose.ring : 'divider',
                    transition: 'border-color 0.22s ease, background 0.22s ease',
                    flexShrink: 0,
                    ...t.applyStyles('dark', {
                        background: hasActive
                            ? t.vars.palette.tones.rose.softSubtle
                            : 'hsla(220, 20%, 80%, 0.04)',
                        borderColor: hasActive ? 'rgba(255,255,255,0.35)' : 'divider',
                    }),
                })}
            >
                <Icon
                    sx={(t) => ({
                        fontSize: 18,
                        color: hasActive ? t.palette.tones.rose.fg : 'text.secondary',
                        transition: 'color 0.22s ease',
                    })}
                />
            </Box>
            <ListItemText
                primary={label}
                slotProps={{ primary: { sx: { fontSize: '0.9rem', fontWeight: 600, letterSpacing: '-0.01em' } } }}
            />
            {expanded
                ? <ExpandLessIcon sx={{ fontSize: 20, color: 'text.disabled' }} />
                : <ExpandMoreIcon sx={{ fontSize: 20, color: 'text.disabled' }} />}
        </ListItemButton>
    );
}

function NavLeaf({ icon: Icon, label, onClick, active }) {
    return (
        <Box
            onClick={onClick}
            sx={(t) => ({
                cursor: 'pointer',
                position: 'relative',
                display: 'flex', alignItems: 'center',
                gap: 1.25,
                py: 1.5,
                pl: 4.5, pr: 2,
                color: active ? t.palette.tones.rose.fg : t.vars.palette.text.secondary,
                fontWeight: active ? 700 : 500,
                bgcolor: active ? t.vars.palette.tones.rose.softSubtle : 'transparent',
                ...t.applyStyles('dark', {
                    color: active ? t.vars.palette.text.primary : t.vars.palette.text.secondary,
                }),
                transition: 'color 0.18s ease, background 0.18s ease, padding-left 0.18s ease',
                '& .MuiSvgIcon-root': { color: 'inherit' },
                '&:hover, &:focus, &:active': {
                    color: t.vars.palette.text.primary,
                    bgcolor: 'hsla(220, 20%, 50%, 0.07)',
                    pl: 5,
                    ...t.applyStyles('dark', { bgcolor: 'hsla(220, 20%, 80%, 0.07)' }),
                },
                '&::before': active ? {
                    content: '""',
                    position: 'absolute',
                    left: 0, top: 8, bottom: 8,
                    width: 3,
                    borderRadius: '0 3px 3px 0',
                    background: `linear-gradient(135deg, ${t.vars.palette.tones.rose.headerBg} 0%, ${t.vars.palette.tones.rose.headerBg} 100%)`,
                } : undefined,
            })}
        >
            <Icon sx={{ fontSize: 18, color: 'inherit' }} />
            <Typography sx={{ fontSize: '0.84rem', fontWeight: 'inherit', color: 'inherit' }}>{label}</Typography>
        </Box>
    );
}

function MiniNavButton({ icon: Icon, title, active, onClick }) {
    return (
        <Tooltip title={title} placement="right" arrow>
            <IconButton
                onClick={onClick}
                sx={(t) => ({
                    width: 48, height: 48,
                    borderRadius: '12px',
                    border: '1px solid',
                    borderColor: active ? t.vars.palette.tones.rose.ring : 'divider',
                    background: active
                        ? t.vars.palette.tones.rose.softSubtle
                        : 'hsla(220, 20%, 50%, 0.04)',
                    color: active ? t.palette.tones.rose.fg : 'text.secondary',
                    transition: 'all 0.2s ease',
                    '&:hover': {
                        borderColor: t.vars.palette.tones.rose.ring,
                        background: t.vars.palette.tones.rose.softSubtle,
                        color: t.palette.tones.rose.fg,
                        transform: 'translateY(-1px)',
                    },
                    ...t.applyStyles('dark', {
                        background: active
                            ? t.vars.palette.tones.rose.softSubtle
                            : 'hsla(220, 20%, 80%, 0.04)',
                    }),
                })}
            >
                <Icon sx={{ fontSize: 22 }} />
            </IconButton>
        </Tooltip>
    );
}

export function Sidebar() {
    const navigate = useNavigate();
    const location = useLocation();
    const { handleLogout } = useAuth();
    const [expandedMenu, setExpandedMenu] = useState(null);
    const { isMinimized, setIsMinimized } = useContext(SidebarContext);
    const theme = useTheme();
    const { mode, systemMode } = useColorScheme();
    const { hasAnyPermission } = usePermissions();
    const resolvedMode = mode === 'system' ? systemMode : mode;

    const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

    useEffect(() => {
        if (isMediumOrDown) setIsMinimized(true);
        else setIsMinimized(false);
    }, [isMediumOrDown, setIsMinimized]);

    const toggleMenu = (menu) => setExpandedMenu(expandedMenu === menu ? null : menu);
    const isActive = (path) => location.pathname === path;
    const sidebarWidth = isMinimized ? 80 : 280;

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
    ]);

    const inventoryItems = canViewInventorySection
        ? [{ key: 'assets', icon: AppsIcon, label: 'Activos', path: '/inventario/activos' }]
        : [];

    // Split mantenimiento into submodules: Empresas and Solicitudes
    const maintenanceItems = canViewMaintenanceSection
        ? [
            { key: 'companies', icon: BusinessIcon, label: 'Empresas', path: '/mantenimiento/empresas' },
            { key: 'requests', icon: ConstructionIcon, label: 'Solicitudes', path: '/mantenimiento/solicitudes' },
        ]
        : [];

    const securityItems = canViewSecuritySection
        ? [
            canViewUsersSubmodule ? { key: 'users', icon: PeopleIcon, label: 'Usuarios', path: '/seguridad/usuarios' } : null,
            canViewRolesSubmodule ? { key: 'roles', icon: VerifiedUserIcon, label: 'Roles', path: '/seguridad/roles' } : null,
        ]
            .filter(Boolean)
        : [];

    const showInventorySection = canViewInventorySection;
    const showSecuritySection = canViewSecuritySection;
    const showMaintenanceSection = canViewMaintenanceSection;

    const inventoryActive = showInventorySection && inventoryItems.some((item) => isActive(item.path));
    const maintenanceActive = showMaintenanceSection && maintenanceItems.some((item) => isActive(item.path));
    const securityActive = showSecuritySection && securityItems.some((item) => isActive(item.path));

    return (
        <Box
            component="nav"
            className="sidebar"
            sx={(t) => ({
                width: sidebarWidth,
                height: '100vh',
                display: 'flex',
                flexDirection: 'column',
                position: 'fixed',
                left: 0,
                top: 0,
                zIndex: 1210,
                overflow: 'hidden',
                color: 'text.primary',
                transition: 'width 0.3s ease-in-out',
                ...panelSurfaceSx(t),
                borderRight: '1px solid',
                borderColor: 'divider',
            })}
        >
            <Box
                className="sidebar-logo"
                sx={{
                    position: 'relative',
                    zIndex: 1,
                    p: 2,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    display: 'flex',
                    flexDirection: isMinimized ? 'column' : 'row',
                    alignItems: 'center',
                    justifyContent: isMinimized ? 'center' : 'space-between',
                    gap: isMinimized ? 1 : 2,
                    minHeight: 88,
                }}
            >
                {!isMinimized && (
                    <img
                        src={resolvedMode === 'dark' ? '/logo_una_blanco.png' : '/logo_una.png'}
                        alt="Logo UNA"
                        onClick={() => navigate('/home')}
                        style={{ maxWidth: '60%', height: 'auto', maxHeight: '60px', cursor: 'pointer' }}
                    />
                )}
                <IconButton
                    onClick={() => setIsMinimized(!isMinimized)}
                    sx={(t) => ({
                        color: 'text.secondary',
                        borderRadius: '10px',
                        width: isMinimized ? 48 : 'auto',
                        height: isMinimized ? 48 : 'auto',
                        transition: 'color 0.2s ease, background 0.2s ease',
                        '&:hover': {
                            color: 'text.primary',
                            bgcolor: 'hsla(220, 20%, 50%, 0.08)',
                        },
                        ...t.applyStyles('dark', {
                            '&:hover': {
                                bgcolor: 'hsla(220, 20%, 80%, 0.06)',
                            },
                        }),
                    })}
                >
                    {isMinimized ? <MenuOpenIcon /> : <MenuIcon />}
                </IconButton>
            </Box>

            {!isMinimized && (
                <Box
                    className="sidebar-nav"
                    sx={{ position: 'relative', zIndex: 1, flex: 1, overflowY: 'auto', p: 0 }}
                >
                    <List sx={{ p: 0 }}>
                        {showInventorySection ? (
                            <ListItem disablePadding sx={{ display: 'block', borderBottom: '1px solid', borderColor: 'divider' }}>
                                <NavSection
                                    icon={WarehouseIcon}
                                    label="Gestión Inventarios"
                                    expanded={expandedMenu === 'inventory'}
                                    onToggle={() => toggleMenu('inventory')}
                                    hasActive={inventoryActive}
                                />
                                <Collapse in={expandedMenu === 'inventory'} timeout={220} unmountOnExit>
                                    <Box sx={{ py: 0.5 }}>
                                        {inventoryItems.map((item) => (
                                            <NavLeaf
                                                key={item.key}
                                                icon={item.icon}
                                                label={item.label}
                                                onClick={() => navigate(item.path)}
                                                active={isActive(item.path)}
                                            />
                                        ))}
                                    </Box>
                                </Collapse>
                            </ListItem>
                        ) : null}

                        {showSecuritySection ? (
                            <ListItem disablePadding sx={{ display: 'block', borderBottom: '1px solid', borderColor: 'divider' }}>
                                <NavSection
                                    icon={ShieldIcon}
                                    label="Gestión Seguridad"
                                    expanded={expandedMenu === 'security'}
                                    onToggle={() => toggleMenu('security')}
                                    hasActive={securityActive}
                                />
                                <Collapse in={expandedMenu === 'security'} timeout={220} unmountOnExit>
                                    <Box sx={{ py: 0.5 }}>
                                        {securityItems.map((item) => (
                                            <NavLeaf
                                                key={item.key}
                                                icon={item.icon}
                                                label={item.label}
                                                onClick={() => navigate(item.path)}
                                                active={isActive(item.path)}
                                            />
                                        ))}
                                    </Box>
                                </Collapse>
                            </ListItem>
                        ) : null}

                        {showMaintenanceSection ? (
                            <ListItem disablePadding sx={{ display: 'block', borderBottom: '1px solid', borderColor: 'divider' }}>
                                <NavSection
                                    icon={BuildIcon}
                                    label="Gestión Mantenimiento"
                                    expanded={expandedMenu === 'maintenance'}
                                    onToggle={() => toggleMenu('maintenance')}
                                    hasActive={maintenanceActive}
                                />
                                <Collapse in={expandedMenu === 'maintenance'} timeout={220} unmountOnExit>
                                    <Box sx={{ py: 0.5 }}>
                                        {maintenanceItems.map((item) => (
                                            <NavLeaf
                                                key={item.key}
                                                icon={item.icon}
                                                label={item.label}
                                                onClick={() => navigate(item.path)}
                                                active={isActive(item.path)}
                                            />
                                        ))}
                                    </Box>
                                </Collapse>
                            </ListItem>
                        ) : null}

                    </List>
                </Box>
            )}

            {isMinimized && (
                <Box
                    className="sidebar-nav-minimized"
                    sx={{
                        position: 'relative',
                        zIndex: 1,
                        flex: 1,
                        overflowY: 'auto',
                        p: 1.25,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 1,
                        alignItems: 'center',
                    }}
                >
                    {showInventorySection ? (
                        <MiniNavButton
                            icon={WarehouseIcon}
                            title="Gestión Inventarios"
                            active={inventoryActive}
                            onClick={() => { setIsMinimized(false); toggleMenu('inventory'); }}
                        />
                    ) : null}
                    {showMaintenanceSection ? (
                        <MiniNavButton
                            icon={BuildIcon}
                            title="Gestión Mantenimiento"
                            active={maintenanceActive}
                            onClick={() => { setIsMinimized(false); toggleMenu('maintenance'); }}
                        />
                    ) : null}
                    {showSecuritySection ? (
                        <MiniNavButton
                            icon={ShieldIcon}
                            title="Gestión Seguridad"
                            active={securityActive}
                            onClick={() => { setIsMinimized(false); toggleMenu('security'); }}
                        />
                    ) : null}
                </Box>
            )}

            <Box
                className="sidebar-footer"
                sx={{
                    position: 'relative',
                    zIndex: 1,
                    p: isMinimized ? 1.25 : 2,
                    borderTop: '1px solid',
                    borderColor: 'divider',
                    display: 'flex',
                    justifyContent: 'center',
                }}
            >
                {isMinimized ? (
                    <Tooltip title="Cerrar Sesión" placement="right" arrow>
                        <IconButton
                            onClick={handleLogout}
                            sx={(t) => ({
                                width: 48, height: 48,
                                borderRadius: '12px',
                                color: '#fff',
                                background: `linear-gradient(135deg, ${t.vars.palette.tones.rose.headerBg} 0%, ${t.vars.palette.tones.rose.headerBg} 100%)`,
                                boxShadow: t.palette.tones.rose.shadowResting,
                                transition: 'box-shadow 0.22s ease, transform 0.22s ease, background 0.22s ease',
                                '&:hover': {
                                    background: t.palette.primary.dark,
                                    boxShadow: t.palette.tones.rose.shadowHover,
                                    transform: 'translateY(-1px)',
                                },
                            })}
                        >
                            <LogoutIcon sx={{ fontSize: 20 }} />
                        </IconButton>
                    </Tooltip>
                ) : (
                    <Button
                        fullWidth
                        variant="contained"
                        startIcon={<LogoutIcon sx={{ fontSize: 18 }} />}
                        onClick={handleLogout}
                        disableElevation
                        sx={(t) => ({
                            textTransform: 'none',
                            fontWeight: 700,
                            fontSize: '0.86rem',
                            borderRadius: '10px',
                            py: 1,
                            letterSpacing: '0.01em',
                            color: '#fff',
                            background: `linear-gradient(135deg, ${t.vars.palette.tones.rose.headerBg} 0%, ${t.vars.palette.tones.rose.headerBg} 100%)`,
                            boxShadow: t.palette.tones.rose.shadowResting,
                            transition: 'box-shadow 0.22s ease, transform 0.22s ease, background 0.22s ease',
                            '&:hover': {
                                background: t.palette.primary.dark,
                                boxShadow: t.palette.tones.rose.shadowHover,
                                transform: 'translateY(-1px)',
                            },
                        })}
                    >
                        Cerrar Sesión
                    </Button>
                )}
            </Box>
        </Box>
    );
}

export default Sidebar;
