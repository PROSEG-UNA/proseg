import { useState } from 'react';
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
import LoginIcon from '@mui/icons-material/Login';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { alpha } from '@mui/material/styles';
import { useAuth } from '../hooks/useAuth';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { useFormValidation } from '../../../common/hooks/useFormValidation';
import '../css/LoginPage.css';

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

export function LoginPage() {
    const [showPassword, setShowPassword] = useState(false);
    const { loading, alert, handleAlertClose, handleLogin } = useAuth();
    const { formData, errors, touched, handleChange, handleBlur, validateForm } = useFormValidation(
        { identifier: '', loginPassword: '' },
        ['identifier', 'loginPassword']
    );

    const handleSubmit = (e) => {
        e.preventDefault();
        if (validateForm()) {
            handleLogin(formData.identifier, formData.loginPassword);
        }
    };

    const handleTogglePasswordVisibility = () => {
        setShowPassword((prev) => !prev);
    };

    return (
        <Box
                className="login-page"
                sx={(theme) => ({
                    '&::before': { backgroundColor: alpha(theme.palette.primary.main, 0.15) },
                })}
            >
                <Container maxWidth="sm" className="login-container">
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
                                <LoginIcon sx={{ fontSize: 35, color: 'primary.contrastText' }} />
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
                                fieldName="identifier"
                                fullWidth
                                label="Usuario o Email"
                                name="identifier"
                                type="text"
                                value={formData.identifier}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                error={touched.identifier && !!errors.identifier}
                                helperText={touched.identifier && errors.identifier}
                                sx={fieldSx}
                            />
                            <ValidatedTextField
                                fieldName="loginPassword"
                                fullWidth
                                label="Contraseña"
                                name="loginPassword"
                                type={showPassword ? 'text' : 'password'}
                                value={formData.loginPassword}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
                                error={touched.loginPassword && !!errors.loginPassword}
                                helperText={touched.loginPassword && errors.loginPassword}
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
                            <Button
                                fullWidth
                                variant="contained"
                                color="primary"
                                size="large"
                                type="submit"
                                disabled={loading}
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
                                    <Box className="login-spinner">
                                        <CircularProgress size={20} color="inherit" />
                                        Iniciando sesión...
                                    </Box>
                                ) : (
                                    'Iniciar Sesión'
                                )}
                            </Button>
                        </form>

                        <Typography variant="body2" sx={{ textAlign: 'center', color: 'text.secondary' }}>
                            ¿No tienes cuenta?{' '}
                            <Link
                                component={RouterLink}
                                to="/registro"
                                sx={{ color: 'primary.main', fontWeight: 600 }}
                                underline="none"
                            >
                                Regístrate
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

export default LoginPage;