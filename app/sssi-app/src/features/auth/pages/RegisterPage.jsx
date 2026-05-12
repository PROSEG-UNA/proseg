import { useEffect, useRef, useState } from 'react';
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
    Divider,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import logo from '../../../assets/background-spsg.png';
import { useAuth } from '../hooks/useAuth';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { Helmet } from 'react-helmet-async';

const TURNSTILE_SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY || '0x4AAAAAADEo_zmnakZDiJdz';
const TURNSTILE_SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

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

// ── Helpers ────────────────────────────────────────────────────────────────────
function getPasswordStrength(password) {
    if (!password) return { score: 0, label: '', color: 'transparent' };
    let score = 0;
    if (password.length >= 8)          score++;
    if (password.length >= 12)         score++;
    if (/[A-Z]/.test(password))        score++;
    if (/[0-9]/.test(password))        score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    if (score <= 1) return { score: 20,  label: 'Muy débil',  color: RED[500]  };
    if (score === 2) return { score: 40, label: 'Débil',      color: '#f59e0b' };
    if (score === 3) return { score: 60, label: 'Regular',    color: '#d97706' };
    if (score === 4) return { score: 80, label: 'Fuerte',     color: '#059669' };
    return                 { score: 100, label: 'Muy fuerte', color: '#047857' };
}

function Req({ label, met }) {
    return (
        <Typography variant="caption" sx={{
            display: 'flex', alignItems: 'center', gap: 0.75,
            color: met ? '#059669' : 'text.secondary',
            fontWeight: met ? 600 : 400, lineHeight: 1.9,
            transition: 'color 0.2s',
        }}>
            <Box component="span" sx={{
                width: 6, height: 6, borderRadius: '50%',
                bgcolor: met ? '#059669' : 'text.disabled',
                flexShrink: 0, transition: 'background 0.2s',
            }} />
            {label}
        </Typography>
    );
}

