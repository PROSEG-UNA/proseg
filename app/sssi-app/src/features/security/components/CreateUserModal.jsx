import { useState } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
    IconButton,
    Stack,
    Box,
    Divider,
    LinearProgress,
    alpha,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import CloseIcon from '@mui/icons-material/Close';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { createUser } from '../services/usersService';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { useFormValidation } from '../../../common/hooks/useFormValidation';

const RED = {
    50:  '#fff1f2',
    400: '#f87171',
    600: '#dc2626',
    700: '#b91c1c',
    800: '#991b1b',
    900: '#7f1d1d',
};

const INITIAL_FORM = {
    username: '',
    firstName: '',
    lastName: '',
    email: '',
};

export default function CreateUserModal({ open, onClose, onSaved }) {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const isSmall = useMediaQuery(theme.breakpoints.down('md'));
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const { formData, errors, touched, handleChange, handleBlur, validateForm, resetForm } = useFormValidation(
        INITIAL_FORM,
        ['username', 'firstName', 'lastName', 'email']
    );

    const headerGradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;
    const accentColor = isDark ? RED[400] : RED[600];

    const resetAndClose = () => {
        resetForm();
        onClose?.();
    };

    const handleSubmit = async () => {
        if (!validateForm()) {
            setAlert({
                type: 'error',
                message: 'Revisa los campos del formulario antes de continuar.',
            });
            return;
        }

        setSaving(true);
        try {
            await createUser(formData);
            setAlert({
                type: 'success',
                message: 'Usuario creado correctamente. Se envió la contraseña al correo indicado.',
            });
            onSaved?.();
        } catch (err) {
            setAlert({
                type: 'error',
                message: err?.response?.data?.message || err?.message || 'No se pudo crear el usuario.',
            });
        } finally {
            setSaving(false);
        }
    };

    const handleAlertClose = () => {
        if (alert?.type === 'success') {
            resetAndClose();
        }
        setAlert(null);
    };

    return (
        <>
            <Dialog
                open={open}
                onClose={() => !saving && resetAndClose()}
                maxWidth="md"
                fullWidth
                slotProps={{
                    backdrop: { sx: { backdropFilter: 'blur(3px)' } },
                }}
                PaperProps={{
                    sx: {
                        maxHeight: isSmall ? '100vh' : '92vh',
                        height: isSmall ? '100vh' : 'auto',
                        borderRadius: isSmall ? 0 : '16px',
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column',
                        boxShadow: isDark
                            ? `0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(220,38,38,0.3)`
                            : '0 24px 48px rgba(0,0,0,0.14)',
                        bgcolor: 'background.paper',
                    },
                }}
            >
                <Box sx={{ flexShrink: 0, background: headerGradient, px: { xs: 2.5, sm: 3 }, py: 2.25, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Box sx={{ width: 38, height: 38, borderRadius: '10px', background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.22)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <PersonAddIcon sx={{ color: '#fff', fontSize: 20 }} />
                        </Box>
                        <Box>
                            <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: { xs: 15, sm: 15.5 }, lineHeight: 1.2, letterSpacing: '-0.01em' }}>
                                Crear usuario
                            </Typography>
                            <Typography sx={{ color: 'rgba(255,255,255,0.65)', fontSize: { xs: 11, sm: 11.5 } }}>
                                Completa los datos y el sistema enviará la contraseña por correo
                            </Typography>
                        </Box>
                    </Box>
                    <IconButton onClick={resetAndClose} size="small" sx={{ color: 'rgba(255,255,255,0.75)', border: '1px solid rgba(255,255,255,0.2)', p: 0.625, '&:hover': { bgcolor: 'rgba(255,255,255,0.15)', color: '#fff' } }} disabled={saving}>
                        <CloseIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                </Box>

                {saving && (
                    <LinearProgress sx={{ flexShrink: 0, height: 2, bgcolor: alpha(accentColor, 0.15), '& .MuiLinearProgress-bar': { bgcolor: accentColor } }} />
                )}

                <DialogContent sx={{ p: 0, flex: 1, minHeight: 0, overflowY: 'auto', display: 'flex', flexDirection: 'column', bgcolor: 'background.paper' }}>
                    <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2, flexShrink: 0 }}>
                        <Typography sx={{ fontSize: 10.5, fontWeight: 800, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.5 }}>
                            Información básica
                        </Typography>
                        <Stack spacing={1.5}>
                        <ValidatedTextField
                            fieldName="username"
                            fullWidth
                            required
                            label="Nombre de usuario"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.username && !!errors.username}
                            helperText={touched.username && errors.username}
                            sx={{
                                '& .MuiOutlinedInput-root': {
                                    borderRadius: '10px',
                                    '& fieldset': { borderColor: 'divider' },
                                    '&:hover fieldset': { borderColor: alpha(accentColor, 0.5) },
                                    '&.Mui-focused fieldset': { borderColor: accentColor },
                                },
                                '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
                            }}
                        />
                        <ValidatedTextField
                            fieldName="firstName"
                            fullWidth
                            required
                            label="Nombre"
                            name="firstName"
                            value={formData.firstName}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.firstName && !!errors.firstName}
                            helperText={touched.firstName && errors.firstName}
                            sx={{
                                '& .MuiOutlinedInput-root': {
                                    borderRadius: '10px',
                                    '& fieldset': { borderColor: 'divider' },
                                    '&:hover fieldset': { borderColor: alpha(accentColor, 0.5) },
                                    '&.Mui-focused fieldset': { borderColor: accentColor },
                                },
                                '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
                            }}
                        />
                        <ValidatedTextField
                            fieldName="lastName"
                            fullWidth
                            required
                            label="Apellido"
                            name="lastName"
                            value={formData.lastName}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            disabled={saving}
                            size="small"
                            error={touched.lastName && !!errors.lastName}
                            helperText={touched.lastName && errors.lastName}
                            sx={{
                                '& .MuiOutlinedInput-root': {
                                    borderRadius: '10px',
                                    '& fieldset': { borderColor: 'divider' },
                                    '&:hover fieldset': { borderColor: alpha(accentColor, 0.5) },
                                    '&.Mui-focused fieldset': { borderColor: accentColor },
                                },
                                '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
                            }}
                        />
                        <ValidatedTextField
                            fieldName="email"
                            fullWidth
                            required
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
                            sx={{
                                '& .MuiOutlinedInput-root': {
                                    borderRadius: '10px',
                                    '& fieldset': { borderColor: 'divider' },
                                    '&:hover fieldset': { borderColor: alpha(accentColor, 0.5) },
                                    '&.Mui-focused fieldset': { borderColor: accentColor },
                                },
                                '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
                            }}
                        />
                        </Stack>
                    </Box>
                </DialogContent>

                <Divider sx={{ flexShrink: 0 }} />

                <Box
                    sx={{
                        flexShrink: 0,
                        px: { xs: 2.5, sm: 3 },
                        py: 1.75,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        bgcolor: isDark ? alpha('#000', 0.25) : alpha(RED[50], 0.7),
                    }}
                >
                    <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                        Se enviará una contraseña temporal por email
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                        <Button
                            onClick={resetAndClose}
                            variant="outlined"
                            size="small"
                            disabled={saving}
                            sx={{ borderColor: 'divider', color: 'text.secondary', textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px', '&:hover': { borderColor: accentColor, color: accentColor, bgcolor: alpha(accentColor, 0.05) } }}
                        >
                            Cancelar
                        </Button>
                        <Button
                            onClick={handleSubmit}
                            disabled={saving}
                            variant="contained"
                            size="small"
                            sx={{
                                background: headerGradient,
                                textTransform: 'none',
                                fontWeight: 700,
                                fontSize: 12.5,
                                borderRadius: '8px',
                                boxShadow: `0 4px 14px ${alpha(RED[600], isDark ? 0.4 : 0.3)}`,
                                px: 2.5,
                                letterSpacing: '0.01em',
                                '&:hover': {
                                    background: isDark
                                        ? `linear-gradient(135deg, ${RED[800]}, ${RED[700]})`
                                        : `linear-gradient(135deg, ${RED[700]}, ${RED[900]})`,
                                    boxShadow: `0 6px 18px ${alpha(RED[600], 0.4)}`,
                                },
                                '&:disabled': { opacity: 0.55 },
                            }}
                        >
                            {saving ? 'Creando…' : 'Crear usuario'}
                        </Button>
                    </Box>
                </Box>
            </Dialog>

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={handleAlertClose}
            />
        </>
    );
}