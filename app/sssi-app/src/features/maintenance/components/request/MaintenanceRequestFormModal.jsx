import { useEffect, useState } from 'react';
import { Box, MenuItem, TextField, Typography, useTheme } from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import { createMaintenanceRequest, fetchMaintenanceRequestById, updateMaintenanceRequest } from '../../services/requestsService';
import { fetchCompanies } from '../../services/companiesService';
import { MAINTENANCE_PRIORITY_OPTIONS, MAINTENANCE_STATUS_OPTIONS } from '../../maintenanceUtils';

const INITIAL_VALUES = {
    companyId: '',
    assetId: '',
    title: '',
    description: '',
    status: 'PENDING',
    priority: 'LOW',
    scheduledDate: '',
    observations: '',
};

export default function MaintenanceRequestFormModal({ open, onClose, onSaved, requestId = null, initialCompanyId = '' }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const isEdit = !!requestId;

    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingRequest, setLoadingRequest] = useState(false);
    const [loadingOptions, setLoadingOptions] = useState(false);
    const [companies, setCompanies] = useState([]);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open) {
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setLoadingRequest(false);
            setLoadingOptions(false);
            setCompanies([]);
            setAlert(null);
            return;
        }

        setFormValues((prev) => ({ ...INITIAL_VALUES, companyId: initialCompanyId || prev.companyId || '' }));
    }, [open, initialCompanyId]);

    useEffect(() => {
        if (!open) return;
        let cancelled = false;
        setLoadingOptions(true);

        fetchCompanies({ page: 0, size: 200 })
            .then((page) => {
                if (!cancelled) setCompanies(page.content ?? []);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingOptions(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open]);

    useEffect(() => {
        if (!open || !requestId) return;
        let cancelled = false;
        setLoadingRequest(true);

        fetchMaintenanceRequestById(requestId)
            .then((request) => {
                if (cancelled || !request) return;
                setFormValues({
                    companyId: request.company?.id ?? initialCompanyId ?? '',
                    assetId: request.assetId ?? '',
                    title: request.title ?? '',
                    description: request.description ?? '',
                    status: request.status ?? 'PENDING',
                    priority: request.priority ?? 'LOW',
                    scheduledDate: request.scheduledDate ?? '',
                    observations: request.observations ?? '',
                });
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo cargar la solicitud' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoadingRequest(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open, requestId, initialCompanyId]);

    const validateField = (key, value) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;

        if (['companyId', 'assetId', 'title', 'status', 'priority'].includes(key) && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'title' && trimmed && trimmed.length > 150) {
            error = 'El título no puede superar los 150 caracteres';
        }

        setErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        setFormValues((prev) => ({ ...prev, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleBlur = (key) => {
        setTouched((prev) => ({ ...prev, [key]: true }));
        validateField(key, formValues[key]);
    };

    const handleSave = async () => {
        const requiredFields = ['companyId', 'assetId', 'title', 'status', 'priority'];
        const nextTouched = {};
        const nextErrors = {};

        requiredFields.forEach((key) => {
            nextTouched[key] = true;
            if (!formValues[key]?.trim()) {
                nextErrors[key] = 'Este campo es requerido';
            }
        });

        if (formValues.title?.trim()?.length > 150) {
            nextErrors.title = 'El título no puede superar los 150 caracteres';
            nextTouched.title = true;
        }

        setTouched((prev) => ({ ...prev, ...nextTouched }));
        setErrors((prev) => ({ ...prev, ...nextErrors }));

        if (Object.keys(nextErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                companyId: formValues.companyId,
                assetId: formValues.assetId,
                title: formValues.title.trim(),
                description: formValues.description.trim() || null,
                status: formValues.status,
                priority: formValues.priority,
                scheduledDate: formValues.scheduledDate || null,
                observations: formValues.observations.trim() || null,
            };

            if (isEdit) {
                await updateMaintenanceRequest(requestId, payload);
            } else {
                await createMaintenanceRequest(payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar la solicitud' });
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

    const contentSx = {
        overflowY: 'auto',
        '&::-webkit-scrollbar': { width: '5px' },
        '&::-webkit-scrollbar-track': { background: 'transparent' },
        '&::-webkit-scrollbar-thumb': { background: `color-mix(in srgb, ${accentColor} 25%, transparent)`, borderRadius: '4px' },
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                icon={ConstructionIcon}
                title={isEdit ? 'Editar solicitud' : 'Nueva solicitud de mantenimiento'}
                subtitle={isEdit ? 'Actualiza los datos de la solicitud' : 'Registra una nueva solicitud de mantenimiento'}
                loading={saving || loadingRequest}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{ label: saving ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear solicitud', onClick: handleSave, disabled: saving || loadingRequest || loadingOptions }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                    <SearchableSelect
                        label="Empresa"
                        value={formValues.companyId}
                        onChange={(value) => handleChange('companyId', value)}
                        onBlur={() => handleBlur('companyId')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving || loadingOptions}
                        error={touched.companyId && !!errors.companyId}
                        helperText={touched.companyId ? (errors.companyId || ' ') : ' '}
                        sx={fieldSx}
                        items={companies}
                        getItemLabel={(company) => `${company.name} — ${company.legalId}`}
                        getItemValue={(company) => company.id}
                    />
                    <TextField
                        label="Id del activo"
                        value={formValues.assetId}
                        onChange={(e) => handleChange('assetId', e.target.value)}
                        onBlur={() => handleBlur('assetId')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.assetId && !!errors.assetId}
                        helperText={touched.assetId ? (errors.assetId || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Título"
                        value={formValues.title}
                        onChange={(e) => handleChange('title', e.target.value)}
                        onBlur={() => handleBlur('title')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.title && !!errors.title}
                        helperText={touched.title ? (errors.title || ' ') : ' '}
                        sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                    />
                    <TextField
                        label="Descripción"
                        value={formValues.description}
                        onChange={(e) => handleChange('description', e.target.value)}
                        fullWidth
                        size="small"
                        disabled={saving}
                        multiline
                        minRows={3}
                        sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        onChange={(e) => handleChange('status', e.target.value)}
                        onBlur={() => handleBlur('status')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.status && !!errors.status}
                        helperText={touched.status ? (errors.status || ' ') : ' '}
                        sx={fieldSx}
                    >
                        {MAINTENANCE_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Prioridad"
                        value={formValues.priority}
                        onChange={(e) => handleChange('priority', e.target.value)}
                        onBlur={() => handleBlur('priority')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.priority && !!errors.priority}
                        helperText={touched.priority ? (errors.priority || ' ') : ' '}
                        sx={fieldSx}
                    >
                        {MAINTENANCE_PRIORITY_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <DatePicker
                        label="Fecha programada"
                        value={formValues.scheduledDate ? dayjs(formValues.scheduledDate) : null}
                        onChange={(value) => handleChange('scheduledDate', value ? value.format('YYYY-MM-DD') : '')}
                        disabled={saving}
                        slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx } }}
                    />
                    <TextField
                        label="Observaciones"
                        value={formValues.observations}
                        onChange={(e) => handleChange('observations', e.target.value)}
                        fullWidth
                        size="small"
                        disabled={saving}
                        multiline
                        minRows={3}
                        sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                    />
                    <Typography sx={{ gridColumn: '1 / -1', color: 'text.secondary', fontSize: 12.5 }}>
                        Si abres esta ventana desde la empresa, el campo Empresa se cargará automáticamente.
                    </Typography>
                </Box>
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}

