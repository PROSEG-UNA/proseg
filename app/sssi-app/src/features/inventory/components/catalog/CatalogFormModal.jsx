import { useState, useEffect } from 'react';
import {
    Box, Typography,
    TextField, FormControlLabel, Switch,
    useTheme,
} from '@mui/material';
import AlertModal from '../../../../common/components/AlertModal.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { createCatalogItem, updateCatalogItem, fetchCatalogOptions } from '../../services/catalogService';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';

export default function CatalogFormModal({ open, onClose, onSaved, config, row }) {
    const theme = useTheme();

    const { title = '', icon: Icon = null, formFields = [], baseUrl = '' } = config ?? {};
    const isEditMode = !!row;

    const accentColor = theme.vars.palette.tones.rose.fg;

    const [formValues, setFormValues] = useState({});
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const [selectOptions, setSelectOptions] = useState({});
    const [loadingOptions, setLoadingOptions] = useState(false);

    const initForm = () => {
        if (!open || !config) return;
        const values = {};
        formFields.forEach((field) => {
            if (field.type === 'boolean') {
                values[field.key] = field.getInitialValue ? field.getInitialValue(row) : (row?.[field.key] ?? false);
            } else if (field.type === 'select') {
                values[field.key] = field.getInitialValue ? field.getInitialValue(row) : '';
            } else {
                values[field.key] = row?.[field.key] ?? '';
            }
        });
        setFormValues(values);
        setErrors({});
        setTouched({});
        setSaving(false);
        setAlert(null);
    };

    useEffect(initForm, [open, row]);

    const loadSelectOptions = () => {
        if (!open || !config) return;
        const selectFields = formFields.filter((f) => f.type === 'select');
        if (selectFields.length === 0) return;

        let cancelled = false;
        setLoadingOptions(true);

        Promise.all(
            selectFields.map((field) =>
                fetchCatalogOptions(field.optionsUrl).then((opts) => ({ key: field.key, opts }))
            )
        )
            .then((results) => {
                if (!cancelled) {
                    const options = {};
                    results.forEach(({ key, opts }) => { options[key] = opts; });
                    setSelectOptions(options);
                }
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingOptions(false);
            });

        return () => { cancelled = true; };
    };

    useEffect(loadSelectOptions, [open, config]);

    const validateSingleField = (key, value) => {
        const field = formFields.find((f) => f.key === key);
        if (!field || !field.required || field.type === 'boolean') return true;
        const empty = !value || (typeof value === 'string' && !value.trim());
        setErrors((prev) => ({ ...prev, [key]: empty ? 'Este campo es requerido' : '' }));
        return !empty;
    };

    const handleChange = (key, value) => {
        setFormValues((prev) => ({ ...prev, [key]: value }));
        if (touched[key]) validateSingleField(key, value);
    };

    const handleBlur = (key) => {
        setTouched((prev) => ({ ...prev, [key]: true }));
        validateSingleField(key, formValues[key]);
    };

    const handleSave = async () => {
        const newTouched = {};
        const newErrors = {};
        formFields.forEach((field) => {
            newTouched[field.key] = true;
            if (field.required && field.type !== 'boolean') {
                const val = formValues[field.key];
                const empty = !val || (typeof val === 'string' && !val.trim());
                if (empty) newErrors[field.key] = 'Este campo es requerido';
            }
        });
        setTouched(newTouched);
        setErrors(newErrors);

        if (Object.keys(newErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {};
            formFields.forEach((field) => { payload[field.key] = formValues[field.key]; });

            if (isEditMode) {
                await updateCatalogItem(baseUrl, row.id, payload);
            } else {
                await createCatalogItem(baseUrl, payload);
            }
            onSaved?.();
        } catch (e) {
            setAlert({ type: 'error', message: e?.response?.data?.message ?? e?.message ?? 'Error al guardar' });
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
        '& .MuiInputBase-input': { color: 'text.primary' },
    };

    const contentSx = {
        overflowY: 'auto',
        '&::-webkit-scrollbar': { width: '5px' },
        '&::-webkit-scrollbar-track': { background: 'transparent' },
        '&::-webkit-scrollbar-thumb': { background: `color-mix(in srgb, ${accentColor} 25%, transparent)`, borderRadius: '4px' },
        '&::-webkit-scrollbar-thumb:hover': { background: `color-mix(in srgb, ${accentColor} 45%, transparent)` },
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={Icon}
                title={isEditMode ? `Editar ${title}` : `Nueva ${title}`}
                subtitle={isEditMode
                    ? `Modifica los datos de la ${title.toLowerCase()}`
                    : `Completa los datos para crear la ${title.toLowerCase()}`}
                loading={saving}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{
                    label: saving ? 'Guardando…' : isEditMode ? 'Guardar cambios' : `Crear ${title}`,
                    onClick: handleSave,
                    disabled: saving,
                }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {formFields.map((field) => {
                        if (field.type === 'boolean') {
                            return (
                                <FormControlLabel
                                    key={field.key}
                                    control={
                                        <Switch
                                            checked={formValues[field.key] ?? false}
                                            onChange={(e) => handleChange(field.key, e.target.checked)}
                                            disabled={saving}
                                            size="small"
                                            sx={{
                                                '& .MuiSwitch-switchBase.Mui-checked': { color: accentColor },
                                                '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': { backgroundColor: accentColor },
                                            }}
                                        />
                                    }
                                    label={
                                        <Typography sx={{ fontSize: 13.5, color: 'text.primary' }}>
                                            {field.label}
                                        </Typography>
                                    }
                                />
                            );
                        }

                        if (field.type === 'select') {
                            return (
                                <SearchableSelect
                                    key={field.key}
                                    label={field.label}
                                    value={formValues[field.key] ?? ''}
                                    onChange={(val) => handleChange(field.key, val)}
                                    onBlur={() => handleBlur(field.key)}
                                    fullWidth
                                    size="small"
                                    required={field.required}
                                    disabled={saving || loadingOptions}
                                    error={touched[field.key] && !!errors[field.key]}
                                    helperText={touched[field.key] ? (errors[field.key] || ' ') : ' '}
                                    sx={fieldSx}
                                    items={selectOptions[field.key] ?? []}
                                    getItemLabel={field.getOptionLabel}
                                    getItemValue={field.getOptionValue}
                                />
                            );
                        }

                        return (
                            <TextField
                                key={field.key}
                                label={field.label}
                                value={formValues[field.key] ?? ''}
                                onChange={(e) => handleChange(field.key, e.target.value)}
                                onBlur={() => handleBlur(field.key)}
                                fullWidth
                                size="small"
                                multiline={field.type === 'textarea'}
                                rows={field.type === 'textarea' ? 3 : undefined}
                                required={field.required}
                                disabled={saving}
                                error={touched[field.key] && !!errors[field.key]}
                                helperText={touched[field.key] ? (errors[field.key] || ' ') : ' '}
                                sx={fieldSx}
                            />
                        );
                    })}
                </Box>
            </GeneralModal>

            <AlertModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
