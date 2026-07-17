import { Box, Container } from '@mui/material';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import CleaningPanel from '../../components/cleaning/CleaningPanel.jsx';

export default function CleaningPage() {
    const { hasAnyPermission } = usePermissions();

    const canViewCleaning = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.READ,
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    if (!canViewCleaning) return <AccessDeniedState />;

    return (
        <Box className="transport-cleaning-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Depuración"
                    description="Importa y registra giras depuradas desde el archivo previo de comisión."
                />

                <Box sx={{ pt: 3 }}>
                    <CleaningPanel />
                </Box>
            </Container>
        </Box>
    );
}
