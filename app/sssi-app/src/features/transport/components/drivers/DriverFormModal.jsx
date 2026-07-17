import { useEffect, useState } from 'react';
import { Box, MenuItem, TextField } from '@mui/material';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createDriver, fetchDriverById, updateDriver } from '../../services/drivers/driversService';
import { DRIVER_STATUS_OPTIONS } from '../../transportUtils';

const INITIAL_VALUES = {
    firstName: '',
    lastName: '',
    documentId: '',
    licenseNumber: '',
    phone: '',
    email: '',
    status: 'ACTIVE',
};

export default function DriverFormModal({ open, onClose, onSaved, driverId = null }) {
    const isEdit = !!driverId;
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
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
                    firstName: driver.firstName ?? '',
                    lastName: driver.lastName ?? '',
                    documentId: driver.documentId ?? '',
                    licenseNumber: driver.licenseNumber ?? '',
                    phone: driver.phone ?? '',
                    email: driver.email ?? '',
                    status: driver.status ?? 'ACTIVE',
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

        if ((key === 'firstName' || key === 'lastName' || key === 'licenseNumber') && !trimmedValue) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'email' && trimmedValue) {
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedValue)) {
                error = 'El correo electrónico no es válido';
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
        const keysToValidate = ['firstName', 'lastName', 'licenseNumber', 'email'];
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
                firstName: formValues.firstName.trim(),
                lastName: formValues.lastName.trim(),
                documentId: formValues.documentId.trim() || null,
                licenseNumber: formValues.licenseNumber.trim(),
                phone: formValues.phone.trim() || null,
                email: formValues.email.trim() || null,
                status: formValues.status,
            };

            if (isEdit) {
                await updateDriver(driverId, payload);
            } else {
                await createDriver(payload);
            }

            onSaved?.();
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
                        value={formValues.firstName}
                        onChange={(event) => handleChange('firstName', event.target.value)}
                        onBlur={() => handleBlur('firstName')}
                        error={!!errors.firstName}
                        helperText={errors.firstName}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Apellido"
                        value={formValues.lastName}
                        onChange={(event) => handleChange('lastName', event.target.value)}
                        onBlur={() => handleBlur('lastName')}
                        error={!!errors.lastName}
                        helperText={errors.lastName}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Identificación"
                        value={formValues.documentId}
                        onChange={(event) => handleChange('documentId', event.target.value)}
                        fullWidth
                    />
                    <TextField
                        label="Licencia"
                        value={formValues.licenseNumber}
                        onChange={(event) => handleChange('licenseNumber', event.target.value)}
                        onBlur={() => handleBlur('licenseNumber')}
                        error={!!errors.licenseNumber}
                        helperText={errors.licenseNumber}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Teléfono"
                        value={formValues.phone}
                        onChange={(event) => handleChange('phone', event.target.value)}
                        fullWidth
                    />
                    <TextField
                        label="Correo"
                        value={formValues.email}
                        onChange={(event) => handleChange('email', event.target.value)}
                        onBlur={() => handleBlur('email')}
                        error={!!errors.email}
                        helperText={errors.email}
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
