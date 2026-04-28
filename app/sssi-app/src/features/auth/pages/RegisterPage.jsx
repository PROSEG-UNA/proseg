import { useEffect, useRef, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
    Container,
    Box,
    TextField,
    Button,
    Card,
    CircularProgress,
    Typography,
    Link,
    IconButton,
    InputAdornment,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { alpha } from '@mui/material/styles';
import { useAuth } from '../hooks/useAuth';
import AlertModal from '../../../common/components/AlertModal.jsx';
import '../css/RegisterPage.css';

const TURNSTILE_SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY || '0x4AAAAAADEo_zmnakZDiJdz';
const TURNSTILE_SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

const fieldSx = (theme) => ({
    '& .MuiOutlinedInput-root': {
        '& .MuiInputAdornment-root': {
            marginLeft: 0,
            backgroundColor: 'transparent',
        },
        '& .MuiIconButton-root': {
            backgroundColor: 'transparent !important',
        },
        '& .MuiOutlinedInput-input': {
            backgroundColor: 'transparent',
        },
        '& .MuiOutlinedInput-input:-webkit-autofill': {
            WebkitBoxShadow: '0 0 0 1000px transparent inset',
            WebkitTextFillColor: 'inherit',
            transition: 'background-color 9999s ease-out 0s',
            caretColor: 'inherit',
        },
        '& fieldset': { borderColor: theme.palette.grey[400] },
        '&:hover fieldset': { borderColor: theme.palette.primary.main },
    },
});

export function RegisterPage() {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        captchaToken: '',
    });
    const [showPassword, setShowPassword] = useState(false);
    const [turnstileReady, setTurnstileReady] = useState(false);
    const [turnstileError, setTurnstileError] = useState('');
    const turnstileContainerRef = useRef(null);
    const turnstileWidgetIdRef = useRef(null);
    const { loading, alert, handleAlertClose, handleRegister } = useAuth();

    useEffect(() => {
        const renderTurnstile = () => {
            if (!window.turnstile || !turnstileContainerRef.current || turnstileWidgetIdRef.current !== null) {
                return;
            }

            turnstileWidgetIdRef.current = window.turnstile.render(turnstileContainerRef.current, {
                sitekey: TURNSTILE_SITE_KEY,
                callback: (token) => {
                    setFormData((prev) => ({ ...prev, captchaToken: token }));
                },
                'expired-callback': () => {
                    setFormData((prev) => ({ ...prev, captchaToken: '' }));
                },
                'error-callback': () => {
                    setFormData((prev) => ({ ...prev, captchaToken: '' }));
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

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        handleRegister(formData);
    };

    const handleTogglePasswordVisibility = () => {
        setShowPassword((prev) => !prev);
    };

    return (
        <Box
                className="register-page"
                sx={(theme) => ({
                    '&::before': { backgroundColor: alpha(theme.palette.primary.main, 0.15) },
                })}
            >
                <Container maxWidth="sm" className="register-container">
                    <Card
                        sx={{
                            padding: 3,
                            boxShadow: 1,
                            borderRadius: 2,
                            backgroundColor: 'rgba(255, 255, 255, 0.98)',
                            borderTop: '5px solid',
                            borderTopColor: 'primary.main',
                        }}
                    >
                        <Box sx={{ textAlign: 'center', mb: 2 }}>
                            <Box
                                sx={{
                                    width: 50,
                                    height: 50,
                                    bgcolor: 'primary.main',
                                    borderRadius: '50%',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    margin: '0 auto',
                                    mb: 1,
                                }}
                            >
                                <PersonAddIcon sx={{ fontSize: 35, color: 'primary.contrastText' }} />
                            </Box>
                            <Typography
                                variant="h4"
                                component="h1"
                                sx={{ fontWeight: 'bold', color: 'primary.main', letterSpacing: '1px' }}
                            >
                                SPSG
                            </Typography>
                            <Typography
                                variant="body2"
                                sx={{ color: 'text.secondary', mt: 0.5, fontSize: '0.95rem', fontWeight: 500 }}
                            >
                                Sistema Programa Servicios Generales
                            </Typography>
                        </Box>

                        <form onSubmit={handleSubmit}>
                            <TextField
                                fullWidth
                                label="Nombre de usuario"
                                name="username"
                                type="text"
                                value={formData.username}
                                onChange={handleChange}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                sx={fieldSx}
                            />
                            <TextField
                                fullWidth
                                label="Nombre"
                                name="firstName"
                                type="text"
                                value={formData.firstName}
                                onChange={handleChange}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                sx={fieldSx}
                            />
                            <TextField
                                fullWidth
                                label="Apellido"
                                name="lastName"
                                type="text"
                                value={formData.lastName}
                                onChange={handleChange}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                sx={fieldSx}
                            />
                            <TextField
                                fullWidth
                                label="Email"
                                name="email"
                                type="email"
                                value={formData.email}
                                onChange={handleChange}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                sx={fieldSx}
                            />
                            <TextField
                                fullWidth
                                label="Contraseña"
                                name="password"
                                type={showPassword ? 'text' : 'password'}
                                value={formData.password}
                                onChange={handleChange}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                slotProps={{
                                    input: {
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <IconButton
                                                    onClick={handleTogglePasswordVisibility}
                                                    edge="end"
                                                    aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                                    disableRipple
                                                    sx={{
                                                        p: 0.5,
                                                        color: 'text.secondary',
                                                        '&:hover': {
                                                            backgroundColor: 'transparent',
                                                            color: 'text.primary',
                                                        },
                                                    }}
                                                >
                                                    {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    },
                                }}
                                sx={fieldSx}
                            />
                            <Box sx={{ mt: 2, mb: 1, display: 'flex', justifyContent: 'center' }}>
                                <Box ref={turnstileContainerRef} />
                            </Box>
                            {turnstileError ? (
                                <Typography variant="body2" color="error" sx={{ mb: 1, textAlign: 'center' }}>
                                    {turnstileError}
                                </Typography>
                            ) : null}
                            <Button
                                fullWidth
                                variant="contained"
                                color="primary"
                                size="large"
                                type="submit"
                                disabled={loading || !turnstileReady || !formData.captchaToken}
                                sx={{
                                    mt: 3,
                                    mb: 2,
                                    py: 1.5,
                                    fontSize: '1rem',
                                    fontWeight: 'bold',
                                    textTransform: 'none',
                                }}
                            >
                                {loading ? (
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <CircularProgress size={20} color="inherit" />
                                        Registrando...
                                    </Box>
                                ) : (
                                    'Registrarse'
                                )}
                            </Button>
                        </form>

                        <Typography variant="body2" sx={{ textAlign: 'center', color: 'text.secondary' }}>
                            ¿Ya tienes cuenta?{' '}
                            <Link
                                component={RouterLink}
                                to="/login"
                                sx={{ color: 'primary.main', fontWeight: 600 }}
                                underline="none"
                            >
                                Inicia sesión
                            </Link>
                        </Typography>
                    </Card>
                </Container>

                <AlertModal
                    open={!!alert}
                    type={alert?.type}
                    message={alert?.message}
                    onClose={handleAlertClose}
                />
        </Box>
    );
}

export default RegisterPage;