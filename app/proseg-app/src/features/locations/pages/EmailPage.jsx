import { Box, Container } from '@mui/material';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import BuildingEmailManager from '../components/email/BuildingEmailManager.jsx';

export function EmailPage() {
    const { hasPermission } = usePermissions();
    const canView = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.READ)
        || hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.MANAGE)
        || hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.DELETE);

    return (
        <Box>
            <Container maxWidth="xl" sx={{ pt: 1, pb: 2, mt: 2 }}>
                {canView ? (
                    <>
                        <PageHeader
                            title="Correos"
                            description="Gestión de correos asociados a los edificios."
                            sx={{ mb: 3 }}
                            titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
                        />

                        <BuildingEmailManager />
                    </>
                ) : (
                    <AccessDeniedState />
                )}
            </Container>
        </Box>
    );
}

export default EmailPage;
