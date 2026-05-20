import { useState } from 'react';
import {
  Box,
  Typography,
  Container,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { RoseButton } from '../../../common/components/RoseButton';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import UsersTable from '../components/UsersTable.jsx';
import CreateUserModal from '../components/CreateUserModal.jsx';
import '../css/UserPage.css';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';

export function UserPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { hasPermission } = usePermissions();
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));
  const canViewUsers = hasPermission(PERMISSIONS.USERS.READ_ALL);
  const canCreateUser = hasPermission(PERMISSIONS.USERS.CREATE);

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

        <Container maxWidth="xl" className="user-content">
          {canViewUsers ? (
            <>
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
                {canCreateUser ? (
                  <RoseButton startIcon={<AddIcon />} onClick={() => setCreateOpen(true)} sx={{ px: '28px' }}>
                    Crear
                  </RoseButton>
                ) : null}
              </Box>

              <UsersTable refreshKey={refreshKey} />
              <CreateUserModal
                open={createOpen}
                onClose={() => setCreateOpen(false)}
                onSaved={handleRefresh}
              />
            </>
          ) : (
            <AccessDeniedState />
          )}
        </Container>
    </Box>
  );
}

export default UserPage;