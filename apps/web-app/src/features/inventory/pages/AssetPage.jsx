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
import Catalog from '../components/catalog/Catalog';
import AssetTable from '../components/asset/AssetTable.jsx';
import AssetFormModal from '../components/asset/AssetFormModal.jsx';

export function AssetPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = useCallback(() => setRefreshKey((k) => k + 1), []);

  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <Box className="asset-page">
        <Header
          title="Gestión de Activos"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />


        <Catalog />
        <Container maxWidth="xl" className="asset-content" sx={{ pb: 3 }}>
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
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
              Crear
            </Button>
          </Box>

          <AssetTable refreshKey={refreshKey} onRefresh={handleRefresh} />
          <AssetFormModal open={createOpen} onClose={() => setCreateOpen(false)} onSaved={handleRefresh} />
        </Container>
    </Box>
  );
}

export default AssetPage;