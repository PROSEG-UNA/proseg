import { useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Button,
  Menu,
  MenuItem,
  Container,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ImportIcon from '../../../common/components/icons/ImportIcon.jsx';
import ExportIcon from '../../../common/components/icons/ExportIcon.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import Catalog from '../components/catalog/Catalog';
import AssetTable from '../components/asset/AssetTable.jsx';
import AssetFormModal from '../components/asset/AssetFormModal.jsx';
import ImportAssetsModal from '../components/asset/ImportAssetsModal.jsx';
import { exportAssets } from '../services/assetExportService.js';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { queryKeys } from '../../../common/query';

export function AssetPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [exportAnchorEl, setExportAnchorEl] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [alert, setAlert] = useState(null);
  const queryClient = useQueryClient();
  const { hasPermission } = usePermissions();

  const handleRefresh = useCallback(
    () => { void queryClient.invalidateQueries({ queryKey: queryKeys.inventory.assets() }); },
    [queryClient]
  );

  const canViewAssets = hasPermission(PERMISSIONS.INVENTORY.READ);
  const canCreateAsset = hasPermission(PERMISSIONS.INVENTORY.MANAGE);
  const canImportAssets = hasPermission(PERMISSIONS.INVENTORY.IMPORT);
  const canExportAssets = hasPermission(PERMISSIONS.INVENTORY.IMPORT) || hasPermission(PERMISSIONS.INVENTORY.EXPORT);

  const exportMenuOpen = Boolean(exportAnchorEl);

  const handleOpenExportMenu = (event) => {
    setExportAnchorEl(event.currentTarget);
  };

  const handleCloseExportMenu = () => {
    if (exporting) return;
    setExportAnchorEl(null);
  };

  const triggerBrowserDownload = (blob, filename) => {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
  };

  const handleExport = async (format) => {
    setExporting(true);
    try {
      const { blob, filename } = await exportAssets({ format });
      triggerBrowserDownload(blob, filename);
      setAlert({
        type: 'success',
        message: `Exportación completada (${format.toUpperCase()})`,
      });
    } catch (error) {
      const message = error?.response?.data?.message || 'No fue posible exportar los activos';
      setAlert({ type: 'error', message });
    } finally {
      setExporting(false);
      setExportAnchorEl(null);
    }
  };

  return (
    <Box className="asset-page">


        <Container maxWidth="xl" className="asset-content">
          {canViewAssets ? (
            <>
              <Catalog />
              <PageHeader
                title="Lista de Activos"
                description="Administración y seguimiento de activos del inventario."
                action={(canImportAssets || canExportAssets || canCreateAsset) ? (
                  <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                    {canImportAssets && (
                      <Button
                        variant="outlined"
                        startIcon={<ImportIcon style={{ fontSize: 16, marginRight: 2 }} />}
                        onClick={() => setImportOpen(true)}
                        sx={{
                          textTransform: 'none',
                          fontWeight: 700,
                          borderRadius: '10px',
                          px: 2.5,
                        }}
                      >
                        Importar
                      </Button>
                    )}
                    {canExportAssets && (
                      <>
                        <Button
                          variant="outlined"
                          startIcon={<ExportIcon style={{ fontSize: 16, marginRight: 2 }} />}
                          onClick={handleOpenExportMenu}
                          sx={{
                            textTransform: 'none',
                            fontWeight: 700,
                            borderRadius: '10px',
                            px: 2.5,
                          }}
                          disabled={exporting}
                        >
                          {exporting ? 'Exportando...' : 'Exportar'}
                        </Button>
                        <Menu
                          anchorEl={exportAnchorEl}
                          open={exportMenuOpen}
                          onClose={handleCloseExportMenu}
                          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                        >
                          <MenuItem onClick={() => handleExport('xlsx')} disabled={exporting}>
                            Exportar en Excel (.xlsx)
                          </MenuItem>
                          <MenuItem onClick={() => handleExport('csv')} disabled={exporting}>
                            Exportar en CSV (.csv)
                          </MenuItem>
                        </Menu>
                      </>
                    )}
                    {canCreateAsset && (
                      <PrimaryButton startIcon={<AddIcon />} onClick={() => setCreateOpen(true)} sx={{ px: '28px' }}>
                        Crear
                      </PrimaryButton>
                    )}
                  </Box>
                ) : null}
                sx={{ mb: 3 }}
                titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
              />

              <AssetTable />
              <AssetFormModal open={createOpen} onClose={() => setCreateOpen(false)} onSaved={handleRefresh} />
              <ImportAssetsModal open={importOpen} onClose={() => setImportOpen(false)} onImported={handleRefresh} />
            </>
          ) : (
            <AccessDeniedState />
          )}
        </Container>

      <DialogModal
        open={!!alert}
        type={alert?.type}
        message={alert?.message}
        onClose={() => setAlert(null)}
      />
    </Box>
  );
}

export default AssetPage;