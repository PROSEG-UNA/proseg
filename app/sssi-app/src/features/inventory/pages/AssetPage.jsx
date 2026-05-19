import { useState, useCallback } from 'react';
import {
  Box,
  Typography,
  Container,
  Button,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import Catalog from '../components/catalog/Catalog';
import AssetTable from '../components/asset/AssetTable.jsx';
import AssetFormModal from '../components/asset/AssetFormModal.jsx';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';

export function AssetPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { hasPermission } = usePermissions();

  const handleRefresh = useCallback(() => setRefreshKey((k) => k + 1), []);

  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));
  const canViewAssets = hasPermission(PERMISSIONS.INVENTORY.READ);
  const canCreateAsset = hasPermission(PERMISSIONS.INVENTORY.MANAGE);

  return (
    <Box className="asset-page">
        <Header
          title="Gestión de Activos"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />


        <Container maxWidth="xl" className="asset-content" sx={{ pb: 3 }}>
          {canViewAssets ? (
            <>
              <Catalog />
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                <Typography
                  variant="h5"
                  sx={{
                    fontWeight: 700,
                    color: 'primary.icon',
                    fontSize: '1.65rem',
                    letterSpacing: '0.3px',
                  }}
                >
                  Lista de Activos
                </Typography>
                {canCreateAsset ? (
                  <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
                    Crear
                  </Button>
                ) : null}
              </Box>

              <AssetTable refreshKey={refreshKey} onRefresh={handleRefresh} />
              <AssetFormModal open={createOpen} onClose={() => setCreateOpen(false)} onSaved={handleRefresh} />
            </>
          ) : (
            <AccessDeniedState />
          )}
        </Container>
    </Box>
  );
}

export default AssetPage;