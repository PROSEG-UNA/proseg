import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
    Box,
    Container,
    Tab,
    Tabs,
    Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import CleaningServicesOutlinedIcon from '@mui/icons-material/CleaningServicesOutlined';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';
import DriversTable from '../components/drivers/DriversTable.jsx';
import DriverFormModal from '../components/drivers/DriverFormModal.jsx';
import VehiclesTable from '../components/vehicles/VehiclesTable.jsx';
import VehicleFormModal from '../components/vehicles/VehicleFormModal.jsx';
import TransportMaintenanceTable from '../components/maintenance/TransportMaintenanceTable.jsx';
import VehicleMaintenanceFormModal from '../components/maintenance/VehicleMaintenanceFormModal.jsx';
import ToursTable from '../components/tours/ToursTable.jsx';
import TourFormModal from '../components/tours/TourFormModal.jsx';
import AssignmentPanel from '../components/assignment/AssignmentPanel.jsx';
import CleaningPanel from '../components/cleaning/CleaningPanel.jsx';
import { queryKeys } from '../../../common/query';

export default function TransportPage() {
    const [tabIndex, setTabIndex] = useState(0);
    const [driverFormOpen, setDriverFormOpen] = useState(false);
    const [driverFormId, setDriverFormId] = useState(null);
    const [vehicleFormOpen, setVehicleFormOpen] = useState(false);
    const [vehicleFormId, setVehicleFormId] = useState(null);
    const [maintenanceFormOpen, setMaintenanceFormOpen] = useState(false);
    const [maintenanceFormId, setMaintenanceFormId] = useState(null);
    const [tourFormOpen, setTourFormOpen] = useState(false);
    const [tourFormId, setTourFormId] = useState(null);

    const queryClient = useQueryClient();
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewDrivers = hasAnyPermission([
        PERMISSIONS.TRANSPORT.DRIVERS.READ,
        PERMISSIONS.TRANSPORT.DRIVERS.MANAGE,
        PERMISSIONS.TRANSPORT.DRIVERS.DELETE,
    ]);

    const canViewVehicles = hasAnyPermission([
        PERMISSIONS.TRANSPORT.VEHICLES.READ,
        PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
        PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
    ]);

    const canViewMaintenance = hasAnyPermission([
        PERMISSIONS.TRANSPORT.MAINTENANCE.READ,
        PERMISSIONS.TRANSPORT.MAINTENANCE.MANAGE,
        PERMISSIONS.TRANSPORT.MAINTENANCE.DELETE,
    ]);

    const canViewTours = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.READ,
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
    ]);

    const canViewAssignment = hasAnyPermission([
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    const canViewCleaning = canViewTours || canViewAssignment;

    const tabs = useMemo(() => [
        canViewCleaning ? { key: 'cleaning', label: 'Depuracion', icon: CleaningServicesOutlinedIcon } : null,
        canViewDrivers ? { key: 'drivers', label: 'Choferes', icon: BadgeOutlinedIcon } : null,
        canViewVehicles ? { key: 'vehicles', label: 'Vehículos', icon: DirectionsCarFilledOutlinedIcon } : null,
        canViewMaintenance ? { key: 'maintenance', label: 'Mantenimiento', icon: BuildCircleOutlinedIcon } : null,
        canViewTours ? { key: 'tours', label: 'Giras', icon: AltRouteOutlinedIcon } : null,
        canViewAssignment ? { key: 'assignment', label: 'Asignaciones', icon: AssignmentTurnedInOutlinedIcon } : null,
    ].filter(Boolean), [canViewCleaning, canViewDrivers, canViewVehicles, canViewMaintenance, canViewTours, canViewAssignment]);

    const safeTabIndex = tabIndex < tabs.length ? tabIndex : 0;
    const currentTab = tabs[safeTabIndex] ?? tabs[0] ?? null;

    const handleTabChange = (_, newValue) => setTabIndex(newValue);

    const openCreateDriver = () => {
        setDriverFormId(null);
        setDriverFormOpen(true);
    };

    const openEditDriver = (driver) => {
        setDriverFormId(driver.id);
        setDriverFormOpen(true);
    };

    const openCreateVehicle = () => {
        setVehicleFormId(null);
        setVehicleFormOpen(true);
    };

    const openEditVehicle = (vehicle) => {
        setVehicleFormId(vehicle.id);
        setVehicleFormOpen(true);
    };

    const openCreateMaintenance = () => {
        setMaintenanceFormId(null);
        setMaintenanceFormOpen(true);
    };

    const openEditMaintenance = (record) => {
        setMaintenanceFormId(record.id);
        setMaintenanceFormOpen(true);
    };

    const openCreateTour = () => {
        setTourFormId(null);
        setTourFormOpen(true);
    };

    const openEditTour = (tour) => {
        setTourFormId(tour.id);
        setTourFormOpen(true);
    };

    const invalidateTransport = (queryKey) => {
        void queryClient.invalidateQueries({ queryKey });
    };

    const refreshDrivers = () => invalidateTransport(queryKeys.transport.drivers());
    const refreshVehicles = () => invalidateTransport(queryKeys.transport.vehicles());
    const refreshMaintenance = () => invalidateTransport(queryKeys.transport.maintenance());
    const refreshTours = () => invalidateTransport(queryKeys.transport.tours());
    const refreshAssignment = () => invalidateTransport(queryKeys.transport.assignment());

    if (!canViewCleaning && !canViewDrivers && !canViewVehicles && !canViewMaintenance && !canViewTours && !canViewAssignment) {
        return <AccessDeniedState />;
    }

    return (
        <Box className="transport-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <Box sx={{ display: 'flex', alignItems: { xs: 'flex-start', md: 'center' }, justifyContent: 'space-between', gap: 2, mb: 2, flexDirection: { xs: 'column', md: 'row' } }}>
                    <Box sx={{ flex: 1 }}>
                        <Typography variant="h4" sx={{ fontWeight: 800, color: 'primary.icon', fontSize: { xs: '1.55rem', sm: '1.9rem' } }}>
                            Transporte
                        </Typography>
                        <Typography sx={{ color: 'text.secondary', fontSize: 14 }}>
                            Gestión de choferes, vehículos, mantenimiento, giras y asignaciones desde un solo módulo.
                        </Typography>
                    </Box>
                    {currentTab?.key === 'drivers' && hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateDriver} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : currentTab?.key === 'vehicles' && hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateVehicle} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : currentTab?.key === 'maintenance' && hasPermission(PERMISSIONS.TRANSPORT.MAINTENANCE.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateMaintenance} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : currentTab?.key === 'tours' && hasPermission(PERMISSIONS.TRANSPORT.TOURS.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateTour} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                </Box>

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                    <Tabs value={safeTabIndex} onChange={handleTabChange} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile>
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
                    {currentTab?.key === 'cleaning' && (
                        <CleaningPanel onImported={() => { refreshTours(); refreshAssignment(); }} />
                    )}

                    {currentTab?.key === 'drivers' && (
                        <Box>
                            <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon', mb: 2 }}>
                                Lista de choferes
                            </Typography>
                            <DriversTable onEditDriver={openEditDriver} />
                        </Box>
                    )}

                    {currentTab?.key === 'vehicles' && (
                        <Box>
                            <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon', mb: 2 }}>
                                Lista de vehículos
                            </Typography>
                            <VehiclesTable onEditVehicle={openEditVehicle} />
                        </Box>
                    )}

                    {currentTab?.key === 'maintenance' && (
                        <Box>
                            <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon', mb: 2 }}>
                                Lista de mantenimientos
                            </Typography>
                            <TransportMaintenanceTable onEditMaintenance={openEditMaintenance} />
                        </Box>
                    )}

                    {currentTab?.key === 'tours' && (
                        <Box>
                            <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon', mb: 2 }}>
                                Lista de giras
                            </Typography>
                            <ToursTable onEditTour={openEditTour} />
                        </Box>
                    )}

                    {currentTab?.key === 'assignment' && (
                        <AssignmentPanel />
                    )}
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

            {vehicleFormOpen ? (
                <VehicleFormModal
                    open={vehicleFormOpen}
                    vehicleId={vehicleFormId}
                    onClose={() => setVehicleFormOpen(false)}
                    onSaved={refreshVehicles}
                />
            ) : null}

            {maintenanceFormOpen ? (
                <VehicleMaintenanceFormModal
                    open={maintenanceFormOpen}
                    maintenanceId={maintenanceFormId}
                    onClose={() => setMaintenanceFormOpen(false)}
                    onSaved={refreshMaintenance}
                />
            ) : null}

            {tourFormOpen ? (
                <TourFormModal
                    open={tourFormOpen}
                    tourId={tourFormId}
                    onClose={() => setTourFormOpen(false)}
                    onSaved={refreshTours}
                />
            ) : null}
        </Box>
    );
}
