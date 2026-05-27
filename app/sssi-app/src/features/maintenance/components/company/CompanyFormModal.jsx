import { useEffect, useState } from 'react';
import { Box, TextField, Typography, useTheme } from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { createCompany, fetchCompanyById, updateCompany } from '../../services/companiesService';
import { joinTextList, normalizeTextList } from '../../maintenanceUtils';

const INITIAL_VALUES = {
    name: '',
    legalId: '',
    contactEmail: '',
    contactPhone: '',
    address: '',
    keycloakUserIdsText: '',
};

export default function CompanyFormModal({ open, onClose, onSaved, companyId = null }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const isEdit = !!companyId;

    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingCompany, setLoadingCompany] = useState(false);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open) {
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setLoadingCompany(false);
            setAlert(null);
            return;
        }

        if (!companyId) {
            setFormValues(INITIAL_VALUES);
            return;
        }

        let cancelled = false;
        setLoadingCompany(true);

        fetchCompanyById(companyId)
            .then((company) => {
                if (cancelled || !company) return;
                setFormValues({
                    name: company.name ?? '',
                    legalId: company.legalId ?? '',
                    contactEmail: company.contactEmail ?? '',
                    contactPhone: company.contactPhone ?? '',
                    address: company.address ?? '',
                    keycloakUserIdsText: joinTextList(company.keycloakUserIds ?? []),
                });
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo cargar la empresa' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoadingCompany(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open, companyId]);

    const validateField = (key, value) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;

        if ((key === 'name' || key === 'legalId') && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'contactEmail' && trimmed) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(trimmed)) error = 'El correo electrónico no es válido';
        }

        if (!error && key === 'contactPhone' && trimmed) {
            if (trimmed.length < 6) error = 'El teléfono es demasiado corto';
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
        const requiredFields = ['name', 'legalId'];
        const nextTouched = {};
        const nextErrors = {};

        requiredFields.forEach((key) => {
            nextTouched[key] = true;
            if (!formValues[key]?.trim()) {
                nextErrors[key] = 'Este campo es requerido';
            }
        });

        if (formValues.contactEmail?.trim()) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(formValues.contactEmail.trim())) {
                nextErrors.contactEmail = 'El correo electrónico no es válido';
                nextTouched.contactEmail = true;
            }
        }

        if (formValues.contactPhone?.trim() && formValues.contactPhone.trim().length < 6) {
            nextErrors.contactPhone = 'El teléfono es demasiado corto';
            nextTouched.contactPhone = true;
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
                name: formValues.name.trim(),
                legalId: formValues.legalId.trim(),
                contactEmail: formValues.contactEmail.trim() || null,
                contactPhone: formValues.contactPhone.trim() || null,
                address: formValues.address.trim() || null,
                keycloakUserIds: normalizeTextList(formValues.keycloakUserIdsText),
            };

            if (isEdit) {
                await updateCompany(companyId, payload);
            } else {
                await createCompany(payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar la empresa' });
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
                icon={BusinessIcon}
                title={isEdit ? 'Editar empresa' : 'Nueva empresa'}
                subtitle={isEdit ? 'Actualiza la información de la empresa' : 'Registra una nueva empresa para mantenimiento'}
                loading={saving || loadingCompany}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{ label: saving ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear empresa', onClick: handleSave, disabled: saving || loadingCompany }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                    <TextField
                        label="Nombre"
                        value={formValues.name}
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
                        label="Cédula jurídica"
                        value={formValues.legalId}
                        onChange={(e) => handleChange('legalId', e.target.value)}
                        onBlur={() => handleBlur('legalId')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.legalId && !!errors.legalId}
                        helperText={touched.legalId ? (errors.legalId || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Correo de contacto"
                        value={formValues.contactEmail}
                        onChange={(e) => handleChange('contactEmail', e.target.value)}
                        onBlur={() => handleBlur('contactEmail')}
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.contactEmail && !!errors.contactEmail}
                        helperText={touched.contactEmail ? (errors.contactEmail || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Teléfono de contacto"
                        value={formValues.contactPhone}
                        onChange={(e) => handleChange('contactPhone', e.target.value)}
                        onBlur={() => handleBlur('contactPhone')}
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.contactPhone && !!errors.contactPhone}
                        helperText={touched.contactPhone ? (errors.contactPhone || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Dirección"
                        value={formValues.address}
                        onChange={(e) => handleChange('address', e.target.value)}
                        onBlur={() => handleBlur('address')}
                        fullWidth
                        size="small"
                        disabled={saving}
                        multiline
                        minRows={3}
                        sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                    />
                    <TextField
                        label="Ids de usuarios Keycloak"
                        value={formValues.keycloakUserIdsText}
                        onChange={(e) => handleChange('keycloakUserIdsText', e.target.value)}
                        fullWidth
                        size="small"
                        disabled={saving}
                        multiline
                        minRows={4}
                        helperText="Separa los ids con salto de línea, coma o punto y coma"
                        sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                    />
                    <Typography sx={{ gridColumn: '1 / -1', color: 'text.secondary', fontSize: 12.5 }}>
                        Los usuarios vinculados pueden administrarse luego desde el panel de detalle de la empresa.
                    </Typography>
                </Box>
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}

