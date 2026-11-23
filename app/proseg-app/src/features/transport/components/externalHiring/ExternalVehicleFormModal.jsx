import { useEffect, useMemo, useState } from 'react';
import { Autocomplete, Box, FormControlLabel, Switch, TextField } from '@mui/material';
import HandshakeOutlinedIcon from '@mui/icons-material/HandshakeOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createExternalVehicle, fetchExternalVehicleById, updateExternalVehicle } from '../../services/externalVehicles/externalVehiclesService';
import { useExternalVehicleOptions } from '../../hooks/useTransportOptions.js';
import { vehicleSavedMessage } from '../vehicles/vehicleChanges.js';
import { describeExternalVehicleChanges } from './externalVehicleChanges.js';

const INITIAL_VALUES = {
    provider: '',
    plate: '',
    capacity: '',
    available: true,
};

export default function ExternalVehicleFormModal({ open, onClose, onSaved, externalVehicleId = null, initialValues = null }) {
    const isEdit = !!externalVehicleId;
    const [formValues, setFormValues] = useState(() => ({ ...INITIAL_VALUES, ...initialValues }));
    const [originalValues, setOriginalValues] = useState(() => ({ ...INITIAL_VALUES, ...initialValues }));
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingVehicle, setLoadingVehicle] = useState(false);
    const [alert, setAlert] = useState(null);
    const vehicleOptions = useExternalVehicleOptions(open);
    const providerOptions = useMemo(
        () => [...new Set(vehicleOptions.options.map((option) => option.provider).filter(Boolean))]
            .sort((first, second) => first.localeCompare(second, 'es')),
        [vehicleOptions.options],
    );

    useEffect(() => {
        if (!open || !externalVehicleId) return undefined;

        let cancelled = false;
        const loadVehicle = async () => {
            setLoadingVehicle(true);
            try {
                const vehicle = await fetchExternalVehicleById(externalVehicleId);
                if (cancelled || !vehicle) return;
                const loadedValues = {
                    provider: vehicle.provider ?? '',
                    plate: vehicle.plate ?? '',
                    capacity: vehicle.capacity ?? '',
                    available: vehicle.available ?? true,
                };
                setOriginalValues(loadedValues);
                setFormValues(loadedValues);
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar el vehículo externo') });
                }
            } finally {
                if (!cancelled) setLoadingVehicle(false);
            }
        };

        void loadVehicle();

        return () => {
            cancelled = true;
        };
    }, [open, externalVehicleId]);

    const validateField = (key, value) => {
        const trimmedValue = typeof value === 'string' ? value.trim() : value;
        let error = '';

        if ((key === 'provider' || key === 'plate') && !trimmedValue) {
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
        const keysToValidate = ['provider', 'plate', 'capacity'];
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
                provider: formValues.provider.trim(),
                plate: formValues.plate.trim(),
                capacity: formValues.capacity ? Number(formValues.capacity) : null,
                available: formValues.available,
            };

            const saved = isEdit
                ? await updateExternalVehicle(externalVehicleId, payload)
                : await createExternalVehicle(payload);

            onSaved?.(saved, vehicleSavedMessage({
                kind: 'Vehículo externo',
                plate: payload.plate,
                created: !isEdit,
                changes: isEdit ? describeExternalVehicleChanges(originalValues, payload) : [],
            }));
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar el vehículo externo') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={HandshakeOutlinedIcon}
                title={isEdit ? 'Editar vehículo externo' : 'Crear vehículo externo'}
                subtitle="Completa la información del vehículo que ofrece el proveedor."
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
                    <Autocomplete
                        freeSolo
                        options={providerOptions}
                        inputValue={formValues.provider}
                        onInputChange={(_, value, reason) => {
                            if (reason === 'input' || reason === 'clear') handleChange('provider', value);
                        }}
                        onChange={(_, value) => handleChange('provider', value ?? '')}
                        sx={{ gridColumn: { md: '1 / -1' } }}
                        renderInput={(params) => (
                            <TextField
                                {...params}
                                label="Proveedor"
                                onBlur={() => handleBlur('provider')}
                                error={!!errors.provider}
                                helperText={errors.provider}
                                required
                            />
                        )}
                    />
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
