import { useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Container,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import '../css/RolePage.css';
import RolesTable from '../components/RolesTable.jsx';
import RoleFormModal from '../components/RoleFormModal.jsx';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { queryKeys } from '../../../common/query';

export function RolePage() {
  const [createOpen, setCreateOpen] = useState(false);
  const queryClient = useQueryClient();
  const { hasPermission } = usePermissions();

  const handleRefresh = useCallback(
    () => { void queryClient.invalidateQueries({ queryKey: queryKeys.security.roles() }); },
    [queryClient]
  );
  const canViewRoles = hasPermission(PERMISSIONS.ROLES.READ_COMPOSITE);
  const canCreateRole = hasPermission(PERMISSIONS.ROLES.CREATE);

  return (
      <Box className="role-page">

        <Container maxWidth="xl" className="role-content">
          {canViewRoles ? (
            <>
              <PageHeader
                title="Lista de Roles"
                description="Administración de perfiles de acceso y permisos del sistema."
                action={canCreateRole ? (
                  <PrimaryButton startIcon={<AddIcon />} onClick={() => setCreateOpen(true)} sx={{ px: '28px' }}>
                    Crear
                  </PrimaryButton>
                ) : null}
                sx={{ mb: 3 }}
                titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
              />

              <RolesTable />
              <RoleFormModal open={createOpen} onClose={() => setCreateOpen(false)} onSaved={handleRefresh} />
            </>
          ) : (
            <AccessDeniedState />
          )}
        </Container>
      </Box>
  );
}

export default RolePage;
