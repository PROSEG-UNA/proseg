import { useEffect, useState } from 'react';
import { Box, MenuItem, TextField } from '@mui/material';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createVehicle, fetchVehicleById, updateVehicle } from '../../services/vehicles/vehiclesService';
import { VEHICLE_STATUS_OPTIONS } from '../../transportUtils';

const INITIAL_VALUES = {
    plate: '',
    brand: '',
    model: '',
    year: '',
    capacity: '',
    status: 'AVAILABLE',
};

export default function VehicleFormModal({ open, onClose, onSaved, vehicleId = null }) {
    const isEdit = !!vehicleId;
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
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
                    brand: vehicle.brand ?? '',
                    model: vehicle.model ?? '',
                    year: vehicle.year ?? '',
                    capacity: vehicle.capacity ?? '',
                    status: vehicle.status ?? 'AVAILABLE',
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

        if ((key === 'plate' || key === 'brand' || key === 'model') && !trimmedValue) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'year' && trimmedValue) {
            if (!/^\d{4}$/.test(String(trimmedValue))) {
                error = 'Ingresa un año válido';
            }
        }

        if (!error && key === 'capacity' && trimmedValue) {
            if (Number.isNaN(Number(trimmedValue))) {
                error = 'Ingresa un valor numérico';
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
        const keysToValidate = ['plate', 'brand', 'model', 'year', 'capacity'];
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
                brand: formValues.brand.trim(),
                model: formValues.model.trim(),
                year: formValues.year ? Number(formValues.year) : null,
                capacity: formValues.capacity ? Number(formValues.capacity) : null,
                status: formValues.status,
            };

            if (isEdit) {
                await updateVehicle(vehicleId, payload);
            } else {
                await createVehicle(payload);
            }

            onSaved?.();
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
                        label="Marca"
                        value={formValues.brand}
                        onChange={(event) => handleChange('brand', event.target.value)}
                        onBlur={() => handleBlur('brand')}
                        error={!!errors.brand}
                        helperText={errors.brand}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Modelo"
                        value={formValues.model}
                        onChange={(event) => handleChange('model', event.target.value)}
                        onBlur={() => handleBlur('model')}
                        error={!!errors.model}
                        helperText={errors.model}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Año"
                        value={formValues.year}
                        onChange={(event) => handleChange('year', event.target.value)}
                        onBlur={() => handleBlur('year')}
                        error={!!errors.year}
                        helperText={errors.year}
                        fullWidth
                    />
                    <TextField
                        label="Capacidad"
                        value={formValues.capacity}
                        onChange={(event) => handleChange('capacity', event.target.value)}
                        onBlur={() => handleBlur('capacity')}
                        error={!!errors.capacity}
                        helperText={errors.capacity}
                        fullWidth
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        onChange={(event) => handleChange('status', event.target.value)}
                        fullWidth
                    >
                        {VEHICLE_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
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
