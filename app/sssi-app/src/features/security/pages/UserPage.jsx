import { useState } from 'react';
import {
  Box,
  Container,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import UsersTable from '../components/UsersTable.jsx';
import CreateUserModal from '../components/CreateUserModal.jsx';
import '../css/UserPage.css';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';

export function UserPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { hasPermission } = usePermissions();
  const canViewUsers = hasPermission(PERMISSIONS.USERS.READ_ALL);
  const canCreateUser = hasPermission(PERMISSIONS.USERS.CREATE);

  const handleRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  return (
    <Box className="user-page">

        <Container maxWidth="xl" className="user-content">
          {canViewUsers ? (
            <>
              <PageHeader
                title="Lista de Usuarios"
                description="Administración de cuentas, acceso y permisos de usuarios."
                action={canCreateUser ? (
                  <PrimaryButton startIcon={<AddIcon />} onClick={() => setCreateOpen(true)} sx={{ px: '28px' }}>
                    Crear
                  </PrimaryButton>
                ) : null}
                sx={{ mb: 3 }}
                titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
              />

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