import { useState, useCallback } from 'react';
import {
  Box,
  Container,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import Catalog from '../components/catalog/Catalog';
import AssetTable from '../components/asset/AssetTable.jsx';
import AssetFormModal from '../components/asset/AssetFormModal.jsx';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';

export function AssetPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { hasPermission } = usePermissions();

  const handleRefresh = useCallback(() => setRefreshKey((k) => k + 1), []);

  const canViewAssets = hasPermission(PERMISSIONS.INVENTORY.READ);
  const canCreateAsset = hasPermission(PERMISSIONS.INVENTORY.MANAGE);

  return (
    <Box className="asset-page">


        <Container maxWidth="xl" className="asset-content">
          {canViewAssets ? (
            <>
              <Catalog />
              <PageHeader
                title="Lista de Activos"
                description="Administración y seguimiento de activos del inventario."
                action={canCreateAsset ? (
                  <PrimaryButton startIcon={<AddIcon />} onClick={() => setCreateOpen(true)} sx={{ px: '28px' }}>
                    Crear
                  </PrimaryButton>
                ) : null}
                sx={{ mb: 3 }}
                titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
              />

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