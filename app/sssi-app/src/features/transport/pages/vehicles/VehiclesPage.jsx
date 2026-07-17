import { useState } from 'react';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import VehiclesTable from '../../components/vehicles/VehiclesTable.jsx';
import VehicleFormModal from '../../components/vehicles/VehicleFormModal.jsx';

export default function VehiclesPage() {
    const [vehicleFormOpen, setVehicleFormOpen] = useState(false);
    const [vehicleFormId, setVehicleFormId] = useState(null);
    const [vehiclesRefresh, setVehiclesRefresh] = useState(0);
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewVehicles = hasAnyPermission([
        PERMISSIONS.TRANSPORT.VEHICLES.READ,
        PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
        PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
    ]);

    const openCreateVehicle = () => {
        setVehicleFormId(null);
        setVehicleFormOpen(true);
    };

    const openEditVehicle = (vehicle) => {
        setVehicleFormId(vehicle.id);
        setVehicleFormOpen(true);
    };

    const refreshVehicles = () => setVehiclesRefresh((value) => value + 1);

    if (!canViewVehicles) return <AccessDeniedState />;

    return (
        <Box className="transport-vehicles-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Vehículos"
                    description="Gestión de vehículos."
                    action={hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateVehicle} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <VehiclesTable
                        refreshKey={vehiclesRefresh}
                        onRefresh={refreshVehicles}
                        onEditVehicle={openEditVehicle}
                    />
                </Box>
            </Container>

            {vehicleFormOpen ? (
                <VehicleFormModal
                    open={vehicleFormOpen}
                    vehicleId={vehicleFormId}
                    onClose={() => setVehicleFormOpen(false)}
                    onSaved={refreshVehicles}
                />
            ) : null}
        </Box>
    );
}
