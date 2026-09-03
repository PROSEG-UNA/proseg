import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Container, Typography, Tabs, Tab } from '@mui/material';
import PendingActionsOutlinedIcon from '@mui/icons-material/PendingActionsOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { usePermissions } from '../../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { useMaintenanceRegisterData } from '../../hooks/register/useMaintenanceRegisterData';
import RegisterCard from '../../components/register/RegisterCard.jsx';
import RegisterTable from '../../components/register/RegisterTable.jsx';
import RegisterFormModal from '../../components/register/RegisterFormModal.jsx';
import { queryKeys } from '../../../../common/query';

export default function RegisterPage() {
    const [modalState, setModalState] = useState({ open: false, requestId: null });
    const [tabIndex, setTabIndex] = useState(0);
    const queryClient = useQueryClient();

    const { hasPermission, hasAnyPermission } = usePermissions();

    const canView = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REGISTERS.READ,
        PERMISSIONS.MAINTENANCE.REGISTERS.MANAGE,
        PERMISSIONS.MAINTENANCE.REGISTERS.HISTORY,
    ]);
    const canManage = hasPermission(PERMISSIONS.MAINTENANCE.REGISTERS.MANAGE);

    const { rows: cards, loading: loadingCards } = useMaintenanceRegisterData({
        mode: 'assigned',
        pageSize: 50,
    });

    const openRegister = (register) => setModalState({ open: true, requestId: register.id });
    const closeRegister = () => setModalState({ open: false, requestId: null });
    const refreshAll = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.maintenance.register() });
    };

    if (!canView) return <AccessDeniedState />;

    return (
        <Box className="maintenance-register-page">
            <Container maxWidth="xl" sx={{ pb: 3, pt: 3 }}>
                <PageHeader
                    title="Registros"
                    description="Registra el mantenimiento realizado a los activos de tus solicitudes asignadas"
                />

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                    <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile>
                        <Tab
                            label="Solicitudes pendientes asignadas"
                            icon={<PendingActionsOutlinedIcon sx={{ fontSize: 18 }} />}
                            iconPosition="start"
                            sx={{ textTransform: 'none', fontWeight: 700 }}
                        />
                        <Tab
                            label="Historial de registros"
                            icon={<HistoryOutlinedIcon sx={{ fontSize: 18 }} />}
                            iconPosition="start"
                            sx={{ textTransform: 'none', fontWeight: 700 }}
                        />
                    </Tabs>
                </Box>

                <Box sx={{ pt: 3 }}>
                    {tabIndex === 0 && (
                        <>
                            {!loadingCards && cards.length === 0 ? (
                                <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>
                                    No tienes solicitudes de mantenimiento pendientes asignadas.
                                </Typography>
                            ) : (
                                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1fr 1fr 1fr' }, gap: 2 }}>
                                    {cards.map((register) => (
                                        <RegisterCard key={register.id} register={register} onClick={openRegister} />
                                    ))}
                                </Box>
                            )}
                        </>
                    )}

                    {tabIndex === 1 && (
                        <RegisterTable onOpenRegister={openRegister} />
                    )}
                </Box>
            </Container>

            <RegisterFormModal
                open={modalState.open}
                requestId={modalState.requestId}
                canManage={canManage}
                onClose={closeRegister}
                onSaved={refreshAll}
            />
        </Box>
    );
}
