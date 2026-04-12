import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
    Container,
    Box,
    TextField,
    Button,
    Card,
    CircularProgress,
    Alert,
    Typography,
    Link,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import { alpha } from '@mui/material/styles';
import AppTheme from '../../../common/theme/AppTheme';
import '../css/RegisterPage.css';

const fieldSx = (theme) => ({
    '& .MuiOutlinedInput-root': {
        '& fieldset': { borderColor: theme.palette.grey[400] },
        '&:hover fieldset': { borderColor: theme.palette.primary.main },
    },
});

export function RegisterPage() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({ name: '', email: '', password: '' });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await new Promise(resolve => setTimeout(resolve, 1000));
            navigate('/login');
        } catch (err) {
            setError('Error al registrar el usuario. Intenta de nuevo. ', err);
            setLoading(false);
        }
    };

    return (
        <AppTheme>
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
                                SSSI
                            </Typography>
                            <Typography
                                variant="body2"
                                sx={{ color: 'text.secondary', mt: 0.5, fontSize: '0.95rem', fontWeight: 500 }}
                            >
                                Crear nueva cuenta
                            </Typography>
                        </Box>

                        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                        <form onSubmit={handleSubmit}>
                            <TextField
                                fullWidth
                                label="Nombre completo"
                                name="name"
                                type="text"
                                value={formData.name}
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
                                type="password"
                                value={formData.password}
                                onChange={handleChange}
                                margin="dense"
                                variant="outlined"
                                required
                                disabled={loading}
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
            </Box>
        </AppTheme>
    );
}

export default RegisterPage;