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
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import './RegisterPage.css';

export function RegisterPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
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
      navigate('/login');
    } catch (err) {
      setError('Error al registrar el usuario. Intenta de nuevo. ', err)
      setLoading(false);
    }
  };

  const fieldSx = {
    '& .MuiOutlinedInput-root': {
      '&:hover fieldset': { borderColor: '#C41E3A' },
      '&.Mui-focused fieldset': { borderColor: '#C41E3A' },
    },
  };

  return (
    <Box className="register-page">
      <Container maxWidth="sm" className="register-container">
        <Card
          sx={{
            padding: 3,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.2)',
            borderRadius: 2,
            backgroundColor: 'rgba(255, 255, 255, 0.98)',
            borderTop: '5px solid #C41E3A',
          }}
        >
          <Box sx={{ textAlign: 'center', mb: 2 }}>
            <Box
              sx={{
                width: 50,
                height: 50,
                backgroundColor: '#C41E3A',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto',
                mb: 1,
              }}
            >
              <PersonAddIcon sx={{ fontSize: 35, color: '#FFFFFF' }} />
            </Box>
            <Typography
              variant="h4"
              component="h1"
              sx={{ fontWeight: 'bold', color: '#C41E3A', letterSpacing: '1px' }}
            >
              SSSI
            </Typography>
            <Typography
              variant="body2"
              sx={{ color: '#666666', mt: 0.5, fontSize: '0.95rem', fontWeight: 500 }}
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
                '&:disabled': { opacity: 0.7 },
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

          <Typography variant="body2" sx={{ textAlign: 'center', color: '#666666' }}>
            ¿Ya tienes cuenta?{' '}
            <Link to="/login" style={{ color: '#C41E3A', fontWeight: 600, textDecoration: 'none' }}>
              Inicia sesión
            </Link>
          </Typography>
        </Card>
      </Container>
    </Box>
  );
}

export default RegisterPage;