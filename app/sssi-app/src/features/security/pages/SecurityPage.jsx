import { useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  Button,
  Container,
  Card,
  CardContent,
  CardActions,
  Grid,
  IconButton,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import '../css/SecurityPage.css';

export function SecurityPage() {
  const navigate = useNavigate();

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
            Gestión de Seguridad
          </Typography>
          <Button 
            color="inherit" 
            onClick={() => navigate('/inventory/activos')}
            sx={{ mr: 2, textTransform: 'none', fontSize: '1rem' }}
          >
            Inventario
          </Button>
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="security-content">
        <Box sx={{ py: 4 }}>
          <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 3 }}>
            Bienvenido a Gestión de Seguridad
          </Typography>
          <Typography variant="body1" sx={{ color: '#666', mb: 4 }}>
            Administra usuarios, roles y permisos de tu organización.
          </Typography>

          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={5}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flex: 1, textAlign: 'center' }}>
                  <PeopleIcon sx={{ fontSize: 60, color: '#C41E3A', mb: 2 }} />
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Gestión de Usuarios
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#666' }}>
                    Administra los usuarios de la organización
                  </Typography>
                </CardContent>
                <CardActions sx={{ justifyContent: 'center' }}>
                  <Button 
                    variant="contained" 
                    sx={{ backgroundColor: '#C41E3A' }}
                    onClick={() => navigate('/security/usuarios')}
                  >
                    Ir a Usuarios
                  </Button>
                </CardActions>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={5}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flex: 1, textAlign: 'center' }}>
                  <VerifiedUserIcon sx={{ fontSize: 60, color: '#C41E3A', mb: 2 }} />
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Gestión de Roles
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#666' }}>
                    Administra los roles y permisos del sistema
                  </Typography>
                </CardContent>
                <CardActions sx={{ justifyContent: 'center' }}>
                  <Button 
                    variant="contained" 
                    sx={{ backgroundColor: '#C41E3A' }}
                    onClick={() => navigate('/security/roles')}
                  >
                    Ir a Roles
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          </Grid>
        </Box>
      </Container>
    </Box>
  );
}

export default SecurityPage;
