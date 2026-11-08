import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  IconButton,
  Badge,
  Menu,
  MenuItem,
  ListSubheader,
  Snackbar,
  Alert,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { usePermissions } from '../hooks';
import { PERMISSIONS } from '../constants/permissions';
import { useAssetDashboard } from '../../features/inventory/hooks/useAssetDashboard';

const SESSION_KEY = 'proseg:expirationAlertShown';

export function ExpirationAlerts({ sx }) {
  const { hasPermission } = usePermissions();
  const canView = hasPermission(PERMISSIONS.INVENTORY.READ);
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const { summary } = useAssetDashboard(30, { enabled: canView, staleTime: 5 * 60 * 1000 });

  const warranty = summary?.warranty?.expiringSoon ?? 0;
  const firmware = summary?.firmware?.expiringSoon ?? 0;
  const total = warranty + firmware;

  useEffect(() => {
    if (!canView || total <= 0) return;
    try {
      if (!sessionStorage.getItem(SESSION_KEY)) {
        sessionStorage.setItem(SESSION_KEY, '1');
        setSnackbarOpen(true);
      }
    } catch {
      // ignore
    }
  }, [canView, total]);

  if (!canView) return null;

  const menuOpen = Boolean(anchorEl);

  const handleCloseMenu = () => setAnchorEl(null);

  const handleGoToPanel = () => {
    navigate('/inventario/vencimientos');
    handleCloseMenu();
  };

  return (
    <>
      <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} aria-label="Alertas de vencimiento" sx={sx}>
        <Badge color="warning" badgeContent={total}>
          <NotificationsIcon sx={{ fontSize: 20 }} />
        </Badge>
      </IconButton>
      <Menu anchorEl={anchorEl} open={menuOpen} onClose={handleCloseMenu}>
        {total === 0 ? (
          <MenuItem disabled>Sin activos por vencer</MenuItem>
        ) : (
          <>
            <ListSubheader disableSticky>Por vencer (30 días)</ListSubheader>
            <MenuItem disabled>Garantía: {warranty}</MenuItem>
            <MenuItem disabled>Soporte de firmware: {firmware}</MenuItem>
            <MenuItem onClick={handleGoToPanel}>Ver panel de vencimientos</MenuItem>
          </>
        )}
      </Menu>
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={8000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity="warning" onClose={() => setSnackbarOpen(false)}>
          Hay {total} activo(s) con garantía o soporte por vencer en 30 días
        </Alert>
      </Snackbar>
    </>
  );
}

export default ExpirationAlerts;
