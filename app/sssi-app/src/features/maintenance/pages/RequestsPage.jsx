import { usePermissions } from '../../../common/hooks/index.js';

import { useEffect, useMemo, useState } from 'react';
import { Box, Container, Tab, Tabs, Typography, useMediaQuery, useTheme } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ConstructionIcon from '@mui/icons-material/Construction';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import { Header } from '../../../common/components/Header';
import { PageHeader } from '../../../common/components/index.js';
import { NavDrawer } from '../../../common/components/NavDrawer';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../common/constants/permissions';
import MaintenanceRequestTable from '../components/request/MaintenanceRequestTable.jsx';
import MaintenanceRequestFormModal from '../components/request/MaintenanceRequestFormModal.jsx';
import MaintenanceTechnicianTable from '../components/technician/MaintenanceTechnicianTable.jsx';
import MaintenanceTechnicianFormModal from '../components/technician/MaintenanceTechnicianFormModal.jsx';

export default function RequestsPage() {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [tabIndex, setTabIndex] = useState(0);

    const [requestFormState, setRequestFormState] = useState({ open: false, requestId: null, initialCompanyId: '' });
    const [technicianFormState, setTechnicianFormState] = useState({ open: false, technicianId: null, maintenanceRequestId: '' });

    const [requestsRefresh, setRequestsRefresh] = useState(0);
    const [techniciansRefresh, setTechniciansRefresh] = useState(0);

    const { hasPermission, hasAnyPermission } = usePermissions();
    const theme = useTheme();
    const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

    const canViewRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.READ,
        PERMISSIONS.MAINTENANCE.REQUESTS.MANAGE,
        PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    ]);

    const canViewTechnicians = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.TECHNICIANS.READ,
        PERMISSIONS.MAINTENANCE.TECHNICIANS.MANAGE,
        PERMISSIONS.MAINTENANCE.TECHNICIANS.DELETE,
    ]);

    const tabs = useMemo(() => [
        canViewRequests ? { key: 'requests', label: 'Solicitudes', icon: ConstructionIcon } : null,
        canViewTechnicians ? { key: 'technicians', label: 'Técnicos', icon: MiscellaneousServicesIcon } : null,
    ].filter(Boolean), [canViewRequests, canViewTechnicians]);

    const currentTab = tabs[tabIndex] ?? tabs[0] ?? null;

    useEffect(() => {
        if (tabIndex >= tabs.length) setTabIndex(0);
    }, [tabIndex, tabs.length]);

    const handleTabChange = (_, newValue) => setTabIndex(newValue);

    const openCreateRequest = (initialCompanyId = '') => setRequestFormState({ open: true, requestId: null, initialCompanyId });
    const openEditRequest = (request) => setRequestFormState({ open: true, requestId: request.id, initialCompanyId: request.companyId ?? '' });
    const openCreateTechnician = (maintenanceRequestId = '') => setTechnicianFormState({ open: true, technicianId: null, maintenanceRequestId });
    const openEditTechnician = (technician) => setTechnicianFormState({ open: true, technicianId: technician.id, maintenanceRequestId: '' });

    const refreshRequests = () => setRequestsRefresh((v) => v + 1);
    const refreshTechnicians = () => setTechniciansRefresh((v) => v + 1);

    if (!canViewRequests && !canViewTechnicians) return <AccessDeniedState />;

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
                    description="Gestión de solicitudes y técnicos."
                    action={currentTab?.key === 'requests' && hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={() => openCreateRequest('')} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                    <Tabs value={tabIndex} onChange={handleTabChange} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile>
                        {tabs.map((tab) => {
                            const Icon = tab.icon;
                            return (
                                <Tab
                                    key={tab.key}
                                    label={tab.label}
                                    icon={<Icon sx={{ fontSize: 18 }} />}
                                    iconPosition="start"
                                    sx={{ textTransform: 'none', fontWeight: 700 }}
                                />
                            );
                        })}
                    </Tabs>
                </Box>

                <Box sx={{ pt: 3 }}>
                    {currentTab?.key === 'requests' && (
                        <Box>
                            <MaintenanceRequestTable
                                refreshKey={requestsRefresh}
                                onRefresh={refreshRequests}
                                onEditRequest={openEditRequest}
                                onCreateTechnicianForRequest={openCreateTechnician}
                            />
                        </Box>
                    )}

                    {currentTab?.key === 'technicians' && (
                        <Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                                <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon' }}>
                                    Lista de técnicos
                                </Typography>
                                {hasPermission(PERMISSIONS.MAINTENANCE.TECHNICIANS.MANAGE) ? (
                                    <PrimaryButton startIcon={<AddIcon />} onClick={() => openCreateTechnician('')} sx={{ px: '28px' }}>
                                        Crear
                                    </PrimaryButton>
                                ) : null}
                            </Box>
                            <MaintenanceTechnicianTable
                                refreshKey={techniciansRefresh}
                                onRefresh={refreshTechnicians}
                                onEditTechnician={openEditTechnician}
                            />
                        </Box>
                    )}
                </Box>
            </Container>

            <MaintenanceRequestFormModal
                open={requestFormState.open}
                requestId={requestFormState.requestId}
                initialCompanyId={requestFormState.initialCompanyId}
                onClose={() => setRequestFormState({ open: false, requestId: null, initialCompanyId: '' })}
                onSaved={refreshRequests}
            />

            <MaintenanceTechnicianFormModal
                open={technicianFormState.open}
                technicianId={technicianFormState.technicianId}
                maintenanceRequestId={technicianFormState.maintenanceRequestId}
                onClose={() => setTechnicianFormState({ open: false, technicianId: null, maintenanceRequestId: '' })}
                onSaved={refreshTechnicians}
            />
        </Box>
    );
}

