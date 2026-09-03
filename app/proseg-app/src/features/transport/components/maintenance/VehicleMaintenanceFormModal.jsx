import { useEffect, useState } from 'react';
import { Box, MenuItem, TextField } from '@mui/material';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createVehicleMaintenance, fetchVehicleMaintenanceById, updateVehicleMaintenance } from '../../services/maintenance/maintenanceService';
import { VEHICLE_MAINTENANCE_STATUS_OPTIONS } from '../../transportUtils';
import { useVehicleOptions } from '../../hooks/useTransportOptions';
import { useQueryAlert } from '../../../../common/hooks/index.js';

const INITIAL_VALUES = {
    vehicleId: '',
    title: '',
    type: '',
    scheduledDate: '',
    cost: '',
    status: 'SCHEDULED',
    notes: '',
};

export default function VehicleMaintenanceFormModal({ open, onClose, onSaved, maintenanceId = null }) {
    const isEdit = !!maintenanceId;
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingMaintenance, setLoadingMaintenance] = useState(false);
    const vehicles = useVehicleOptions(open);

    const vehicleOptions = vehicles.options;
    const loadingVehicles = vehicles.loading;

    const { alert, setAlert, closeAlert } = useQueryAlert(
        vehicles.error ? getFriendlyApiErrorMessage(vehicles.error, 'No se pudieron cargar los vehículos') : null
    );

    useEffect(() => {
        if (!open || !maintenanceId) return undefined;

        let cancelled = false;
        const loadMaintenance = async () => {
            setLoadingMaintenance(true);
            try {
                const record = await fetchVehicleMaintenanceById(maintenanceId);
                if (cancelled || !record) return;
                setFormValues({
                    vehicleId: record.vehicle?.id ?? record.vehicleId ?? '',
                    title: record.title ?? '',
                    type: record.type ?? '',
                    scheduledDate: record.scheduledDate ?? '',
                    cost: record.cost ?? '',
                    status: record.status ?? 'SCHEDULED',
                    notes: record.notes ?? '',
                });
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar el mantenimiento') });
                }
            } finally {
                if (!cancelled) setLoadingMaintenance(false);
            }
        };

        void loadMaintenance();

        return () => {
            cancelled = true;
        };
    }, [open, maintenanceId, setAlert]);

    const validateField = (key, value) => {
        const trimmedValue = typeof value === 'string' ? value.trim() : value;
        let error = '';

        if ((key === 'vehicleId' || key === 'title' || key === 'type') && !trimmedValue) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'cost' && trimmedValue) {
            if (Number.isNaN(Number(trimmedValue))) {
                error = 'Ingresa un monto válido';
            }
        }

        setErrors((previousValue) => ({ ...previousValue, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        setFormValues((previousValue) => ({ ...previousValue, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleBlur = (key) => {
        setTouched((previousValue) => ({ ...previousValue, [key]: true }));
        validateField(key, formValues[key]);
    };

    const handleSave = async () => {
        const keysToValidate = ['vehicleId', 'title', 'type', 'cost'];
        const nextTouched = {};
        let hasError = false;

        keysToValidate.forEach((key) => {
            nextTouched[key] = true;
            if (!validateField(key, formValues[key])) {
                hasError = true;
            }
        });

        setTouched((previousValue) => ({ ...previousValue, ...nextTouched }));

        if (hasError) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                vehicleId: formValues.vehicleId.trim(),
                title: formValues.title.trim(),
                type: formValues.type.trim(),
                scheduledDate: formValues.scheduledDate || null,
                cost: formValues.cost ? Number(formValues.cost) : null,
                status: formValues.status,
                notes: formValues.notes.trim() || null,
            };

            if (isEdit) {
                await updateVehicleMaintenance(maintenanceId, payload);
            } else {
                await createVehicleMaintenance(payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar el mantenimiento') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={BuildCircleOutlinedIcon}
                title={isEdit ? 'Editar mantenimiento' : 'Crear mantenimiento'}
                subtitle="Registra la información principal del mantenimiento."
                loading={loadingMaintenance}
                primaryButton={{
                    label: isEdit ? 'Guardar cambios' : 'Crear',
                    onClick: handleSave,
                    loading: saving,
                    disabled: saving || loadingMaintenance || loadingVehicles,
                }}
                secondaryButton={{
                    label: 'Cancelar',
                    onClick: onClose,
                    disabled: saving,
                }}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    <TextField
                        select
                        label="Vehículo"
                        value={formValues.vehicleId}
                        onChange={(event) => handleChange('vehicleId', event.target.value)}
                        onBlur={() => handleBlur('vehicleId')}
                        error={!!errors.vehicleId}
                        helperText={errors.vehicleId}
                        required
                        fullWidth
                        disabled={loadingVehicles}
                    >
                        <MenuItem value="">Selecciona un vehículo</MenuItem>
                        {vehicleOptions.map((option) => (
                            <MenuItem key={option.id} value={option.id}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label="Tipo"
                        value={formValues.type}
                        onChange={(event) => handleChange('type', event.target.value)}
                        onBlur={() => handleBlur('type')}
                        error={!!errors.type}
                        helperText={errors.type}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Actividad"
                        value={formValues.title}
                        onChange={(event) => handleChange('title', event.target.value)}
                        onBlur={() => handleBlur('title')}
                        error={!!errors.title}
                        helperText={errors.title}
                        required
                        fullWidth
                        sx={{ gridColumn: { md: '1 / -1' } }}
                    />
                    <Box>
                        <Box component="label" sx={{ display: 'block', color: 'text.secondary', fontSize: 12, mb: 0.75 }}>
                            Fecha programada
                        </Box>
                        <TextField
                            type="date"
                            value={formValues.scheduledDate}
                            onChange={(event) => handleChange('scheduledDate', event.target.value)}
                            fullWidth
                            size="small"
                        />
                    </Box>
                    <TextField
                        label="Costo"
                        value={formValues.cost}
                        onChange={(event) => handleChange('cost', event.target.value)}
                        onBlur={() => handleBlur('cost')}
                        error={!!errors.cost}
                        helperText={errors.cost}
                        fullWidth
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        onChange={(event) => handleChange('status', event.target.value)}
                        fullWidth
                    >
                        {VEHICLE_MAINTENANCE_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label="Notas"
                        value={formValues.notes}
                        onChange={(event) => handleChange('notes', event.target.value)}
                        multiline
                        minRows={3}
                        fullWidth
                        sx={{ gridColumn: { md: '1 / -1' } }}
                    />
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={closeAlert}
            />
        </>
    );
}
