import { useState } from 'react';
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
import UsersTable from '../components/UsersTable.jsx';
import CreateUserModal from '../components/CreateUserModal.jsx';
import '../css/UserPage.css';

export function UserPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const handleRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  return (
    <Box className="user-page">
        <Header
          title="Gestión de Usuarios"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

        <Container maxWidth="lg" className="user-content">
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
              Lista de Usuarios
            </Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
              Crear
            </Button>
          </Box>

          <UsersTable refreshKey={refreshKey} />
          <CreateUserModal
            open={createOpen}
            onClose={() => setCreateOpen(false)}
            onSaved={handleRefresh}
          />
        </Container>
    </Box>
  );
}

export default UserPage;