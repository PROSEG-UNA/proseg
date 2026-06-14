import { useEffect, useState } from 'react';
import { Box, TextField, Typography, Button, useTheme, InputAdornment } from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import PersonAddAlt1Icon from '@mui/icons-material/PersonAddAlt1';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { createCompany, fetchCompanyById, fetchCompanyUsers, inviteCompanyUser, updateCompany } from '../../services/company/companiesService';
import { searchUsers } from '../../../security/services/usersService';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';

const LIMITS = {
    legalId:      { max: 20 },
    name:         { max: 120 },
    contactEmail: { max: 120 },
    contactPhone: { max: 20 },
    address:      { max: 255 },
    username:     { max: 64 },
    firstName:    { max: 24 },
    lastName:     { max: 24 },
    email:        { max: 120 },
};

const maskLegalId = (value) => value.replace(/[^0-9-]/g, '').slice(0, LIMITS.legalId.max);
const maskPhone   = (value) => value.replace(/[^0-9+() -]/g, '').slice(0, LIMITS.contactPhone.max);
const capAt       = (field, value) => value.slice(0, LIMITS[field].max);

const getUserLabel = (u) => {
    const name = `${u.firstName || ''} ${u.lastName || ''}`.trim();
    if (name) return `${name} · ${u.email || u.username || u.id}`;
    return u.email || u.username || u.id;
};

const INITIAL_VALUES = {
    name: '',
    legalId: '',
    contactEmail: '',
    contactPhone: '',
    address: '',
};

const INITIAL_INVITE_VALUES = {
    username: '',
    firstName: '',
    lastName: '',
    email: '',
};

function CharCounter({ current, max, error }) {
    const near  = current >= max * 0.85;
    const color = error ? 'error.main' : near ? 'warning.main' : 'text.disabled';
    return (
        <Box component="span" sx={{ color, fontSize: 11, whiteSpace: 'nowrap' }}>
            {current}/{max}
        </Box>
    );
}

