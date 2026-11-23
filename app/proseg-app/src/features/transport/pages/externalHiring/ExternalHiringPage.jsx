import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Button, Container, MenuItem, TextField } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import ExportIcon from '../../../../common/components/icons/ExportIcon.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import ExternalVehiclesTable from '../../components/externalHiring/ExternalVehiclesTable.jsx';
import ExternalVehicleFormModal from '../../components/externalHiring/ExternalVehicleFormModal.jsx';
import { VEHICLE_AVAILABILITY_FILTER_OPTIONS } from '../../components/vehicles/vehicleColumns.jsx';
import { exportExternalVehicles } from '../../services/transportExportService.js';
import { downloadBlob } from '../../transportUtils.js';
import { queryKeys } from '../../../../common/query';

export default function ExternalHiringPage() {
    const [formOpen, setFormOpen] = useState(false);
    const [formId, setFormId] = useState(null);
    const [availabilityFilter, setAvailabilityFilter] = useState('all');
    const [exporting, setExporting] = useState(false);
    const [alert, setAlert] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canView = hasAnyPermission([
        PERMISSIONS.TRANSPORT.VEHICLES.READ,
        PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
        PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
    ]);
    const canExport = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.READ);
    const canManage = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE);

    const handleExport = async () => {
        if (exporting) return;
        setExporting(true);
        try {
            const { blob, fileName } = await exportExternalVehicles(
                availabilityFilter === 'all' ? {} : { available: availabilityFilter },
            );
            downloadBlob(blob, fileName);
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo exportar') });
        } finally {
            setExporting(false);
        }
    };

    const openCreate = () => {
        setFormId(null);
        setFormOpen(true);
    };

    const openEdit = (vehicle) => {
        setFormId(vehicle.id);
        setFormOpen(true);
    };

    const refresh = (_saved, message) => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.externalVehicles() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
        if (message) setAlert({ type: 'success', message });
    };

    if (!canView) return <AccessDeniedState />;

    return (
        <Box className="transport-external-hiring-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Contratación Externa"
                    description="Proveedores y vehículos que se pueden contratar para las giras."
                    action={canExport || canManage ? (
                        <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                            {canExport && (
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
                            {canManage && (
                                <PrimaryButton startIcon={<AddIcon />} onClick={openCreate} sx={{ px: '28px' }}>
                                    Crear
                                </PrimaryButton>
                            )}
                        </Box>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <TextField
                        select
                        size="small"
                        label="Estado"
                        value={availabilityFilter}
                        onChange={(event) => setAvailabilityFilter(event.target.value)}
                        sx={{ width: { xs: '100%', sm: 320 } }}
                    >
                        {VEHICLE_AVAILABILITY_FILTER_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                        ))}
                    </TextField>
                </Box>

                <Box sx={{ pt: 3 }}>
                    <ExternalVehiclesTable onEditExternalVehicle={openEdit} availabilityFilter={availabilityFilter} />
                </Box>
            </Container>

            {formOpen ? (
                <ExternalVehicleFormModal
                    open={formOpen}
                    externalVehicleId={formId}
                    onClose={() => setFormOpen(false)}
                    onSaved={refresh}
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
