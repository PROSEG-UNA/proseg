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
import '../css/RolePage.css';
import RolesTable from '../components/RolesTable.jsx';
import RoleFormModal from '../components/RoleFormModal.jsx';

export function RolePage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = useCallback(() => setRefreshKey((k) => k + 1), []);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  return (
      <Box className="role-page">
        <Header
            title="Gestión de Roles"
            onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

        <Container maxWidth="lg" className="role-content">
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
            <Typography
                variant="h5"
                sx={{
                  fontWeight: 700,
                  color: 'primary.icon',
                  fontSize: '1.8rem',
                  letterSpacing: '0.3px',
                }}
            >
              Lista de Roles
            </Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
              Crear
            </Button>
          </Box>

          <RolesTable refreshKey={refreshKey} onRefresh={handleRefresh} />
          <RoleFormModal open={createOpen} onClose={() => setCreateOpen(false)} onSaved={handleRefresh} />
        </Container>
      </Box>
  );
}

export default RolePage;
