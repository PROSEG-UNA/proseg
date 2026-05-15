import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
    Box,
    TextField,
    Typography,
    Link,
    IconButton,
    useTheme,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import logo from '../../../assets/background-spsg.png';
import { useAuth } from '../hooks/useAuth';
import DialogModal from '../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import { Helmet } from 'react-helmet-async';

function PageShell() {
    const theme = useTheme();
    const overlay = theme.palette.mode === 'dark' ? 'rgba(0,0,0,0.28)' : 'rgba(0,0,0,0.18)';
    return (
        <Box sx={{
            position: 'fixed',
            inset: 0,
            backgroundImage: `linear-gradient(${overlay}, ${overlay}), url(${logo})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundRepeat: 'no-repeat',
            zIndex: -1,
        }} />
    );
}

export function LoginPage() {
    const [showPassword, setShowPassword] = useState(false);
    const { loading, alert, handleAlertClose, handleLogin } = useAuth();
    const { formData, errors, touched, handleChange, handleBlur, validateForm } = useFormValidation(
        { identifier: '', loginPassword: '' },
        ['identifier', 'loginPassword']
    );

    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
            '& input:-webkit-autofill': {
                WebkitBoxShadow: '0 0 0 1000px transparent inset',
                WebkitTextFillColor: 'inherit',
                transition: 'background-color 9999s ease-out 0s',
            },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
    };

    const handleSubmit = (e) => {
        e?.preventDefault();
        if (validateForm()) {
            handleLogin(formData.identifier, formData.loginPassword);
        }
    };

    const hasPasswordError =
        touched.loginPassword &&
        formData.loginPassword.trim() !== '' &&
        !!errors.loginPassword;

    return (
        <>
            <Helmet>
                <title>Iniciar Sesión | SPSG</title>
            </Helmet>
            <PageShell />
            <GeneralModal
                open={true}
                onClose={() => {}}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={AdminPanelSettingsIcon}
                title="Sistema Programa Servicios Generales"
                subtitle="Gestión de Servicios Institucionales"
                loading={loading}
                footerLeft={
                    <Typography variant="body2" sx={{ fontSize: 12, color: 'text.secondary' }}>
                        ¿No estás registrado?{' '}
                        <Link
                            component={RouterLink}
                            to="/registro"
                            underline="none"
                            sx={{
                                fontWeight: 700,
                                color: accentColor,
                                '&:hover': { opacity: 0.8 },
                                transition: 'opacity 0.2s',
                            }}
                        >
                            Regístrate
                        </Link>
                    </Typography>
                }
                primaryButton={{
                    label: 'Iniciar sesión',
                    onClick: handleSubmit,
                    disabled: loading,
                    loading: loading,
                }}
            >
                <Box component="form" onSubmit={handleSubmit} sx={{ px: 4, pt: 3, pb: 2 }}>
                    <Typography sx={{
                        fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                        letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.5,
                    }}>
                        Iniciar sesión
                    </Typography>

                    <ValidatedTextField
                        fieldName="identifier"
                        fullWidth
                        label="Usuario o Email"
                        name="identifier"
                        type="text"
                        value={formData.identifier}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        size="small"
                        variant="outlined"
                        required
                        disabled={loading}
                        error={touched.identifier && !!errors.identifier}
                        helperText={touched.identifier && errors.identifier}
                        sx={{ ...fieldSx, mb: 1.5 }}
                    />

                    <Box sx={{ position: 'relative', mb: 1 }}>
                        <TextField
                            fullWidth
                            label="Contraseña"
                            name="loginPassword"
                            type={showPassword ? 'text' : 'password'}
                            value={formData.loginPassword}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            size="small"
                            variant="outlined"
                            required
                            disabled={loading}
                            error={hasPasswordError}
                            helperText={hasPasswordError ? errors.loginPassword : ''}
                            sx={{
                                ...fieldSx,
                                '& .MuiOutlinedInput-input': { paddingRight: '40px' },
                            }}
                        />
                        <IconButton
                            onClick={() => setShowPassword((p) => !p)}
                            tabIndex={-1}
                            aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                            size="small"
                            sx={{
                                position: 'absolute',
                                right: 8,
                                top: hasPasswordError ? 'calc(50% - 10px)' : '50%',
                                transform: 'translateY(-50%)',
                                color: 'text.secondary',
                                p: 0.5,
                                '&:hover': { color: accentColor, bgcolor: 'transparent' },
                            }}
                        >
                            {showPassword
                                ? <VisibilityOffIcon sx={{ fontSize: 18 }} />
                                : <VisibilityIcon sx={{ fontSize: 18 }} />}
                        </IconButton>
                    </Box>

                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2.5 }}>
                        <Link
                            component={RouterLink}
                            to="/forgot-password"
                            underline="none"
                            sx={{
                                fontSize: 12, fontWeight: 600, color: accentColor,
                                '&:hover': { opacity: 0.8 },
                                transition: 'opacity 0.2s',
                            }}
                        >
                            ¿Olvidaste tu contraseña?
                        </Link>
                    </Box>
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

export default LoginPage;
