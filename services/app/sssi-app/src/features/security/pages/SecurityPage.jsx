import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  Button,
  Card,
  CardContent,
  CardHeader,
  Switch,
  FormControlLabel,
  Grid,
  IconButton,
  Container,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import SecurityIcon from '@mui/icons-material/Security';
import LockIcon from '@mui/icons-material/Lock';
import ScheduleIcon from '@mui/icons-material/Schedule';
import '../css/SecurityPage.css';

export function SecurityPage() {
  const navigate = useNavigate();
  const [settings, setSettings] = useState({
    twoFactor: false,
    apiKey: true,
    sessionTimeout: true,
  });

  const handleToggle = (key) => {
    setSettings(prev => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="security-page">
      <AppBar position="static" sx={{ backgroundColor: '#C41E3A' }}>
        <Toolbar>
          <Typography 
            variant="h5" 
            sx={{ 
              flexGrow: 1, 
              fontWeight: 700,
              fontSize: '1.5rem',
              letterSpacing: '0.5px',
              textShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
            }}
          >
            Configuración de Seguridad
          </Typography>
          <Button 
            color="inherit" 
            onClick={() => navigate('/inventory')}
            sx={{ mr: 2, textTransform: 'none', fontSize: '1rem' }}
          >
            Inventario
          </Button>
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Container maxWidth="md" className="security-content">
        <Grid container spacing={3} className="security-grid">
          <Grid item xs={12} sm={6} md={4}>
            <Card>
              <CardHeader
                avatar={<SecurityIcon sx={{ color: '#C41E3A' }} />}
                title="Autenticación"
              />
              <CardContent>
                <FormControlLabel
                  control={
                    <Switch
                      checked={settings.twoFactor}
                      onChange={() => handleToggle('twoFactor')}
                      sx={{
                        '& .MuiSwitch-switchBase.Mui-checked': {
                          color: '#C41E3A',
                        },
                        '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                          backgroundColor: '#C41E3A',
                        },
                      }}
                    />
                  }
                  label="Factor Doble"
                />
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={4}>
            <Card>
              <CardHeader
                avatar={<LockIcon sx={{ color: '#C41E3A' }} />}
                title="Claves API"
              />
              <CardContent>
                <FormControlLabel
                  control={
                    <Switch
                      checked={settings.apiKey}
                      onChange={() => handleToggle('apiKey')}
                      sx={{
                        '& .MuiSwitch-switchBase.Mui-checked': {
                          color: '#C41E3A',
                        },
                        '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                          backgroundColor: '#C41E3A',
                        },
                      }}
                    />
                  }
                  label="Activado"
                />
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={4}>
            <Card>
              <CardHeader
                avatar={<ScheduleIcon sx={{ color: '#C41E3A' }} />}
                title="Sesiones"
              />
              <CardContent>
                <FormControlLabel
                  control={
                    <Switch
                      checked={settings.sessionTimeout}
                      onChange={() => handleToggle('sessionTimeout')}
                      sx={{
                        '& .MuiSwitch-switchBase.Mui-checked': {
                          color: '#C41E3A',
                        },
                        '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                          backgroundColor: '#C41E3A',
                        },
                      }}
                    />
                  }
                  label="Timeout activo"
                />
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Card className="security-activity-log">
              <CardHeader title="Últimas acciones" />
              <CardContent>
                <Typography variant="body2" color="textSecondary" className="security-activity-item">
                  Acceso desde 192.168.1.100 - Hoy 10:30 AM
                </Typography>
                <Typography variant="body2" color="textSecondary" className="security-activity-item">
                  Cambio de contraseña - Ayer 2:15 PM
                </Typography>
                <Typography variant="body2" color="textSecondary" className="security-activity-item">
                  Nuevo dispositivo autorizado - Hace 3 días
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Box className="security-footer">
          <Button 
            variant="outlined" 
            color="error"
            sx={{ 
              color: '#d32f2f',
              borderColor: '#d32f2f',
              '&:hover': {
                borderColor: '#d32f2f',
                backgroundColor: 'rgba(211, 47, 47, 0.1)',
              },
            }}
          >
            Cerrar todas las sesiones
          </Button>
        </Box>
      </Container>
    </Box>
  );
}

export default SecurityPage;
