import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
    Box, Typography, TextField, Alert, InputAdornment, IconButton,
    LinearProgress, alpha, useTheme,
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import LockResetIcon from '@mui/icons-material/LockReset';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import { forgotPassword, resetPassword } from '../services/authService.js';
import { Helmet } from 'react-helmet-async';
import logo from '../../../assets/background-spsg.png';
import GeneralModal from '../../../common/components/GeneralModal.jsx';

const RED = {
    50: '#fff1f2',
    100: '#ffe4e6',
    200: '#fecdd3',
    400: '#f87171',
    500: '#ef4444',
    600: '#dc2626',
    700: '#b91c1c',
    800: '#991b1b',
    900: '#7f1d1d',
};

function getPasswordStrength(password) {
    if (!password) return { score: 0, label: '', color: 'transparent' };
    let score = 0;
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    if (score <= 1) return { score: 20, label: 'Muy débil', color: RED[500] };
    if (score === 2) return { score: 40, label: 'Débil', color: '#f59e0b' };
    if (score === 3) return { score: 60, label: 'Regular', color: '#d97706' };
    if (score === 4) return { score: 80, label: 'Fuerte', color: '#059669' };
    return { score: 100, label: 'Muy fuerte', color: '#047857' };
}

function LoginBackground() {
    const theme = useTheme();
    const overlay = theme.palette.mode === 'dark' ? 'rgba(0,0,0,0.28)' : 'rgba(0,0,0,0.18)';
    return (
        <Box sx={{
            position: 'fixed', inset: 0,
            backgroundImage: `linear-gradient(${overlay}, ${overlay}), url(${logo})`,
            backgroundSize: 'cover', backgroundPosition: 'center', backgroundRepeat: 'no-repeat',
            zIndex: -1,
        }} />
    );
}

function PatternBackground() {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const strokeColor = isDark ? 'rgba(185,28,28,0.16)' : 'rgba(220,38,38,0.10)';
    const dotColor = isDark ? 'rgba(248,113,113,0.10)' : 'rgba(185,28,28,0.08)';
    const svgPattern = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60'%3E%3Cdefs%3E%3Cpattern id='g' width='60' height='60' patternUnits='userSpaceOnUse'%3E%3Cpath d='M 60 0 L 0 0 0 60' fill='none' stroke='${encodeURIComponent(strokeColor)}' stroke-width='1'/%3E%3C/pattern%3E%3Cpattern id='d' width='60' height='60' patternUnits='userSpaceOnUse'%3E%3Ccircle cx='30' cy='30' r='1.5' fill='${encodeURIComponent(dotColor)}'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='60' height='60' fill='url(%23g)'/%3E%3Crect width='60' height='60' fill='url(%23d)'/%3E%3C/svg%3E")`;
    return (
        <Box sx={{
            position: 'fixed', inset: 0,
            bgcolor: isDark ? '#0b0b0c' : '#faf7f7',
            backgroundImage: svgPattern,
            backgroundRepeat: 'repeat',
            backgroundPosition: 'center',
            zIndex: -1,
        }} />
    );
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

function ForgotPasswordStep({ onSent }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const navigate = useNavigate();

    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputBase-input': { color: 'text.primary' },
        '[data-mui-color-scheme="dark"] &': {
            '& .MuiFormHelperText-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiInputLabel-root.Mui-error':     { color: 'hsl(220, 20%, 65%)' },
            '& .MuiFormLabel-asterisk.Mui-error':  { color: 'hsl(220, 20%, 65%)' },
        },
    };

    const handleSubmit = async () => {
        setError('');
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            setError('Ingresá un correo electrónico válido.');
            return;
        }
        setLoading(true);
        try {
            await forgotPassword(email);
            onSent(email);
        } catch {
            onSent(email);
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <Helmet><title>Olvidé mi contraseña | SPGS</title></Helmet>
            <LoginBackground />
            <GeneralModal
                open={true}
                onClose={() => {}}
                showCloseButton={false}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={EmailOutlinedIcon}
                title="Olvidé mi contraseña"
                subtitle="Te enviaremos un enlace de restablecimiento"
                loading={loading}
                secondaryButton={{ label: 'Volver al login', onClick: () => navigate('/login') }}
                primaryButton={{ label: 'Enviar enlace', onClick: handleSubmit, disabled: loading, loading }}
            >
                <Box sx={{ px: 3, pt: 3, pb: 1 }}>
                    <Typography sx={{ fontSize: 14, color: 'text.secondary', lineHeight: 1.6, mb: 2.5 }}>
                        Ingresá el correo asociado a tu cuenta y te enviaremos
                        las instrucciones para restablecer tu contraseña.
                    </Typography>

                    {error && (
                        <Alert severity="error" sx={{ mb: 2, borderRadius: '10px', fontSize: 13 }}>
                            {error}
                        </Alert>
                    )}

                    <Typography sx={{
                        fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                        letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.25,
                    }}>
                        Correo electrónico
                    </Typography>

                    <TextField
                        label="Tu correo registrado"
                        type="email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleSubmit()}
                        fullWidth size="small"
                        sx={{ ...fieldSx, mb: 2.5 }}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <EmailOutlinedIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
                                </InputAdornment>
                            ),
                        }}
                    />
                </Box>
            </GeneralModal>
        </>
    );
}

