import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Container,
  Box,
  TextField,
  Button,
  Card,
  CircularProgress,
  Alert,
  Typography,
} from '@mui/material';
import LoginIcon from '@mui/icons-material/Login';
import '../css/LoginPage.css';

export function LoginPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await new Promise(resolve => setTimeout(resolve, 1000));
      navigate('/inventario');
    } catch (err) {
      setError('Error en la autenticación. Intenta de nuevo.', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="login-page">
      <Container maxWidth="sm" className="login-container">
        <Card
          sx={{
            padding: 4,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.2)',
            borderRadius: 2,
            backgroundColor: 'rgba(255, 255, 255, 0.98)',
            borderTop: '5px solid #C41E3A',
          }}
        >
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Box
              sx={{
                width: 60,
                height: 60,
                backgroundColor: '#C41E3A',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto',
                mb: 2,
              }}
            >
              <LoginIcon sx={{ fontSize: 35, color: '#FFFFFF' }} />
            </Box>
            <Typography
              variant="h4"
              component="h1"
              sx={{
                fontWeight: 'bold',
                color: '#C41E3A',
                letterSpacing: '1px',
              }}
            >
              SSSI
            </Typography>
            <Typography
              variant="body2"
              sx={{
                color: '#666666',
                mt: 0.5,
                fontSize: '0.95rem',
                fontWeight: 500,
              }}
            >
              Sistema de Sección de Seguridad Institucional
            </Typography>
          </Box>

          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              margin="normal"
              variant="outlined"
              required
              disabled={loading}
              sx={{
                '& .MuiOutlinedInput-root': {
                  '&:hover fieldset': {
                    borderColor: '#C41E3A',
                  },
                  '&.Mui-focused fieldset': {
                    borderColor: '#C41E3A',
                  },
                },
              }}
            />

            <TextField
              fullWidth
              label="Contraseña"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              margin="normal"
              variant="outlined"
              required
              disabled={loading}
              sx={{
                '& .MuiOutlinedInput-root': {
                  '&:hover fieldset': {
                    borderColor: '#C41E3A',
                  },
                  '&.Mui-focused fieldset': {
                    borderColor: '#C41E3A',
                  },
                },
              }}
            />

            <Button
              fullWidth
              variant="contained"
              size="large"
              type="submit"
              disabled={loading}
              sx={{
                backgroundColor: '#C41E3A',
                mt: 3,
                mb: 2,
                py: 1.5,
                fontSize: '1rem',
                fontWeight: 'bold',
                textTransform: 'none',
                '&:hover': {
                  backgroundColor: '#A01A2E',
                  boxShadow: '0 6px 20px rgba(196, 30, 58, 0.3)',
                },
                '&:disabled': {
                  opacity: 0.7,
                },
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

          <Typography variant="body2" sx={{ textAlign: 'center', color: '#666666' }}>
            ¿No tienes cuenta?{' '}
            <Link to="/registro" style={{ color: '#C41E3A', fontWeight: 600, textDecoration: 'none' }}>
              Regístrate
            </Link>
          </Typography>
        </Card>
      </Container>
    </Box>
  );
}

export default LoginPage;
