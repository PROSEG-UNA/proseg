import { usePermissions } from '../../../../common/hooks/index.js';

import { useState } from 'react';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import MaintenanceRequestTable from '../../components/request/MaintenanceRequestTable.jsx';
import MaintenanceRequestFormModal from '../../components/request/MaintenanceRequestFormModal.jsx';

export default function RequestsPage() {
    const [requestFormState, setRequestFormState] = useState({ open: false, requestId: null, initialCompanyId: '' });
    const [requestsRefresh, setRequestsRefresh] = useState(0);

    const { hasAnyPermission } = usePermissions();

    const canViewRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.READ,
        PERMISSIONS.MAINTENANCE.REQUESTS.REQUEST,
        PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE,
        PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    ]);

    const canCreateRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.REQUEST,
    ]);

    const openCreateRequest = (initialCompanyId = '') => setRequestFormState({ open: true, requestId: null, initialCompanyId });
    const openEditRequest = (request) => setRequestFormState({ open: true, requestId: request.id, initialCompanyId: request.companyId ?? '' });

    const refreshRequests = () => setRequestsRefresh((v) => v + 1);

    if (!canViewRequests) return <AccessDeniedState />;

    return (
        <Box className="maintenance-requests-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Solicitudes"
                    description="Gestión de solicitudes"
                    action={canCreateRequests ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={() => openCreateRequest('')} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <MaintenanceRequestTable
                        refreshKey={requestsRefresh}
                        onRefresh={refreshRequests}
                        onEditRequest={openEditRequest}
                    />
                </Box>
            </Container>

            <MaintenanceRequestFormModal
                open={requestFormState.open}
                requestId={requestFormState.requestId}
                initialCompanyId={requestFormState.initialCompanyId}
                onClose={() => setRequestFormState({ open: false, requestId: null, initialCompanyId: '' })}
                onSaved={refreshRequests}
            />
        </Box>
    );
}