function EmailSentView({ email }) {
    const navigate = useNavigate();
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';

    return (
        <>
            <Helmet><title>Revisá tu correo | SPGS</title></Helmet>
            <LoginBackground />
            <GeneralModal
                open={true}
                onClose={() => {}}
                showCloseButton={false}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={EmailOutlinedIcon}
                title="Revisá tu correo"
                subtitle="El enlace puede tardar unos minutos"
                primaryButton={{ label: 'Volver al inicio de sesión', onClick: () => navigate('/login') }}
            >
                <Box sx={{ p: 3.5, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                    <Box sx={{
                        width: 64, height: 64, borderRadius: '50%',
                        bgcolor: alpha('#059669', 0.1), border: `1.5px solid ${alpha('#059669', 0.2)}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                        <EmailOutlinedIcon sx={{ fontSize: 30, color: '#059669' }} />
                    </Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: 'text.primary', textAlign: 'center' }}>
                        ¡Listo! Revisá tu bandeja
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', textAlign: 'center', lineHeight: 1.7 }}>
                        Si <strong>{email}</strong> está registrado en SPGS, recibirás un
                        correo con el enlace para restablecer tu contraseña.
                        El enlace es válido por <strong>1 hora</strong>.
                    </Typography>
                    <Box sx={{
                        width: '100%', p: 1.5, borderRadius: '10px',
                        border: '1.5px solid', borderColor: isDark ? alpha(RED[700], 0.3) : RED[100],
                        bgcolor: isDark ? alpha(RED[900], 0.2) : RED[50],
                    }}>
                        <Typography sx={{ fontSize: 12, color: isDark ? RED[300] : RED[700], textAlign: 'center', lineHeight: 1.5 }}>
                            Si no lo ves, revisá tu carpeta de spam o correo no deseado.
                        </Typography>
                    </Box>
                </Box>
            </GeneralModal>
        </>
    );
}

function InvalidTokenView() {
    const navigate = useNavigate();

    return (
        <>
            <PatternBackground />
            <GeneralModal
                open={true}
                onClose={() => {}}
                showCloseButton={false}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={ErrorIcon}
                title="Enlace inválido"
                subtitle="Este link ya no está disponible"
                primaryButton={{ label: 'Solicitar nuevo enlace', onClick: () => navigate('/forgot-password') }}
            >
                <Box sx={{ p: 3.5, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                    <Box sx={{
                        width: 64, height: 64, borderRadius: '50%',
                        bgcolor: alpha(RED[600], 0.1), border: `1.5px solid ${alpha(RED[600], 0.2)}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                        <ErrorIcon sx={{ fontSize: 32, color: RED[600] }} />
                    </Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: 'text.primary', textAlign: 'center' }}>
                        Link expirado o utilizado
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', textAlign: 'center', lineHeight: 1.6 }}>
                        Este enlace de restablecimiento ya no es válido. Puede que haya vencido
                        o ya fue utilizado. Solicitá uno nuevo desde la pantalla de login.
                    </Typography>
                </Box>
            </GeneralModal>
        </>
    );
}

function SuccessView() {
    const navigate = useNavigate();

    return (
        <>
            <PatternBackground />
            <GeneralModal
                open={true}
                onClose={() => {}}
                showCloseButton={false}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={CheckCircleIcon}
                title="¡Contraseña actualizada!"
                subtitle="Ya podés iniciar sesión en SPGS"
                primaryButton={{ label: 'Iniciar sesión', onClick: () => navigate('/login') }}
            >
                <Box sx={{ p: 3.5, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                    <Box sx={{
                        width: 64, height: 64, borderRadius: '50%',
                        bgcolor: alpha('#059669', 0.1), border: `1.5px solid ${alpha('#059669', 0.2)}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                        <CheckCircleIcon sx={{ fontSize: 32, color: '#059669' }} />
                    </Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: 'text.primary', textAlign: 'center' }}>
                        Contraseña restablecida correctamente
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', textAlign: 'center', lineHeight: 1.6 }}>
                        Tu contraseña fue actualizada. Podés iniciar sesión con tus nuevas credenciales.
                    </Typography>
                </Box>
            </GeneralModal>
        </>
    );
}

function ResetPasswordForm({ token }) {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [showPwd, setShowPwd] = useState(false);
    const [showCfm, setShowCfm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [tokenInvalid, setTokenInvalid] = useState(false);
    const [success, setSuccess] = useState(false);

    const strength = getPasswordStrength(password);
    const passwordsMatch = confirm && password === confirm;
    const mismatch = confirm && password !== confirm;

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputBase-input': { color: 'text.primary' },
        '[data-mui-color-scheme="dark"] &': {
            '& .MuiFormHelperText-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiInputLabel-root.Mui-error':     { color: 'hsl(220, 20%, 65%)' },
            '& .MuiFormLabel-asterisk.Mui-error':  { color: 'hsl(220, 20%, 65%)' },
        },
    };

    const handleSubmit = async () => {
        setError('');
        if (password.length < 8) {
            setError('La contraseña debe tener al menos 8 caracteres.');
            return;
        }
        if (password !== confirm) {
            setError('Las contraseñas no coinciden.');
            return;
        }
        setLoading(true);
        try {
            await resetPassword(token, password);
            setSuccess(true);
        } catch (err) {
            const status = err?.response?.status;
            if (status === 400 || status === 404) {
                setTokenInvalid(true);
                return;
            }
            const msg = err?.response?.data?.message;
            setError(msg ?? 'Ocurrió un error. Intentá de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    if (tokenInvalid) return <InvalidTokenView />;
    if (success) return <SuccessView />;

    return (
        <>
            <PatternBackground />
            <GeneralModal
                open={true}
                onClose={() => {}}
                showCloseButton={false}
                maxWidth="xs"
                fullScreenAt="xs"
                icon={LockResetIcon}
                title="Restablecé tu contraseña"
                subtitle="Ingresá tu nueva contraseña"
                loading={loading}
                footerLeft={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                        <LockOutlinedIcon sx={{ fontSize: 13, color: 'text.disabled' }} />
                        <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                            {password.length === 0 ? 'Ingresá tu contraseña' : strength.label}
                        </Typography>
                    </Box>
                }
                primaryButton={{ label: 'Restablecer contraseña', onClick: handleSubmit, disabled: loading, loading }}
            >
                <Box sx={{ px: 3, pt: 2.5, pb: 1 }}>
                    <Typography sx={{
                        fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                        letterSpacing: '0.08em', textTransform: 'uppercase', mb: 1.5,
                    }}>
                        Nueva contraseña
                    </Typography>

                    {error && (
                        <Alert severity="error" sx={{ mb: 2, borderRadius: '10px', fontSize: 13 }}>
                            {error}
                        </Alert>
                    )}

                    <TextField
                        label="Nueva contraseña"
                        type={showPwd ? 'text' : 'password'}
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        fullWidth size="small"
                        helperText=" "
                        sx={{ ...fieldSx, mb: 0.5 }}
                        slotProps={{
                            input: {
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton
                                            onClick={() => setShowPwd(p => !p)}
                                            tabIndex={-1}
                                            aria-label={showPwd ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                            edge="end" size="small"
                                            sx={{ color: 'text.secondary', p: 0.5, '&:hover': { color: accentColor, bgcolor: 'transparent' } }}
                                        >
                                            {showPwd ? <VisibilityOff sx={{ fontSize: 18 }} /> : <Visibility sx={{ fontSize: 18 }} />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            },
                        }}
                    />

                    {password && (
                        <Box sx={{ mb: 1.5 }}>
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
                        </Box>
                    )}

                    <TextField
                        label="Confirmar contraseña"
                        type={showCfm ? 'text' : 'password'}
                        value={confirm}
                        onChange={e => setConfirm(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleSubmit()}
                        fullWidth size="small"
                        sx={{ ...fieldSx, mb: 1 }}
                        error={mismatch}
                        helperText={
                            mismatch ? 'Las contraseñas no coinciden' :
                            passwordsMatch ? '✓ Las contraseñas coinciden' : ' '
                        }
                        slotProps={{
                            formHelperText: { sx: { color: passwordsMatch ? '#059669' : undefined, fontWeight: 600 } },
                            input: {
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton
                                            onClick={() => setShowCfm(p => !p)}
                                            tabIndex={-1}
                                            aria-label={showCfm ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                            edge="end" size="small"
                                            sx={{ color: 'text.secondary', p: 0.5, '&:hover': { color: accentColor, bgcolor: 'transparent' } }}
                                        >
                                            {showCfm ? <VisibilityOff sx={{ fontSize: 18 }} /> : <Visibility sx={{ fontSize: 18 }} />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            },
                        }}
                    />

                    <Box sx={{
                        p: 1.75, mb: 2.5,
                        borderRadius: '10px', border: '1.5px solid', borderColor: 'divider',
                        bgcolor: isDark ? alpha('#fff', 0.02) : alpha('#000', 0.02),
                    }}>
                        <Typography sx={{
                            fontSize: 10.5, fontWeight: 800, color: 'text.disabled',
                            letterSpacing: '0.08em', textTransform: 'uppercase', mb: 0.75,
                        }}>
                            Requisitos
                        </Typography>
                        <Req label="Al menos 8 caracteres" met={password.length >= 8} />
                        <Req label="Una letra mayúscula" met={/[A-Z]/.test(password)} />
                        <Req label="Un número" met={/[0-9]/.test(password)} />
                        <Req label="Un carácter especial (!@#$%...)" met={/[^A-Za-z0-9]/.test(password)} />
                    </Box>
                </Box>
            </GeneralModal>
        </>
    );
}

export function ResetPasswordPage() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const [emailSent, setEmailSent] = useState(false);
    const [sentToEmail, setSentToEmail] = useState('');

    if (token) {
        return <ResetPasswordForm token={token} />;
    }

    if (emailSent) {
        return <EmailSentView email={sentToEmail} />;
    }

    return (
        <ForgotPasswordStep
            onSent={(email) => {
                setSentToEmail(email);
                setEmailSent(true);
            }}
        />
    );
}

export default ResetPasswordPage;
