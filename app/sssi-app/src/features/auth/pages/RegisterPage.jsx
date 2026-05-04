import { useEffect, useRef, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
    Container,
    Box,
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
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
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
    const [showPassword, setShowPassword] = useState(false);
    const [turnstileReady, setTurnstileReady] = useState(false);
    const [turnstileError, setTurnstileError] = useState('');
    const turnstileContainerRef = useRef(null);
    const turnstileWidgetIdRef = useRef(null);
    const { loading, alert, handleAlertClose, handleRegister } = useAuth();
    
    const { formData, errors, touched, handleChange, handleBlur, validateForm, setFormValue } = useFormValidation(
        { username: '', email: '', registerPassword: '', firstName: '', lastName: '', captchaToken: '' },
        ['username', 'email', 'registerPassword', 'firstName', 'lastName']
    );

    useEffect(() => {
        const renderTurnstile = () => {
            if (!window.turnstile || !turnstileContainerRef.current || turnstileWidgetIdRef.current !== null) {
                return;
            }

            turnstileWidgetIdRef.current = window.turnstile.render(turnstileContainerRef.current, {
                sitekey: TURNSTILE_SITE_KEY,
                callback: (token) => {
                    setFormValue('captchaToken', token);
                },
                'expired-callback': () => {
                    setFormValue('captchaToken', '');
                },
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

    const handleSubmit = (e) => {
        e.preventDefault();
        if (validateForm()) {
            handleRegister(formData);
        }
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
                            <ValidatedTextField
                                fieldName="username"
                                fullWidth
                                label="Nombre de usuario"
                                name="username"
                                type="text"
                                value={formData.username}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                error={touched.username && !!errors.username}
                                helperText={touched.username && errors.username}
                                sx={fieldSx}
                            />
                            <ValidatedTextField
                                fieldName="firstName"
                                fullWidth
                                label="Nombre"
                                name="firstName"
                                type="text"
                                value={formData.firstName}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                margin="dense"
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
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                error={touched.lastName && !!errors.lastName}
                                helperText={touched.lastName && errors.lastName}
                                sx={fieldSx}
                            />
                            <ValidatedTextField
                                fieldName="email"
                                fullWidth
                                label="Email"
                                name="email"
                                type="email"
                                value={formData.email}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                error={touched.email && !!errors.email}
                                helperText={touched.email && errors.email}
                                sx={fieldSx}
                            />
                            <ValidatedTextField
                                fieldName="registerPassword"
                                fullWidth
                                label="Contraseña"
                                name="registerPassword"
                                type={showPassword ? 'text' : 'password'}
                                value={formData.registerPassword}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                error={touched.registerPassword && !!errors.registerPassword}
                                helperText={touched.registerPassword && errors.registerPassword}
                                InputProps={{
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