// ── Shell ─────────────────────────────────────────────────────────────────────
function PageShell({ children }) {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const overlay = isDark ? 'rgba(0,0,0,0.28)' : 'rgba(0,0,0,0.18)';

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
                <PersonAddIcon sx={{ color: '#fff', fontSize: 22 }} />
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

// ── RegisterPage ──────────────────────────────────────────────────────────────
export function RegisterPage() {
    const [showPassword, setShowPassword] = useState(false);
    const [turnstileReady, setTurnstileReady] = useState(false);
    const [turnstileError, setTurnstileError] = useState('');
    const turnstileContainerRef = useRef(null);
    const turnstileWidgetIdRef = useRef(null);
    const { loading, alert, handleAlertClose, handleRegister } = useAuth();

    const { formData, errors, touched, handleChange, handleBlur, validateForm, setFormValue } = useFormValidation(
        { username: '', email: '', password: '', firstName: '', lastName: '', captchaToken: '' },
        ['username', 'email', 'password', 'firstName', 'lastName']
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

    // Derivados de fortaleza
    const strength = getPasswordStrength(formData.password);

    const hasPasswordError =
        touched.password &&
        formData.password.trim() !== '' &&
        !!errors.password;

    useEffect(() => {
        const renderTurnstile = () => {
            if (!window.turnstile || !turnstileContainerRef.current || turnstileWidgetIdRef.current !== null) return;
            turnstileWidgetIdRef.current = window.turnstile.render(turnstileContainerRef.current, {
                sitekey: TURNSTILE_SITE_KEY,
                callback: (token) => { setFormValue('captchaToken', token); },
                'expired-callback': () => { setFormValue('captchaToken', ''); },
                'error-callback': () => {
                    setFormValue('captchaToken', '');
                    setTurnstileError('No se pudo cargar el captcha. Intenta nuevamente.');
                },
            });
            setTurnstileReady(true);
            setTurnstileError('');
        };

        const existingScript = document.querySelector(`script[src="${TURNSTILE_SCRIPT_SRC}"]`);
        if (!existingScript) {
            const script = document.createElement('script');
            script.src = TURNSTILE_SCRIPT_SRC;
            script.async = true;
            script.defer = true;
            script.onload = renderTurnstile;
            script.onerror = () => setTurnstileError('No se pudo cargar el captcha de Turnstile.');
            document.head.appendChild(script);
        } else if (window.turnstile) {
            renderTurnstile();
        } else {
            existingScript.addEventListener('load', renderTurnstile, { once: true });
            existingScript.addEventListener('error', () => setTurnstileError('No se pudo cargar el captcha de Turnstile.'), { once: true });
        }

        return () => {
            if (window.turnstile && turnstileWidgetIdRef.current !== null) {
                window.turnstile.remove(turnstileWidgetIdRef.current);
                turnstileWidgetIdRef.current = null;
            }
        };
    }, []);

    useEffect(() => {
        if (loading || !alert || alert.type !== 'error') return;
        if (window.turnstile && turnstileWidgetIdRef.current !== null) {
            window.turnstile.reset(turnstileWidgetIdRef.current);
        }
        setFormValue('captchaToken', '');
        setTurnstileError('Debes completar nuevamente el captcha para reintentar.');
    }, [alert, loading, setFormValue]);

    const handleSubmit = (e) => {
        e?.preventDefault();
        if (validateForm()) handleRegister(formData);
    };

    return (
        <PageShell>
            <Helmet>
                <title>Registro | SPSG</title>
            </Helmet>
            <ModalHeader />

            {loading && (
                <LinearProgress sx={{
                    height: 2,
                    bgcolor: alpha(RED[600], 0.15),
                    '& .MuiLinearProgress-bar': { bgcolor: RED[600] },
                }} />
            )}

            <Box component="form" onSubmit={handleSubmit} sx={{ px: 3, pt: 2.5, pb: 1 }}>

                {/* ── Sección: Datos de acceso ── */}
                <Typography sx={{
                    fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                    letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.25,
                }}>
                    Datos de acceso
                </Typography>

                <ValidatedTextField
                    fieldName="username"
                    fullWidth
                    label="Nombre de usuario"
                    name="username"
                    type="text"
                    value={formData.username}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    size="small"
                    variant="outlined"
                    required
                    disabled={loading}
                    error={touched.username && !!errors.username}
                    helperText={touched.username && errors.username}
                    sx={{ ...fieldSx, mb: 1.25 }}
                />

                {/* Campo contraseña con ojo absoluto */}
                <Box sx={{ position: 'relative', mb: 0 }}>
                    <TextField
                        fullWidth
                        label="Contraseña"
                        name="password"
                        type={showPassword ? 'text' : 'password'}
                        value={formData.password}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        size="small"
                        variant="outlined"
                        required
                        disabled={loading}
                        error={hasPasswordError}
                        helperText={hasPasswordError ? errors.password : ''}
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

                {formData.password && (
                    <Box sx={{ mt: 1.25, mb: 1.5 }}>
                        {/* Barra */}
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography sx={{
                                fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                                letterSpacing: '0.06em', textTransform: 'uppercase',
                            }}>
                                Fortaleza
                            </Typography>
                            <Typography sx={{ fontSize: 11, fontWeight: 700, color: strength.color }}>
                                {strength.label}
                            </Typography>
                        </Box>
                        <LinearProgress
                            variant="determinate"
                            value={strength.score}
                            sx={{
                                height: 4, borderRadius: 2,
                                bgcolor: isDark ? alpha('#fff', 0.08) : alpha('#000', 0.06),
                                '& .MuiLinearProgress-bar': {
                                    bgcolor: strength.color, borderRadius: 2,
                                    transition: 'width 0.35s ease, background-color 0.35s ease',
                                },
                            }}
                        />

                        {/* Requisitos */}
                        <Box sx={{
                            mt: 1.25, p: 1.5,
                            borderRadius: '10px', border: '1.5px solid', borderColor: 'divider',
                            bgcolor: isDark ? alpha('#fff', 0.02) : alpha('#000', 0.02),
                        }}>
                            <Typography sx={{
                                fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                                letterSpacing: '0.08em', textTransform: 'uppercase', mb: 0.5,
                            }}>
                                Requisitos
                            </Typography>
                            <Req label="Al menos 8 caracteres"            met={formData.password.length >= 8} />
                            <Req label="Una letra mayúscula"              met={/[A-Z]/.test(formData.password)} />
                            <Req label="Un número"                        met={/[0-9]/.test(formData.password)} />
                            <Req label="Un carácter especial (!@#$%...)"  met={/[^A-Za-z0-9]/.test(formData.password)} />
                        </Box>
                    </Box>
                )}

                <Divider sx={{ borderColor: isDark ? alpha(RED[700], 0.2) : RED[100], mb: 1.75, mt: formData.password ? 0 : 1.5 }} />

                {/* ── Sección: Datos personales ── */}
                <Typography sx={{
                    fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                    letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.25,
                }}>
                    Datos personales
                </Typography>

                {/* Nombre y Apellido en fila */}
                <Box sx={{ display: 'flex', gap: 1.25, mb: 1.25 }}>
                    <ValidatedTextField
                        fieldName="firstName"
                        fullWidth
                        label="Nombre"
                        name="firstName"
                        type="text"
                        value={formData.firstName}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        size="small"
                        variant="outlined"
                        required
                        disabled={loading}
                        error={touched.firstName && !!errors.firstName}
                        helperText={touched.firstName && errors.firstName}
                        sx={fieldSx}
                    />
                    <ValidatedTextField
                        fieldName="lastName"
                        fullWidth
                        label="Apellido"
                        name="lastName"
                        type="text"
                        value={formData.lastName}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        size="small"
                        variant="outlined"
                        required
                        disabled={loading}
                        error={touched.lastName && !!errors.lastName}
                        helperText={touched.lastName && errors.lastName}
                        sx={fieldSx}
                    />
                </Box>

                <ValidatedTextField
                    fieldName="email"
                    fullWidth
                    label="Email"
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    size="small"
                    variant="outlined"
                    required
                    disabled={loading}
                    error={touched.email && !!errors.email}
                    helperText={touched.email && errors.email}
                    sx={{ ...fieldSx, mb: 1.5 }}
                />

                {/* Turnstile captcha */}
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 1 }}>
                    <Box ref={turnstileContainerRef} />
                    {turnstileError && (
                        <Typography variant="caption" sx={{ color: RED[600], mt: 0.5, textAlign: 'center' }}>
                            {turnstileError}
                        </Typography>
                    )}
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
                    ¿Ya tienes cuenta?{' '}
                    <Link
                        component={RouterLink}
                        to="/login"
                        underline="none"
                        sx={{
                            fontWeight: 700, color: accentColor,
                            '&:hover': { color: isDark ? RED[300] : RED[800] },
                            transition: 'color 0.2s',
                        }}
                    >
                        Inicia sesión
                    </Link>
                </Typography>

                <Button
                    onClick={handleSubmit}
                    disabled={loading || !turnstileReady || !formData.captchaToken}
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
                        : 'Registrarse'}
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

export default RegisterPage;