import { useEffect, useRef, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
    Box,
    TextField,
    Typography,
    Link,
    IconButton,
    InputAdornment,
    LinearProgress,
    useTheme,
    Divider,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import logo from '../../../assets/background-spsg.png';
import { useAuth } from '../hooks/useAuth';
import DialogModal from '../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { Helmet } from 'react-helmet-async';

const TURNSTILE_SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY || '0x4AAAAAADEo_zmnakZDiJdz';
const TURNSTILE_SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

function getPasswordStrength(password) {
    if (!password) return { score: 0, label: '', color: 'transparent' };
    let score = 0;
    if (password.length >= 8)          score++;
    if (password.length >= 12)         score++;
    if (/[A-Z]/.test(password))        score++;
    if (/[0-9]/.test(password))        score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    if (score <= 1) return { score: 20,  label: 'Muy débil',  color: '#ef4444' };
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
        '& .MuiInputBase-input': { color: 'text.primary' },
        '[data-mui-color-scheme="dark"] &': {
            '& .MuiFormHelperText-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiInputLabel-root.Mui-error':     { color: 'hsl(220, 20%, 65%)' },
            '& .MuiFormLabel-asterisk.Mui-error':  { color: 'hsl(220, 20%, 65%)' },
        },
    };

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
        if (!formData.captchaToken) return;
        if (validateForm()) handleRegister(formData);
    };

    return (
        <>
            <Helmet>
                <title>Registro | SPSG</title>
            </Helmet>
            <PageShell />
            <GeneralModal
                open={true}
                onClose={() => {}}
                showCloseButton={false}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={PersonAddIcon}
                title="Sistema Programa Servicios Generales"
                subtitle="Gestión de Servicios Institucionales"
                loading={loading}
                footerLeft={
                    <Typography variant="body2" sx={{ fontSize: 12, color: 'text.secondary' }}>
                        ¿Ya tienes cuenta?
                        <Box component="br" sx={{ display: { sm: 'none' } }} />
                        <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>{' '}</Box>
                        <Link
                            component={RouterLink}
                            to="/login"
                            underline="none"
                            sx={{
                                fontWeight: 700,
                                color: accentColor,
                                '&:hover': { opacity: 0.8 },
                                transition: 'opacity 0.2s',
                            }}
                        >
                            Inicia sesión
                        </Link>
                    </Typography>
                }
                primaryButton={{
                    label: 'Registrarse',
                    onClick: handleSubmit,
                    disabled: loading || !turnstileReady || !formData.captchaToken,
                    loading: loading,
                }}
            >
                <Box component="form" onSubmit={handleSubmit} sx={{ px: 3, pt: 2.5, pb: 1 }}>
                    <button type="submit" style={{ display: 'none' }} tabIndex={-1} />
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
                        helperText={(touched.username && errors.username) || ' '}
                        sx={{ ...fieldSx, mb: 1.25 }}
                    />

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
                        helperText={hasPasswordError ? errors.password : ' '}
                        sx={fieldSx}
                        slotProps={{
                            input: {
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton
                                            onClick={() => setShowPassword((p) => !p)}
                                            tabIndex={-1}
                                            aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                            edge="end"
                                            size="small"
                                            sx={{ color: 'text.secondary', '&:hover': { color: accentColor, bgcolor: 'transparent' } }}
                                        >
                                            {showPassword
                                                ? <VisibilityOffIcon sx={{ fontSize: 18 }} />
                                                : <VisibilityIcon sx={{ fontSize: 18 }} />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            },
                        }}
                    />

                    {formData.password && (
                        <Box sx={{ mt: 0.25, mb: 1.5 }}>
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
                                    bgcolor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)',
                                    '& .MuiLinearProgress-bar': {
                                        bgcolor: strength.color, borderRadius: 2,
                                        transition: 'width 0.35s ease, background-color 0.35s ease',
                                    },
                                }}
                            />

                            <Box sx={{
                                mt: 1.25, p: 1.5,
                                borderRadius: '10px', border: '1.5px solid', borderColor: 'divider',
                                bgcolor: isDark ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)',
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

                    <Divider sx={{ borderColor: 'divider', mb: 1.75, mt: formData.password ? 0 : 0.5 }} />

                    <Typography sx={{
                        fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                        letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.25,
                    }}>
                        Datos personales
                    </Typography>

                    <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 1.25, mb: { xs: 1.25, sm: 1.5 } }}>
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
                            helperText={(touched.firstName && errors.firstName) || ' '}
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
                            helperText={(touched.lastName && errors.lastName) || ' '}
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
                        helperText={(touched.email && errors.email) || ' '}
                        sx={{ ...fieldSx, mb: 1.25 }}
                    />

                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 1 }}>
                        <Box ref={turnstileContainerRef} />
                        {turnstileError && (
                            <Typography variant="caption" sx={{ color: 'error.main', mt: 0.5, textAlign: 'center' }}>
                                {turnstileError}
                            </Typography>
                        )}
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

export default RegisterPage;
