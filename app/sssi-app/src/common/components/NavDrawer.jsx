import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../features/auth/hooks/useAuth';
import {
  Box,
  Collapse,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
} from '@mui/material';
import { RoseButton } from './RoseButton';
import AppsIcon from '@mui/icons-material/Apps';
import CloseIcon from '@mui/icons-material/Close';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import ShieldIcon from '@mui/icons-material/Shield';
import { useColorScheme } from '@mui/material/styles';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../constants/permissions';

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

const iconBoxSx = (t, { active = false } = {}) => ({
  width: 34, height: 34, mr: 1.5,
  borderRadius: '10px',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  background: active
    ? t.vars.palette.tones.rose.softSubtle
    : 'hsla(220, 20%, 50%, 0.06)',
  border: '1px solid',
  borderColor: active ? t.vars.palette.tones.rose.ring : 'divider',
  transition: 'border-color 0.22s ease, transform 0.22s ease, background 0.22s ease',
  flexShrink: 0,
  ...t.applyStyles('dark', {
    background: active
      ? t.vars.palette.tones.rose.softSubtle
      : 'hsla(220, 20%, 80%, 0.04)',
    borderColor: active ? 'rgba(255,255,255,0.35)' : 'divider',
  }),
});

function NavSection({ icon: Icon, label, expanded, onToggle, children }) {
  return (
    <Box sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
      <ListItemButton
        onClick={onToggle}
        sx={(t) => ({
          paddingTop: '10px !important',
          paddingBottom: '10px !important',
          px: 2,
          color: 'text.primary',
          transition: 'background 0.18s ease',
          '&:hover': { bgcolor: 'hsla(220, 20%, 50%, 0.05)' },
          '&:hover .nav-section-icon': {
            borderColor: t.vars.palette.tones.rose.ring,
          },
          ...t.applyStyles('dark', {
            '&:hover': { bgcolor: 'hsla(220, 20%, 80%, 0.04)' },
          }),
        })}
      >
        <Box className="nav-section-icon" sx={(t) => iconBoxSx(t)}>
          <Icon sx={{ fontSize: 18, color: 'text.secondary', transition: 'color 0.22s ease' }} />
        </Box>
        <ListItemText
          primary={label}
          slotProps={{ primary: { sx: { fontSize: '0.9rem', fontWeight: 600, letterSpacing: '-0.01em' } } }}
        />
        {expanded
          ? <ExpandLessIcon sx={{ fontSize: 20, color: 'text.disabled' }} />
          : <ExpandMoreIcon sx={{ fontSize: 20, color: 'text.disabled' }} />}
      </ListItemButton>
      <Collapse in={expanded} timeout={220} unmountOnExit>
        <Box sx={{ py: 0.5 }}>
          {children}
        </Box>
      </Collapse>
    </Box>
  );
}

function NavLeaf({ icon: Icon, label, onClick }) {
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
        color: 'text.secondary',
        transition: 'color 0.18s ease, background 0.18s ease, padding-left 0.18s ease',
        '& .MuiSvgIcon-root': { color: 'inherit' },
        '&:hover, &:focus, &:active': {
          color: t.vars.palette.text.primary,
          bgcolor: 'hsla(220, 20%, 50%, 0.07)',
          pl: 5,
          ...t.applyStyles('dark', { bgcolor: 'hsla(220, 20%, 80%, 0.07)' }),
        },
      })}
    >
      <Icon sx={{ fontSize: 18, color: 'inherit' }} />
      <Typography sx={{ fontSize: '0.84rem', fontWeight: 500, color: 'inherit' }}>{label}</Typography>
    </Box>
  );
}

export function NavDrawer({ open, onClose }) {
  const { handleLogout } = useAuth();
  const navigate = useNavigate();
  const [expandedMenu, setExpandedMenu] = useState(null);
  const { mode, systemMode } = useColorScheme();
  const { hasAnyPermission } = usePermissions();
  const resolvedMode = mode === 'system' ? systemMode : mode;

  const toggleMenu = (menu) => setExpandedMenu(expandedMenu === menu ? null : menu);
  const handleNavigation = (path) => { navigate(path); onClose(); };
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

  const inventoryItems = canViewInventorySection
    ? [{ key: 'assets', icon: AppsIcon, label: 'Activos', path: '/inventario/activos' }]
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

  return (
    <Drawer
      anchor="left"
      open={open}
      onClose={onClose}
      slotProps={{
        backdrop: { sx: { backdropFilter: 'blur(3px)' } },
        paper: {
          sx: (t) => ({
            width: 280,
            border: 'none',
            borderRight: '1px solid',
            borderColor: 'divider',
            position: 'relative',
            overflow: 'hidden',
            color: 'text.primary',
            ...panelSurfaceSx(t),
          }),
        },
      }}
    >
      <Box
        role="presentation"
        sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column' }}
      >
        <Box
          sx={{
            p: 2,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid',
            borderColor: 'divider',
          }}
        >
          <img
            src={resolvedMode === 'dark' ? '/logo_una_blanco.png' : '/logo_una.png'}
            alt="Logo UNA"
            onClick={() => handleNavigation('/home')}
            style={{ maxWidth: '50%', height: 'auto', maxHeight: '50px', cursor: 'pointer' }}
          />
          <IconButton
            onClick={onClose}
            sx={(t) => ({
              color: 'text.secondary',
              borderRadius: '10px',
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
            <CloseIcon sx={{ fontSize: 20 }} />
          </IconButton>
        </Box>

        <List sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
          {showInventorySection ? (
            <ListItem disablePadding sx={{ display: 'block' }}>
              <NavSection
                icon={WarehouseIcon}
                label="Gestión Inventarios"
                expanded={expandedMenu === 'inventory'}
                onToggle={() => toggleMenu('inventory')}
              >
                {inventoryItems.map((item) => (
                  <NavLeaf
                    key={item.key}
                    icon={item.icon}
                    label={item.label}
                    onClick={() => handleNavigation(item.path)}
                  />
                ))}
              </NavSection>
            </ListItem>
          ) : null}

          {showSecuritySection ? (
            <ListItem disablePadding sx={{ display: 'block' }}>
              <NavSection
                icon={ShieldIcon}
                label="Gestión Seguridad"
                expanded={expandedMenu === 'security'}
                onToggle={() => toggleMenu('security')}
              >
                {securityItems.map((item) => (
                  <NavLeaf
                    key={item.key}
                    icon={item.icon}
                    label={item.label}
                    onClick={() => handleNavigation(item.path)}
                  />
                ))}
              </NavSection>
            </ListItem>
          ) : null}
        </List>

        <Box sx={{ p: 2, borderTop: '1px solid', borderColor: 'divider' }}>
          <RoseButton
            fullWidth
            startIcon={<LogoutIcon sx={{ fontSize: 18 }} />}
            onClick={handleLogout}
          >
            Cerrar Sesión
          </RoseButton>
        </Box>
      </Box>
    </Drawer>
  );
}

export default NavDrawer;