export default function CompanyFormModal({ open, onClose, onSaved, companyId = null }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const isEdit = !!companyId;

    const [formValues, setFormValues]         = useState(INITIAL_VALUES);
    const [errors, setErrors]                 = useState({});
    const [touched, setTouched]               = useState({});
    const [saving, setSaving]                 = useState(false);
    const [loadingCompany, setLoadingCompany] = useState(false);
    const [availableUsers, setAvailableUsers] = useState([]);
    const [userSearch, setUserSearch]         = useState('');
    const debouncedUserSearch                 = useDebounce(userSearch, 350);
    const [selectedUsers, setSelectedUsers]   = useState({});
    const [loadingUsers, setLoadingUsers]     = useState(false);
    const [alert, setAlert]                   = useState(null);
    const [inviteOpen, setInviteOpen]         = useState(false);
    const [inviteSaving, setInviteSaving]     = useState(false);
    const [inviteValues, setInviteValues]     = useState(INITIAL_INVITE_VALUES);
    const [inviteErrors, setInviteErrors]     = useState({});
    const [inviteTouched, setInviteTouched]   = useState({});

    useEffect(() => {
        if (!open) {
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setLoadingCompany(false);
            setAlert(null);
            setInviteOpen(false);
            setInviteSaving(false);
            setInviteValues(INITIAL_INVITE_VALUES);
            setInviteErrors({});
            setInviteTouched({});
            return;
        }

        if (!companyId) {
            setFormValues(INITIAL_VALUES);
            setSelectedUsers({});
            return;
        }

        let cancelled = false;
        setLoadingCompany(true);

        Promise.all([
            fetchCompanyById(companyId),
            fetchCompanyUsers(companyId),
        ])
            .then(([company, users]) => {
                if (cancelled || !company) return;
                setFormValues({
                    name:         company.name         ?? '',
                    legalId:      company.legalId      ?? '',
                    contactEmail: company.contactEmail ?? '',
                    contactPhone: company.contactPhone ?? '',
                    address:      company.address      ?? '',
                });
                const userMap = {};
                (users ?? []).forEach((u) => { userMap[u.id] = u; });
                setSelectedUsers(userMap);
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo cargar la empresa' });
                }
            })
            .finally(() => { if (!cancelled) setLoadingCompany(false); });

        return () => { cancelled = true; };
    }, [open, companyId]);

    useEffect(() => {
        if (!open) return;
        let cancelled = false;
        setLoadingUsers(true);

        searchUsers({ page: 0, size: 200, search: debouncedUserSearch })
            .then((page) => { if (!cancelled) setAvailableUsers(page.content ?? []); })
            .catch(() => {})
            .finally(() => { if (!cancelled) setLoadingUsers(false); });

        return () => { cancelled = true; };
    }, [open, debouncedUserSearch]);

    const validateField = (key, value) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;
        const max = LIMITS[key]?.max;

        if (key === 'name' && !trimmed) error = 'Este campo es requerido';
        if (!error && max && trimmed?.length > max) error = `Máximo ${max} caracteres`;
        if (!error && key === 'contactEmail' && trimmed) {
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) error = 'El correo electrónico no es válido';
        }
        if (!error && key === 'contactPhone' && trimmed) {
            if (trimmed.length < 6) error = 'El teléfono es demasiado corto';
        }

        setErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, rawValue) => {
        let value = rawValue;
        if (key === 'legalId')           value = maskLegalId(rawValue);
        else if (key === 'contactPhone') value = maskPhone(rawValue);
        else if (LIMITS[key])            value = capAt(key, rawValue);

        setFormValues((prev) => ({ ...prev, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleBlur = (key) => {
        setTouched((prev) => ({ ...prev, [key]: true }));
        validateField(key, formValues[key]);
    };

    const handleSave = async () => {
        const nextTouched = {};
        const nextErrors  = {};

        nextTouched.name = true;
        if (!formValues.name?.trim()) nextErrors.name = 'Este campo es requerido';

        Object.entries(formValues).forEach(([key, val]) => {
            const trimmed = val?.trim?.() ?? '';
            const max = LIMITS[key]?.max;
            if (!nextErrors[key] && max && trimmed.length > max) {
                nextErrors[key]  = `Máximo ${max} caracteres`;
                nextTouched[key] = true;
            }
        });

        if (formValues.contactEmail?.trim()) {
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formValues.contactEmail.trim())) {
                nextErrors.contactEmail  = 'El correo electrónico no es válido';
                nextTouched.contactEmail = true;
            }
        }

        if (formValues.contactPhone?.trim() && formValues.contactPhone.trim().length < 6) {
            nextErrors.contactPhone  = 'El teléfono es demasiado corto';
            nextTouched.contactPhone = true;
        }

        setTouched((prev) => ({ ...prev, ...nextTouched }));
        setErrors((prev)  => ({ ...prev, ...nextErrors  }));

        if (Object.keys(nextErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                name:            formValues.name.trim(),
                legalId:         formValues.legalId.trim()     || null,
                contactEmail:    formValues.contactEmail.trim() || null,
                contactPhone:    formValues.contactPhone.trim() || null,
                address:         formValues.address.trim()      || null,
                keycloakUserIds: Object.keys(selectedUsers),
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

    const validateInviteField = (key, rawValue) => {
        const value = rawValue?.trim?.() ?? '';
        let error   = '';
        const max   = LIMITS[key]?.max;

        if (!value) {
            error = 'Este campo es requerido';
        } else if (max && value.length > max) {
            error = `Máximo ${max} caracteres`;
        } else if (key === 'username' && !/^[a-zA-Z0-9_-]{3,64}$/.test(value)) {
            error = 'Debe tener entre 3 y 64 caracteres y solo usar letras, números, guiones y guiones bajos';
        } else if ((key === 'firstName' || key === 'lastName') && value.length < 2) {
            error = 'Debe tener al menos 2 caracteres';
        } else if (key === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            error = 'El correo electrónico no es válido';
        }

        setInviteErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleInviteChange = (key, rawValue) => {
        const value = LIMITS[key] ? capAt(key, rawValue) : rawValue;
        setInviteValues((prev) => ({ ...prev, [key]: value }));
        if (inviteTouched[key]) validateInviteField(key, value);
    };

    const handleInviteBlur = (key) => {
        setInviteTouched((prev) => ({ ...prev, [key]: true }));
        validateInviteField(key, inviteValues[key]);
    };

    const openInviteModal = () => {
        setInviteOpen(true);
        setInviteValues((prev) => ({
            ...INITIAL_INVITE_VALUES,
            email: userSearch.includes('@') ? userSearch.trim() : prev.email,
        }));
        setInviteErrors({});
        setInviteTouched({});
    };

    const closeInviteModal = (force = false) => {
        if (inviteSaving && !force) return;
        setInviteOpen(false);
        setInviteValues(INITIAL_INVITE_VALUES);
        setInviteErrors({});
        setInviteTouched({});
    };

    const handleInviteUser = async () => {
        const requiredFields = ['username', 'firstName', 'lastName', 'email'];
        const nextTouched    = {};
        let hasErrors        = false;

        requiredFields.forEach((key) => {
            nextTouched[key] = true;
            if (!validateInviteField(key, inviteValues[key])) hasErrors = true;
        });

        setInviteTouched((prev) => ({ ...prev, ...nextTouched }));
        if (hasErrors) return;

        setInviteSaving(true);
        try {
            const payload = {
                username:  inviteValues.username.trim(),
                firstName: inviteValues.firstName.trim(),
                lastName:  inviteValues.lastName.trim(),
                email:     inviteValues.email.trim(),
            };
            const created     = await inviteCompanyUser(payload);
            const invitedUser = {
                id:        created?.userId,
                username:  created?.username  ?? payload.username,
                email:     created?.email     ?? payload.email,
                firstName: payload.firstName,
                lastName:  payload.lastName,
                status:    created?.status,
            };

            if (!invitedUser.id) throw new Error('No se recibió el identificador del usuario creado');

            setAvailableUsers((prev) => [invitedUser, ...prev.filter((u) => u.id !== invitedUser.id)]);
            setSelectedUsers((prev) => ({ ...prev, [invitedUser.id]: invitedUser }));
            setUserSearch('');
            setAlert({ type: 'success', message: 'Usuario invitado correctamente y agregado a la empresa.' });
            closeInviteModal(true);
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo invitar al usuario') });
        } finally {
            setInviteSaving(false);
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

    const fieldHelper = (key, extraMsg) => {
        const errMsg  = touched[key] ? errors[key] : '';
        const current = formValues[key]?.length ?? 0;
        const max     = LIMITS[key]?.max;
        return (
            <Box component="span" sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}>
                <span>{errMsg || extraMsg || ' '}</span>
                {max && <CharCounter current={current} max={max} error={!!errMsg && current > max} />}
            </Box>
        );
    };

    const inviteHelper = (key) => {
        const errMsg  = inviteTouched[key] ? inviteErrors[key] : '';
        const current = inviteValues[key]?.length ?? 0;
        const max     = LIMITS[key]?.max;
        return (
            <Box component="span" sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}>
                <span>{errMsg || ' '}</span>
                {max && <CharCounter current={current} max={max} error={!!errMsg && current > max} />}
            </Box>
        );
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
                        label="Cédula jurídica"
                        value={formValues.legalId}
                        onChange={(e) => handleChange('legalId', e.target.value)}
                        onBlur={() => handleBlur('legalId')}
                        fullWidth size="small" disabled={saving}
                        placeholder="Opcional — solo dígitos y guiones"
                        inputProps={{ inputMode: 'numeric' }}
                        error={touched.legalId && !!errors.legalId}
                        helperText={fieldHelper('legalId')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Nombre"
                        value={formValues.name}
                        onChange={(e) => handleChange('name', e.target.value)}
                        onBlur={() => handleBlur('name')}
                        required fullWidth size="small" disabled={saving}
                        error={touched.name && !!errors.name}
                        helperText={fieldHelper('name')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Correo de contacto"
                        value={formValues.contactEmail}
                        onChange={(e) => handleChange('contactEmail', e.target.value)}
                        onBlur={() => handleBlur('contactEmail')}
                        fullWidth size="small" disabled={saving}
                        inputProps={{ inputMode: 'email' }}
                        error={touched.contactEmail && !!errors.contactEmail}
                        helperText={fieldHelper('contactEmail')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Teléfono de contacto"
                        value={formValues.contactPhone}
                        onChange={(e) => handleChange('contactPhone', e.target.value)}
                        onBlur={() => handleBlur('contactPhone')}
                        fullWidth size="small" disabled={saving}
                        placeholder="+506 0000-0000"
                        inputProps={{ inputMode: 'tel' }}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start" sx={{ mr: 0.5 }}>
                                    <Typography sx={{ fontSize: 14, color: 'text.secondary' }}>📞</Typography>
                                </InputAdornment>
                            ),
                        }}
                        error={touched.contactPhone && !!errors.contactPhone}
                        helperText={fieldHelper('contactPhone')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Dirección"
                        value={formValues.address}
                        onChange={(e) => handleChange('address', e.target.value)}
                        onBlur={() => handleBlur('address')}
                        fullWidth size="small" disabled={saving}
                        multiline minRows={3}
                        error={touched.address && !!errors.address}
                        helperText={fieldHelper('address')}
                        sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                    />

                    <Box sx={{ gridColumn: '1 / -1', display: 'flex', flexDirection: 'column', gap: 1 }}>
                        <SearchableSelect
                            label="Usuarios del sistema"
                            value={''}
                            onChange={(id) => {
                                const user = availableUsers.find((u) => u.id === id);
                                if (!user) return;
                                if (selectedUsers[user.id]) {
                                    setAlert({ type: 'warning', message: 'El usuario ya está seleccionado' });
                                } else {
                                    setSelectedUsers((prev) => ({ ...prev, [user.id]: user }));
                                }
                                setUserSearch('');
                            }}
                            onBlur={() => {}}
                            items={availableUsers}
                            getItemLabel={(u) => {
                                const name = `${u.firstName || ''} ${u.lastName || ''}`.trim();
                                if (name) return `${name} - ${u.email || u.username || u.id}`;
                                return `${u.username || u.email || u.id} - ${u.email || '—'}`;
                            }}
                            getItemValue={(u) => u.id}
                            fullWidth size="small"
                            disabled={saving || loadingUsers}
                            helperText="Busca y agrega usuarios desde el sistema"
                            externalSearch={userSearch}
                            onSearchChange={(value) => setUserSearch(value)}
                            pageSize={8}
                            onCreate={openInviteModal}
                            createLabel="Invitar nuevo usuario"
                        />

                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                            {Object.values(selectedUsers).length === 0 ? (
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>No hay usuarios seleccionados.</Typography>
                            ) : (
                                Object.values(selectedUsers).map((u) => (
                                    <Button
                                        key={u.id}
                                        size="small"
                                        variant="outlined"
                                        onClick={() => setSelectedUsers((prev) => { const c = { ...prev }; delete c[u.id]; return c; })}
                                        sx={{ textTransform: 'none' }}
                                    >
                                        {getUserLabel(u)} ×
                                    </Button>
                                ))
                            )}
                        </Box>
                    </Box>

                    <Typography sx={{ gridColumn: '1 / -1', color: 'text.secondary', fontSize: 12.5 }}>
                        Los usuarios vinculados pueden administrarse luego desde el panel de detalle de la empresa.
                    </Typography>
                </Box>
            </GeneralModal>

            <GeneralModal
                open={inviteOpen}
                onClose={closeInviteModal}
                maxWidth="sm"
                icon={PersonAddAlt1Icon}
                title="Invitar usuario"
                subtitle="Crea una cuenta nueva sin salir del registro de empresa"
                loading={inviteSaving}
                secondaryButton={{ label: 'Cancelar', onClick: closeInviteModal, disabled: inviteSaving }}
                primaryButton={{ label: inviteSaving ? 'Invitando…' : 'Invitar usuario', onClick: handleInviteUser, disabled: inviteSaving }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'grid', gridTemplateColumns: '1fr', gap: 2 }}>
                    <TextField
                        label="Nombre de usuario"
                        value={inviteValues.username}
                        onChange={(e) => handleInviteChange('username', e.target.value)}
                        onBlur={() => handleInviteBlur('username')}
                        required fullWidth size="small" disabled={inviteSaving}
                        placeholder="ej. juan_perez"
                        error={inviteTouched.username && !!inviteErrors.username}
                        helperText={inviteHelper('username')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Nombre"
                        value={inviteValues.firstName}
                        onChange={(e) => handleInviteChange('firstName', e.target.value)}
                        onBlur={() => handleInviteBlur('firstName')}
                        required fullWidth size="small" disabled={inviteSaving}
                        error={inviteTouched.firstName && !!inviteErrors.firstName}
                        helperText={inviteHelper('firstName')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Apellido"
                        value={inviteValues.lastName}
                        onChange={(e) => handleInviteChange('lastName', e.target.value)}
                        onBlur={() => handleInviteBlur('lastName')}
                        required fullWidth size="small" disabled={inviteSaving}
                        error={inviteTouched.lastName && !!inviteErrors.lastName}
                        helperText={inviteHelper('lastName')}
                        sx={fieldSx}
                    />
                    <TextField
                        label="Correo electrónico"
                        value={inviteValues.email}
                        onChange={(e) => handleInviteChange('email', e.target.value)}
                        onBlur={() => handleInviteBlur('email')}
                        required fullWidth size="small" disabled={inviteSaving}
                        inputProps={{ inputMode: 'email' }}
                        error={inviteTouched.email && !!inviteErrors.email}
                        helperText={inviteHelper('email')}
                        sx={fieldSx}
                    />
                </Box>
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}