import { Box, Container } from '@mui/material';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import AssignmentPanel from '../../components/assignment/AssignmentPanel.jsx';

export default function AssignmentPage() {
    const { hasAnyPermission } = usePermissions();

    const canViewAssignment = hasAnyPermission([
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    if (!canViewAssignment) return <AccessDeniedState />;

    return (
        <Box className="transport-assignment-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Asignaciones"
                    description="Generación y actualización de asignaciones."
                />

                <Box sx={{ pt: 3 }}>
                    <AssignmentPanel />
                </Box>
            </Container>
        </Box>
    );
}
