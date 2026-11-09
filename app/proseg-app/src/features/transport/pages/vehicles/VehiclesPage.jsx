import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Button, Container, Tab, Tabs } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import CarRentalOutlinedIcon from '@mui/icons-material/CarRentalOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import ExportIcon from '../../../../common/components/icons/ExportIcon.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import VehiclesTable from '../../components/vehicles/VehiclesTable.jsx';
import VehicleFormModal from '../../components/vehicles/VehicleFormModal.jsx';
import BorrowedVehiclesTable from '../../components/vehicles/BorrowedVehiclesTable.jsx';
import BorrowedVehicleFormModal from '../../components/vehicles/BorrowedVehicleFormModal.jsx';
import { exportBorrowedVehicles, exportVehicles } from '../../services/transportExportService.js';
import { downloadBlob } from '../../transportUtils.js';
import { queryKeys } from '../../../../common/query';

const VEHICLE_SUB_TABS = [
    { key: 'own', label: 'Vehículos', icon: DirectionsCarFilledOutlinedIcon },
    { key: 'borrowed', label: 'Vehículos prestados', icon: CarRentalOutlinedIcon },
];

export default function VehiclesPage() {
    const [vehicleFormOpen, setVehicleFormOpen] = useState(false);
    const [vehicleFormId, setVehicleFormId] = useState(null);
    const [vehicleSubTab, setVehicleSubTab] = useState('own');
    const [borrowedVehicleFormOpen, setBorrowedVehicleFormOpen] = useState(false);
    const [borrowedVehicleFormId, setBorrowedVehicleFormId] = useState(null);
    const [exporting, setExporting] = useState(false);
    const [alert, setAlert] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewVehicles = hasAnyPermission([
        PERMISSIONS.TRANSPORT.VEHICLES.READ,
        PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
        PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
    ]);
    const canExportVehicles = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.READ);
    const canManageVehicles = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE);

    const handleExport = async () => {
        if (exporting) return;
        setExporting(true);
        try {
            const { blob, fileName } = vehicleSubTab === 'borrowed'
                ? await exportBorrowedVehicles()
                : await exportVehicles();
            downloadBlob(blob, fileName);
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo exportar') });
        } finally {
            setExporting(false);
        }
    };

    const openCreateVehicle = () => {
        setVehicleFormId(null);
        setVehicleFormOpen(true);
    };

    const openEditVehicle = (vehicle) => {
        setVehicleFormId(vehicle.id);
        setVehicleFormOpen(true);
    };

    const openCreateBorrowedVehicle = () => {
        setBorrowedVehicleFormId(null);
        setBorrowedVehicleFormOpen(true);
    };

    const openEditBorrowedVehicle = (vehicle) => {
        setBorrowedVehicleFormId(vehicle.id);
        setBorrowedVehicleFormOpen(true);
    };

    const refreshVehicles = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.vehicles() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    const refreshBorrowedVehicles = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.borrowedVehicles() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    if (!canViewVehicles) return <AccessDeniedState />;

    return (
        <Box className="transport-vehicles-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Vehículos"
                    description="Gestión de vehículos."
                    action={canExportVehicles || canManageVehicles ? (
                        <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                            {canExportVehicles && (
                                <Button
                                    variant="outlined"
                                    startIcon={<ExportIcon style={{ fontSize: 16, marginRight: 2 }} />}
                                    onClick={handleExport}
                                    disabled={exporting}
                                    sx={{ textTransform: 'none', fontWeight: 700, borderRadius: '10px', px: 2.5 }}
                                >
                                    {exporting ? 'Exportando...' : 'Exportar'}
                                </Button>
                            )}
                            {canManageVehicles && (
                                <PrimaryButton
                                    startIcon={<AddIcon />}
                                    onClick={vehicleSubTab === 'borrowed' ? openCreateBorrowedVehicle : openCreateVehicle}
                                    sx={{ px: '28px' }}
                                >
                                    Crear
                                </PrimaryButton>
                            )}
                        </Box>
                    ) : null}
                />

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mt: 3 }}>
                    <Tabs
                        value={vehicleSubTab}
                        onChange={(_, value) => setVehicleSubTab(value)}
                        variant="scrollable"
                        scrollButtons="auto"
                        allowScrollButtonsMobile
                    >
                        {VEHICLE_SUB_TABS.map((tab) => {
                            const Icon = tab.icon;
                            return (
                                <Tab
                                    key={tab.key}
                                    value={tab.key}
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
                    {vehicleSubTab === 'own' && <VehiclesTable onEditVehicle={openEditVehicle} />}
                    {vehicleSubTab === 'borrowed' && <BorrowedVehiclesTable onEditBorrowedVehicle={openEditBorrowedVehicle} />}
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

            {borrowedVehicleFormOpen ? (
                <BorrowedVehicleFormModal
                    open={borrowedVehicleFormOpen}
                    borrowedVehicleId={borrowedVehicleFormId}
                    onClose={() => setBorrowedVehicleFormOpen(false)}
                    onSaved={refreshBorrowedVehicles}
                />
            ) : null}

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </Box>
    );
}
