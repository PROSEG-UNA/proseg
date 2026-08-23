import {useState, useEffect} from 'react';
import {useSearchParams, useNavigate} from 'react-router-dom';
import {
    Box, Typography, TextField, Button, CircularProgress,
    Alert, InputAdornment, IconButton, LinearProgress,
    Divider, alpha, useTheme,
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import MailIcon from '@mui/icons-material/Mail';
import PersonIcon from '@mui/icons-material/Person';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import {getInvitationInfo, setPassword as setPasswordApi} from '../../security/services/usersService.js';
import {Helmet} from "react-helmet-async";
import { APP_CONFIG } from '../../../config/appConfig.js';

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

function parseExpiry(value) {
    if (!value) return null;
    if (typeof value === 'string') return new Date(value);
    const asMs = value < 1e12 ? value * 1000 : value;
    return new Date(asMs);
}

function formatExpiry(value) {
    const d = parseExpiry(value);
    if (!d || isNaN(d)) return '';
    return d.toLocaleString('es-CR', {
        day: '2-digit', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

function getTimeRemaining(value) {
    const d = parseExpiry(value);
    if (!d || isNaN(d)) return null;
    const diff = d - Date.now();
    if (diff <= 0) return null;
    const h = Math.floor(diff / 3_600_000);
    const m = Math.floor((diff % 3_600_000) / 60_000);
    if (h > 0) return `${h}h ${m}m restantes`;
    return `${m} minutos restantes`;
}

function getPasswordStrength(password) {
    if (!password) return {score: 0, label: '', color: 'transparent'};
    let score = 0;
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    if (score <= 1) return {score: 20, label: 'Muy débil', color: RED[500]};
    if (score === 2) return {score: 40, label: 'Débil', color: '#f59e0b'};
    if (score === 3) return {score: 60, label: 'Regular', color: '#d97706'};
    if (score === 4) return {score: 80, label: 'Fuerte', color: '#059669'};
    return {score: 100, label: 'Muy fuerte', color: '#047857'};
}

function PageShell({children}) {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';

    const strokeColor = isDark
        ? 'rgba(185,28,28,0.16)'
        : 'rgba(220,38,38,0.10)';

    const dotColor = isDark
        ? 'rgba(248,113,113,0.10)'
        : 'rgba(185,28,28,0.08)';

    const svgPattern = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60'%3E%3Cdefs%3E%3Cpattern id='g' width='60' height='60' patternUnits='userSpaceOnUse'%3E%3Cpath d='M 60 0 L 0 0 0 60' fill='none' stroke='${encodeURIComponent(strokeColor)}' stroke-width='1'/%3E%3C/pattern%3E%3Cpattern id='d' width='60' height='60' patternUnits='userSpaceOnUse'%3E%3Ccircle cx='30' cy='30' r='1.5' fill='${encodeURIComponent(dotColor)}'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='60' height='60' fill='url(%23g)'/%3E%3Crect width='60' height='60' fill='url(%23d)'/%3E%3C/svg%3E")`;

    return (
        <Box
            sx={{
                minHeight: '100dvh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: isDark ? '#0b0b0c' : '#faf7f7',
                backgroundImage: svgPattern,
                backgroundRepeat: 'repeat',
                backgroundAttachment: 'fixed',
                backgroundPosition: 'center',

                p: 2,
            }}
        >
            <Box
                sx={{
                    width: '100%',
                    maxWidth: 460,
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
                }}
            >
                {children}
            </Box>
        </Box>
    );
}

function ModalHeader({title, subtitle}) {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const gradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;
    return (
        <Box sx={{background: gradient, px: 3, py: 2.5, display: 'flex', alignItems: 'center', gap: 1.75}}>
            <Box sx={{
                width: 42, height: 42, borderRadius: '10px',
                background: 'rgba(255,255,255,0.15)',
                border: '1px solid rgba(255,255,255,0.22)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
                <AdminPanelSettingsIcon sx={{color: '#fff', fontSize: 22}}/>
            </Box>
            <Box>
                <Typography
                    sx={{color: '#fff', fontWeight: 800, fontSize: 16, lineHeight: 1.2, letterSpacing: '-0.01em'}}>
                    {title}
                </Typography>
                <Typography sx={{color: 'rgba(255,255,255,0.65)', fontSize: 12, mt: 0.25}}>
                    {subtitle}
                </Typography>
            </Box>
        </Box>
    );
}

function Req({label, met}) {
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
            }}/>
            {label}
        </Typography>
    );
}

function InvalidTokenView() {
    const navigate = useNavigate();
    return (
        <PageShell>
            <ModalHeader title="Enlace inválido" subtitle="Este link ya no está disponible"/>
            <Box sx={{p: 3.5, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2}}>
                <Box sx={{
                    width: 64, height: 64, borderRadius: '50%',
                    bgcolor: alpha(RED[600], 0.1), border: `1.5px solid ${alpha(RED[600], 0.2)}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                    <ErrorIcon sx={{fontSize: 32, color: RED[600]}}/>
                </Box>
                <Typography variant="h6" sx={{fontWeight: 700, color: 'text.primary', textAlign: 'center'}}>
                    Link expirado o utilizado
                </Typography>
                <Typography variant="body2" sx={{color: 'text.secondary', textAlign: 'center', lineHeight: 1.6}}>
                    Este enlace de activación ya no es válido. Puede que haya vencido o ya fue utilizado.
                    Contactá a tu administrador para recibir una nueva invitación.
                </Typography>
                <Button fullWidth onClick={() => navigate('/login')} sx={{
                    border: '1.5px solid', borderColor: RED[700], color: RED[700],
                    fontWeight: 700, borderRadius: '8px', py: 1.1, textTransform: 'none', fontSize: 13.5,
                    '&:hover': {bgcolor: alpha(RED[700], 0.05), borderColor: RED[800]},
                }}>
                    Ir al inicio de sesión
                </Button>
            </Box>
        </PageShell>
    );
}

function SuccessView({info}) {
    const navigate = useNavigate();
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const gradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;
    return (
        <PageShell>
            <ModalHeader title="¡Cuenta activada!" subtitle={`Ya podés iniciar sesión en ${APP_CONFIG.name}`}/>
            <Box sx={{p: 3.5, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2}}>
                <Box sx={{
                    width: 64, height: 64, borderRadius: '50%',
                    bgcolor: alpha('#059669', 0.1), border: `1.5px solid ${alpha('#059669', 0.2)}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                    <CheckCircleIcon sx={{fontSize: 32, color: '#059669'}}/>
                </Box>
                <Typography variant="h6" sx={{fontWeight: 700, color: 'text.primary', textAlign: 'center'}}>
                    Contraseña configurada correctamente
                </Typography>
                {info && (
                    <Typography variant="body2" sx={{color: 'text.secondary', textAlign: 'center'}}>
                        Bienvenido/a, <strong>{info.firstName} {info.lastName}</strong>. Tu cuenta está lista para usar.
                    </Typography>
                )}
            </Box>
            <Box sx={{
                px: 3, py: 2,
                borderTop: '1px solid', borderColor: 'divider',
                bgcolor: isDark ? alpha('#000', 0.25) : alpha(RED[50], 0.7),
            }}>
                <Button fullWidth onClick={() => navigate('/login')} variant="contained" sx={{
                    background: gradient, textTransform: 'none', fontWeight: 700,
                    fontSize: 13.5, borderRadius: '8px',
                    boxShadow: `0 4px 14px ${alpha(RED[600], isDark ? 0.4 : 0.3)}`,
                    '&:hover': {
                        background: isDark
                            ? `linear-gradient(135deg, ${RED[800]}, ${RED[700]})`
                            : `linear-gradient(135deg, ${RED[700]}, ${RED[900]})`,
                    },
                }}>
                    Iniciar sesión
                </Button>
            </Box>
        </PageShell>
    );
}

export function SetPasswordPage() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const accentColor = isDark ? RED[400] : RED[700];
    const gradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;

    const [info, setInfo] = useState(null);
    const [tokenValid, setTokenValid] = useState(null);
    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [showPwd, setShowPwd] = useState(false);
    const [showCfm, setShowCfm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    const strength = getPasswordStrength(password);
    const passwordsMatch = confirm && password === confirm;
    const mismatch = confirm && password !== confirm;
    const timeRemaining = info ? getTimeRemaining(info.expiresAt) : null;

    useEffect(() => {
        if (!token) {
            setTokenValid(false);
            return;
        }

        getInvitationInfo(token)
            .then(data => {
                setInfo(data);
                setTokenValid(true);
            })
            .catch(() => setTokenValid(false));
    }, [token]);

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
            await setPasswordApi(token, password, confirm);
            setSuccess(true);
        } catch (err) {
            const status = err?.response?.status;
            if (status === 400 || status === 404) {
                setTokenValid(false);
                return;
            }
            const msg = err?.response?.data?.message;
            setError(msg ?? 'Ocurrió un error. Intentá de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': {borderColor: 'divider'},
            '&:hover fieldset': {borderColor: alpha(accentColor, 0.5)},
            '&.Mui-focused fieldset': {borderColor: accentColor},
        },
        '& .MuiInputLabel-root.Mui-focused': {color: accentColor},
    };

    if (tokenValid === null) {
        return (
            <Box sx={{
                minHeight: '100dvh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: 'background.default'
            }}>
                <CircularProgress size={32} sx={{color: RED[700]}}/>
            </Box>
        );
    }
    if (!tokenValid) return <InvalidTokenView/>;
    if (success) return <SuccessView info={info}/>;

    return (
        <PageShell>
            <Helmet>
                <title>{`Configurar contraseña - ${APP_CONFIG.name}`}</title>
            </Helmet>
            <ModalHeader title="Configurá tu contraseña" subtitle={`Activá tu cuenta en ${APP_CONFIG.name}`}/>

            {loading && (
                <LinearProgress sx={{
                    height: 2, bgcolor: alpha(RED[600], 0.15),
                    '& .MuiLinearProgress-bar': {bgcolor: RED[600]},
                }}/>
            )}

            {info && (
                <Box sx={{px: 3, pt: 2.5}}>
                    <Typography sx={{
                        fontSize: 10.5,
                        fontWeight: 800,
                        color: 'text.disabled',
                        letterSpacing: '0.08em',
                        textTransform: 'uppercase',
                        mb: 1.25
                    }}>
                        Tu invitación
                    </Typography>
                    <Box sx={{
                        border: '1.5px solid',
                        borderColor: isDark ? alpha(RED[700], 0.35) : RED[100],
                        borderRadius: '12px',
                        bgcolor: isDark ? alpha(RED[900], 0.2) : RED[50],
                        p: 1.75,
                        display: 'flex', flexDirection: 'column', gap: 0.875,
                    }}>
                        <Box sx={{display: 'flex', alignItems: 'center', gap: 1.25}}>
                            <Box sx={{
                                width: 34, height: 34, borderRadius: '50%',
                                bgcolor: isDark ? alpha(RED[700], 0.3) : RED[100],
                                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                            }}>
                                <PersonIcon sx={{fontSize: 16, color: isDark ? RED[300] : RED[700]}}/>
                            </Box>
                            <Box>
                                <Typography sx={{
                                    fontSize: 10.5,
                                    color: 'text.disabled',
                                    fontWeight: 700,
                                    letterSpacing: '0.06em',
                                    textTransform: 'uppercase',
                                    lineHeight: 1
                                }}>
                                    Invitado
                                </Typography>
                                <Typography sx={{
                                    fontSize: 14,
                                    fontWeight: 700,
                                    color: 'text.primary',
                                    lineHeight: 1.3,
                                    mt: 0.2
                                }}>
                                    {info.firstName} {info.lastName}
                                </Typography>
                            </Box>
                        </Box>

                        <Divider sx={{borderColor: isDark ? alpha(RED[700], 0.25) : RED[100], my: 0.25}}/>

                        <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                            <MailIcon sx={{fontSize: 14, color: isDark ? RED[400] : RED[600], flexShrink: 0}}/>
                            <Typography sx={{fontSize: 12.5, color: 'text.secondary'}}>{info.email}</Typography>
                        </Box>

                        <Box sx={{display: 'flex', alignItems: 'flex-start', gap: 1}}>
                            <AccessTimeIcon
                                sx={{fontSize: 14, color: isDark ? RED[400] : RED[600], flexShrink: 0, mt: '2px'}}/>
                            <Box>
                                <Typography sx={{fontSize: 12.5, color: 'text.secondary'}}>
                                    Vence el {formatExpiry(info.expiresAt)}
                                </Typography>
                                {timeRemaining && (
                                    <Typography sx={{
                                        fontSize: 11,
                                        color: isDark ? RED[400] : RED[700],
                                        fontWeight: 700,
                                        mt: 0.15
                                    }}>
                                        {timeRemaining}
                                    </Typography>
                                )}
                            </Box>
                        </Box>
                    </Box>
                </Box>
            )}

            <Box sx={{px: 3, pt: 2.5, pb: 1}}>
                <Typography sx={{
                    fontSize: 10.5,
                    fontWeight: 800,
                    color: 'text.disabled',
                    letterSpacing: '0.08em',
                    textTransform: 'uppercase',
                    mb: 1.5
                }}>
                    Nueva contraseña
                </Typography>

                {error && (
                    <Alert severity="error" sx={{mb: 2, borderRadius: '10px', fontSize: 13}}>
                        {error}
                    </Alert>
                )}

                <TextField
                    label="Contraseña"
                    type={showPwd ? 'text' : 'password'}
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    fullWidth size="small" sx={{...fieldSx, mb: 1.5}}
                    InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton onClick={() => setShowPwd(p => !p)} edge="end" size="small">
                                    {showPwd ? <VisibilityOff fontSize="small"/> : <Visibility fontSize="small"/>}
                                </IconButton>
                            </InputAdornment>
                        ),
                    }}
                />

                {password && (
                    <Box sx={{mb: 1.5}}>
                        <Box sx={{display: 'flex', justifyContent: 'space-between', mb: 0.5}}>
                            <Typography sx={{
                                fontSize: 10.5,
                                fontWeight: 800,
                                color: 'text.disabled',
                                letterSpacing: '0.06em',
                                textTransform: 'uppercase'
                            }}>
                                Fortaleza
                            </Typography>
                            <Typography sx={{fontSize: 11, fontWeight: 700, color: strength.color}}>
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
                    fullWidth size="small" sx={{...fieldSx, mb: 2}}
                    error={mismatch}
                    helperText={
                        mismatch ? 'Las contraseñas no coinciden' :
                            passwordsMatch ? '✓ Las contraseñas coinciden' : ''
                    }
                    FormHelperTextProps={{sx: {color: passwordsMatch ? '#059669' : undefined, fontWeight: 600}}}
                    InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton onClick={() => setShowCfm(p => !p)} edge="end" size="small">
                                    {showCfm ? <VisibilityOff fontSize="small"/> : <Visibility fontSize="small"/>}
                                </IconButton>
                            </InputAdornment>
                        ),
                    }}
                />

                <Box sx={{
                    p: 1.75, mb: 2.5,
                    borderRadius: '10px', border: '1.5px solid', borderColor: 'divider',
                    bgcolor: isDark ? alpha('#fff', 0.02) : alpha('#000', 0.02),
                }}>
                    <Typography sx={{
                        fontSize: 10.5,
                        fontWeight: 800,
                        color: 'text.disabled',
                        letterSpacing: '0.08em',
                        textTransform: 'uppercase',
                        mb: 0.75
                    }}>
                        Requisitos
                    </Typography>
                    <Req label="Al menos 8 caracteres" met={password.length >= 8}/>
                    <Req label="Una letra mayúscula" met={/[A-Z]/.test(password)}/>
                    <Req label="Un número" met={/[0-9]/.test(password)}/>
                    <Req label="Un carácter especial (!@#$%...)" met={/[^A-Za-z0-9]/.test(password)}/>
                </Box>
            </Box>

            <Box sx={{
                px: 3, py: 1.75,
                borderTop: '1px solid', borderColor: 'divider',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                bgcolor: isDark ? alpha('#000', 0.25) : alpha(RED[50], 0.7),
            }}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 0.75}}>
                    <LockOutlinedIcon sx={{fontSize: 13, color: 'text.disabled'}}/>
                    <Typography sx={{fontSize: 11.5, color: 'text.disabled', fontWeight: 500}}>
                        {password.length === 0 ? 'Ingresá tu contraseña' : strength.label}
                    </Typography>
                </Box>

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
                        '&:disabled': {opacity: 0.5, color: '#fff'},
                    }}
                >
                    {loading
                        ? <CircularProgress size={16} sx={{color: '#fff'}}/>
                        : 'Activar mi cuenta'}
                </Button>
            </Box>
        </PageShell>
    );
}

export default SetPasswordPage;