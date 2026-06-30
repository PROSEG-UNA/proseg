import { useState, useEffect } from 'react';
import {
    Box, Typography, TextField, MenuItem, Divider, useTheme,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import SettingsInputCompositIcon from '@mui/icons-material/SettingsInputComposite';
import { createMaintenanceRequest, updateMaintenanceRequest, fetchMaintenanceRequestById } from '../../services/maintenanceService.js';
import GeneralModal from "../../../common/components/GeneralModal.jsx";
import DialogModal from "../../../common/components/DialogModal.jsx";

const STATUS_OPTIONS = [
    { value: 'PENDING', label: 'Pendiente' },
    { value: 'IN_PROGRESS', label: 'En progreso' },
    { value: 'COMPLETED', label: 'Completado' },
    { value: 'CANCELLED', label: 'Cancelado' },
];

const PRIORITY_OPTIONS = [
    { value: 'LOW', label: 'Baja' },
    { value: 'MEDIUM', label: 'Media' },
    { value: 'HIGH', label: 'Alta' },
    { value: 'URGENT', label: 'Urgente' },
];

const INIT = {
    title: '',
    description: '',
    assetId: '',
    status: 'PENDING',
    priority: 'MEDIUM',
    scheduledDate: '',
    observations: '',
};

export default function MaintenanceFormModal({ open, onClose, onSaved, requestId = null }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const isEdit = !!requestId;

    const [formValues, setFormValues] = useState(INIT);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const [loadingRequest, setLoadingRequest] = useState(false);

    useEffect(() => {
        if (!open) return;
        setFormValues(INIT);
        setErrors({});
        setTouched({});
        setSaving(false);
        setAlert(null);
    }, [open]);

    const loadRequestData = () => {
        if (!open || !requestId) return;
        let cancelled = false;
        setLoadingRequest(true);

        fetchMaintenanceRequestById(requestId)
            .then(request => {
                if (cancelled) return;
                if (request) {
                    setFormValues({
                        title: request.title ?? '',
                        description: request.description ?? '',
                        assetId: request.assetId ?? '',
                        status: request.status ?? 'PENDING',
                        priority: request.priority ?? 'MEDIUM',
                        scheduledDate: request.scheduledDate ?? '',
                        observations: request.observations ?? '',
                    });
                }
            })
            .catch(() => {
                if (!cancelled) setAlert({ type: 'error', message: 'No se pudo cargar la solicitud' });
            })
            .finally(() => { if (!cancelled) setLoadingRequest(false); });

        return () => { cancelled = true; };
    };

    useEffect(loadRequestData, [open, requestId]);

    const validateField = (key, value) => {
        const required = ['title', 'assetId', 'status', 'priority'];
        let error = '';

        if (required.includes(key) && (!value || (typeof value === 'string' && !value.trim()))) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'title' && value?.trim()) {
            if (value.trim().length < 3) error = 'Mínimo 3 caracteres';
            if (value.trim().length > 200) error = 'Máximo 200 caracteres';
        }

        if (!error && key === 'assetId' && value?.trim()) {
            if (!/^[a-f0-9-]{36}$/.test(value.trim())) {
                error = 'Formato de UUID inválido para el activo';
            }
        }

        setErrors(prev => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        setFormValues(prev => ({ ...prev, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleBlur = (key) => {
        setTouched(prev => ({ ...prev, [key]: true }));
        validateField(key, formValues[key]);
    };

    const doSave = async () => {
        setSaving(true);
        try {
            const payload = {
                title: formValues.title.trim(),
                description: formValues.description?.trim() || null,
                assetId: formValues.assetId.trim(),
                status: formValues.status,
                priority: formValues.priority,
                scheduledDate: formValues.scheduledDate || null,
                observations: formValues.observations?.trim() || null,
            };

            if (isEdit) {
                await updateMaintenanceRequest(requestId, payload);
            } else {
                await createMaintenanceRequest(payload);
            }

            onSaved?.();
            onClose();
        } catch (e) {
            const data = e?.response?.data;
            const mainMsg = data?.message ?? e?.message ?? 'Error al guardar';
            const fieldErrors = data?.errors;
            const fullMsg = fieldErrors?.length
                ? `${mainMsg}:\n${fieldErrors.map(err => `• ${err}`).join('\n')}`
                : mainMsg;
            setAlert({ type: 'error', message: fullMsg });
        } finally {
            setSaving(false);
        }
    };

    const handleSave = async () => {
        const requiredFields = ['title', 'assetId', 'status', 'priority'];

        const newTouched = {};
        const newErrors = {};

        requiredFields.forEach(key => {
            newTouched[key] = true;
            const val = formValues[key];
            if (!val || (typeof val === 'string' && !val.trim())) {
                newErrors[key] = 'Este campo es requerido';
            }
        });

        if (!newErrors.title && formValues.title.trim().length < 3) {
            newErrors.title = 'Mínimo 3 caracteres';
            newTouched.title = true;
        }

        if (!newErrors.assetId && formValues.assetId?.trim()) {
            if (!/^[a-f0-9-]{36}$/.test(formValues.assetId.trim())) {
                newErrors.assetId = 'Formato de UUID inválido para el activo';
                newTouched.assetId = true;
            }
        }

        setTouched(prev => ({ ...prev, ...newTouched }));
        setErrors(prev => ({ ...prev, ...newErrors }));

        if (Object.keys(newErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        await doSave();
    };

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputBase-input': { color: 'text.primary' },
        '[data-mui-color-scheme="dark"] &': {
            '& .MuiFormHelperText-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiInputLabel-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiFormLabel-asterisk.Mui-error': { color: 'hsl(220, 20%, 65%)' },
        },
    };

    const contentSx = {
        overflowY: 'auto',
        '&::-webkit-scrollbar': { width: '5px' },
        '&::-webkit-scrollbar-track': { background: 'transparent' },
        '&::-webkit-scrollbar-thumb': { background: `color-mix(in srgb, ${accentColor} 25%, transparent)`, borderRadius: '4px' },
        '&::-webkit-scrollbar-thumb:hover': { background: `color-mix(in srgb, ${accentColor} 45%, transparent)` },
    };

    const sectionLabel = (text) => (
        <Typography sx={{
            fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em',
            textTransform: 'uppercase', color: accentColor, mb: 1.5,
        }}>
            {text}
        </Typography>
    );

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                icon={SettingsInputCompositIcon}
                title={isEdit ? 'Editar Solicitud' : 'Nueva Solicitud'}
                subtitle={isEdit ? 'Actualiza los datos de la solicitud' : 'Completa los datos para registrar la solicitud'}
                loading={saving || loadingRequest}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{
                    label: saving
                        ? 'Guardando…'
                        : isEdit ? 'Guardar cambios' : 'Crear Solicitud',
                    onClick: handleSave,
                    disabled: saving || loadingRequest,
                    startIcon: <AddCircleOutlinedIcon />,
                }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 3 }}>

                    <Box>
                        {sectionLabel('Información básica')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <TextField
                                label="Título" value={formValues.title} required
                                onChange={e => handleChange('title', e.target.value)}
                                onBlur={() => handleBlur('title')}
                                fullWidth size="small" disabled={saving}
                                error={touched.title && !!errors.title}
                                helperText={touched.title ? (errors.title || ' ') : ' '}
                                sx={fieldSx}
                            />
                            <TextField
                                label="Descripción" value={formValues.description}
                                onChange={e => handleChange('description', e.target.value)}
                                fullWidth size="small" multiline rows={3} disabled={saving}
                                sx={fieldSx}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Activo')}
                        <TextField
                            label="ID del Activo" value={formValues.assetId} required
                            onChange={e => handleChange('assetId', e.target.value)}
                            onBlur={() => handleBlur('assetId')}
                            fullWidth size="small" disabled={saving}
                            error={touched.assetId && !!errors.assetId}
                            helperText={touched.assetId ? (errors.assetId || ' ') : 'Formato UUID'}
                            placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                            sx={fieldSx}
                        />
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Estado y Prioridad')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <TextField
                                select label="Estado" value={formValues.status} required
                                onChange={e => handleChange('status', e.target.value)}
                                onBlur={() => handleBlur('status')}
                                fullWidth size="small" disabled={saving}
                                error={touched.status && !!errors.status}
                                helperText={touched.status ? (errors.status || ' ') : ' '}
                                sx={fieldSx}
                            >
                                {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                            </TextField>
                            <TextField
                                select label="Prioridad" value={formValues.priority} required
                                onChange={e => handleChange('priority', e.target.value)}
                                onBlur={() => handleBlur('priority')}
                                fullWidth size="small" disabled={saving}
                                error={touched.priority && !!errors.priority}
                                helperText={touched.priority ? (errors.priority || ' ') : ' '}
                                sx={fieldSx}
                            >
                                {PRIORITY_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                            </TextField>
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Fecha Programada')}
                        <DatePicker
                            label="Fecha de mantenimiento"
                            value={formValues.scheduledDate ? dayjs(formValues.scheduledDate) : null}
                            onChange={v => handleChange('scheduledDate', v ? v.format('YYYY-MM-DD') : '')}
                            disabled={saving}
                            slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx } }}
                        />
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Observaciones')}
                        <TextField
                            label="Observaciones" value={formValues.observations}
                            onChange={e => handleChange('observations', e.target.value)}
                            fullWidth size="small" multiline rows={3} disabled={saving}
                            sx={fieldSx}
                        />
                    </Box>

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

