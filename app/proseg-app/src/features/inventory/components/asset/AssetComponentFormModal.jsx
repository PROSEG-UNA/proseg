import { useEffect, useState } from 'react';
import { Box, TextField, Typography, useTheme } from '@mui/material';
import WidgetsOutlinedIcon from '@mui/icons-material/WidgetsOutlined';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { createAssetComponent, updateAssetComponent } from '../../services/assetComponentsService';

const INIT = {
    name: '',
    quantity: '1',
    location: '',
    observations: '',
};

export default function AssetComponentFormModal({
    open,
    onClose,
    onSaved,
    assetId,
    componentData = null,
}) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const isEdit = !!componentData?.id;

    const [values, setValues] = useState(INIT);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open) return;
        setValues(componentData ? {
            name: componentData.name ?? '',
            quantity: componentData.quantity != null ? String(componentData.quantity) : '1',
            location: componentData.location ?? '',
            observations: componentData.observations ?? '',
        } : INIT);
        setErrors({});
        setTouched({});
        setSaving(false);
        setAlert(null);
    }, [open, componentData]);

    const validateField = (key, rawValue) => {
        const value = typeof rawValue === 'string' ? rawValue : String(rawValue ?? '');
        let error = '';

        if (key === 'name' && !value.trim()) {
            error = 'El nombre es obligatorio';
        }
        if (!error && key === 'name' && value.trim().length > 255) {
            error = 'El nombre no puede superar los 255 caracteres';
        }
        if (key === 'quantity') {
            if (!value.trim()) {
                error = 'La cantidad es obligatoria';
            } else {
                const n = Number(value);
                if (!Number.isInteger(n) || n < 1) {
                    error = 'La cantidad debe ser un entero mayor o igual a 1';
                }
            }
        }
        if (!error && key === 'location' && value.length > 255) {
            error = 'La ubicación no puede superar los 255 caracteres';
        }
        if (!error && key === 'observations' && value.length > 1000) {
            error = 'Las observaciones no pueden superar los 1000 caracteres';
        }

        setErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        setValues((prev) => ({ ...prev, [key]: value }));
        if (touched[key]) {
            validateField(key, value);
        }
    };

    const handleBlur = (key) => {
        setTouched((prev) => ({ ...prev, [key]: true }));
        validateField(key, values[key]);
    };

    const handleSave = async () => {
        const fields = ['name', 'quantity', 'location', 'observations'];
        const nextTouched = {};
        const nextErrors = {};

        fields.forEach((field) => {
            nextTouched[field] = true;
            const ok = validateField(field, values[field]);
            if (!ok) {
                nextErrors[field] = true;
            }
        });

        setTouched((prev) => ({ ...prev, ...nextTouched }));
        if (Object.keys(nextErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                name: values.name.trim(),
                quantity: Number(values.quantity),
                location: values.location.trim() || null,
                observations: values.observations.trim() || null,
            };

            if (isEdit) {
                await updateAssetComponent(componentData.id, payload);
            } else {
                await createAssetComponent(assetId, payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            const data = error?.response?.data;
            const mainMsg = data?.message ?? error?.message ?? 'No se pudo guardar el componente';
            const fieldErrors = data?.errors;
            const fullMsg = fieldErrors?.length
                ? `${mainMsg}:\n${fieldErrors.map((err) => `• ${err}`).join('\n')}`
                : mainMsg;
            setAlert({ type: 'error', message: fullMsg });
        } finally {
            setSaving(false);
        }
    };

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="sm"
                icon={WidgetsOutlinedIcon}
                title={isEdit ? 'Editar componente' : 'Nuevo componente'}
                subtitle="Completa los datos del componente asociado"
                loading={saving}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{
                    label: saving ? 'Guardando…' : (isEdit ? 'Guardar cambios' : 'Agregar componente'),
                    onClick: handleSave,
                    disabled: saving,
                    startIcon: isEdit ? <EditOutlinedIcon /> : <AddCircleOutlinedIcon />,
                }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: accentColor }}>
                        Datos del componente
                    </Typography>

                    <TextField
                        label="Nombre"
                        value={values.name}
                        onChange={(e) => handleChange('name', e.target.value)}
                        onBlur={() => handleBlur('name')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.name && !!errors.name}
                        helperText={touched.name ? (errors.name || ' ') : ' '}
                        sx={fieldSx}
                    />

                    <TextField
                        label="Cantidad"
                        value={values.quantity}
                        onChange={(e) => handleChange('quantity', e.target.value.replace(/[^0-9]/g, ''))}
                        onBlur={() => handleBlur('quantity')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.quantity && !!errors.quantity}
                        helperText={touched.quantity ? (errors.quantity || ' ') : ' '}
                        sx={fieldSx}
                    />

                    <TextField
                        label="Ubicación"
                        value={values.location}
                        onChange={(e) => handleChange('location', e.target.value)}
                        onBlur={() => handleBlur('location')}
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.location && !!errors.location}
                        helperText={touched.location ? (errors.location || ' ') : ' '}
                        sx={fieldSx}
                    />

                    <TextField
                        label="Observaciones"
                        value={values.observations}
                        onChange={(e) => handleChange('observations', e.target.value)}
                        onBlur={() => handleBlur('observations')}
                        fullWidth
                        size="small"
                        multiline
                        minRows={3}
                        disabled={saving}
                        error={touched.observations && !!errors.observations}
                        helperText={touched.observations ? (errors.observations || ' ') : ' '}
                        sx={fieldSx}
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

