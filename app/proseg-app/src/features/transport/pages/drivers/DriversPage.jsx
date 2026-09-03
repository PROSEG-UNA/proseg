import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import DriversTable from '../../components/drivers/DriversTable.jsx';
import DriverFormModal from '../../components/drivers/DriverFormModal.jsx';
import { queryKeys } from '../../../../common/query';

export default function DriversPage() {
    const [driverFormOpen, setDriverFormOpen] = useState(false);
    const [driverFormId, setDriverFormId] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewDrivers = hasAnyPermission([
        PERMISSIONS.TRANSPORT.DRIVERS.READ,
        PERMISSIONS.TRANSPORT.DRIVERS.MANAGE,
        PERMISSIONS.TRANSPORT.DRIVERS.DELETE,
    ]);

    const openCreateDriver = () => {
        setDriverFormId(null);
        setDriverFormOpen(true);
    };

    const openEditDriver = (driver) => {
        setDriverFormId(driver.id);
        setDriverFormOpen(true);
    };

    const refreshDrivers = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.drivers() });
    };

    if (!canViewDrivers) return <AccessDeniedState />;

    return (
        <Box className="transport-drivers-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Choferes"
                    description="Gestión de choferes."
                    action={hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateDriver} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <DriversTable onEditDriver={openEditDriver} />
                </Box>
            </Container>

            {driverFormOpen ? (
                <DriverFormModal
                    open={driverFormOpen}
                    driverId={driverFormId}
                    onClose={() => setDriverFormOpen(false)}
                    onSaved={refreshDrivers}
                />
            ) : null}
        </Box>
    );
}
