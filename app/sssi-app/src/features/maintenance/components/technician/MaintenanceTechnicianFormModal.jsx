import { useEffect, useState } from 'react';
import { Box, FormControlLabel, Switch, TextField, Typography, useTheme, InputAdornment, MenuItem } from '@mui/material';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import { createMaintenanceTechnician, fetchMaintenanceTechnicianById, updateMaintenanceTechnician } from '../../services/techniciansService';
import { fetchMaintenanceRequests } from '../../services/requestsService';
import { searchUsers } from '../../../security/services/usersService';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';

const INITIAL_VALUES = {
    maintenanceRequestId: '',
    fullName: '',
    position: '',
    email: '',
    phone: '',
    leader: false,
};

export default function MaintenanceTechnicianFormModal({ open, onClose, onSaved, technicianId = null, maintenanceRequestId = '' }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const isEdit = !!technicianId;

    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingTechnician, setLoadingTechnician] = useState(false);
    const [loadingOptions, setLoadingOptions] = useState(false);
    const [availableUsers, setAvailableUsers] = useState([]);
    const [userSearch, setUserSearch] = useState('');
    const [userPage, setUserPage] = useState(0);
    const debouncedUserSearch = useDebounce(userSearch, 350);
    const [requests, setRequests] = useState([]);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open) {
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setLoadingTechnician(false);
            setLoadingOptions(false);
            setRequests([]);
            setAlert(null);
            return;
        }

        setFormValues((prev) => ({ ...INITIAL_VALUES, maintenanceRequestId: maintenanceRequestId || prev.maintenanceRequestId || '' }));
    }, [open, maintenanceRequestId]);

    useEffect(() => {
        if (!open || isEdit || maintenanceRequestId) return;
        let cancelled = false;
        setLoadingOptions(true);

        fetchMaintenanceRequests({ page: 0, size: 200 })
            .then((page) => {
                if (!cancelled) setRequests(page.content ?? []);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingOptions(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open, isEdit, maintenanceRequestId]);

    useEffect(() => {
        if (!open) return;
        let cancelled = false;
        setLoadingOptions(true);

        searchUsers({ page: userPage, size: 8, search: debouncedUserSearch })
            .then((page) => {
                if (!cancelled) setAvailableUsers(page.content ?? []);
            })
            .catch(() => {})
            .finally(() => { if (!cancelled) setLoadingOptions(false); });

        return () => { cancelled = true; };
    }, [open, userPage, debouncedUserSearch]);

    useEffect(() => {
        if (!open || !technicianId) return;
        let cancelled = false;
        setLoadingTechnician(true);

        fetchMaintenanceTechnicianById(technicianId)
            .then((technician) => {
                if (cancelled || !technician) return;
                setFormValues({
                    maintenanceRequestId: maintenanceRequestId || '',
                    fullName: technician.fullName ?? '',
                    position: technician.position ?? '',
                    email: technician.email ?? '',
                    phone: technician.phone ?? '',
                    keycloakUserId: technician.keycloakUserId ?? '',
                    leader: !!technician.leader,
                });
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo cargar el técnico' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoadingTechnician(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open, technicianId, maintenanceRequestId]);

    const validateField = (key, value) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;

        if (['maintenanceRequestId', 'fullName'].includes(key) && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'email' && trimmed) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(trimmed)) error = 'El correo electrónico no es válido';
        }

        if (!error && key === 'phone' && trimmed && trimmed.length < 6) {
            error = 'El teléfono es demasiado corto';
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
        const nextTouched = { fullName: true };
        const nextErrors = {};

        if (!formValues.fullName?.trim()) nextErrors.fullName = 'Este campo es requerido';

        if (!isEdit && !maintenanceRequestId && !formValues.maintenanceRequestId?.trim()) {
            nextTouched.maintenanceRequestId = true;
            nextErrors.maintenanceRequestId = 'Este campo es requerido';
        }

        if (formValues.email?.trim()) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(formValues.email.trim())) {
                nextErrors.email = 'El correo electrónico no es válido';
                nextTouched.email = true;
            }
        }

        if (formValues.phone?.trim() && formValues.phone.trim().length < 6) {
            nextErrors.phone = 'El teléfono es demasiado corto';
            nextTouched.phone = true;
        }

        setTouched((prev) => ({ ...prev, ...nextTouched }));
        setErrors((prev) => ({ ...prev, ...nextErrors }));

        if (Object.keys(nextErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        const resolvedRequestId = maintenanceRequestId || formValues.maintenanceRequestId || null;

        setSaving(true);
        try {
            const payload = {
                fullName: formValues.fullName.trim(),
                position: formValues.position.trim() || null,
                email: formValues.email.trim() || null,
                phone: formValues.phone.trim() || null,
                leader: !!formValues.leader,
                keycloakUserId: formValues.keycloakUserId || null,
            };

            if (isEdit) {
                await updateMaintenanceTechnician(technicianId, payload);
            } else {
                await createMaintenanceTechnician(resolvedRequestId, payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar el técnico' });
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

    const requestOptionsDisabled = saving || loadingOptions || loadingTechnician || !!maintenanceRequestId;

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="sm"
                icon={MiscellaneousServicesIcon}
                title={isEdit ? 'Editar técnico' : 'Nuevo técnico de mantenimiento'}
                subtitle={isEdit ? 'Actualiza los datos del técnico' : 'Registra un técnico para una solicitud de mantenimiento'}
                loading={saving || loadingTechnician}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{ label: saving ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear técnico', onClick: handleSave, disabled: saving || loadingTechnician || loadingOptions }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'grid', gridTemplateColumns: '1fr', gap: 2 }}>
                    {!isEdit && !maintenanceRequestId && (
                        <SearchableSelect
                            label="Solicitud de mantenimiento"
                            value={formValues.maintenanceRequestId}
                            onChange={(value) => handleChange('maintenanceRequestId', value)}
                            onBlur={() => handleBlur('maintenanceRequestId')}
                            required
                            fullWidth
                            size="small"
                            disabled={requestOptionsDisabled}
                            error={touched.maintenanceRequestId && !!errors.maintenanceRequestId}
                            helperText={touched.maintenanceRequestId ? (errors.maintenanceRequestId || ' ') : ' '}
                            sx={fieldSx}
                            items={requests}
                            getItemLabel={(request) => `${request.title} — ${request.company?.name ?? 'Sin empresa'}`}
                            getItemValue={(request) => request.id}
                        />
                    )}
                    {maintenanceRequestId && !isEdit && (
                        <Typography sx={{ fontSize: 13.25, color: 'text.secondary' }}>
                            El técnico se asociará a la solicitud seleccionada desde el detalle.
                        </Typography>
                    )}
                    <TextField
                        label="Buscar usuario del sistema (opcional)"
                        value={userSearch}
                        onChange={(e) => setUserSearch(e.target.value)}
                        fullWidth
                        size="small"
                        disabled={saving}
                        placeholder="Busca por nombre, correo o usuario"
                        InputProps={{ startAdornment: <InputAdornment position="start">🔍</InputAdornment> }}
                    />

                    <TextField
                        select
                        label="Usuarios encontrados"
                        value={formValues.keycloakUserId || ''}
                        onChange={(e) => {
                            const v = e.target.value;
                            setFormValues((prev) => ({ ...prev, keycloakUserId: v }));
                            const sel = availableUsers.find(u => u.id === v);
                            if (sel) {
                                // auto fill if empty
                                setFormValues((prev) => ({
                                    ...prev,
                                    fullName: prev.fullName || ((sel.firstName || '') + ' ' + (sel.lastName || '')).trim(),
                                    email: prev.email || sel.email || '',
                                    phone: prev.phone || '',
                                }));
                            }
                        }}
                        fullWidth
                        size="small"
                        disabled={saving}
                        helperText={formValues.keycloakUserId ? `ID: ${formValues.keycloakUserId}` : 'Elige un usuario del sistema (opcional)'}
                    >
                        {availableUsers.length === 0 ? (
                            <MenuItem disabled value="">Sin resultados</MenuItem>
                        ) : (
                            availableUsers.map((user) => (
                                <MenuItem key={user.id} value={user.id}>{`${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username} · {user.email || '—'}</MenuItem>
                            ))
                        )}
                    </TextField>
                    <TextField
                        label="Nombre completo"
                        value={formValues.fullName}
                        onChange={(e) => handleChange('fullName', e.target.value)}
                        onBlur={() => handleBlur('fullName')}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.fullName && !!errors.fullName}
                        helperText={touched.fullName ? (errors.fullName || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Puesto"
                        value={formValues.position}
                        onChange={(e) => handleChange('position', e.target.value)}
                        fullWidth
                        size="small"
                        disabled={saving}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Correo"
                        value={formValues.email}
                        onChange={(e) => handleChange('email', e.target.value)}
                        onBlur={() => handleBlur('email')}
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.email && !!errors.email}
                        helperText={touched.email ? (errors.email || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Teléfono"
                        value={formValues.phone}
                        onChange={(e) => handleChange('phone', e.target.value)}
                        onBlur={() => handleBlur('phone')}
                        fullWidth
                        size="small"
                        disabled={saving}
                        error={touched.phone && !!errors.phone}
                        helperText={touched.phone ? (errors.phone || ' ') : ' '}
                        sx={fieldSx}
                    />
                    <FormControlLabel
                        control={
                            <Switch
                                checked={formValues.leader}
                                onChange={(e) => handleChange('leader', e.target.checked)}
                                disabled={saving}
                                size="small"
                                sx={{
                                    '& .MuiSwitch-switchBase.Mui-checked': { color: accentColor },
                                    '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': { backgroundColor: accentColor },
                                }}
                            />
                        }
                        label={<Typography sx={{ fontSize: 13.5, color: 'text.primary' }}>¿Es líder?</Typography>}
                    />
                </Box>
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}


