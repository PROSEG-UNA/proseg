import { usePermissions } from '../../../common/hooks/index.js';

import { useState } from 'react';
import { Box, Container, useMediaQuery, useTheme } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { Header } from '../../../common/components/Header';
import { PageHeader } from '../../../common/components/index.js';
import { NavDrawer } from '../../../common/components/NavDrawer';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../common/constants/permissions';
import MaintenanceRequestTable from '../components/request/MaintenanceRequestTable.jsx';
import MaintenanceRequestFormModal from '../components/request/MaintenanceRequestFormModal.jsx';

export default function RequestsPage() {
    const [drawerOpen, setDrawerOpen] = useState(false);

    const [requestFormState, setRequestFormState] = useState({ open: false, requestId: null, initialCompanyId: '' });
    const [requestsRefresh, setRequestsRefresh] = useState(0);

    const { hasPermission, hasAnyPermission } = usePermissions();
    const theme = useTheme();
    const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

    const canViewRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.READ,
        PERMISSIONS.MAINTENANCE.REQUESTS.MANAGE,
        PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    ]);

    const openCreateRequest = (initialCompanyId = '') => setRequestFormState({ open: true, requestId: null, initialCompanyId });
    const openEditRequest = (request) => setRequestFormState({ open: true, requestId: request.id, initialCompanyId: request.companyId ?? '' });

    const refreshRequests = () => setRequestsRefresh((v) => v + 1);

    if (!canViewRequests) return <AccessDeniedState />;

    return (
        <Box className="maintenance-requests-page">
            <Header
                title="Gestión de Mantenimiento"
                onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
            />
            <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

            <Container maxWidth="xl" sx={{ pb: 3, pt: 3 }}>
                <PageHeader
                    title="Solicitudes"
                    description="Gestión de solicitudes"
                    action={hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.MANAGE) ? (
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
