import { useEffect, useState } from 'react';
import { Box, FormControlLabel, MenuItem, Switch, TextField } from '@mui/material';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createVehicle, fetchVehicleById, updateVehicle } from '../../services/vehicles/vehiclesService';
import { VEHICLE_TYPE_OPTIONS } from '../../transportUtils';

const INITIAL_VALUES = {
    plate: '',
    type: '',
    capacity: '',
    available: true,
};

export default function VehicleFormModal({ open, onClose, onSaved, vehicleId = null, initialValues = null }) {
    const isEdit = !!vehicleId;
    const [formValues, setFormValues] = useState(() => ({ ...INITIAL_VALUES, ...initialValues }));
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingVehicle, setLoadingVehicle] = useState(false);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open || !vehicleId) return undefined;

        let cancelled = false;
        const loadVehicle = async () => {
            setLoadingVehicle(true);
            try {
                const vehicle = await fetchVehicleById(vehicleId);
                if (cancelled || !vehicle) return;
                setFormValues({
                    plate: vehicle.plate ?? '',
                    type: vehicle.type ?? '',
                    capacity: vehicle.capacity ?? '',
                    available: vehicle.available ?? true,
                });
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar el vehículo') });
                }
            } finally {
                if (!cancelled) setLoadingVehicle(false);
            }
        };

        void loadVehicle();

        return () => {
            cancelled = true;
        };
    }, [open, vehicleId]);

    const validateField = (key, value) => {
        const trimmedValue = typeof value === 'string' ? value.trim() : value;
        let error = '';

        if (key === 'plate' && !trimmedValue) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'capacity' && trimmedValue) {
            if (!/^\d+$/.test(String(trimmedValue)) || Number(trimmedValue) < 1) {
                error = 'Ingresa un número entero mayor a 0';
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
        const keysToValidate = ['plate', 'capacity'];
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
                plate: formValues.plate.trim(),
                type: formValues.type || null,
                capacity: formValues.capacity ? Number(formValues.capacity) : null,
                available: formValues.available,
            };

            const saved = isEdit
                ? await updateVehicle(vehicleId, payload)
                : await createVehicle(payload);

            onSaved?.(saved);
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar el vehículo') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={DirectionsCarFilledOutlinedIcon}
                title={isEdit ? 'Editar vehículo' : 'Crear vehículo'}
                subtitle="Completa la información principal del vehículo."
                loading={loadingVehicle}
                primaryButton={{
                    label: isEdit ? 'Guardar cambios' : 'Crear',
                    onClick: handleSave,
                    loading: saving,
                    disabled: saving || loadingVehicle,
                }}
                secondaryButton={{
                    label: 'Cancelar',
                    onClick: onClose,
                    disabled: saving,
                }}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    <TextField
                        label="Placa"
                        value={formValues.plate}
                        onChange={(event) => handleChange('plate', event.target.value)}
                        onBlur={() => handleBlur('plate')}
                        error={!!errors.plate}
                        helperText={errors.plate}
                        required
                        fullWidth
                    />
                    <TextField
                        select
                        label="Tipo"
                        value={formValues.type}
                        onChange={(event) => handleChange('type', event.target.value)}
                        slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
                        fullWidth
                    >
                        <MenuItem value="">Sin especificar</MenuItem>
                        {VEHICLE_TYPE_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label="Capacidad"
                        value={formValues.capacity}
                        onChange={(event) => handleChange('capacity', event.target.value)}
                        onBlur={() => handleBlur('capacity')}
                        error={!!errors.capacity}
                        helperText={errors.capacity}
                        fullWidth
                    />
                    <FormControlLabel
                        control={(
                            <Switch
                                checked={formValues.available}
                                onChange={(event) => handleChange('available', event.target.checked)}
                            />
                        )}
                        label="Disponible"
                    />
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
