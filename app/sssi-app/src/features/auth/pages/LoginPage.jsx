import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
    Box,
    Button,
    CircularProgress,
    Typography,
    Link,
    IconButton,
    TextField,
    LinearProgress,
    alpha,
    useTheme,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import logo from '../../../assets/background-spsg.png';
import { useAuth } from '../hooks/useAuth';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import {Helmet} from "react-helmet-async";

const RED = {
    50:  '#fff1f2',
    100: '#ffe4e6',
    200: '#fecdd3',
    400: '#f87171',
    500: '#ef4444',
    600: '#dc2626',
    700: '#b91c1c',
    800: '#991b1b',
    900: '#7f1d1d',
};

// ── Shell ─────────────────────────────────────────────────────────────────────
function PageShell({ children }) {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const overlay = isDark
        ? 'rgba(0,0,0,0.28)'
        : 'rgba(0,0,0,0.18)';

    return (
        <Box sx={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundImage: `linear-gradient(${overlay}, ${overlay}), url(${logo})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundAttachment: 'fixed',
            backgroundRepeat: 'no-repeat',
            position: 'relative',


        }}>
            <Box sx={{
                width: '100%',
                maxWidth: 480,
                bgcolor: 'background.paper',
                borderRadius: '16px',
                overflow: 'hidden',
                border: isDark
                    ? `1px solid ${alpha(RED[700], 0.28)}`
                    : `1px solid ${alpha(RED[200], 0.9)}`,
                backdropFilter: 'blur(10px)',
                boxShadow: isDark
                    ? `0 24px 48px rgba(0,0,0,0.55),0 0 0 1px ${alpha(RED[700], 0.18)},0 0 32px ${alpha(RED[900], 0.18)}`
                    : `0 24px 48px rgba(0,0,0,0.10),0 0 0 1px rgba(255,255,255,0.7)`,
            }}>
                {children}
            </Box>
        </Box>
    );
}

// ── Header ────────────────────────────────────────────────────────────────────
function ModalHeader() {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const gradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;

    return (
        <Box sx={{ background: gradient, px: 3, py: 2.5, display: 'flex', alignItems: 'center', gap: 1.75 }}>
            <Box sx={{
                width: 42, height: 42, borderRadius: '10px',
                background: 'rgba(255,255,255,0.15)',
                border: '1px solid rgba(255,255,255,0.22)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
                <AdminPanelSettingsIcon sx={{ color: '#fff', fontSize: 22 }} />
            </Box>
            <Box>
                <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: 16, lineHeight: 1.2, letterSpacing: '-0.01em' }}>
                    Sistema Programa Servicios Generales
                </Typography>
                <Typography sx={{ color: 'rgba(255,255,255,0.65)', fontSize: 12, mt: 0.25 }}>
                    Gestión de Servicios Institucionales
                </Typography>
            </Box>
        </Box>
    );
}

// ── LoginPage ─────────────────────────────────────────────────────────────────
export function LoginPage() {
    const [showPassword, setShowPassword] = useState(false);
    const { loading, alert, handleAlertClose, handleLogin } = useAuth();
    const { formData, errors, touched, handleChange, handleBlur, validateForm } = useFormValidation(
        { identifier: '', loginPassword: '' },
        ['identifier', 'loginPassword']
    );

    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const accentColor = isDark ? RED[400] : RED[700];
    const gradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: alpha(accentColor, 0.5) },
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
        e.preventDefault();
        if (validateForm()) {
            handleLogin(formData.identifier, formData.loginPassword);
        }
    };

    const hasPasswordError =
        touched.loginPassword &&
        formData.loginPassword.trim() !== '' &&
        !!errors.loginPassword;

    return (
        <PageShell>
            <Helmet>
                <title>Iniciar Sesión | SPSG</title>
            </Helmet>
            <ModalHeader />

            {loading && (
                <LinearProgress sx={{
                    height: 2,
                    bgcolor: alpha(RED[600], 0.15),
                    '& .MuiLinearProgress-bar': { bgcolor: RED[600] },
                }} />
            )}

            <Box component="form" onSubmit={handleSubmit} sx={{ px: 4, pt: 3, pb: 2 }}>
                <Typography sx={{
                    fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                    letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.5,
                }}>
                    Iniciar sesión
                </Typography>

                {/* Usuario o Email */}
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

                {/* Contraseña — TextField + botón ojo con position:absolute para evitar
                    cualquier interferencia de InputProps con ValidatedTextField u otros wrappings */}
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
                            /* Reservar espacio a la derecha para el ícono */
                            '& .MuiOutlinedInput-input': { paddingRight: '40px' },
                        }}
                    />
                    {/* Botón ojo absolutamente posicionado — 100% visible sin importar
                        el comportamiento interno de TextField */}
                    <IconButton
                        onClick={() => setShowPassword((p) => !p)}
                        tabIndex={-1}
                        aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                        size="small"
                        sx={{
                            position: 'absolute',
                            right: 8,
                            /* Sube un poco si hay helper text para quedar centrado en el input */
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

                {/* Olvidaste tu contraseña */}
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2.5 }}>
                    <Link
                        component={RouterLink}
                        to="/forgot-password"
                        underline="none"
                        sx={{
                            fontSize: 12, fontWeight: 600, color: accentColor,
                            '&:hover': { color: isDark ? RED[300] : RED[800] },
                            transition: 'color 0.2s',
                        }}
                    >
                        ¿Olvidaste tu contraseña?
                    </Link>
                </Box>
            </Box>

            {/* Footer */}
            <Box sx={{
                px: 3, py: 1.75,
                borderTop: '1px solid', borderColor: 'divider',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                bgcolor: isDark ? alpha('#000', 0.25) : alpha(RED[50], 0.7),
            }}>
                <Typography variant="body2" sx={{ fontSize: 12, color: 'text.secondary' }}>
                    ¿No estás registrado?{' '}
                    <Link
                        component={RouterLink}
                        to="/registro"
                        underline="none"
                        sx={{
                            fontWeight: 700, color: accentColor,
                            '&:hover': { color: isDark ? RED[300] : RED[800] },
                            transition: 'color 0.2s',
                        }}
                    >
                        Regístrate
                    </Link>
                </Typography>

                <Button
                    onClick={handleSubmit}
                    disabled={loading}
                    variant="contained"
                    size="small"
                    sx={{
                        background: gradient,
                        textTransform: 'none', fontWeight: 700, fontSize: 12.5,
                        borderRadius: '8px',
                        boxShadow: `0 4px 14px ${alpha(RED[600], isDark ? 0.4 : 0.3)}`,
                        px: 2.5, letterSpacing: '0.01em',
                        '&:hover': {
                            background: isDark
                                ? `linear-gradient(135deg, ${RED[800]}, ${RED[700]})`
                                : `linear-gradient(135deg, ${RED[700]}, ${RED[900]})`,
                            boxShadow: `0 6px 18px ${alpha(RED[600], 0.4)}`,
                        },
                        '&:disabled': { opacity: 0.5, color: '#fff' },
                    }}
                >
                    {loading
                        ? <CircularProgress size={16} sx={{ color: '#fff' }} />
                        : 'Iniciar sesión'}
                </Button>
            </Box>

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={handleAlertClose}
            />
        </PageShell>
    );
}

export default LoginPage;