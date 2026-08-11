import { useState } from 'react';
import {
    Box,
    Typography,
    Stack,
    useTheme,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import DialogModal from '../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import { createUser } from '../services/usersService';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import { getFriendlyApiErrorMessage } from '../../../common/utils/index.js';

const INITIAL_FORM = {
    username: '',
    firstName: '',
    lastName: '',
    email: '',
};

export default function CreateUserModal({ open, onClose, onSaved }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const { formData, errors, touched, handleChange, handleBlur, validateForm, resetForm } = useFormValidation(
        INITIAL_FORM,
        ['username', 'firstName', 'lastName', 'email']
    );

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
    };

    const resetAndClose = () => {
        resetForm();
        onClose?.();
    };

    const handleSubmit = async () => {
        if (!validateForm()) {
            setAlert({ type: 'error', message: 'Revisa los campos del formulario antes de continuar.' });
            return;
        }
        setSaving(true);
        try {
            await createUser(formData);
            setAlert({ type: 'success', message: 'Usuario creado correctamente. Se envió la contraseña al correo indicado.' });
            onSaved?.();
        } catch (err) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(err, 'No se pudo crear el usuario.') });
        } finally {
            setSaving(false);
        }
    };

    const handleAlertClose = () => {
        if (alert?.type === 'success') resetAndClose();
        setAlert(null);
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={() => !saving && resetAndClose()}
                maxWidth="md"
                icon={PersonAddIcon}
                title="Crear usuario"
                subtitle="Completa los datos y el sistema enviará la contraseña por correo"
                loading={saving}
                footerLeft={
                    <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                        Se enviará una contraseña temporal por email
                    </Typography>
                }
                secondaryButton={{ label: 'Cancelar', onClick: resetAndClose, disabled: saving }}
                primaryButton={{ label: saving ? 'Creando…' : 'Crear usuario', onClick: handleSubmit, disabled: saving, loading: saving }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2 }}>
                    <Typography sx={{ fontSize: 10.5, fontWeight: 800, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.5 }}>
                        Información básica
                    </Typography>
                    <Stack spacing={1.5}>
                        <ValidatedTextField
                            fieldName="username"
                            fullWidth required
                            label="Nombre de usuario"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.username && !!errors.username}
                            helperText={touched.username && errors.username}
                            sx={fieldSx}
                        />
                        <ValidatedTextField
                            fieldName="firstName"
                            fullWidth required
                            label="Nombre"
                            name="firstName"
                            value={formData.firstName}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.firstName && !!errors.firstName}
                            helperText={touched.firstName && errors.firstName}
                            sx={fieldSx}
                        />
                        <ValidatedTextField
                            fieldName="lastName"
                            fullWidth required
                            label="Apellido"
                            name="lastName"
                            value={formData.lastName}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.lastName && !!errors.lastName}
                            helperText={touched.lastName && errors.lastName}
                            sx={fieldSx}
                        />
                        <ValidatedTextField
                            fieldName="email"
                            fullWidth required
                            type="email"
                            label="Email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.email && !!errors.email}
                            helperText={touched.email && errors.email}
                            sx={fieldSx}
                        />
                    </Stack>
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={handleAlertClose}
            />
        </>
    );
}
