import { useState } from 'react';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import TransportMaintenanceTable from '../../components/maintenance/TransportMaintenanceTable.jsx';
import VehicleMaintenanceFormModal from '../../components/maintenance/VehicleMaintenanceFormModal.jsx';

export default function VehicleMaintenancePage() {
    const [maintenanceFormOpen, setMaintenanceFormOpen] = useState(false);
    const [maintenanceFormId, setMaintenanceFormId] = useState(null);
    const [maintenanceRefresh, setMaintenanceRefresh] = useState(0);
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewMaintenance = hasAnyPermission([
        PERMISSIONS.TRANSPORT.MAINTENANCE.READ,
        PERMISSIONS.TRANSPORT.MAINTENANCE.MANAGE,
        PERMISSIONS.TRANSPORT.MAINTENANCE.DELETE,
    ]);

    const openCreateMaintenance = () => {
        setMaintenanceFormId(null);
        setMaintenanceFormOpen(true);
    };

    const openEditMaintenance = (record) => {
        setMaintenanceFormId(record.id);
        setMaintenanceFormOpen(true);
    };

    const refreshMaintenance = () => setMaintenanceRefresh((value) => value + 1);

    if (!canViewMaintenance) return <AccessDeniedState />;

    return (
        <Box className="transport-maintenance-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Mantenimiento"
                    description="Gestión del mantenimiento de vehículos."
                    action={hasPermission(PERMISSIONS.TRANSPORT.MAINTENANCE.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateMaintenance} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <TransportMaintenanceTable
                        refreshKey={maintenanceRefresh}
                        onRefresh={refreshMaintenance}
                        onEditMaintenance={openEditMaintenance}
                    />
                </Box>
            </Container>

            {maintenanceFormOpen ? (
                <VehicleMaintenanceFormModal
                    open={maintenanceFormOpen}
                    maintenanceId={maintenanceFormId}
                    onClose={() => setMaintenanceFormOpen(false)}
                    onSaved={refreshMaintenance}
                />
            ) : null}
        </Box>
    );
}
