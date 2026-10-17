import { useEffect, useState } from 'react';
import { Box, Checkbox, ListItemText, MenuItem, TextField } from '@mui/material';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createDriver, fetchDriverById, updateDriver } from '../../services/drivers/driversService';
import { DRIVER_STATUS_OPTIONS, VEHICLE_TYPE_OPTIONS, vehicleTypeLabel } from '../../transportUtils';

const ALL_VEHICLE_TYPES = VEHICLE_TYPE_OPTIONS.map((option) => option.value);

const INITIAL_VALUES = {
    name: '',
    status: 'ACTIVE',
    allowedVehicleTypes: ALL_VEHICLE_TYPES,
};

export default function DriverFormModal({ open, onClose, onSaved, driverId = null, initialValues = null }) {
    const isEdit = !!driverId;
    const [formValues, setFormValues] = useState(() => ({ ...INITIAL_VALUES, ...initialValues }));
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingDriver, setLoadingDriver] = useState(false);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open || !driverId) return undefined;

        let cancelled = false;
        const loadDriver = async () => {
            setLoadingDriver(true);
            try {
                const driver = await fetchDriverById(driverId);
                if (cancelled || !driver) return;
                setFormValues({
                    name: driver.name ?? '',
                    status: driver.status ?? 'ACTIVE',
                    allowedVehicleTypes: driver.allowedVehicleTypes ?? ALL_VEHICLE_TYPES,
                });
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar el chofer') });
                }
            } finally {
                if (!cancelled) setLoadingDriver(false);
            }
        };

        void loadDriver();

        return () => {
            cancelled = true;
        };
    }, [open, driverId]);

    const validateField = (key, value) => {
        const trimmedValue = typeof value === 'string' ? value.trim() : value;
        let error = '';

        if (key === 'name' && !trimmedValue) {
            error = 'Este campo es requerido';
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
        const keysToValidate = ['name'];
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
                name: formValues.name.trim(),
                status: formValues.status,
                allowedVehicleTypes: formValues.allowedVehicleTypes,
            };

            const saved = isEdit
                ? await updateDriver(driverId, payload)
                : await createDriver(payload);

            onSaved?.(saved);
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar el chofer') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={BadgeOutlinedIcon}
                title={isEdit ? 'Editar chofer' : 'Crear chofer'}
                subtitle="Completa la información principal del chofer."
                loading={loadingDriver}
                primaryButton={{
                    label: isEdit ? 'Guardar cambios' : 'Crear',
                    onClick: handleSave,
                    loading: saving,
                    disabled: saving || loadingDriver,
                }}
                secondaryButton={{
                    label: 'Cancelar',
                    onClick: onClose,
                    disabled: saving,
                }}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    <TextField
                        label="Nombre"
                        value={formValues.name}
                        onChange={(event) => handleChange('name', event.target.value)}
                        onBlur={() => handleBlur('name')}
                        error={!!errors.name}
                        helperText={errors.name}
                        required
                        fullWidth
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        onChange={(event) => handleChange('status', event.target.value)}
                        fullWidth
                    >
                        {DRIVER_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Tipos de vehículo que puede manejar"
                        value={formValues.allowedVehicleTypes}
                        onChange={(event) => handleChange('allowedVehicleTypes', event.target.value)}
                        slotProps={{
                            select: {
                                multiple: true,
                                renderValue: (selected) => selected.map(vehicleTypeLabel).join(', '),
                            },
                        }}
                        sx={{ gridColumn: { md: '1 / -1' } }}
                        fullWidth
                    >
                        {VEHICLE_TYPE_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                <Checkbox
                                    checked={formValues.allowedVehicleTypes.includes(option.value)}
                                    tabIndex={-1}
                                    sx={{ m: 0, mr: 1.5, p: 0 }}
                                />
                                <ListItemText primary={option.label} />
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
